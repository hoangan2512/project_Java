package server.repository;

import model.Item;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * Lấy toàn bộ danh sách sản phẩm (Dùng cho các mục đích quản lý/hiển thị chung)
     */
    public List<Item> getAllItems() {
        List<Item> itemList = new ArrayList<>();
        // language=SQLite
        String sql = "SELECT * FROM items";
        
        // SỬA LỖI: Dùng try-with-resources cho Connection
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
             
            while (rs.next()) {
                Item currentItem = new Item();
                currentItem.setId(rs.getInt("id"));
                currentItem.setName(rs.getString("name"));
                currentItem.setDescription(rs.getString("description"));
                currentItem.setStarting_price(rs.getDouble("starting_price"));
                currentItem.setSeller_id(rs.getInt("seller_id"));
                currentItem.setImgPath(rs.getString("imgpath"));
                currentItem.setImgPath1(rs.getString("imgpath1"));
                currentItem.setImgPath2(rs.getString("imgpath2"));
                currentItem.setImgPath3(rs.getString("imgpath3"));
                currentItem.setImgPath4(rs.getString("imgpath4"));
                currentItem.setImgPath5(rs.getString("imgpath5"));
                currentItem.setImgPath6(rs.getString("imgpath6"));
                currentItem.setCategories(rs.getString("categories"));
                // Cần thêm đọc cột moderation_status nếu Model Item có hỗ trợ
                itemList.add(currentItem);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy danh sách sản phẩm: " + e.getMessage());
        }
        return itemList;
    }

    /**
     * Kiểm tra trùng lặp mã sản phẩm hoặc tên sản phẩm trong cùng danh mục
     */
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
    
    public List<Item> getPendingItems() {
        List<Item> itemList = new ArrayList<>();
        // language=SQLite
        String sql = "SELECT * FROM items WHERE moderation_status = 'PENDING_APPROVAL'";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
             
            while (rs.next()) {
                Item currentItem = new Item();
                currentItem.setId(rs.getInt("id"));
                currentItem.setName(rs.getString("name"));
                currentItem.setDescription(rs.getString("description"));
                currentItem.setStarting_price(rs.getDouble("starting_price"));
                currentItem.setSeller_id(rs.getInt("seller_id"));
                // ... map các fields khác
                itemList.add(currentItem);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy danh sách sản phẩm chờ duyệt: " + e.getMessage());
        }
        return itemList;
    }
    
    public boolean updateItemModerationStatus(int itemId, String status) {
        // language=SQLite
        String sql = "UPDATE items SET moderation_status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setString(1, status); // APPROVED hoặc REJECTED
            pstmt.setInt(2, itemId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
             System.err.println("Lỗi khi duyệt sản phẩm ID " + itemId + ": " + e.getMessage());
             return false;
        }
    }

    public int countItemsBySellerId(int sellerId) {
        String sql = "SELECT COUNT(id) FROM items WHERE seller_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, sellerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1); // Trả về giá trị của cột COUNT
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}