package server.repository;

import model.Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ItemRepository {

    public int addItem(Item item) {
        // 1. Thêm user_prdID vào câu lệnh SQL và tăng lên 12 dấu chấm hỏi (?)
        String sql = "INSERT INTO items (user_prdID, name, description, starting_price, seller_id, imgpath, imgpath1, imgpath2, imgpath3, imgpath4, imgpath5, imgpath6, categories) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?)";
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            // 2. Set giá trị mã sản phẩm vào vị trí số 1
            pstmt.setString(1, item.getUser_prdID());

            // 3. Các giá trị khác bị đẩy lùi xuống 1 số so với code cũ
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
                // Lấy ID tự động tăng (Primary Key) của Database
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
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
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Item currentItem = new Item();

                currentItem.setId(rs.getInt("id"));
                currentItem.setName(rs.getString("name"));
                currentItem.setDescription(rs.getString("description"));
                currentItem.setStarting_price(rs.getDouble("starting_price"));
                currentItem.setSeller_id(rs.getInt("seller_id"));

                // Lấy các đường dẫn ảnh
                currentItem.setImgPath(rs.getString("imgpath"));
                currentItem.setImgPath1(rs.getString("imgpath1"));
                currentItem.setImgPath2(rs.getString("imgpath2"));
                currentItem.setImgPath3(rs.getString("imgpath3"));
                currentItem.setImgPath4(rs.getString("imgpath4"));
                currentItem.setImgPath5(rs.getString("imgpath5"));
                currentItem.setImgPath6(rs.getString("imgpath6"));
                currentItem.setCategories(rs.getString("categories"));

                itemList.add(currentItem);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy danh sách sản phẩm: " + e.getMessage());
            e.printStackTrace();
        }
        return itemList;
    }

    public String checkProductConflicts(String name, String categories, String userPrdId, int sellerId) {
        Connection conn = DatabaseConnection.getInstance().getConnection();

        // 1. KIỂM TRA TRÙNG ID (Chặn cứng trong mọi trường hợp)
        if (userPrdId != null && !userPrdId.trim().isEmpty()) {
            String sqlId = "SELECT id FROM items WHERE user_prdID = ? AND seller_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlId)) {
                ps.setString(1, userPrdId.trim());
                ps.setInt(2, sellerId);
                if (ps.executeQuery().next()) {
                    return "DUPLICATE_ID"; // Trả về lỗi trùng ID
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        // 2. KIỂM TRA TRÙNG TÊN + DANH MỤC (Cảnh báo mềm)
        String sqlNameCat = "SELECT user_prdID FROM items WHERE LOWER(name) = LOWER(?) AND categories = ? AND seller_id = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sqlNameCat)) {
            ps.setString(1, name);
            ps.setString(2, categories);
            ps.setInt(3, sellerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String existingId = rs.getString("user_prdID");
                if (existingId == null) existingId = "Không có mã";
                return "DUPLICATE_NAME_CAT:" + existingId; // Trả về lỗi kèm ID đang bị trùng
            }
        } catch (Exception e) { e.printStackTrace(); }

        return "OK"; // Không trùng gì cả
    }
}