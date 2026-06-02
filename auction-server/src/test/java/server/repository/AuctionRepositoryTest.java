package server.repository;

import model.Auction;
import model.Item;
import model.SearchCriteria;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionRepositoryTest {

    private static AuctionRepository repository;
    private static ItemRepository itemRepository;

    @BeforeAll
    public static void setUp() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            // Create tables for testing
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT NOT NULL UNIQUE," +
                    "password TEXT NOT NULL," +
                    "email TEXT," +
                    "role TEXT," +
                    "is_banned BOOLEAN DEFAULT 0" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS items (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL," +
                    "description TEXT," +
                    "seller_id INTEGER," +
                    "starting_price REAL," +
                    "moderation_status TEXT," +
                    "category_id INTEGER," +
                    "image_url TEXT," +
                    "FOREIGN KEY (seller_id) REFERENCES users(id)" +
                    ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS auctions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "item_id INTEGER NOT NULL," +
                    "start_time DATETIME NOT NULL," +
                    "end_time DATETIME NOT NULL," +
                    "status TEXT," +
                    "current_price REAL," +
                    "highest_bidder_id INTEGER," +
                    "FOREIGN KEY (item_id) REFERENCES items(id)," +
                    "FOREIGN KEY (highest_bidder_id) REFERENCES users(id)" +
                    ")");

        } catch (SQLException e) {
            System.err.println("Database setup for tests failed: " + e.getMessage());
            fail("Could not set up database tables for testing.", e);
        }

        repository = new AuctionRepository();
        itemRepository = new ItemRepository();

        setupTestData();
    }

    private static void setupTestData() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            // Clean up old test data to avoid conflicts
            stmt.executeUpdate("DELETE FROM auctions");
            stmt.executeUpdate("DELETE FROM items");
            stmt.executeUpdate("DELETE FROM users");

            // Insert a test user
            stmt.executeUpdate("INSERT INTO users (id, username, password, email, role) VALUES (1, 'testuser', 'password', 'test@example.com', 'SELLER')");
            stmt.executeUpdate("INSERT INTO users (id, username, password, email, role) VALUES (2, 'testbidder', 'password', 'bidder@example.com', 'BIDDER')");


            // Insert a test item
            String insertItemSql = "INSERT INTO items (name, seller_id, starting_price, moderation_status) VALUES ('Test Item 1', 1, 100.0, 'APPROVED')";
            stmt.executeUpdate(insertItemSql);

        } catch (SQLException e) {
            System.err.println("Setup failed: " + e.getMessage());
        }
    }

    @AfterAll
    public static void tearDown() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            // Clean up after tests
            stmt.executeUpdate("DELETE FROM auctions");
            stmt.executeUpdate("DELETE FROM items");
            stmt.executeUpdate("DELETE FROM users");

        } catch (SQLException e) {
            System.err.println("Teardown failed: " + e.getMessage());
        }
    }

    @Test
    public void testCreateAndGetAuction() {
        // 1. Get an existing Item ID (from setup)
        int itemId = -1;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT id FROM items WHERE name = 'Test Item 1' LIMIT 1")) {
            if (rs.next()) itemId = rs.getInt(1);
        } catch (SQLException e) {
            fail("Could not find test item");
        }

        assertTrue(itemId > 0, "Test item must exist");

        // 2. Create an Auction
        Auction auction = new Auction();
        auction.setItem_id(itemId);
        auction.setStart_time(LocalDateTime.now().plusDays(1));
        auction.setEnd_time(LocalDateTime.now().plusDays(2));
        auction.setStatus("TEST_STATUS"); // Custom status for easy cleanup
        auction.setCurrent_price(100.0);

        boolean created = repository.createAuction(auction);
        assertTrue(created, "Auction should be created successfully");

        // 3. Retrieve the created auction
        List<Auction> testAuctions = repository.getAuctionsByStatus("TEST_STATUS");
        assertFalse(testAuctions.isEmpty(), "Should retrieve at least one test auction");

        Auction retrievedAuction = testAuctions.get(0);

        // Verify crucial fields, ESPECIALLY the ID
        assertTrue(retrievedAuction.getId() > 0, "Auction ID must be correctly mapped and greater than 0");
        assertEquals(itemId, retrievedAuction.getItem_id(), "Item ID should match");
        assertEquals(100.0, retrievedAuction.getCurrent_price(), "Current price should match");
        assertNotNull(retrievedAuction.getItem(), "Item object should be mapped");
        assertEquals("Test Item 1", retrievedAuction.getItem().getName(), "Item name should be mapped");
    }

    @Test
    public void testGetAllAuctionsForList_MapsIdCorrectly() {
        List<Auction> auctions = repository.getAllAuctionsForList();
        if (!auctions.isEmpty()) {
            Auction firstAuction = auctions.get(0);
            assertTrue(firstAuction.getId() > 0, "ID must be mapped in getAllAuctionsForList");
            assertNotNull(firstAuction.getItem(), "Item must be mapped");
        }
    }

    @Test
    public void testUpdateBid() {
        // Create an auction to update
        int itemId = -1;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT id FROM items WHERE name = 'Test Item 1' LIMIT 1")) {
            if (rs.next()) itemId = rs.getInt(1);
        } catch (SQLException e) {
            fail("Could not find test item");
        }
        Auction auction = new Auction();
        auction.setItem_id(itemId);
        auction.setStart_time(LocalDateTime.now());
        auction.setEnd_time(LocalDateTime.now().plusDays(1));
        auction.setStatus("ACTIVE");
        auction.setCurrent_price(100.0);
        repository.createAuction(auction);

        // Find the created auction
        int auctionId = -1;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT id FROM auctions WHERE status = 'ACTIVE' LIMIT 1")) {
            if (rs.next()) auctionId = rs.getInt(1);
        } catch (SQLException e) {
            fail("Database query failed");
        }

        if (auctionId > 0) {
            boolean updated = repository.updateBid(auctionId, 9999.0, 2);
            assertTrue(updated, "Bid should be updated");

            Auction retrieved = repository.getAuctionById(auctionId);
            assertNotNull(retrieved);
            assertEquals(9999.0, retrieved.getCurrent_price());
            assertEquals(2, retrieved.getHighest_bidder_id());
        } else {
            fail("Could not find auction to update");
        }
    }

    @Test
    public void testSearchAdvanced_MapsIdCorrectly() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setKeyword(""); // Match all or most

        List<Auction> results = repository.searchAdvanced(criteria);
        if (!results.isEmpty()) {
            Auction firstAuction = results.get(0);
            assertTrue(firstAuction.getId() > 0, "ID must be mapped in searchAdvanced results");
            assertNotNull(firstAuction.getItem(), "Item must be mapped");
        }
    }
}
