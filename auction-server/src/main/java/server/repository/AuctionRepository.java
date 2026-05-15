package server.repository;

import model.Auction;
import model.Item;
import model.SearchCriteria;

import java.io.File;
import java.nio.file.Files;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

/**
 * Repository để quản lý các phiên đấu giá trong cơ sở dữ liệu.
 */
public class AuctionRepository {

    /**
     * Ánh xạ một dòng từ ResultSet sang một đối tượng Auction.
     */
    private Auction mapRowToAuction(ResultSet rs) throws SQLException {
        Auction auction = new Auction();
        auction.setId(rs.getInt("id"));
        auction.setItem_id(rs.getInt("item_id"));

        Timestamp start = rs.getTimestamp("start_time");
        if (start != null) auction.setStart_time(start.toLocalDateTime());

        Timestamp end = rs.getTimestamp("end_time");
        if (end != null) auction.setEnd_time(end.toLocalDateTime());

        auction.setStatus(rs.getString("status"));
        auction.setCurrent_price(rs.getDouble("current_price"));
        auction.setHighest_bidder_id(rs.getInt("highest_bidder_id"));

        Item item = new Item();
        item.setId(rs.getInt("item_id"));
        item.setName(rs.getString("name"));
        item.setCategories(rs.getString("categories"));
        item.setDescription(rs.getString("description"));
        item.setSeller_id(rs.getInt("seller_id"));
        
        String imgPath = rs.getString("imgPath");
        item.setImgPath(imgPath);

        if (imgPath != null && !imgPath.trim().isEmpty()) {
            try {
                File file = new File("src/main/resources" + imgPath);
                if (!file.exists()) {
                    file = new File("auction-server/src/main/resources" + imgPath);
                }
                
                if (file.exists()) {
                    byte[] fileBytes = Files.readAllBytes(file.toPath());
                    item.setImageBytes(fileBytes);
                }
            } catch (Exception e) {
                System.err.println("Không thể đọc file ảnh: " + imgPath);
            }
        }

        auction.setItem(item);
        return auction;
    }

    public boolean createAuction(Auction auction) {
        // language=SQLite
        String sql = "INSERT INTO auctions (item_id, start_time, end_time, status, current_price, highest_bidder_id) VALUES(?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, auction.getItem_id());
            pstmt.setTimestamp(2, Timestamp.valueOf(auction.getStart_time()));
            pstmt.setTimestamp(3, Timestamp.valueOf(auction.getEnd_time()));
            pstmt.setString(4, auction.getStatus());
            pstmt.setDouble(5, auction.getCurrent_price());
            pstmt.setObject(6, auction.getHighest_bidder_id() > 0 ? auction.getHighest_bidder_id() : null);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi khi tạo phiên đấu giá: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public Auction getAuctionById(int id) {
        // language=SQLite
        String sql = "SELECT a.*, i.name, i.categories, i.imgPath, i.description, i.seller_id " +
                     "FROM auctions a JOIN items i ON a.item_id = i.id WHERE a.id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToAuction(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy thông tin phiên đấu giá ID " + id + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private List<Auction> getAuctionsByStatus(String status) {
        List<Auction> auctions = new ArrayList<>();
        // language=SQLite
        String sql = "SELECT a.*, i.name, i.categories, i.imgPath, i.description, i.seller_id " +
                     "FROM auctions a JOIN items i ON a.item_id = i.id WHERE a.status = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    auctions.add(mapRowToAuction(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy danh sách đấu giá có trạng thái " + status + ": " + e.getMessage());
            e.printStackTrace();
        }
        return auctions;
    }

    public List<Auction> getActiveAuctions() {
        return getAuctionsByStatus("RUNNING");
    }

    public List<Auction> getWaitingAuctions() {
        return getAuctionsByStatus("WAITING");
    }

