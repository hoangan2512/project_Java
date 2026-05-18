package server.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Xử lý đấu giá đồng thời, tránh lỗi "Lost update" và các vấn đề về Race Condition.
 * Lớp này hoạt động như một trình quản lý Lock trung tâm cho toàn bộ hệ thống.
 */
public class ConcurrencyHandler {

    private static final ConcurrencyHandler instance = new ConcurrencyHandler();

    // Mỗi phiên đấu giá (Auction) sẽ có một cái "khóa" (Lock) riêng.
    // Việc dùng ConcurrentHashMap giúp đảm bảo an toàn luồng (thread-safe) khi nhiều client
    // cùng truy cập vào danh sách khóa.
    private final ConcurrentHashMap<Integer, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    private ConcurrencyHandler() {}

    public static ConcurrencyHandler getInstance() {
        return instance;
    }

    /**
     * Lấy khóa Lock cho một phiên đấu giá cụ thể.
     * Nếu khóa chưa tồn tại, nó sẽ tự động được tạo mới (computeIfAbsent).
     * 
     * @param auctionId ID của phiên đấu giá
     * @return ReentrantLock tương ứng
     */
    public ReentrantLock getLockForAuction(int auctionId) {
        return auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock());
    }

    /**
     * Tiện ích: Loại bỏ Lock khỏi bộ nhớ khi phiên đấu giá đã kết thúc hoàn toàn
     * Giúp giải phóng tài nguyên (Memory leak prevention).
     */
    public void removeLockForAuction(int auctionId) {
        auctionLocks.remove(auctionId);
    }
}
