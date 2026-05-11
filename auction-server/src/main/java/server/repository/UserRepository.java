package server.repository;

import model.User;
import org.mindrot.jbcrypt.BCrypt; // Bắt buộc phải có thư viện này

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

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

            // ================== THAY ĐỔI Ở ĐÂY ==================
            // Băm mật khẩu (đã được giải mã RSA từ Client gửi lên) trước khi lưu
            String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(10));
            pstmt.setString(2, hashedPassword);
            // ====================================================

            pstmt.setString(3, user.getRole());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding user to the database", e);
            return false;
        }
    }

    public User login(String username, String rawPassword) {
        // ================== THAY ĐỔI Ở ĐÂY ==================
        // Bỏ điều kiện AND password = ?. Chỉ tìm kiếm dựa trên username
        String sql = "SELECT * FROM users WHERE username = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                // Nếu tìm thấy user trong DB
                if (rs.next()) {
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
}