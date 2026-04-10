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
        String sql = "INSERT INTO items (name, description, starting_price, seller_id, created_at) VALUES (?, ?, ?, ?, ?)";
        Connection conn = DatabaseConnection.getInstance();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item.getName());
            pstmt.setString(2, item.getDescription());
            pstmt.setDouble(3, item.getStarting_price());
            pstmt.setInt(4, item.getSeller_id());
            pstmt.setTimestamp(5, java.sql.Timestamp.valueOf(item.getCreated_at()));

            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (Exception e) {
            System.out.println("Lỗi khi thêm sản phẩm");
            e.printStackTrace();
            return false;
        }
    }
    public List<Item> getAllItems(){
        List<Item> itemList = new ArrayList<>();
        String sql = "SELECT * FROM items";
        Connection conn = DatabaseConnection.getInstance();
        try(PreparedStatement pstmt = conn.prepareStatement(sql)){

            ResultSet rs = pstmt.executeQuery();
            while(rs.next()){
                Item currentItem = new Item();

                currentItem.setId(rs.getInt("id"));
                currentItem.setName(rs.getString("name"));
                currentItem.setDescription(rs.getString("description"));
                currentItem.setStarting_price(rs.getDouble("starting_price"));
                currentItem.setSeller_id(rs.getInt("seller_id"));
                LocalDateTime thoiGianTao = rs.getTimestamp("created_at").toLocalDateTime();
                currentItem.setCreated_at(thoiGianTao);

                itemList.add(currentItem);
                }
            } catch (Exception e) {
                System.out.println("Lỗi khi lấy sản phẩm");
                e.printStackTrace();
        }
            return itemList;
        }
    }

