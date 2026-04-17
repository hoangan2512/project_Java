package server.repository;

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
}