    public boolean updateBid(int auctionId, double newPrice, int bidderId) {
        // language=SQLite
        String sql = "UPDATE auctions SET current_price = ?, highest_bidder_id = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, newPrice);
            pstmt.setInt(2, bidderId);
            pstmt.setInt(3, auctionId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật giá thầu cho auction ID " + auctionId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateStatus(int auctionId, String status) {
        // language=SQLite
        String sql = "UPDATE auctions SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, auctionId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật trạng thái cho auction ID " + auctionId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // --- Bổ sung hàm cho Anti-Sniping ---
    public boolean updateEndTime(int auctionId, LocalDateTime newEndTime) {
        // language=SQLite
        String sql = "UPDATE auctions SET end_time = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(newEndTime));
            pstmt.setInt(2, auctionId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi gia hạn thời gian cho auction ID " + auctionId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<Auction> searchAdvanced(SearchCriteria criteria) {
        List<Auction> resultList = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT a.id, a.item_id, a.start_time, a.end_time, a.status, a.current_price, a.highest_bidder_id, " +
            "i.name, i.categories, i.imgPath, i.description, i.seller_id " +
            "FROM auctions a INNER JOIN items i ON a.item_id = i.id WHERE 1=1"
        );
        List<Object> parameters = new ArrayList<>();

        buildSearchQuery(criteria, sql, parameters);

        sql.append(" ORDER BY CASE a.status WHEN 'RUNNING' THEN 1 WHEN 'WAITING' THEN 2 ELSE 3 END ASC, ")
           .append("CASE a.status WHEN 'RUNNING' THEN a.end_time ELSE a.start_time END ASC");

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                pstmt.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    resultList.add(mapRowToAuction(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi thực hiện tìm kiếm nâng cao: " + e.getMessage());
            e.printStackTrace();
        }

        if (criteria.getKeyword() != null && !criteria.getKeyword().isEmpty()) {
            return filterByKeyword(resultList, criteria.getKeyword());
        }

        return resultList;
    }

    private void buildSearchQuery(SearchCriteria criteria, StringBuilder sql, List<Object> parameters) {
        if (criteria.getAuctionId() != null && !criteria.getAuctionId().isEmpty()) {
            sql.append(" AND a.id = ?");
            parameters.add(criteria.getAuctionId());
            return;
        }

        if (criteria.getStatuses() != null && !criteria.getStatuses().isEmpty()) {
            sql.append(" AND (");
            boolean firstStatus = true;
            for (String status : criteria.getStatuses()) {
                if (!firstStatus) sql.append(" OR ");
                
                if ("Bidding".equalsIgnoreCase(status)) {
                    sql.append("a.status = 'RUNNING'");
                } else if ("Newly Listed".equalsIgnoreCase(status)) {
                    sql.append("(a.status = 'WAITING' AND a.start_time > ?)");
                    parameters.add(Timestamp.valueOf(java.time.LocalDateTime.now().plusHours(1)));
                } else if ("Upcoming".equalsIgnoreCase(status)) {
                    sql.append("(a.status = 'WAITING' AND a.start_time <= ?)");
                    parameters.add(Timestamp.valueOf(java.time.LocalDateTime.now().plusHours(1)));
                } else if ("Ending Soon".equalsIgnoreCase(status)) {
                    sql.append("(a.status = 'RUNNING' AND a.end_time <= ?)");
                    parameters.add(Timestamp.valueOf(java.time.LocalDateTime.now().plusHours(1)));
                } else if ("FINISHED".equalsIgnoreCase(status) || "Ended".equalsIgnoreCase(status)) {
                    sql.append("a.status = 'FINISHED'");
                } else {
                    sql.append("a.status = ?");
                    parameters.add(status);
                }
                firstStatus = false;
            }
            sql.append(")");
        } else {
            sql.append(" AND a.status != 'FINISHED'");
        }

        if (criteria.getMinPrice() > 0) {
            sql.append(" AND a.current_price >= ?");
            parameters.add(criteria.getMinPrice());
        }
        if (criteria.getMaxPrice() > 0) {
            sql.append(" AND a.current_price <= ?");
            parameters.add(criteria.getMaxPrice());
        }

        if (criteria.getCategories() != null && !criteria.getCategories().isEmpty()) {
            String inSql = String.join(",", Collections.nCopies(criteria.getCategories().size(), "?"));
            sql.append(" AND i.categories IN (").append(inSql).append(")");
            parameters.addAll(criteria.getCategories());
        }
    }

    private List<Auction> filterByKeyword(List<Auction> sourceList, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return sourceList;
        }
        String finalKeyword = keyword.toLowerCase().trim();
        return sourceList.stream()
            .filter(auction -> auction.getItem() != null &&
                               auction.getItem().getName() != null &&
                               auction.getItem().getName().toLowerCase().contains(finalKeyword))
            .collect(Collectors.toList());
    }
    
    // ==========================================
    // CÁC HÀM DÀNH CHO ADMIN QUẢN LÝ PHIÊN ĐẤU GIÁ
    // ==========================================
    
    /**
     * Dừng ngay lập tức một phiên đấu giá (Chuyển trạng thái sang CANCELLED hoặc FINISHED).
     * Phục vụ cho chức năng Giám sát của Admin khi phát hiện vi phạm.
     */
    public boolean stopAuction(int auctionId) {
        // Sử dụng trạng thái 'CANCELLED' để phân biệt với 'FINISHED' (kết thúc bình thường)
        // Nếu DB của bạn chỉ cho phép 'WAITING', 'RUNNING', 'FINISHED' thì dùng 'FINISHED'
        // language=SQLite
        String sql = "UPDATE auctions SET status = 'FINISHED' WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setInt(1, auctionId);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
             System.err.println("Lỗi khi Admin dừng phiên đấu giá ID " + auctionId + ": " + e.getMessage());
             e.printStackTrace();
             return false;
        }
    }
}