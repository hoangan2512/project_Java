package server.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;


import model.Item;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionService {
    // Lưu trữ danh sách các vật phẩm đấu giá
    private ConcurrentHashMap<Integer, Item> auctionItems = new ConcurrentHashMap<>();

    // --- 3.1.2: QUẢN LÝ SẢN PHẨM (Thêm / Sửa / Xóa) ---
    public String createAuction(Item item) {
        auctionItems.put(item.getId(), item);
        return "SUCCESS: Đã đưa sản phẩm " + item.getName() + " lên sàn.";
    }

    public String updateItemInfo(int itemId, int sellerId, String newDesc) {
        Item item = auctionItems.get(itemId);
        if (item == null) return "ERROR: Không tìm thấy sản phẩm.";
        if (item.getSeller_id() != sellerId) return "ERROR: Bạn không có quyền.";

        // Chỉ cho sửa khi chưa đấu giá (Status OPEN)
        if (!"OPEN".equals(item.getStatus())) return "ERROR: Phiên đã bắt đầu, không thể sửa.";

        item.setDescription(newDesc);
        return "SUCCESS: Cập nhật mô tả thành công.";
    }

    // --- 3.1.3 & 3.1.5: THAM GIA ĐẤU GIÁ & XỬ LÝ LỖI ---
    public synchronized String placeBid(int itemId, int bidderId, double bidAmount) {
        Item item = auctionItems.get(itemId);

        // Lỗi: Sản phẩm không tồn tại
        if (item == null) return "ERROR: Sản phẩm không tồn tại.";

        // Lỗi: Đấu giá khi phiên đã đóng (Ảnh 3.1.5)
        if (!"RUNNING".equals(item.getStatus())) {
            return "ERROR: Phiên đấu giá đang ở trạng thái: " + item.getStatus() + ". Không thể đặt giá.";
        }

        // Lỗi: Đặt giá thấp hơn giá hiện tại (Ảnh 3.1.5)
        if (bidAmount <= item.getCurrentPrice()) {
            return "ERROR: Giá đặt " + bidAmount + " phải cao hơn giá hiện tại " + item.getCurrentPrice();
        }

        // Cập nhật người dẫn đầu (Ảnh 3.1.3)
        item.setCurrentPrice(bidAmount);
        item.setHighestBidderId(bidderId);
        return "SUCCESS: Bạn đã đặt giá thành công cho " + item.getName();
    }

    // --- 3.1.4: KẾT THÚC PHIÊN ĐẤU GIÁ ---
    public void processAutoEnd() {
        LocalDateTime now = LocalDateTime.now();
        for (Item item : auctionItems.values()) {
            // Tự động đóng phiên khi hết thời gian
            if ("RUNNING".equals(item.getStatus()) && now.isAfter(item.getEndTime())) {

                // Chuyển trạng thái: RUNNING -> FINISHED (Ảnh 3.1.4)
                item.setStatus("FINISHED");

                // Xác định người thắng cuộc
                if (item.getHighestBidderId() != -1) {
                    System.out.println("KẾT THÚC: Sản phẩm " + item.getName() + " đã có người thắng cuộc!");
                } else {
                    System.out.println("KẾT THÚC: Sản phẩm " + item.getName() + " không có người đặt giá.");
                }
            }
        }
    }

    // Chuyển đổi trạng thái thủ công (Ảnh 3.1.4)
    public void changeStatus(int itemId, String status) {
        Item item = auctionItems.get(itemId);
        if (item != null) {
            // Logic: OPEN -> RUNNING -> FINISHED -> PAID / CANCELED
            item.setStatus(status);
        }
    }
}