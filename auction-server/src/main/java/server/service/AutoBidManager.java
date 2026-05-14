package server.service;

import model.AutoBidConfig;
import model.Bid;
import server.repository.AuctionRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Xử lý logic đấu giá tự động (Auto-Bidding).
 */
public class AutoBidManager {
    private static final AutoBidManager instance = new AutoBidManager();

    // Lưu trữ cấu hình auto-bid: Key = auctionId, Value = Danh sách các cấu hình của người dùng
    private final Map<Integer, List<AutoBidConfig>> autoBids = new ConcurrentHashMap<>();

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

        // Tạo một luồng riêng để xử lý auto-bid, tránh block luồng chính của client đang đặt giá
        new Thread(() -> {
            boolean bidPlaced;
            do {
                bidPlaced = false;
                double loopCurrentPrice = getCurrentPriceFromDB(auctionId); // Cần hàm này hoặc truyền giá vào

                // Lọc ra những người có khả năng tự động trả giá (còn ngân sách maxBid lớn hơn giá hiện tại + bước giá)
                List<AutoBidConfig> eligibleConfigs = configs.stream()
                        .filter(c -> c.getMaxBid() >= loopCurrentPrice + c.getIncrement())
                        .collect(Collectors.toList());

                if (eligibleConfigs.isEmpty()) {
                    break; // Dừng lại nếu không ai còn đủ tiền đua tiếp
                }

                // Lấy người đầu tiên trong danh sách hợp lệ (nhờ đã sort theo thời gian đăng ký)
                AutoBidConfig winnerConfig = eligibleConfigs.get(0);

                // Tính toán mức giá mới cần đặt
                double nextBidAmount = loopCurrentPrice + winnerConfig.getIncrement();

                // Đảm bảo không vượt quá maxBid
                if (nextBidAmount > winnerConfig.getMaxBid()) {
                     nextBidAmount = winnerConfig.getMaxBid();
                }

                // Tạo đối tượng Bid mới
                Bid autoBid = new Bid();
                autoBid.setAuction_id(auctionId);
                autoBid.setBidder_id(winnerConfig.getBidderId());
                autoBid.setAmount(nextBidAmount);
                autoBid.setBid_time(LocalDateTime.now());

                // GỌI LẠI HÀM PLACE BID CỦA AUCTION SERVICE
                // (Hàm này có lock bên trong nên rất an toàn)
                message.Response response = auctionService.placeBid(autoBid);

                if ("SUCCESS".equals(response.getStatus())) {
                    bidPlaced = true;
                    System.out.println("[AUTO-BID] User " + winnerConfig.getBidderId() + " tự động trả giá: " + nextBidAmount);

                    // Nghỉ 1 chút xíu (ví dụ 100ms) để hệ thống kịp broadcast giá trước khi chạy vòng đua giá tiếp theo
                    try { Thread.sleep(100); } catch (InterruptedException e) {}
                }

            } while (bidPlaced); // Tiếp tục đua giá tự động cho đến khi không ai chịu nâng giá nữa
        }).start();
    }

    // Hàm phụ trợ để lấy giá hiện tại mới nhất từ DB
    private double getCurrentPriceFromDB(int auctionId) {
         AuctionRepository repo = new AuctionRepository();
         model.Auction auction = repo.getAuctionById(auctionId);
         return auction != null ? auction.getCurrent_price() : 0;
    }
}
