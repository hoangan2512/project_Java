package server.service;

import message.Response;
import model.Auction;
import model.Item;
import model.Bid;
import server.network.AuctionServer;
import server.repository.AuctionRepository;
import server.repository.ItemRepository;
import server.repository.BidRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionTimeManager implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(AuctionTimeManager.class.getName());
    private static final String FINISHED_STATUS = "FINISHED";
    private static final String RUNNING_STATUS = "RUNNING";
    private static final String PENDING_APPROVAL_STATUS = "PENDING_APPROVAL";
    private static final long DEFAULT_CHECK_INTERVAL_SECONDS = 5;

    // Singleton instance để AuctionService có thể gọi tới
    private static AuctionTimeManager instance;

    private final AuctionRepository auctionRepository;
    private final ItemRepository itemRepository;
    private final BidRepository bidRepository; // Thêm BidRepository để check kết quả
    private final ScheduledExecutorService scheduler;
    private final long checkIntervalSeconds;
    private final AtomicBoolean started = new AtomicBoolean(false);

    // Lưu trữ thời gian kết thúc (đã gia hạn) của các phiên đấu giá trong RAM để truy xuất nhanh
    private final ConcurrentHashMap<Integer, LocalDateTime> extendedEndTimes = new ConcurrentHashMap<>();

    public AuctionTimeManager() {
        this(new AuctionRepository(), new ItemRepository(), DEFAULT_CHECK_INTERVAL_SECONDS);
    }

    public AuctionTimeManager(AuctionRepository auctionRepository, ItemRepository itemRepository, long checkIntervalSeconds) {
        if (auctionRepository == null) {
            throw new IllegalArgumentException("auctionRepository must not be null");
        }
        if (itemRepository == null) {
            throw new IllegalArgumentException("itemRepository must not be null");
        }
        if (checkIntervalSeconds <= 0) {
            throw new IllegalArgumentException("checkIntervalSeconds must be greater than 0");
        }

        this.auctionRepository = auctionRepository;
        this.checkIntervalSeconds = checkIntervalSeconds;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "auction-time-manager");
            thread.setDaemon(true);
            return thread;
        });
        
        // Gán instance để có thể gọi từ ngoài vào
        instance = this;
    }

    public static AuctionTimeManager getInstance() {
        if (instance == null) {
            instance = new AuctionTimeManager();
        }
        return instance;
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }

        scheduler.scheduleAtFixedRate(
                this::manageAuctionSchedulesSafely,
                0,
                checkIntervalSeconds,
                TimeUnit.SECONDS
        );
    }

    public void stop() {
        if (!started.compareAndSet(true, false)) {
            return;
        }

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        stop();
    }
    /**
     * Phương thức cho phép các Service khác (như AuctionService) dời lịch kết thúc của một phiên đấu giá.
     */
    public void extendAuction(int auctionId, LocalDateTime newEndTime) {
        extendedEndTimes.put(auctionId, newEndTime);
    }

    public boolean isExpired(Auction auction) {
        if (auction == null) return true;

        // Ưu tiên lấy thời gian kết thúc đã được gia hạn từ bộ nhớ cache
        LocalDateTime endTime = extendedEndTimes.getOrDefault(auction.getId(), auction.getEnd_time());
        
        if (endTime == null) return true;

        return !LocalDateTime.now().isBefore(endTime);
    }

    private void manageAuctionSchedulesSafely() {
        try {
            manageAuctionSchedules();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while managing auction schedules", e);
        }
    }

    private void manageAuctionSchedules() {
        // 1. Kiểm tra và mở các phiên đấu giá đã đến giờ
        List<Auction> waitingAuctions = auctionRepository.getWaitingAuctions();
        for (Auction auction : waitingAuctions) {
            Item item = itemRepository.getItemById(auction.getItem_id());
            if (item != null && "APPROVED".equals(item.getModeration_status())) {
                if (auction.getStart_time() != null && !LocalDateTime.now().isBefore(auction.getStart_time())) {
                    boolean updated = auctionRepository.updateStatus(auction.getId(), RUNNING_STATUS);
                    if (updated) {
                        auction.setStatus(RUNNING_STATUS);
                        LOGGER.log(Level.INFO, "Auction {0} started automatically.", auction.getId());
                        notifyAuctionStarted(auction); // Phát Broadcast cho Client biết phiên đã bắt đầu
                    }
                }
            }
        }

        // 2. Kiểm tra và đóng các phiên đấu giá đã hết hạn
        List<Auction> activeAuctions = auctionRepository.getActiveAuctions();
        for (Auction auction : activeAuctions) {
            if (!isExpired(auction)) {
                continue;
            }

            boolean updated = auctionRepository.updateStatus(auction.getId(), FINISHED_STATUS);
            if (updated) {
                auction.setStatus(FINISHED_STATUS);
                extendedEndTimes.remove(auction.getId());

                // Gửi thông báo kết thúc cho toàn bộ hệ thống
                notifyAuctionFinished(auction);

                // KIỂM TRA VÀ GỬI THÔNG BÁO CHO NGƯỜI CHIẾN THẮNG
                notifyAuctionWinner(auction);

                LOGGER.log(Level.INFO, "Auction {0} finished automatically.", auction.getId());
            }
        }
    }

    private void notifyAuctionStarted(Auction auction) {
        Response response = new Response(
                "AUCTION_START",
                auction,
                "Auction has started."
        );
        AuctionServer.broadcast(response);
    }

    private void notifyAuctionFinished(Auction auction) {
        Response response = new Response(
                "AUCTION_END",
                auction,
                "Auction has ended."
        );
        AuctionServer.broadcast(response);
    }

    /**
     * Tìm người trả giá cao nhất khi phiên kết thúc và gửi thông báo đích danh
     */
    private void notifyAuctionWinner(Auction auction) {
        Bid highestBid = bidRepository.getHighestBid(auction.getId());

        // Nếu phiên đấu giá kết thúc mà KHÔNG CÓ AI bid, thì bỏ qua không làm gì cả
        if (highestBid == null) {
            LOGGER.log(Level.INFO, "Auction {0} kết thúc mà không có lượt trả giá nào.", auction.getId());
            return;
        }

        // Lấy ID người chiến thắng và mức giá cuối cùng
        int winnerId = highestBid.getBidder_id();
        double winningAmount = highestBid.getAmount();

        // Đóng gói thông báo
        Response winNotification = new Response(
                "AUCTION_WON",
                auction,
                "Chúc mừng! Bạn đã chiến thắng phiên đấu giá với mức giá " + winningAmount
        );

        // Gọi hàm sendMessageToUser của AuctionServer để gửi đích danh
        AuctionServer.sendMessageToUser(winnerId, winNotification);

        LOGGER.log(Level.INFO, "Đã gửi thông báo chiến thắng cho User ID {0} tại Auction {1}", new Object[]{winnerId, auction.getId()});
    }
}