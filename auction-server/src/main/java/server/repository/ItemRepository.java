package server.repository;

import model.Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ItemRepository {

    public boolean addItem(Item item) {
        // Đã xóa created_at và điều chỉnh còn đúng 11 dấu ?
        String sql = "INSERT INTO items (name, description, starting_price, seller_id, imgpath, imgpath1, imgpath2, imgpath3, imgpath4, imgpath5, imgpath6) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Set 4 giá trị cơ bản
            pstmt.setString(1, item.getName());
            pstmt.setString(2, item.getDescription());
            pstmt.setDouble(3, item.getStarting_price());
            pstmt.setInt(4, item.getSeller_id());

            // Đôn số thứ tự của các ảnh lên bắt đầu từ số 5
            pstmt.setString(5, item.getImgPath());
            pstmt.setString(6, item.getImgPath1());
            pstmt.setString(7, item.getImgPath2());
            pstmt.setString(8, item.getImgPath3());
            pstmt.setString(9, item.getImgPath4());
            pstmt.setString(10, item.getImgPath5());
            pstmt.setString(11, item.getImgPath6());

            int rows = pstmt.executeUpdate();
            return rows > 0;

        } catch (Exception e) {
            System.err.println("Lỗi khi thêm sản phẩm: " + e.getMessage());
            e.printStackTrace();
            return false;
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

                itemList.add(currentItem);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy danh sách sản phẩm: " + e.getMessage());
            e.printStackTrace();
        }
        return itemList;
    }
}