package server.service;

import model.Auction;
import server.repository.AuctionRepository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.time.LocalDateTime;

public class AuctionService {

    // Lưu trữ các phiên đấu giá đang chạy trên RAM
    private ConcurrentHashMap<Integer, Auction> activeAuctions = new ConcurrentHashMap<>();

    // Mỗi phiên đấu giá (Auction) sẽ có một cái "khóa" riêng để tránh tranh chấp giá
    private final ConcurrentHashMap<Integer, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    // Tiêm Repository vào để lưu Database
    private AuctionRepository auctionRepository = new AuctionRepository();

    // --- LOGIC TẠO PHIÊN ĐẤU GIÁ ---
    public String createAuction(Auction auction) {
        // Lưu vào bộ nhớ tạm
        activeAuctions.put(auction.getId(), auction);
        // Tạo khóa Lock cho phiên đấu giá này
        auctionLocks.put(auction.getId(), new ReentrantLock());

        return "SUCCESS: Phiên đấu giá ID " + auction.getId() + " đã lên sàn.";
    }

    // --- LOGIC ĐẶT GIÁ ĐỒNG BỘ CHẶT CHẼ ---
    public String placeBid(int auctionId, int bidderId, double bidAmount) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) return "ERROR: Phiên đấu giá không tồn tại hoặc đã kết thúc.";

        // Lấy khóa cho riêng phiên đấu giá này
        ReentrantLock lock = auctionLocks.get(auctionId);
        lock.lock(); // Bắt đầu chặn các luồng khác truy cập vào phiên này

        try {
            // 1. Kiểm tra trạng thái (Phải đang RUNNING)
            if (!"RUNNING".equals(auction.getStatus())) {
                return "ERROR: Phiên đấu giá đang " + auction.getStatus();
            }

            // 2. Kiểm tra thời gian (Đề phòng trường hợp hết giờ mà processAutoEnd chưa chạy kịp)
            if (LocalDateTime.now().isAfter(auction.getEnd_time())) {
                auction.setStatus("FINISHED");
                return "ERROR: Phiên đấu giá đã kết thúc theo thời gian.";
            }

            // 3. Kiểm tra giá
            if (bidAmount <= auction.getCurrent_price()) {
                return "ERROR: Giá đặt phải cao hơn giá hiện tại (" + auction.getCurrent_price() + ")";
            }

            // 4. CẬP NHẬT DỮ LIỆU TRONG BỘ NHỚ
            auction.setCurrent_price(bidAmount);
            auction.setHighest_bidder_id(bidderId);

            // 5. CẬP NHẬT XUỐNG DATABASE (Quan trọng để đồng bộ)
            // Bạn sẽ cần viết hàm updateBid(...) trong AuctionRepository
            // auctionRepository.updateBid(auctionId, bidAmount, bidderId);

            return "SUCCESS: Bạn đang dẫn đầu phiên đấu giá!";
        } finally {
            lock.unlock(); // Luôn giải phóng khóa dù có lỗi xảy ra
        }
    }

    // --- LOGIC KẾT THÚC PHIÊN (Chạy ngầm) ---
    public void processAutoEnd() {
        LocalDateTime now = LocalDateTime.now();

        for (Auction auction : activeAuctions.values()) {
            if ("RUNNING".equals(auction.getStatus()) && now.isAfter(auction.getEnd_time())) {

                ReentrantLock lock = auctionLocks.get(auction.getId());
                if (lock.tryLock()) { // Thử khóa, nếu không có ai đang click đặt giá thì mới khóa
                    try {
                        auction.setStatus("FINISHED");

                        // Cập nhật xuống DB
                        // auctionRepository.updateStatus(auction.getId(), "FINISHED");

                        System.out.println("Phiên đấu giá " + auction.getId() + " đã tự động kết thúc.");
                    } finally {
                        lock.unlock();
                    }
                }
            }
        }
    }

    // --- LOGIC THAY ĐỔI TRẠNG THÁI THỦ CÔNG ---
    public void changeStatus(int auctionId, String status) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction != null) {
            auction.setStatus(status);
            // auctionRepository.updateStatus(auctionId, status);
        }
    }
}