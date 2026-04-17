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
        String sql = "INSERT INTO auctions (item_id, start_time, end_time, status) VALUES(?, ?, ?, ?)";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try(PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setInt(1, auction.getItem_id());
            pstmt.setTimestamp(2, Timestamp.valueOf(auction.getStart_time()));
            pstmt.setTimestamp(3, Timestamp.valueOf(auction.getEnd_time()));
            pstmt.setString(4, auction.getStatus());

            int rows = pstmt.executeUpdate();
            return  rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }
    public List<Auction> getActiveAuctions(){
        List<Auction> activeAuctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE status = 'ACTIVE'";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try(PreparedStatement psmt = conn.prepareStatement(sql)){
            ResultSet rs = psmt.executeQuery();
            while(rs.next()){
                Auction auction = new Auction();
                auction.setId(rs.getInt("id"));
                auction.setStart_time((rs.getTimestamp("start_time").toLocalDateTime()));
                auction.setEnd_time((rs.getTimestamp("end_time").toLocalDateTime()));
                auction.setStatus(rs.getString("status"));

                activeAuctions.add(auction);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return activeAuctions;
    }
}
