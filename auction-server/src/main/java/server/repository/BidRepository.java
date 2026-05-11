package server.repository;

import model.Bid; 

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class BidRepository {

    public boolean placeBid(Bid bid) {
        String sql = "INSERT INTO bids (auction_id, bidder_id, amount, bid_time) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setInt(1, bid.getAuction_id());
            pstmt.setInt(2, bid.getBidder_id());
            pstmt.setDouble(3, bid.getAmount());

            // Ép kiểu thời gian từ LocalDateTime sang Timestamp cho SQLite
            pstmt.setTimestamp(4, Timestamp.valueOf(bid.getBid_time()));

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (Exception e) {
            System.out.println("Lỗi khi lưu lượt trả giá vào Database!");
            e.printStackTrace();
            return false;
        }
    }

    // TÌM NGƯỜI ĐANG TRẢ GIÁ CAO NHẤT HIỆN TẠI (Để kiểm tra lúc đấu giá)
    public Bid getHighestBid(int auctionId) {
        // Tuyệt chiêu SQL: Sắp xếp giá giảm dần (DESC) và chỉ lấy 1 dòng đầu tiên (LIMIT 1)
        String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY amount DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setInt(1, auctionId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Bid highestBid = new Bid();
                highestBid.setId(rs.getInt("id"));
                highestBid.setAuction_id(rs.getInt("auction_id"));
                highestBid.setBidder_id(rs.getInt("bidder_id"));
                highestBid.setAmount(rs.getDouble("amount"));

                // Ép ngược từ Timestamp dưới DB lên lại LocalDateTime cho Java
                highestBid.setBid_time(rs.getTimestamp("bid_time").toLocalDateTime());

                return highestBid;
            }
        } catch (Exception e) {
            System.out.println("Lỗi khi tìm giá cao nhất!");
            e.printStackTrace();
        }

        // Sẽ trả về null nếu sản phẩm này chưa có ai trả giá (Vẫn giữ nguyên giá khởi điểm)
        return null;
    }

    // 3. LẤY LỊCH SỬ TRẢ GIÁ CỦA 1 SẢN PHẨM (Để hiển thị lên bảng xếp hạng)
    public List<Bid> getBidHistory(int auctionId) {
        List<Bid> history = new ArrayList<>();

        // Sắp xếp theo thời gian mới nhất lên đầu
        String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY bid_time DESC";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setInt(1, auctionId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Bid bid = new Bid();
                bid.setId(rs.getInt("id"));
                bid.setAuction_id(rs.getInt("auction_id"));
                bid.setBidder_id(rs.getInt("bidder_id"));
                bid.setAmount(rs.getDouble("amount"));
                bid.setBid_time(rs.getTimestamp("bid_time").toLocalDateTime());

                history.add(bid);
            }
        } catch (Exception e) {
            System.out.println("Lỗi khi lấy lịch sử trả giá!");
            e.printStackTrace();
        }
        return history;
    }
}
