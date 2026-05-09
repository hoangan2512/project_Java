package server.repository;

import model.User;

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

public class UserRepository {
    private static final Logger LOGGER = Logger.getLogger(UserRepository.class.getName());

    public boolean isUserExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        // KHÔNG dùng try-with-resources cho Connection ở đây vì nó sẽ đóng connection chung của DatabaseConnection
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getRole());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding user to the database", e);
            return false;
        }
    }

    public User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        // KHÔNG dùng try-with-resources cho Connection ở đây
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("role");
                    User loggedInUser = User.createUser(
                            role,
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password")
                    );

                    // Log a successful authentication attempt
                    LOGGER.log(Level.INFO, "Authentication successful for user: ''{0}'' with role: {1}", new Object[]{username, role});
                    return loggedInUser;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error during login query for user: " + username, e);
        }

        // If we reach here, it means login failed (user not found or password mismatch)
        LOGGER.log(Level.WARNING, "Authentication failed for user: ''{0}''", username);
        return null;
    }

    public User getUserById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("role");
                    return User.createUser(
                            role,
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password")
                    );
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting user by id: " + id, e);
        }
        return null;
    }
    
    public Map<Integer, String> getUsernamesByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<Integer, String> usernameMap = new HashMap<>();
        String inSql = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT id, username FROM users WHERE id IN (" + inSql + ")";
        
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
