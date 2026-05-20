package server.service;

import model.AutoBidConfig;
import model.Bid;
import server.repository.AuctionRepository;
import server.repository.BidRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Xử lý logic đấu giá tự động (Auto-Bidding).
 */
public class AutoBidManager {
    private static final AutoBidManager instance = new AutoBidManager();

    // Lưu trữ cấu hình auto-bid: Key = auctionId, Value = Danh sách các cấu hình của người dùng
    private final Map<Integer, List<AutoBidConfig>> autoBids = new ConcurrentHashMap<>();

    // Sử dụng ScheduledExecutorService thay vì tạo Thread mới thủ công và dùng Thread.sleep
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private AutoBidManager() {}

    public static AutoBidManager getInstance() {
        return instance;
    }

    /**
     * Đăng ký một cấu hình Auto-Bid mới cho người dùng.
     */
    public synchronized void registerAutoBid(AutoBidConfig config) {
        autoBids.computeIfAbsent(config.getAuctionId(), k -> new ArrayList<>()).add(config);

        // Sắp xếp lại danh sách theo thời điểm đăng ký (ưu tiên người đăng ký trước)
        autoBids.get(config.getAuctionId()).sort(Comparator.comparing(AutoBidConfig::getRegisteredAt));
        System.out.println("Đăng ký Auto-Bid thành công cho User ID: " + config.getBidderId() + " tại Auction ID: " + config.getAuctionId());
    }

    /**
     * Hàm này được gọi mỗi khi có một Bid mới được đặt thành công.
     * Nó sẽ kiểm tra và thực hiện các lượt đấu giá tự động (nếu có).
     *
     * @param auctionId ID của phiên đấu giá
     * @param currentHighestBid Giá cao nhất hiện tại (vừa được đặt)
     * @param auctionService Truyền service vào để gọi lại hàm placeBid
     */
    public void processAutoBids(int auctionId, double currentHighestBid, AuctionService auctionService) {
        List<AutoBidConfig> configs = autoBids.get(auctionId);
        if (configs == null || configs.isEmpty()) {
            return; // Không có ai đăng ký auto-bid cho phiên này
        }

        // Lập lịch thực thi ngay lập tức, và đệ quy tự gọi lại thay vì dùng while(true) + Thread.sleep
        scheduler.execute(() -> executeNextAutoBid(auctionId, currentHighestBid, auctionService));
    }
    
    private void executeNextAutoBid(int auctionId, double currentPrice, AuctionService auctionService) {
        List<AutoBidConfig> configs = autoBids.get(auctionId);
        if (configs == null || configs.isEmpty()) return;

        // KIỂM TRA ĐIỀU KIỆN QUAN TRỌNG: Chỉ nâng giá nếu người đang giữ giá cao nhất KHÔNG PHẢI là người có Auto-Bid này
        int highestBidderId = getHighestBidderIdFromDB(auctionId);

        double systemMinIncrement = auctionService.getMinimumIncrement(currentPrice);

        // Lọc ra những người có khả năng tự động trả giá (Loại trừ luôn người đang giữ đỉnh bảng)
        List<AutoBidConfig> eligibleConfigs = configs.stream()
                .filter(c -> c.getBidderId() != highestBidderId) // QUAN TRỌNG: Loại bỏ người đang top 1
                .filter(c -> {
                    double requiredNextBid = currentPrice + Math.max(c.getIncrement(), systemMinIncrement);
                    return c.getMaxBid() >= requiredNextBid;
                })
                .toList();

        if (eligibleConfigs.isEmpty()) {
            System.out.println("[AUTO-BID] Kết thúc: Không còn ai đủ điều kiện hoặc cần thiết phải đua giá cho Auction " + auctionId);
            return; // Thoát đệ quy
        }

        // Lấy người đầu tiên trong danh sách hợp lệ (nhờ đã sort theo thời gian đăng ký)
        AutoBidConfig winnerConfig = eligibleConfigs.getFirst();

        double actualIncrement = Math.max(winnerConfig.getIncrement(), systemMinIncrement);
        double nextBidAmount = currentPrice + actualIncrement;

        if (nextBidAmount > winnerConfig.getMaxBid()) {
             nextBidAmount = winnerConfig.getMaxBid();
        }

        Bid autoBid = new Bid();
        autoBid.setAuction_id(auctionId);
        autoBid.setBidder_id(winnerConfig.getBidderId());
        autoBid.setAmount(nextBidAmount);
        autoBid.setBid_time(LocalDateTime.now());

        message.Response response = auctionService.placeAutoBid(autoBid);

        if ("SUCCESS".equals(response.getStatus())) {
            System.out.println("[AUTO-BID] User " + winnerConfig.getBidderId() + " tự động trả giá: " + nextBidAmount);
            
            // Lập lịch cho lần chạy tiếp theo sau 100ms
            final double nextPrice = nextBidAmount;
            scheduler.schedule(() -> executeNextAutoBid(auctionId, nextPrice, auctionService), 100, TimeUnit.MILLISECONDS);
            
        } else {
            System.out.println("[AUTO-BID] Xung đột giá, đọc lại giá mới nhất từ DB...");
            double priceFromDB = getCurrentPriceFromDB(auctionId);
            if (priceFromDB > currentPrice) {
                // Thử lại ngay lập tức với giá mới từ DB
                scheduler.execute(() -> executeNextAutoBid(auctionId, priceFromDB, auctionService));
            } else {
                System.out.println("[AUTO-BID] Kết thúc: Không thể đặt giá mới và giá DB không đổi.");
            }
        }
    }

    private double getCurrentPriceFromDB(int auctionId) {
         AuctionRepository repo = new AuctionRepository();
         model.Auction auction = repo.getAuctionById(auctionId);
         return auction != null ? auction.getCurrent_price() : 0;
    }
    
    private int getHighestBidderIdFromDB(int auctionId) {
         BidRepository repo = new BidRepository();
         Bid highestBid = repo.getHighestBid(auctionId);
         return highestBid != null ? highestBid.getBidder_id() : -1;
    }
}
