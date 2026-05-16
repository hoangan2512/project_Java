package server;

import server.repository.DatabaseConnection;
import java.sql.Connection;
import java.sql.Statement;

public class Main {
    public static void main(String[] args) {
        // language=SQLite
        String usersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE, " +
                "password TEXT NOT NULL, " +
                "role TEXT NOT NULL, " +
                "status TEXT NOT NULL DEFAULT 'ACTIVE'" + // ACTIVE, BANNED
                ");";

        // language=SQLite
        String itemsTable = "CREATE TABLE IF NOT EXISTS items (" +
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
                "moderation_status TEXT NOT NULL DEFAULT 'PENDING_APPROVAL'," + // PENDING_APPROVAL, APPROVED, REJECTED
                "FOREIGN KEY (seller_id) REFERENCES users(id)" +
                ");";

        // language=SQLite
        String auctionsTable = "CREATE TABLE IF NOT EXISTS auctions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "item_id INTEGER NOT NULL, " +
                "start_time DATETIME NOT NULL, " +
                "end_time DATETIME NOT NULL, " +
                "status TEXT DEFAULT 'PENDING_APPROVAL', " + // PENDING_APPROVAL, WAITING, RUNNING, FINISHED
                "current_price DOUBLE DEFAULT 0.0, " +
                "highest_bidder_id INTEGER, " +
                "FOREIGN KEY(item_id) REFERENCES items(id), " +
                "FOREIGN KEY(highest_bidder_id) REFERENCES users(id)" +
                ");";

        // language=SQLite
        String bidsTable = "CREATE TABLE IF NOT EXISTS bids (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "auction_id INTEGER NOT NULL, " +
                "bidder_id INTEGER NOT NULL, " +
                "amount DOUBLE NOT NULL, " +
                "bid_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (auction_id) REFERENCES auctions(id), " +
                "FOREIGN KEY (bidder_id) REFERENCES users(id)" +
                ");";
        
        // language=SQLite
        String createAdmin = "INSERT OR IGNORE INTO users (username, password, role, status) VALUES ('admin', 'admin123', 'ADMIN', 'ACTIVE');";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(usersTable);
            stmt.execute(itemsTable);
            stmt.execute(auctionsTable);
            stmt.execute(bidsTable);
            stmt.execute(createAdmin); // Chạy lệnh tạo admin
            System.out.println("Database structure is up-to-date. Admin user is ready.");
        } catch (Exception e) {
            System.err.println("Error during database initialization!");
            e.printStackTrace();
        }
    }
}
