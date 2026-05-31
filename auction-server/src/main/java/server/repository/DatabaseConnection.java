package server.repository;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static DatabaseConnection instance;

    // Đảm bảo database được lưu vào một file cố định trong thư mục project
    private static final String DB_FILE_PATH = "auction-server/auction_db.db";
    private static final String URL = "jdbc:sqlite:" + DB_FILE_PATH + "?busy_timeout=5000";
    private DatabaseConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            File dbFile = new File(DB_FILE_PATH);
            // Tạo thư mục nếu chưa có
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            System.out.println("Database is persistent at: " + dbFile.getAbsolutePath());
        } catch (ClassNotFoundException e) {
            System.err.println("FATAL: SQLite JDBC driver not found!");
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize database driver.", e);
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    // Quan trọng: Trả về một Connection mới mỗi khi được gọi thay vì dùng chung 1 connection.
    // Điều này giúp tránh lỗi "Connection closed" khi dùng try-with-resources.
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(URL);
        } catch (SQLException e) {
            System.err.println("Failed to establish connection.");
            e.printStackTrace();
            throw new RuntimeException("Could not connect to database.", e);
        }
    }
}