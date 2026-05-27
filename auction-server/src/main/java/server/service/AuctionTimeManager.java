package server.service;

import message.Response;
import model.Auction;
import model.Bid;
import model.Item;
import server.network.AuctionServer;
import server.repository.AuctionRepository;
import server.repository.BidRepository;
import server.repository.ItemRepository;

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

    // Các trạng thái hằng số
    private static final String FINISHED_STATUS = "FINISHED";
    private static final String RUNNING_STATUS = "RUNNING";
    private static final String PENDING_APPROVAL_STATUS = "PENDING_APPROVAL";
    private static final String SUSPENDED_STATUS = "SUSPENDED";
    private static final String REJECTED_STATUS = "REJECTED"; // Thêm hằng số cho Item
    private static final String PROPOSAL_STATUS = "PROPOSAL";
    private static final String DELETE_PROPOSAL_STATUS = "DELETE_PROPOSAL";
    private static final long DEFAULT_CHECK_INTERVAL_SECONDS = 5;

    // Singleton instance để các Service khác có thể gọi tới
    private static AuctionTimeManager instance;

    private final AuctionRepository auctionRepository;
    private final ItemRepository itemRepository;
    private final BidRepository bidRepository;
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
        this.bidRepository = new BidRepository();
        this.itemRepository = itemRepository;
        this.checkIntervalSeconds = checkIntervalSeconds;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "auction-time-manager");
            thread.setDaemon(true); // Để luồng tự tắt khi ứng dụng tắt
            return thread;
        });

        // Gán instance để sử dụng Singleton
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
            return; // Đã start rồi thì bỏ qua
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
     * Phương thức cho phép dời lịch kết thúc của một phiên đấu giá (Ví dụ: tính năng chống bắn tỉa).
     */
    public void extendAuction(int auctionId, LocalDateTime newEndTime) {
        extendedEndTimes.put(auctionId, newEndTime);
    }

    /**
     * Kiểm tra xem phiên đấu giá đã hết hạn hay chưa.
     */
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
            LOGGER.log(Level.SEVERE, "Lỗi khi quản lý lịch trình đấu giá!", e);
        }
    }

    private void manageAuctionSchedules() {
        // ====================================================================
        // 1. XỬ LÝ CÁC PHIÊN ĐANG CHỜ (Mở phiên hoặc Đình chỉ nếu quá hạn duyệt)
        // LƯU Ý: Hàm getWaitingAuctions() phải trả về cả WAITING và PENDING_APPROVAL
        // ====================================================================
        List<Auction> waitingAuctions = auctionRepository.getWaitingAuctions();

        for (Auction auction : waitingAuctions) {
            Item item = itemRepository.getItemById(auction.getItem_id());
            if (item != null) {
                // Kiểm tra xem đã đến giờ mở phiên hay chưa
                boolean isTimeToStart = auction.getStart_time() != null && !LocalDateTime.now().isBefore(auction.getStart_time());

                if (PENDING_APPROVAL_STATUS.equals(item.getModeration_status())) {
                    if (isTimeToStart) {
                        // ==============================================================
                        // LOGIC MỚI: Đã đến giờ mở phiên nhưng Admin VẪN CHƯA DUYỆT
                        // -> Đình chỉ Auction (SUSPENDED) và Từ chối Item (REJECTED)
                        // ==============================================================
                        boolean auctionUpdated = auctionRepository.updateStatus(auction.getId(), SUSPENDED_STATUS);
                        boolean itemUpdated = itemRepository.updateStatus(item.getId(), REJECTED_STATUS);

                        if (auctionUpdated) {
                            auction.setStatus(SUSPENDED_STATUS);
                            LOGGER.log(Level.WARNING, "Auction {0} đã bị đình chỉ (SUSPENDED) do đến giờ mà vẫn chưa được duyệt.", auction.getId());
                        }
                        if (itemUpdated) {
                            item.setModeration_status(REJECTED_STATUS);
                            LOGGER.log(Level.WARNING, "Item {0} đã bị từ chối (REJECTED) do quá hạn duyệt trước giờ mở phiên.", item.getId());
                        }
                    } else {
                        // Nếu trạng thái của Auction chưa đồng bộ với Item (vd lúc mới tạo), set lại thành PENDING_APPROVAL
                        if (!PENDING_APPROVAL_STATUS.equals(auction.getStatus())) {
                            boolean updated = auctionRepository.updateStatus(auction.getId(), PENDING_APPROVAL_STATUS);
                            if (updated) {
                                auction.setStatus(PENDING_APPROVAL_STATUS);
                            }
                        }
                    }
                } else if ((PROPOSAL_STATUS.equals(item.getModeration_status()) || DELETE_PROPOSAL_STATUS.equals(item.getModeration_status())) && isTimeToStart) {
                    // If the item is in PROPOSAL status, and it's time to start the auction
                    // 1. Delete the changes
                    itemRepository.deleteChanges(item.getId());

                    // 2. Start the auction
                    itemRepository.updateStatus(item.getId(), "APPROVED");
                    boolean updated = auctionRepository.updateStatus(auction.getId(), RUNNING_STATUS);
                    if (updated) {
                        auction.setStatus(RUNNING_STATUS);
                        LOGGER.log(Level.INFO, "Auction {0} tự động mở sàn (RUNNING) sau khi xóa proposal.", auction.getId());
                        notifyAuctionStarted(auction);
                    }
                }
                else if ("APPROVED".equals(item.getModeration_status()) && isTimeToStart) {
                    // Nếu item đã APPROVED và đã đến giờ -> Mở sàn (RUNNING)
                    boolean updated = auctionRepository.updateStatus(auction.getId(), RUNNING_STATUS);
                    if (updated) {
                        auction.setStatus(RUNNING_STATUS);
                        LOGGER.log(Level.INFO, "Auction {0} tự động mở sàn (RUNNING).", auction.getId());
                        notifyAuctionStarted(auction);
                    }
                }
            }
        }

        // ====================================================================
        // 2. XỬ LÝ CÁC PHIÊN ĐANG CHẠY (Đóng phiên khi hết giờ và công bố Winner)
        // ====================================================================
        List<Auction> activeAuctions = auctionRepository.getActiveAuctions();

        for (Auction auction : activeAuctions) {
            if (!isExpired(auction)) {
                continue;
            }

            boolean updated = auctionRepository.updateStatus(auction.getId(), FINISHED_STATUS);
            if (updated) {
                auction.setStatus(FINISHED_STATUS);
                extendedEndTimes.remove(auction.getId()); // Dọn dẹp cache

                LOGGER.log(Level.INFO, "Auction {0} đã kết thúc tự động.", auction.getId());

                // Gửi thông báo kết thúc cho toàn bộ hệ thống
                notifyAuctionFinished(auction);

                // KIỂM TRA VÀ GỬI THÔNG BÁO CHO NGƯỜI CHIẾN THẮNG
                notifyAuctionWinner(auction);
            }
        }
    }

    private void notifyAuctionStarted(Auction auction) {
        Response response = new Response(
                "AUCTION_START",
                auction,
                "Phiên đấu giá đã bắt đầu."
        );
        AuctionServer.broadcast(response);
    }

    private void notifyAuctionFinished(Auction auction) {
        Response response = new Response(
                "AUCTION_END",
                auction,
                "Phiên đấu giá đã kết thúc."
        );
        AuctionServer.broadcast(response);
    }

    /**
     * Tìm người trả giá cao nhất khi phiên kết thúc và gửi thông báo đích danh
     */
    private void notifyAuctionWinner(Auction auction) {
        Bid highestBid = bidRepository.getHighestBid(auction.getId());

        // Nếu phiên đấu giá kết thúc mà KHÔNG CÓ AI bid, thì bỏ qua
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