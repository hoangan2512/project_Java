package server.repository;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection conn;
    private String url = "jdbc:sqlite:auction_db.db";

    private DatabaseConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            this.conn = DriverManager.getConnection(url);

            // CHUYỂN DÒNG NÀY VÀO ĐÂY
            File dbFile = new File("auction_db.db");
            System.out.println("Database khởi tạo tại: " + dbFile.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("Lỗi kết nối DB trong Constructor!");
            e.printStackTrace();
        }
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return conn;
    }
}