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
        String UsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE, " +
                "password TEXT NOT NULL, " +
                "role TEXT NOT NULL" +
                ");";
        String ItemsTable = "CREATE TABLE IF NOT EXISTS items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "description TEXT, " +
                "starting_price DOUBLE NOT NULL, " +
                "seller_id INTEGER NOT NULL, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (seller_id) REFERENCES users(id)" +
                ");";
        String auctionsTable = "CREATE TABLE IF NOT EXISTS auctions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "item_id INTEGER NOT NULL, " +
                "start_time DATETIME NOT NULL, " +
                "end_time DATETIME NOT NULL, " +
                "status TEXT DEFAULT 'OPEN', " +
                "FOREIGN KEY(item_id) REFERENCES items(id)" +
                ");";
        String bidsTable = "CREATE TABLE IF NOT EXISTS bids (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "auction_id INTEGER NOT NULL, " +
                "bidder_id INTEGER NOT NULL, " +
                "amount DOUBLE NOT NULL, " +
                "bid_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (auction_id) REFERENCES auctions(id), " +
                "FOREIGN KEY (bidder_id) REFERENCES users(id)" +
                ");";
        try{
            Connection conn = DatabaseConnection.getInstance().getConnection();
            Statement stmt = conn.createStatement();
            stmt.execute(UsersTable);
            stmt.execute(ItemsTable);
            stmt.execute(auctionsTable);
            stmt.execute(bidsTable);
            stmt.close();
        } catch(Exception e){
            System.out.println("Có lỗi xảy ra khi tạo bảng!");
            e.printStackTrace();
        }



    }
}
