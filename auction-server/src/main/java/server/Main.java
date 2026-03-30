package server;

import server.repository.DatabaseConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class Main {
    public static void main(String[] args) {
        System.out.println("Bắt đầu test chức năng Ghi/Đọc của SQLite...");

        // Sử dụng try-with-resources để tự động đóng Statement và ResultSet cho gọn gọn
        try {
            Connection conn = DatabaseConnection.getInstance();
            Statement stmt = conn.createStatement();

            // 1. TẠO BẢNG: Lệnh SQL tạo bảng 'users' nếu nó chưa tồn tại
            String createTableSQL = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT NOT NULL, " +
                    "role TEXT NOT NULL)";
            stmt.execute(createTableSQL);
            System.out.println("✅ Bước 1: Đã tạo bảng 'users' thành công.");

            // 2. THÊM DỮ LIỆU: Lệnh SQL nhét 1 dòng dữ liệu vào bảng
            String insertSQL = "INSERT INTO users (username, role) VALUES ('nguyen_van_a', 'BIDDER')";
            stmt.execute(insertSQL);
            System.out.println("✅ Bước 2: Đã thêm tài khoản 'nguyen_van_a' vào database.");

            // 3. ĐỌC DỮ LIỆU: Lệnh SQL lấy toàn bộ dữ liệu trong bảng users ra xem
            String querySQL = "SELECT * FROM users";
            ResultSet rs = stmt.executeQuery(querySQL);

            System.out.println("✅ Bước 3: Dữ liệu hiện đang được lưu trong Database là:");
            System.out.println("-------------------------------------------------");
            // Vòng lặp để in từng dòng dữ liệu lấy được
            while (rs.next()) {
                System.out.println(" ID: " + rs.getInt("id") +
                        " | Tài khoản: " + rs.getString("username") +
                        " | Vai trò: " + rs.getString("role"));
            }
            System.out.println("-------------------------------------------------");

            // Đóng các tài nguyên
            rs.close();
            stmt.close();

        } catch (Exception e) {
            System.out.println("❌ Có lỗi xảy ra trong quá trình test!");
            e.printStackTrace();
        }
    }
}