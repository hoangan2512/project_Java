package server.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Đường dẫn này sẽ tự động tạo một file tên là "auction_db.db" ngay trong thư mục dự án
    private static final String URL = "jdbc:sqlite:auction_db.db";

    private static Connection connection;

    private DatabaseConnection() {}

    public static Connection getInstance() {
        if (connection == null) {
            try {
                // Tải driver của SQLite
                Class.forName("org.sqlite.JDBC");
                // Mở kết nối (nếu file chưa có, nó sẽ tự tạo file mới)
                connection = DriverManager.getConnection(URL);
                System.out.println("Kết nối SQLite thành công! Đã tìm thấy hoặc tạo mới file Database.");
            } catch (SQLException | ClassNotFoundException e) {
                System.out.println("Kết nối SQLite thất bại!");
                e.printStackTrace();
            }
        }
        return connection;
    }
}