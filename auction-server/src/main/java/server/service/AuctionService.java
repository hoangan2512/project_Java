package server.service;

import model.Item;
import model.Bid;
import server.repository.ItemRepository; // Giả sử bạn đã có
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.time.LocalDateTime;

public class AuctionService {
    private ConcurrentHashMap<Integer, Item> auctionItems = new ConcurrentHashMap<>();
    // Mỗi sản phẩm sẽ có một cái "khóa" riêng để tránh tranh chấp giá
    private final ConcurrentHashMap<Integer, ReentrantLock> itemLocks = new ConcurrentHashMap<>();

    // Giả sử bạn tiêm Repository vào để lưu Database
    private ItemRepository itemRepository = new ItemRepository();

    public String createAuction(Item item) {
        auctionItems.put(item.getId(), item);
        itemLocks.put(item.getId(), new ReentrantLock());
        return "SUCCESS: Sản phẩm " + item.getName() + " đã lên sàn.";
    }

    // --- LOGIC ĐẶT GIÁ ĐỒNG BỘ CHẶT CHẼ ---
    public String placeBid(int itemId, int bidderId, double bidAmount) {
        Item item = auctionItems.get(itemId);
        if (item == null) return "ERROR: Sản phẩm không tồn tại.";

        // Lấy khóa cho riêng sản phẩm này
        ReentrantLock lock = itemLocks.get(itemId);
        lock.lock(); // Bắt đầu chặn các luồng khác truy cập vào Item này

        try {
            // 1. Kiểm tra trạng thái (Phải đang RUNNING)
            if (!"RUNNING".equals(item.getStatus())) {
                return "ERROR: Phiên đấu giá đang " + item.getStatus();
            }

            // 2. Kiểm tra thời gian (Đề phòng trường hợp hết giờ mà processAutoEnd chưa chạy kịp)
            if (LocalDateTime.now().isAfter(item.getEndTime())) {
                item.setStatus("FINISHED");
                return "ERROR: Phiên đấu giá đã kết thúc theo thời gian.";
            }

            // 3. Kiểm tra giá (Ảnh 3.1.5)
            if (bidAmount <= item.getCurrentPrice()) {
                return "ERROR: Giá đặt phải cao hơn giá hiện tại (" + item.getCurrentPrice() + ")";
            }

            // 4. CẬP NHẬT DỮ LIỆU TRONG BỘ NHỚ
            item.setCurrentPrice(bidAmount);
            item.setHighestBidderId(bidderId);

            // 5. CẬP NHẬT XUỐNG DATABASE (Quan trọng để đồng bộ)
            // itemRepository.updatePrice(itemId, bidAmount, bidderId);

            return "SUCCESS: Bạn đang dẫn đầu phiên đấu giá!";
        } finally {
            lock.unlock(); // Luôn giải phóng khóa dù có lỗi xảy ra
        }
    }

    // --- LOGIC KẾT THÚC PHIÊN (Chạy ngầm) ---
    public void processAutoEnd() {
        LocalDateTime now = LocalDateTime.now();
        for (Item item : auctionItems.values()) {
            if ("RUNNING".equals(item.getStatus()) && now.isAfter(item.getEndTime())) {
                ReentrantLock lock = itemLocks.get(item.getId());
                if (lock.tryLock()) { // Thử khóa nếu không ai đang đặt giá
                    try {
                        item.setStatus("FINISHED");
                        // itemRepository.updateStatus(item.getId(), "FINISHED");
                        System.out.println("Sản phẩm " + item.getName() + " đã kết thúc tự động.");
                    } finally {
                        lock.unlock();
                    }
                }
            }
        }
    }

    public void changeStatus(int itemId, String status) {
        Item item = auctionItems.get(itemId);
        if (item != null) {
            item.setStatus(status);
            // itemRepository.updateStatus(itemId, status);
        }
    }
}