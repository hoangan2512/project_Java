package server;

import model.Item;
import model.User;
import server.repository.DatabaseConnection;
import server.repository.ItemRepository;
import server.repository.UserRepository;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Bảng Users
        String UsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE, " +
                "password TEXT NOT NULL, " +
                "role TEXT NOT NULL" +
                ");";

        // Bảng Items (ĐÃ SỬA: Xóa bỏ dòng created_at DATETIME DEFAULT CURRENT_TIMESTAMP)
        String ItemsTable = "CREATE TABLE IF NOT EXISTS items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_prdID TEXT, " +
                "name TEXT NOT NULL, " +
                "description TEXT, " +
                "starting_price DOUBLE NOT NULL, " +
                "seller_id INTEGER NOT NULL, " +
                "imgpath TEXT, " +
                "imgpath1 TEXT, " +
                "imgpath2 TEXT, " +
                "imgpath3 TEXT, " +
                "imgpath4 TEXT, " +
                "imgpath5 TEXT, " +
                "imgpath6 TEXT, " +
                "categories TEXT, " +
                "FOREIGN KEY (seller_id) REFERENCES users(id)" +
                ");";

        // Bảng Auctions
        String auctionsTable = "CREATE TABLE IF NOT EXISTS auctions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "item_id INTEGER NOT NULL, " +
                "start_time DATETIME NOT NULL, " +
                "end_time DATETIME NOT NULL, " +
                "status TEXT DEFAULT 'RUNNING', " +
                "current_price DOUBLE DEFAULT 0.0, " +
                "highest_bidder_id INTEGER, " +
                "FOREIGN KEY(item_id) REFERENCES items(id), " +
                "FOREIGN KEY(highest_bidder_id) REFERENCES users(id)" +
                ");";

        // Bảng Bids
        String bidsTable = "CREATE TABLE IF NOT EXISTS bids (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "auction_id INTEGER NOT NULL, " +
                "bidder_id INTEGER NOT NULL, " +
                "amount DOUBLE NOT NULL, " +
                "bid_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (auction_id) REFERENCES auctions(id), " +
                "FOREIGN KEY (bidder_id) REFERENCES users(id)" +
                ");";

        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            Statement stmt = conn.createStatement();
            stmt.execute(UsersTable);
            stmt.execute(ItemsTable);
            stmt.execute(auctionsTable);
            stmt.execute(bidsTable);
            stmt.close();
            System.out.println("Tạo cấu trúc cơ sở dữ liệu thành công!");
        } catch (Exception e) {
            System.out.println("Có lỗi xảy ra khi tạo bảng!");
            e.printStackTrace();
        }
    }
}