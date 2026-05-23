package server.controller;

import message.Request;
import message.Response;
import model.Auction;
import model.Reason;
import model.SearchCriteria;
import server.network.AuctionServer;
import server.repository.AuctionRepository;
import server.repository.ReasonRepository;
import server.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AuctionController {

    private final AuctionRepository auctionRepo = new AuctionRepository();
    private final UserRepository userRepo = new UserRepository();
    private final ReasonRepository reasonRepo = new ReasonRepository();

    public Response handleCustomSearch(Request request) {
        Response response = new Response();

        if (request.getPayload() instanceof SearchCriteria criteria) {

            // 1. Lấy danh sách các phiên đấu giá
            List<Auction> results = auctionRepo.searchAdvanced(criteria);

            // 2. Thu thập tất cả seller_id từ danh sách kết quả
            List<Integer> sellerIds = results.stream()
                    .filter(auc -> auc.getItem() != null)
                    .map(auc -> auc.getItem().getSeller_id())
                    .distinct()
                    .collect(Collectors.toList());

            // 3. Gọi UserRepository một lần duy nhất để lấy Map<ID, Tên>
            Map<Integer, String> sellerNames = userRepo.getUsernamesByIds(sellerIds);

            // 4. Đóng gói cả 2 vào một mảng Object để gửi về Client
            Object[] dataPackage = new Object[]{results, sellerNames};

            response.setStatus("SUCCESS");
            response.setMessage("Tìm kiếm thành công");
            response.setData(dataPackage);
        } else {
            response.setStatus("FAIL");
            response.setMessage("Dữ liệu tìm kiếm không hợp lệ.");
        }
        return response;
    }

    public Response handleGetList(Request request) {
        Response response = new Response();

        // Lấy danh sách toàn bộ phiên đấu giá từ DB
        List<Auction> list = auctionRepo.getAllAuctions();

        response.setStatus("SUCCESS");
        response.setMessage("Danh sách các phiên đấu giá đã được tải.");
        response.setData(list);

        return response;
    }

    public Response handleGetItemDetail(Request request) {
        Response response = new Response();
        Object itemId = request.getPayload();

        // TODO: Gọi DB để lấy chi tiết sản phẩm theo ID

        response.setStatus("SUCCESS");
        response.setMessage("Thông tin chi tiết vật phẩm ID: " + itemId);
        return response;
    }

    // ==========================================
    // CÁC HÀNH ĐỘNG DÀNH CHO ADMIN QUẢN LÝ PHIÊN ĐẤU GIÁ
    // ==========================================
    
    public Response handleAdminStopAuction(Request request) {
        Response response = new Response();
        
        // Cập nhật để nhận mảng Object chứa auctionId và lý do
        if (request.getPayload() instanceof Object[] payload) {
            if (payload.length == 2 && payload[0] instanceof Integer && payload[1] instanceof String reasonText) {
                int auctionId = (Integer) payload[0];

                boolean success = auctionRepo.stopAuction(auctionId);
                
                if (success) {
                    // Lưu lý do dừng phiên đấu giá vào bảng reasons
                    Reason reason = new Reason();
                    reason.setTargetId(auctionId);
                    reason.setReasonType("AUCTION_STOPPED");
                    reason.setReason(reasonText);
                    reasonRepo.addReason(reason);
                    
                    response.setStatus("SUCCESS");
                    response.setMessage("Đã buộc dừng phiên đấu giá thành công.");
                    
                    // Gửi Broadcast để báo cho tất cả Client (đặc biệt là người đang xem) biết phiên này đã bị hủy/kết thúc
                    Response notifyEnd = new Response();
                    notifyEnd.setStatus("AUCTION_END");
                    notifyEnd.setMessage("Phiên đấu giá " + auctionId + " đã bị hủy bởi Quản trị viên. Lý do: " + reasonText);
                    AuctionServer.broadcast(notifyEnd);
                } else {
                    response.setStatus("FAIL");
                    response.setMessage("Không thể dừng phiên đấu giá, vui lòng thử lại.");
                }
            } else {
                response.setStatus("FAIL");
                response.setMessage("Định dạng dữ liệu không hợp lệ. Yêu cầu [auctionId, reason].");
            }
        } else if (request.getPayload() instanceof Integer) {
             // Tương thích ngược với code cũ nếu Client gửi mỗi Integer
             int auctionId = (Integer) request.getPayload();
             boolean success = auctionRepo.stopAuction(auctionId);
             
             if (success) {
                 response.setStatus("SUCCESS");
                 response.setMessage("Đã buộc dừng phiên đấu giá thành công.");
                 
                 Response notifyEnd = new Response();
                 notifyEnd.setStatus("AUCTION_END");
                 notifyEnd.setMessage("Phiên đấu giá " + auctionId + " đã bị hủy bởi Quản trị viên.");
                 AuctionServer.broadcast(notifyEnd);
             } else {
                 response.setStatus("FAIL");
                 response.setMessage("Không thể dừng phiên đấu giá, vui lòng thử lại.");
             }
        } else {
            response.setStatus("FAIL");
            response.setMessage("Dữ liệu payload không hợp lệ.");
        }

        return response;
    }

    public Response handleGetStopReason(Request request) {
        Response response = new Response();

        try {
            // Kiểm tra và ép kiểu an toàn ID gửi lên từ Client
            if (request.getPayload() instanceof Number) {
                int auctionId = ((Number) request.getPayload()).intValue();

                // Gọi vào ReasonRepository để lấy lý do mới nhất với loại là "AUCTION_STOPPED"
                Reason latestReason = reasonRepo.getLatestReason(auctionId, "AUCTION_STOPPED");

                if (latestReason != null) {
                    response.setStatus("SUCCESS");
                    response.setMessage("Lấy lý do dừng phiên đấu giá thành công.");
                    response.setData(latestReason.getReason()); // Trả về nội dung (String) cho Client
                } else {
                    response.setStatus("FAIL");
                    response.setMessage("Không tìm thấy lý do dừng cho phiên đấu giá này.");
                }
            } else {
                response.setStatus("FAIL");
                response.setMessage("Dữ liệu Payload không hợp lệ. Yêu cầu truyền lên Auction ID kiểu số (Integer).");
            }
        } catch (Exception e) {
            System.err.println("Lỗi hệ thống khi xử lý yêu cầu lấy lý do dừng phiên đấu giá!");
            response.setStatus("ERROR");
            response.setMessage("Đã xảy ra lỗi trên Server khi lấy lý do.");
            e.printStackTrace();
        }

        return response;
    }
}