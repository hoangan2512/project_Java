package server.repository;
//Repository là nơi duy nhất nói chuyện với Database, vì vậy mọi phép tính toán hay truy vấn về "tiền" (balance) phải được thực hiện tại đây để đảm bảo tính đóng gói của OOP.
import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserRepository {
    public boolean addUser(User user) {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getRole());


            int rowsAffected = pstmt.executeUpdate();

            return rowsAffected > 0;

        } catch (Exception e) {
            System.out.println("Lỗi khi thêm User vào Database!");
            e.printStackTrace();
            return false;
        }
    }

    public User login(String username, String password) {

        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role");
                User loggedInUser = User.createUser(
                        role,
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password")
                    );

                System.out.println("Đăng nhập thành công: " + username);
                return loggedInUser;
            }

        } catch (Exception e) {
            System.out.println("Lỗi truy vấn khi đăng nhập!");
            e.printStackTrace();
        }

        System.out.println("Sai tên đăng nhập hoặc mật khẩu: " + username);
        return null;
    }
    // Phương thức lấy số dư từ Database
    public double getBalance(int userId) {
        String sql = "SELECT balance FROM users WHERE id = ?"; // id được lấy từ database sẽ được thay cho dấu ?(là dấu giữ chỗ tham số)
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("balance");
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi truy vấn số dư cho User ID: " + userId);
            e.printStackTrace();
        }
        return -1.0; // Trả về -1 để báo hiệu có lỗi xảy ra
    }
    // Phương thức nạp tiền (Cập nhật số dư cộng dồn)
    public boolean updateBalance(int userId, double amount) {
        // Sử dụng balance = balance + ? để đảm bảo tính nhất quán dữ liệu
        String sql = "UPDATE users SET balance = balance + ? WHERE id = ?";
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, amount);
            pstmt.setInt(2, userId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (Exception e) {
            System.err.println("Lỗi khi cập nhật số dư cho User ID: " + userId);
            e.printStackTrace();
            return false;
        }
    }
}