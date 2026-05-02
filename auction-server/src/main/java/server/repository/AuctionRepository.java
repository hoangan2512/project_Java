package server.repository;

import model.Auction;
import model.Item;
import model.SearchCriteria;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
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
    
    public List<Auction> getWaitingAuctions(){
        List<Auction> waitingAuctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE status = 'WAITING'";
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

                waitingAuctions.add(auction);
            }

        } catch (Exception e) {
            System.err.println("Lỗi khi lấy danh sách đấu giá đang chờ: " + e.getMessage());
            e.printStackTrace();
        }
        return waitingAuctions;
    }

    // ==========================================
    // CÁC HÀM UPDATE ĐỂ PHỤC VỤ CHO AUCTION SERVICE
    // ==========================================

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

    public List<Auction> searchAdvanced(SearchCriteria criteria) {
        List<Auction> resultList = new ArrayList<>();

        // Sử dụng bí danh (alias) rõ ràng: a cho auctions, i cho items
        // Đảm bảo chọn đúng các cột cần thiết để tránh xung đột tên
        StringBuilder sql = new StringBuilder(
                "SELECT a.id, a.item_id, a.start_time, a.end_time, a.status, a.current_price, a.highest_bidder_id, " +
                        "i.name, i.categories, i.imgPath " +
                        "FROM auctions a " +
                        "INNER JOIN items i ON a.item_id = i.id " +
                        "WHERE 1=1 "
        );

        List<Object> parameters = new ArrayList<>();

        // 1. Lọc theo ID Phiên đấu giá
        if (criteria.getAuctionId() != null && !criteria.getAuctionId().isEmpty()) {
            sql.append(" AND a.id = ? ");
            parameters.add(criteria.getAuctionId());
        }

        // 2. Lọc theo Trạng thái (Bảng auctions)
        if (criteria.getStatuses() != null && !criteria.getStatuses().isEmpty()) {
            String inSql = String.join(",", Collections.nCopies(criteria.getStatuses().size(), "?"));
            sql.append(" AND a.status IN (").append(inSql).append(") ");
            parameters.addAll(criteria.getStatuses());
        }

        // 3. Lọc theo GIÁ HIỆN TẠI (Bảng auctions)
        // Đây là nơi xử lý lỗi "no such column: current_price" bằng cách chỉ định rõ a.current_price
        if (criteria.getMinPrice() > 0) {
            sql.append(" AND a.current_price >= ? ");
            parameters.add(criteria.getMinPrice());
        }
        if (criteria.getMaxPrice() > 0) {
            sql.append(" AND a.current_price <= ? ");
            parameters.add(criteria.getMaxPrice());
        }

        // 4. Lọc theo Danh mục (Bảng items)
        if (criteria.getCategories() != null && !criteria.getCategories().isEmpty()) {
            String inSql = String.join(",", Collections.nCopies(criteria.getCategories().size(), "?"));
            sql.append(" AND i.categories IN (").append(inSql).append(") ");
            parameters.addAll(criteria.getCategories());
        }

        System.out.println("SQL đang thực thi: " + sql.toString());

        // Sử dụng connection từ instance để đảm bảo không bị lỗi Close
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                pstmt.setObject(i + 1, parameters.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Auction auction = new Auction();

                    // Map dữ liệu từ bảng Auctions
                    auction.setId(rs.getInt("id"));
                    auction.setItem_id(rs.getInt("item_id"));

                    Timestamp start = rs.getTimestamp("start_time");
                    if (start != null) auction.setStart_time(start.toLocalDateTime());

                    Timestamp end = rs.getTimestamp("end_time");
                    if (end != null) auction.setEnd_time(end.toLocalDateTime());

                    auction.setStatus(rs.getString("status"));
                    auction.setCurrent_price(rs.getDouble("current_price"));
                    auction.setHighest_bidder_id(rs.getInt("highest_bidder_id"));

                    // Map dữ liệu từ bảng Items
                    Item item = new Item();
                    item.setId(rs.getInt("item_id"));
                    item.setName(rs.getString("name"));
                    item.setImgPath(rs.getString("imgPath"));
                    item.setCategories(rs.getString("categories"));

                    // Gắn item vào auction
                    auction.setItem(item);

                    resultList.add(auction);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi SQL thực tế: " + e.getMessage());
            e.printStackTrace();
        }

        return resultList;
    }
}