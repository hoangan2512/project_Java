package server.repository;

import model.User;
import org.mindrot.jbcrypt.BCrypt; 

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.ArrayList;

public class UserRepository {
    private static final Logger LOGGER = Logger.getLogger(UserRepository.class.getName());

    public boolean isUserExists(String username) {
        // language=SQLite
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // Trả về true nếu có ít nhất 1 dòng (tức là user đã tồn tại)
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if user exists: " + username, e);
            return true; // Giả sử tồn tại để tránh tạo mới nếu có lỗi DB
        }
    }

    public boolean addUser(User user) {
        // language=SQLite
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getName());

            // Băm mật khẩu (đã được giải mã RSA từ Client gửi lên) trước khi lưu
            String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(10));
            pstmt.setString(2, hashedPassword);

            pstmt.setString(3, user.getRole());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding user to the database", e);
            return false;
        }
    }

    public User login(String username, String rawPassword) {
        // Bỏ điều kiện AND password = ?. Chỉ tìm kiếm dựa trên username
        // language=SQLite
        String sql = "SELECT * FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                // Nếu tìm thấy user trong DB
                if (rs.next()) {
                    // Kiểm tra xem tài khoản có bị khóa không
                    String status = rs.getString("status");
                    if ("BANNED".equalsIgnoreCase(status)) {
                        LOGGER.log(Level.WARNING, "Tài khoản đang bị khóa: ''{0}''", username);
                        User bannedUser = new User();
                        bannedUser.setStatus("BANNED");
                        return bannedUser; // Trả về một user rỗng chỉ có status là BANNED để controller xử lý
                    }

                    String storedHashedPassword = rs.getString("password");

                    // Dùng BCrypt để kiểm tra xem mật khẩu thô gửi lên có khớp với chuỗi băm trong DB không
                    if (BCrypt.checkpw(rawPassword, storedHashedPassword)) {
                        String role = rs.getString("role");
                        User loggedInUser = User.createUser(
                                role,
                                rs.getInt("id"),
                                rs.getString("username"),
                                storedHashedPassword
                        );
                        loggedInUser.setStatus(status);

                        // Log a successful authentication attempt
                        LOGGER.log(Level.INFO, "Authentication successful for user: ''{0}'' with role: {1}", new Object[]{username, role});
                        return loggedInUser;
                    } else {
                        // Nhập sai mật khẩu
                        LOGGER.log(Level.WARNING, "Sai mật khẩu cho user: ''{0}''", username);
                    }
                } else {
                    // Không tìm thấy username
                    LOGGER.log(Level.WARNING, "Không tìm thấy user: ''{0}''", username);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error during login query for user: " + username, e);
        }

        // Đăng nhập thất bại trả về null
        return null;
    }
    
    // ==========================================
    // CÁC HÀM DÀNH CHO ADMIN QUẢN LÝ USER
    // ==========================================
    
    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();
        // language=SQLite
        String sql = "SELECT id, username, role, status FROM users";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setName(rs.getString("username"));
                user.setRole(rs.getString("role"));
                user.setStatus(rs.getString("status"));
                userList.add(user);
            }
        } catch (SQLException e) {
             LOGGER.log(Level.SEVERE, "Lỗi khi lấy danh sách người dùng", e);
        }
        return userList;
    }
    
    public boolean updateUserStatus(int userId, String status) {
        // language=SQLite
        String sql = "UPDATE users SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setString(1, status);
            pstmt.setInt(2, userId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
             LOGGER.log(Level.SEVERE, "Lỗi khi cập nhật trạng thái user ID " + userId, e);
             return false;
        }
    }

    public Map<Integer, String> getUsernamesByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, String> usernameMap = new HashMap<>();
        String inSql = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        // language=SQLite
        String sql = "SELECT id, username FROM users WHERE id IN (" + inSql + ")";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) {
                pstmt.setInt(i + 1, ids.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    usernameMap.put(rs.getInt("id"), rs.getString("username"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting usernames by ids", e);
        }

        return usernameMap;
    }
}