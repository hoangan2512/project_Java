package server.service;

import message.Response;
import model.Auction;
import model.Bid;
import server.network.AuctionServer;
import server.repository.AuctionRepository;
import server.repository.BidRepository;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionService {

    // Mỗi phiên đấu giá (Auction) sẽ có một cái "khóa" riêng để tránh tranh chấp giá
    // Điều này đảm bảo trong 1 mili-giây, chỉ có 1 luồng được phép xử lý giá cho 1 sản phẩm
    private final ConcurrentHashMap<Integer, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    // Tiêm Repository vào để tương tác với Database
    private final AuctionRepository auctionRepository = new AuctionRepository();
    private final BidRepository bidRepository = new BidRepository();

    /**
     * Hàm lấy khóa Lock cho một phiên đấu giá cụ thể.
     */
    private ReentrantLock getLock(int auctionId) {
        return auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock());
    }

    /**
     * --- LOGIC ĐẶT GIÁ (CORE BUSINESS LOGIC) ---
     * Đây là nơi chứa bộ não của chức năng đấu giá.
     */
    public Response placeBid(Bid bid) {
        int auctionId = bid.getAuction_id();
        ReentrantLock lock = getLock(auctionId);
        
        lock.lock(); // Bắt đầu chặn tất cả các Request khác truy cập vào cùng phiên đấu giá này

        try {
            // 1. Lấy thông tin mới nhất của phiên đấu giá trực tiếp từ Database
            Auction auction = auctionRepository.getAuctionById(auctionId);
            if (auction == null) {
                return new Response("FAIL", null, "Lỗi: Phiên đấu giá không tồn tại.");
            }

            // 2. Kiểm tra trạng thái (Phải đang RUNNING)
            if (!"RUNNING".equals(auction.getStatus())) {
                return new Response("FAIL", null, "Lỗi: Phiên đấu giá đang ở trạng thái " + auction.getStatus() + ".");
            }

            // 3. Kiểm tra thời gian (Đề phòng TimeManager chưa kịp quét)
            if (LocalDateTime.now().isAfter(auction.getEnd_time())) {
                return new Response("FAIL", null, "Lỗi: Phiên đấu giá đã kết thúc.");
            }

            // 4. Kiểm tra giá trị (Giá đặt phải cao hơn giá hiện tại)
            if (bid.getAmount() <= auction.getCurrent_price()) {
                return new Response("FAIL", null, "Lỗi: Giá đặt phải cao hơn giá hiện tại (" + auction.getCurrent_price() + ").");
            }

            // ==========================================
            // MỌI ĐIỀU KIỆN ĐỀU HỢP LỆ -> TIẾN HÀNH LƯU
            // ==========================================
            
            // 5. Cập nhật bảng `auctions` (giá mới và người dẫn đầu mới)
            boolean isUpdated = auctionRepository.updateBid(auctionId, bid.getAmount(), bid.getBidder_id());
            
            if (isUpdated) {
                // 6. Ghi nhận vào lịch sử bảng `bids`
                bidRepository.placeBid(bid);
                
                System.out.println("Đã ghi nhận mức giá mới: " + bid.getAmount() + " từ User " + bid.getBidder_id() + " cho Auction " + auctionId);

                // 7. Gửi thông báo (Broadcast) cho toàn bộ Client đang online biết giá mới
                Response notifyPrice = new Response("NOTIFY_NEW_PRICE", bid, "Có người vừa đặt giá mới!");
                AuctionServer.broadcast(notifyPrice);

                return new Response("SUCCESS", bid, "Đặt giá thành công! Bạn đang dẫn đầu.");
            } else {
                return new Response("FAIL", null, "Lỗi hệ thống khi lưu giá mới.");
            }

        } finally {
            lock.unlock(); // Luôn giải phóng khóa dù có lỗi xảy ra hay không
        }
    }
}
