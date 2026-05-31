package server.service;

import message.Response;
import model.Auction;
import model.Bid;
import model.User;
import server.network.AuctionServer;
import server.repository.AuctionRepository;
import server.repository.BidRepository;
import server.repository.UserRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionService {

    // Lưu trữ thời điểm đặt giá cuối cùng của mỗi user để kiểm soát rate limit
    private final ConcurrentHashMap<Integer, LocalDateTime> lastBidTimes = new ConcurrentHashMap<>();

    // Tiêm Repository vào để tương tác với Database
    private final AuctionRepository auctionRepository = new AuctionRepository();
    private final BidRepository bidRepository = new BidRepository();
    private final UserRepository userRepository = new UserRepository(); 

    // --- CẤU HÌNH ANTI-SNIPING ---
    // Nếu có bid trong 30 giây cuối -> gia hạn thêm 60 giây
    private static final long ANTI_SNIPING_TRIGGER_SECONDS = 30;
    private static final long ANTI_SNIPING_EXTENSION_SECONDS = 60;
    
    // --- CẤU HÌNH RATE LIMIT (ANTI-SPAM) ---
    // Người dùng chỉ được đặt giá tối đa 1 lần mỗi 2 giây
    private static final long BID_COOLDOWN_MILLIS = 2000;

    /**
     * Xác định bước giá (minimum increment) hợp lý dựa trên giá trị hiện tại của sản phẩm.
     * Tránh trường hợp sản phẩm giá trị lớn mà bước giá chỉ có 1đ hoặc 5đ.
     */
    private double calculateMinimumIncrement(double currentPrice) {
        if (currentPrice < 100000) { // Dưới 100k -> bước giá 10k
            return 10000;
        } else if (currentPrice < 1000000) { // Dưới 1 triệu -> bước giá 50k
            return 50000;
        } else if (currentPrice < 10000000) { // Dưới 10 triệu -> bước giá 200k
            return 200000;
        } else if (currentPrice < 50000000) { // Dưới 50 triệu -> bước giá 500k
            return 500000;
        } else { // Trên 50 triệu -> bước giá 1 triệu
            return 1000000;
        }
    }

    /**
     * Hàm phụ trợ xử lý Anti-Sniping (Gia hạn thời gian kết thúc)
     */
    private void handleAntiSniping(Auction auction) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = auction.getEnd_time();

        // Kiểm tra xem thời gian hiện tại có nằm trong X giây cuối cùng trước khi kết thúc không
        long secondsRemaining = Duration.between(now, endTime).getSeconds();
        
        if (secondsRemaining > 0 && secondsRemaining <= ANTI_SNIPING_TRIGGER_SECONDS) {
            // Gia hạn thêm thời gian
            LocalDateTime newEndTime = now.plusSeconds(ANTI_SNIPING_EXTENSION_SECONDS);
            
            // Cập nhật Database
            boolean updated = auctionRepository.updateEndTime(auction.getId(), newEndTime);
            if (updated) {
                auction.setEnd_time(newEndTime);
                System.out.println("[ANTI-SNIPING] Phiên đấu giá " + auction.getId() + " vừa được gia hạn đến: " + newEndTime);
                
                // Cập nhật AuctionTimeManager để nó không kết thúc phiên theo lịch cũ
                AuctionTimeManager.getInstance().extendAuction(auction.getId(), newEndTime);

                // Phát Broadcast thông báo cho Client biết thời gian đã được gia hạn
                Response notifyExtension = new Response("AUCTION_EXTENDED", auction, "Phiên đấu giá đã được gia hạn!");
                AuctionServer.broadcast(notifyExtension);
            }
        }
    }

    /**
     * Kiểm tra xem người dùng có đang spam đặt giá không.
     */
    private boolean isRateLimited(int bidderId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastBidTime = lastBidTimes.get(bidderId);
        
        if (lastBidTime != null) {
            long millisSinceLastBid = java.time.Duration.between(lastBidTime, now).toMillis();
            if (millisSinceLastBid < BID_COOLDOWN_MILLIS) {
                return true; // Bị giới hạn
            }
        }
        
        // Cập nhật thời điểm đặt giá mới nhất
        lastBidTimes.put(bidderId, now);
        return false;
    }

    /**
     * --- LOGIC ĐẶT GIÁ (CORE BUSINESS LOGIC) ---
     * Đây là nơi chứa bộ não của chức năng đấu giá.
     */
    public Response placeBid(Bid bid) {
        int auctionId = bid.getAuction_id();
        int bidderId = bid.getBidder_id();

        // 0. KIỂM TRA TRẠNG THÁI BANNED TỪ DATABASE TRỰC TIẾP
        User bidder = userRepository.getUserById(bidderId);
        if (bidder == null) {
            return new Response("FAIL", null, "Lỗi: Không tìm thấy thông tin tài khoản của bạn.");
        }
        if ("BANNED".equalsIgnoreCase(bidder.getStatus())) {
            return new Response("FAIL", null, "Lỗi: Tài khoản của bạn đã bị khóa. Không thể thực hiện đấu giá.");
        }
        
        // --- KIỂM TRA RATE LIMIT TRƯỚC KHI VÀO LUỒNG ---
        if (isRateLimited(bidderId)) {
            return new Response("FAIL", null, "Lỗi: Bạn thao tác quá nhanh. Vui lòng thử lại sau giây lát.");
        }

        // SỬA ĐỔI: Sử dụng ConcurrencyHandler để lấy Lock thay vì tự quản lý Lock
        ReentrantLock lock = ConcurrencyHandler.getInstance().getLockForAuction(auctionId);
        
        // --- CƠ CHẾ CHỐNG "LOST UPDATE" VÀ "HAI NGƯỜI CÙNG THẮNG" ---
        // Khi Thread A (Client 1) đang thực thi đoạn code bên dưới, 
        // Thread B (Client 2) gọi lệnh này sẽ bị block (đóng băng) lại, 
        // chờ cho đến khi Thread A gọi lock.unlock() thì Thread B mới được đi tiếp.
        lock.lock(); 

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

            if (auction.getHighest_bidder_id() == bidderId) {
                return new Response("FAIL", null, "Bạn đang giữ giá cao nhất, không cần đặt giá cao hơn.");
            }

            // 4. KIỂM TRA BƯỚC GIÁ (Đảm bảo giá đặt hợp lý)
            double currentPrice = auction.getCurrent_price();
            double minIncrement = calculateMinimumIncrement(currentPrice);
            double minAllowedBid = currentPrice + minIncrement;
            
            if (bid.getAmount() < minAllowedBid) {
                return new Response("FAIL", null, "Lỗi: Giá đặt không hợp lệ. Bạn phải đặt tối thiểu: " + String.format("%.0f", minAllowedBid) + "đ (Bước giá hiện tại: " + String.format("%.0f", minIncrement) + "đ)");
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

                // --- GỌI ANTI-SNIPING Ở ĐÂY ---
                handleAntiSniping(auction);

                // 7. Gửi thông báo (Broadcast) cho toàn bộ Client đang online biết giá mới
                Response notifyPrice = new Response("NOTIFY_NEW_PRICE", bid, "Có người vừa đặt giá mới!");
                AuctionServer.broadcast(notifyPrice);

                // 8. KIỂM TRA VÀ KÍCH HOẠT AUTO-BID TỪ ĐỐI THỦ
                AutoBidManager.getInstance().processAutoBids(auctionId, bid.getAmount(), this);

                return new Response("SUCCESS", bid, "Đặt giá thành công! Bạn đang dẫn đầu.");
            } else {
                return new Response("FAIL", null, "Lỗi hệ thống khi lưu giá mới.");
            }

        } finally {
            // Giải phóng khóa để Client tiếp theo được vào đấu giá
            lock.unlock(); 
        }
    }
    
    /**
     * Hàm overload dùng cho AutoBidManager đặt giá mà không bị giới hạn Rate Limit.
     */
    public Response placeAutoBid(Bid autoBid) {
        int auctionId = autoBid.getAuction_id();
        
        User bidder = userRepository.getUserById(autoBid.getBidder_id());
        if (bidder != null && "BANNED".equalsIgnoreCase(bidder.getStatus())) {
            System.out.println("[AUTO-BID] Hủy đặt giá tự động do User " + autoBid.getBidder_id() + " đã bị khóa tài khoản.");
            return new Response("FAIL", null, "Lỗi: Tài khoản bị khóa.");
        }

        // SỬA ĐỔI: Sử dụng ConcurrencyHandler
        ReentrantLock lock = ConcurrencyHandler.getInstance().getLockForAuction(auctionId);
        lock.lock(); 

        try {
            Auction auction = auctionRepository.getAuctionById(auctionId);
            if (auction == null || !"RUNNING".equals(auction.getStatus()) || LocalDateTime.now().isAfter(auction.getEnd_time())) {
                return new Response("FAIL", null, "Lỗi đấu giá tự động.");
            }

            double currentPrice = auction.getCurrent_price();
            double minIncrement = calculateMinimumIncrement(currentPrice);
            double minAllowedBid = currentPrice + minIncrement;
            
            if (autoBid.getAmount() < minAllowedBid) {
                return new Response("FAIL", null, "Lỗi: Giá đặt tự động không hợp lệ.");
            }

            boolean isUpdated = auctionRepository.updateBid(auctionId, autoBid.getAmount(), autoBid.getBidder_id());
            
            if (isUpdated) {
                bidRepository.placeBid(autoBid);
                System.out.println("[AUTO-BID] Đã ghi nhận mức giá tự động: " + autoBid.getAmount() + " từ User " + autoBid.getBidder_id());

                handleAntiSniping(auction);

                Response notifyPrice = new Response("NOTIFY_NEW_PRICE", autoBid, "Có người vừa đặt giá mới (Auto-bid)!");
                AuctionServer.broadcast(notifyPrice);

                return new Response("SUCCESS", autoBid, "Đặt giá tự động thành công!");
            } else {
                return new Response("FAIL", null, "Lỗi hệ thống khi lưu giá mới (Auto-bid).");
            }

        } finally {
            lock.unlock(); 
        }
    }
    
    // Getter public cho AutoBidManager sử dụng
    public double getMinimumIncrement(double currentPrice) {
         return calculateMinimumIncrement(currentPrice);
    }
}
