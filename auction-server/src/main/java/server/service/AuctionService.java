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

    // CHUYỂN THÀNH: Khai báo không dùng từ khóa 'new' trực tiếp tại đây
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;

    /**
     * CHUYỂN ĐỔI 1: Hàm khởi tạo mặc định (Không tham số)
     * Dùng cho việc chạy ứng dụng thực tế (Production code). Khi Server chạy, nó tự động 'new' các Repository gốc.
     */
    public AuctionService() {
        this.auctionRepository = new AuctionRepository();
        this.bidRepository = new BidRepository();
        this.userRepository = new UserRepository();
    }

    /**
     * CHUYỂN ĐỔI 2: Hàm khởi tạo có tham số (Constructor Injection)
     * Dùng riêng cho tầng Test. Mockito sẽ sử dụng hàm này để tiêm (Inject) các Mock Repository giả lập vào.
     */
    public AuctionService(AuctionRepository auctionRepository, BidRepository bidRepository, UserRepository userRepository) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.userRepository = userRepository;
    }

    // --- CẤU HÌNH ANTI-SNIPING ---
    private static final long ANTI_SNIPING_TRIGGER_SECONDS = 30;
    private static final long ANTI_SNIPING_EXTENSION_SECONDS = 60;

    // --- CẤU HÌNH RATE LIMIT (ANTI-SPAM) ---
    private static final long BID_COOLDOWN_MILLIS = 2000;

    /**
     * Xác định bước giá (minimum increment) hợp lý dựa trên giá trị hiện tại của sản phẩm.
     */
    private double calculateMinimumIncrement(double currentPrice) {
        if (currentPrice < 100000) {
            return 10000;
        } else if (currentPrice < 1000000) {
            return 50000;
        } else if (currentPrice < 10000000) {
            return 200000;
        } else if (currentPrice < 50000000) {
            return 500000;
        } else {
            return 1000000;
        }
    }

    /**
     * Hàm phụ trợ xử lý Anti-Sniping (Gia hạn thời gian kết thúc)
     */
    private void handleAntiSniping(Auction auction) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = auction.getEnd_time();

        long secondsRemaining = Duration.between(now, endTime).getSeconds();

        if (secondsRemaining > 0 && secondsRemaining <= ANTI_SNIPING_TRIGGER_SECONDS) {
            LocalDateTime newEndTime = now.plusSeconds(ANTI_SNIPING_EXTENSION_SECONDS);

            boolean updated = auctionRepository.updateEndTime(auction.getId(), newEndTime);
            if (updated) {
                auction.setEnd_time(newEndTime);
                System.out.println("[ANTI-SNIPING] Phiên đấu giá " + auction.getId() + " vừa được gia hạn đến: " + newEndTime);

                AuctionTimeManager.getInstance().extendAuction(auction.getId(), newEndTime);

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
                return true;
            }
        }

        lastBidTimes.put(bidderId, now);
        return false;
    }

    /**
     * --- LOGIC ĐẶT GIÁ (CORE BUSINESS LOGIC) ---
     */
    public Response placeBid(Bid bid) {
        int auctionId = bid.getAuction_id();
        int bidderId = bid.getBidder_id();

        User bidder = userRepository.getUserById(bidderId);
        if (bidder == null) {
            return new Response("FAIL", null, "Lỗi: Không tìm thấy thông tin tài khoản của bạn.");
        }
        if ("BANNED".equalsIgnoreCase(bidder.getStatus())) {
            return new Response("FAIL", null, "Lỗi: Tài khoản của bạn đã bị khóa. Không thể thực hiện đấu giá.");
        }

        if (isRateLimited(bidderId)) {
            return new Response("FAIL", null, "Lỗi: Bạn thao tác quá nhanh. Vui lòng thử lại sau giây lát.");
        }

        ReentrantLock lock = ConcurrencyHandler.getInstance().getLockForAuction(auctionId);
        lock.lock();

        try {
            Auction auction = auctionRepository.getAuctionById(auctionId);
            if (auction == null) {
                return new Response("FAIL", null, "Lỗi: Phiên đấu giá không tồn tại.");
            }

            if (!"RUNNING".equals(auction.getStatus())) {
                return new Response("FAIL", null, "Lỗi: Phiên đấu giá đang ở trạng thái " + auction.getStatus() + ".");
            }

            if (LocalDateTime.now().isAfter(auction.getEnd_time())) {
                return new Response("FAIL", null, "Lỗi: Phiên đấu giá đã kết thúc.");
            }

            if (auction.getHighest_bidder_id() == bidderId) {
                return new Response("FAIL", null, "Bạn đang giữ giá cao nhất, không cần đặt giá cao hơn.");
            }

            double currentPrice = auction.getCurrent_price();
            double minIncrement = calculateMinimumIncrement(currentPrice);
            double minAllowedBid = currentPrice + minIncrement;

            if (bid.getAmount() < minAllowedBid) {
                return new Response("FAIL", null, "Lỗi: Giá đặt không hợp lệ. Bạn phải đặt tối thiểu: " + String.format("%.0f", minAllowedBid) + "đ (Bước giá hiện tại: " + String.format("%.0f", minIncrement) + "đ)");
            }

            boolean isUpdated = auctionRepository.updateBid(auctionId, bid.getAmount(), bid.getBidder_id());

            if (isUpdated) {
                bidRepository.placeBid(bid);
                System.out.println("Đã ghi nhận mức giá mới: " + bid.getAmount() + " từ User " + bid.getBidder_id() + " cho Auction " + auctionId);

                handleAntiSniping(auction);

                Response notifyPrice = new Response("NOTIFY_NEW_PRICE", bid, "Có người vừa đặt giá mới!");
                AuctionServer.broadcast(notifyPrice);

                AutoBidManager.getInstance().processAutoBids(auctionId, bid.getAmount(), this);

                return new Response("SUCCESS", bid, "Đặt giá thành công! Bạn đang dẫn đầu.");
            } else {
                return new Response("FAIL", null, "Lỗi hệ thống khi lưu giá mới.");
            }

        } finally {
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

    public double getMinimumIncrement(double currentPrice) {
        return calculateMinimumIncrement(currentPrice);
    }
}