package server.repository;

import model.Auction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AuctionRepository {

    public boolean createAuction(Auction auction){
        String sql = "INSERT INTO auctions (item_id, start_time, end_time, status, current_price, highest_bidder_id) VALUES(?, ?, ?, ?, ?, ?)";
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, auction.getItem_id());
            pstmt.setTimestamp(2, Timestamp.valueOf(auction.getStart_time()));
            pstmt.setTimestamp(3, Timestamp.valueOf(auction.getEnd_time()));
            pstmt.setString(4, auction.getStatus());
            pstmt.setDouble(5, auction.getCurrent_price());

            if (auction.getHighest_bidder_id() > 0) {
                pstmt.setInt(6, auction.getHighest_bidder_id());
            } else {
                pstmt.setNull(6, java.sql.Types.INTEGER);
            }

            int rows = pstmt.executeUpdate();
            return rows > 0;

        } catch (Exception e) {
            System.err.println("Lỗi khi tạo phiên đấu giá: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<Auction> getActiveAuctions(){
        List<Auction> activeAuctions = new ArrayList<>();
        // Lưu ý: Ở Service bạn đang dùng chữ "RUNNING", nếu Database bạn lưu là "ACTIVE"
        // thì nhớ đồng nhất 1 loại chữ thôi nhé (ví dụ: đổi "ACTIVE" thành "RUNNING" ở đây)
        String sql = "SELECT * FROM auctions WHERE status = 'RUNNING'";
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement psmt = conn.prepareStatement(sql)) {
            ResultSet rs = psmt.executeQuery();

            while(rs.next()){
                Auction auction = new Auction();

                auction.setId(rs.getInt("id"));
                auction.setItem_id(rs.getInt("item_id"));
                auction.setStart_time((rs.getTimestamp("start_time").toLocalDateTime()));
                auction.setEnd_time((rs.getTimestamp("end_time").toLocalDateTime()));
                auction.setStatus(rs.getString("status"));
                auction.setCurrent_price(rs.getDouble("current_price"));
                auction.setHighest_bidder_id(rs.getInt("highest_bidder_id"));

                activeAuctions.add(auction);
            }

        } catch (Exception e) {
            System.err.println("Lỗi khi lấy danh sách đấu giá đang diễn ra: " + e.getMessage());
            e.printStackTrace();
        }
        return activeAuctions;
    }

    // ==========================================
    // CÁC HÀM UPDATE ĐỂ PHỤC VỤ CHO AUCTION SERVICE
    // ==========================================

    // 1. Cập nhật giá thầu và người dẫn đầu mới
    public boolean updateBid(int auctionId, double newPrice, int bidderId) {
        String sql = "UPDATE auctions SET current_price = ?, highest_bidder_id = ? WHERE id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, newPrice);
            pstmt.setInt(2, bidderId);
            pstmt.setInt(3, auctionId);

            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (Exception e) {
            System.err.println("Lỗi khi cập nhật giá thầu xuống DB: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // 2. Cập nhật trạng thái của phiên đấu giá (Ví dụ: Chuyển sang FINISHED)
    public boolean updateStatus(int auctionId, String status) {
        String sql = "UPDATE auctions SET status = ? WHERE id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, auctionId);

            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (Exception e) {
            System.err.println("Lỗi khi cập nhật trạng thái phiên đấu giá: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}