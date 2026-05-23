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
        String sql = "INSERT INTO items (user_prdID, name, description, starting_price, seller_id, imgpath, imgpath1, imgpath2, imgpath3, imgpath4, imgpath5, imgpath6, categories, moderation_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
            pstmt.setString(14, "PENDING_APPROVAL");

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

    public List<Item> getAllItems() {
        List<Item> itemList = new ArrayList<>();
        String sql = "SELECT * FROM items";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Item item = new Item();
                item.setId(rs.getInt("id"));
                item.setUser_prdID(rs.getString("user_prdID"));
                item.setName(rs.getString("name"));
                item.setDescription(rs.getString("description"));
                item.setStarting_price(rs.getDouble("starting_price"));
                item.setSeller_id(rs.getInt("seller_id"));
                item.setImgPath(rs.getString("imgpath"));
                item.setCategories(rs.getString("categories"));
                item.setModeration_status(rs.getString("moderation_status"));
                itemList.add(item);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy danh sách sản phẩm: " + e.getMessage());
        }
        return itemList;
    }

    public Item getItemById(int itemId) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Item item = new Item();
                    item.setId(rs.getInt("id"));
                    item.setUser_prdID(rs.getString("user_prdID"));
                    item.setName(rs.getString("name"));
                    item.setDescription(rs.getString("description"));
                    item.setStarting_price(rs.getDouble("starting_price"));
                    item.setSeller_id(rs.getInt("seller_id"));
                    item.setImgPath(rs.getString("imgpath"));
                    item.setCategories(rs.getString("categories"));
                    item.setModeration_status(rs.getString("moderation_status"));
                    return item;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

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

    // CÁC HÀM DÀNH CHO ADMIN QUẢN LÝ ITEM

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

    public boolean updateStatus(int itemId, String newStatus) {
        // language=SQLite
        String sql = "UPDATE items SET moderation_status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus);
            pstmt.setInt(2, itemId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật trạng thái sản phẩm (ID: " + itemId + "): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteItemById(int itemId) {
        // language=SQLite
        String sql = "DELETE FROM items WHERE id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0; // Trả về true nếu có ít nhất 1 dòng bị xóa

        } catch (SQLException e) {
            System.err.println("Lỗi khi xóa sản phẩm (ID: " + itemId + "): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateItemDescription(int itemId, String newDescription) {
        String sql = "UPDATE items SET description = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newDescription);
            pstmt.setInt(2, itemId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0; // Trả về true nếu cập nhật thành công

        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật mô tả sản phẩm (ID: " + itemId + "): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}