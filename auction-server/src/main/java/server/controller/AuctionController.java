package server.controller;

import message.Request;
import message.Response;
import model.Auction;
import model.SearchCriteria;
import server.network.AuctionServer;
import server.repository.AuctionRepository;
import server.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AuctionController {

    private final AuctionRepository auctionRepo = new AuctionRepository();
    private final UserRepository userRepo = new UserRepository();

    public Response handleCustomSearch(Request request) {
        Response response = new Response();

        if (request.getPayload() instanceof SearchCriteria) {
            SearchCriteria criteria = (SearchCriteria) request.getPayload();

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

        // TODO: Gọi DB để lấy danh sách (List<Item>)
        // List<Item> list = auctionRepo.getAllItems();

        response.setStatus("SUCCESS");
        response.setMessage("Danh sách các vật phẩm đang đấu giá đã được tải.");
        // response.setData(list); // Nhét danh sách vào kiện hàng để trả về

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

    public Response handleAuctionEnd(Request request) {
        Response response = new Response();
        Object winnerData = request.getPayload();

        // TODO: Lưu lịch sử người chiến thắng vào DB

        response.setStatus("SUCCESS");
        response.setMessage("Phiên đấu giá đã kết thúc! Người thắng cuộc là: " + winnerData);

        // Tương tự Bid, bạn có thể tạo lệnh Broadcast ở đây để báo cho cả Server biết phiên này đã kết thúc
        Response notifyEnd = new Response();
        notifyEnd.setStatus("AUCTION_END");
        notifyEnd.setMessage("Phiên đấu giá kết thúc, người thắng: " + winnerData);
        AuctionServer.broadcast(notifyEnd);

        return response;
    }
}
