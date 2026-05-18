package server.repository;

import model.Auction;
import model.Item;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemRepository {

    /**
     * Thêm sản phẩm mới và trả về ID (Primary Key) do DB tự sinh
     */
    public int addItem(Item item) {
        // language=SQLite
        String sql = "INSERT INTO items (user_prdID, name, description, starting_price, seller_id, imgpath, imgpath1, imgpath2, imgpath3, imgpath4, imgpath5, imgpath6, categories) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        // SỬA LỖI: Dùng try-with-resources cho Connection
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, item.getUser_prdID());
            pstmt.setString(2, item.getName());
            pstmt.setString(3, item.getDescription());
            pstmt.setDouble(4, item.getStarting_price());
            pstmt.setInt(5, item.getSeller_id());
            pstmt.setString(6, item.getImgPath());
            pstmt.setString(7, item.getImgPath1());
            pstmt.setString(8, item.getImgPath2());
            pstmt.setString(9, item.getImgPath3());
            pstmt.setString(10, item.getImgPath4());
            pstmt.setString(11, item.getImgPath5());
            pstmt.setString(12, item.getImgPath6());
            pstmt.setString(13, item.getCategories());

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            return -1;
        } catch (Exception e) {
            System.err.println("Lỗi khi thêm sản phẩm: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
     // Lấy toàn bộ danh sách sản phẩm (Dùng cho các mục đích quản lý/hiển thị chung)
     // Kiểm tra trùng lặp mã sản phẩm hoặc tên sản phẩm trong cùng danh mục
    public String checkProductConflicts(String name, String categories, String userPrdId, int sellerId) {
        // 1. KIỂM TRA TRÙNG ID
        if (userPrdId != null && !userPrdId.trim().isEmpty()) {
            // language=SQLite
            String sqlId = "SELECT id FROM items WHERE user_prdID = ? AND seller_id = ?";
            // SỬA LỖI: Dùng try-with-resources cho Connection
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlId)) {
                ps.setString(1, userPrdId.trim());
                ps.setInt(2, sellerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return "DUPLICATE_ID";
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        // 2. KIỂM TRA TRÙNG TÊN + DANH MỤC
        // language=SQLite
        String sqlNameCat = "SELECT user_prdID FROM items WHERE LOWER(name) = LOWER(?) AND categories = ? AND seller_id = ? LIMIT 1";
        // SỬA LỖI: Dùng try-with-resources cho Connection
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlNameCat)) {
            ps.setString(1, name);
            ps.setString(2, categories);
            ps.setInt(3, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return "DUPLICATE_NAME_CAT:" + rs.getString("user_prdID");
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        return "OK";
    }
    
    // ==========================================
    // CÁC HÀM DÀNH CHO ADMIN QUẢN LÝ ITEM
    // ==========================================

    public List<Auction> getAllAuctionsWithItems() {
        List<Auction> auctionList = new ArrayList<>();
        // Chọn rõ các cột hoặc dùng alias để tránh trùng tên 'id'
        String sql = "SELECT a.id AS auction_id, a.current_price, a.status, a.start_time, a.end_time, " +
                "i.id AS item_id, i.user_prdID, i.name, i.description, i.starting_price, " +
                "i.seller_id, i.imgpath, i.categories, i.moderation_status " +
                "FROM auctions a INNER JOIN items i ON a.item_id = i.id";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                // 1. Đọc dữ liệu bảng items
                Item currentItem = new Item();
                currentItem.setId(rs.getInt("item_id"));
                currentItem.setUser_prdID(rs.getString("user_prdID"));
                currentItem.setName(rs.getString("name"));
                currentItem.setDescription(rs.getString("description"));
                currentItem.setStarting_price(rs.getDouble("starting_price"));
                currentItem.setSeller_id(rs.getInt("seller_id"));
                currentItem.setImgPath(rs.getString("imgpath"));
                currentItem.setCategories(rs.getString("categories"));
                currentItem.setModeration_status(rs.getString("moderation_status"));

                // 2. Đọc dữ liệu bảng auctions
                Auction auction = new Auction();
                auction.setId(rs.getInt("auction_id"));

                // Gắn Item vào trong Auction
                auction.setItem(currentItem);

                // Thêm Auction vào danh sách trả về
                auctionList.add(auction);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy danh sách đấu giá: " + e.getMessage());
        }
        return auctionList;
    }

    public Map<String, Integer> countItemsBySellerId(int sellerId) {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("active", 0);
        counts.put("rejected", 0);

        String sql = "SELECT moderation_status, COUNT(id) FROM items WHERE seller_id = ? GROUP BY moderation_status";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, sellerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String status = rs.getString(1);
                    int count = rs.getInt(2);
                    if ("APPROVED".equalsIgnoreCase(status)) {
                        counts.put("active", counts.get("active") + count);
                    } else if ("REJECTED".equalsIgnoreCase(status)) {
                        counts.put("rejected", counts.get("rejected") + count);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return counts;
    }
}
