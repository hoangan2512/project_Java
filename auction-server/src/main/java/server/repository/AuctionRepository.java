package server.repository;

import model.Auction;
import model.Item;
import model.SearchCriteria;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Comparator;


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
    
    public Auction getAuctionById(int id) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement psmt = conn.prepareStatement(sql)) {
            psmt.setInt(1, id);
            ResultSet rs = psmt.executeQuery();

            if (rs.next()) {
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

                return auction;
            }

        } catch (Exception e) {
            System.err.println("Lỗi khi lấy thông tin phiên đấu giá: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
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
                        "i.name, i.categories, i.imgPath, i.description " +
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

        // 2. Lọc theo Trạng thái thời gian (Bảng auctions)
        if (criteria.getStatuses() != null && !criteria.getStatuses().isEmpty()) {
            sql.append(" AND (");
            boolean firstStatus = true;
            
            // Tính toán trước các mốc thời gian để truyền vào SQL an toàn
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            Timestamp yesterday = Timestamp.valueOf(now.minusDays(1));
            Timestamp in30Mins = Timestamp.valueOf(now.plusMinutes(30));
            Timestamp in1Hour = Timestamp.valueOf(now.plusHours(1));

            for (String status : criteria.getStatuses()) {
                if (!firstStatus) {
                    sql.append(" OR ");
                }
                
                if ("Bidding".equalsIgnoreCase(status)) {
                    sql.append(" a.status = 'RUNNING' ");
                } else if ("Newly Listed".equalsIgnoreCase(status)) {
                    // Yêu cầu mới: "Newly Listed" sẽ liệt kê tất cả auction WAITING
                    sql.append(" a.status = 'WAITING' ");
                } else if ("Ending Soon".equalsIgnoreCase(status)) {
                    // Sắp kết thúc: Đang chạy và sẽ kết thúc trong vòng 30 phút tới
                    sql.append(" (a.status = 'RUNNING' AND a.end_time <= ?) ");
                    parameters.add(in30Mins);
                } else if ("Upcoming".equalsIgnoreCase(status)) {
                    // Sắp diễn ra: Trạng thái WAITING và sẽ mở trong vòng 1 giờ tới
                    sql.append(" (a.status = 'WAITING' AND a.start_time <= ?) ");
                    parameters.add(in1Hour);
                } else if ("Ended".equalsIgnoreCase(status)) {
                    // Đã kết thúc: Trạng thái FINISHED
                    sql.append(" a.status = 'FINISHED' ");
                } else {
                    // Trường hợp ngoại lệ phòng hờ
                    sql.append(" a.status = ? ");
                    parameters.add(status);
                }
                
                firstStatus = false;
            }
            sql.append(") ");
        } else {
            // Mặc định khi không chọn status nào: Không hiển thị các phiên đã kết thúc
            sql.append(" AND a.status != 'FINISHED' ");
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
        
        // 5. Thêm điều kiện sắp xếp
        // Ưu tiên RUNNING (sắp kết thúc nhất), sau đó đến WAITING (sắp bắt đầu nhất), cuối cùng là các trạng thái khác
        sql.append(" ORDER BY ");
        sql.append("CASE a.status WHEN 'RUNNING' THEN 1 WHEN 'WAITING' THEN 2 ELSE 3 END ASC, ");
        sql.append("CASE a.status WHEN 'RUNNING' THEN a.end_time ELSE a.start_time END ASC");


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
                    item.setDescription(rs.getString("description"));

                    // Gắn item vào auction
                    auction.setItem(item);

                    resultList.add(auction);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi SQL thực tế: " + e.getMessage());
            e.printStackTrace();
        }

        // Lọc bằng binary search trên list nếu có keyword
        if (criteria.getKeyword() != null && !criteria.getKeyword().isEmpty()) {
            return searchByKeywordBinarySearch(resultList, criteria.getKeyword());
        }

        return resultList;
    }

    private List<Auction> searchByKeywordBinarySearch(List<Auction> sourceList, String keyword) {
        if (sourceList == null || sourceList.isEmpty()) {
            return new ArrayList<>();
        }

        String searchKey = keyword.toLowerCase();

        // 1. Sao chép và sắp xếp list theo tên sản phẩm (bắt buộc cho binary search)
        List<Auction> sortedList = new ArrayList<>(sourceList);
        sortedList.sort(Comparator.comparing(a -> a.getItem() != null && a.getItem().getName() != null ? a.getItem().getName().toLowerCase() : ""));

        // 2. Sử dụng Binary Search để tìm index của một phần tử bắt đầu bằng keyword (Prefix Match)
        int left = 0;
        int right = sortedList.size() - 1;
        int foundIndex = -1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            String midName = sortedList.get(mid).getItem() != null && sortedList.get(mid).getItem().getName() != null 
                    ? sortedList.get(mid).getItem().getName().toLowerCase() 
                    : "";

            // Kiểm tra xem nó có bắt đầu bằng searchKey không
            if (midName.startsWith(searchKey)) {
                foundIndex = mid;
                // Nếu ta muốn tìm kiếm prefix (starts with), ta nên tiếp tục tìm về bên trái để lấy phần tử đầu tiên
                right = mid - 1;
            } else if (midName.compareTo(searchKey) < 0) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        List<Auction> result = new ArrayList<>();
        
        // 3. Nếu tìm thấy một phần tử bằng Binary Search (Prefix Match), mở rộng sang phải để lấy tất cả
        if (foundIndex != -1) {
            int i = foundIndex;
            while (i < sortedList.size()) {
                String name = sortedList.get(i).getItem() != null && sortedList.get(i).getItem().getName() != null 
                        ? sortedList.get(i).getItem().getName().toLowerCase() : "";
                if (name.startsWith(searchKey)) {
                    result.add(sortedList.get(i));
                } else {
                    break; // Do đã sort, nếu phần tử tiếp theo không bắt đầu bằng keyword, thì các phần tử sau cũng vậy
                }
                i++;
            }
        }
        
        // 4. Vì yêu cầu thực tế thường là tìm kiếm "chứa" (contains),
        // và Binary Search CHỈ chạy đúng cho tìm kiếm "bắt đầu bằng" (prefix match) trên mảng đã sắp xếp,
        // nếu danh sách Prefix rỗng, ta sử dụng Linear Search dự phòng cho "chứa" (contains) để đảm bảo UX không bị lỗi.
        if (result.isEmpty()) {
            for (Auction a : sourceList) {
                String name = a.getItem() != null && a.getItem().getName() != null ? a.getItem().getName().toLowerCase() : "";
                if (name.contains(searchKey) && !name.startsWith(searchKey)) { // Tránh trùng lặp nếu nó đã được thêm bằng Prefix Match
                    result.add(a);
                }
            }
        } else {
             // Kết hợp thêm các kết quả "contains" (không phải prefix) vào cuối list nếu có kết quả Prefix
             for (Auction a : sourceList) {
                String name = a.getItem() != null && a.getItem().getName() != null ? a.getItem().getName().toLowerCase() : "";
                if (name.contains(searchKey) && !name.startsWith(searchKey)) { 
                    result.add(a);
                }
            }
        }

        return result;
    }
}