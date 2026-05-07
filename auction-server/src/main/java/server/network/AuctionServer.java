package server.network;

import server.Main;
import server.repository.DatabaseConnection;
import server.service.AuctionTimeManager;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionServer {
    private static final Logger LOGGER = Logger.getLogger(AuctionServer.class.getName());
    private static final int PORT = 2810;

    // Danh sách thread-safe để quản lý các client đang kết nối
    public static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    // Trình quản lý thời gian của các phiên đấu giá
    private static final AuctionTimeManager auctionTimeManager = new AuctionTimeManager();

    public static void main(String[] args) {
        // 0. KHỞI TẠO DB TRƯỚC KHI LÀM BẤT CỨ VIỆC GÌ KHÁC
        // Nếu không gọi hàm này, DB mới tinh sẽ không có các bảng (users, items, auctions, bids)
        // và Thread của AuctionTimeManager sẽ ném lỗi "no such table: auctions"
        LOGGER.info("Initializing database tables if not exist...");
        // Gọi hàm main của class Main để chạy các lệnh CREATE TABLE IF NOT EXISTS
        // Đây là cách fix nhanh, chuẩn nhất là tách phần khởi tạo bảng ra một hàm riêng
        server.Main.main(new String[]{}); 

        // 1. Kết nối cơ sở dữ liệu
        LOGGER.info("Connecting to the database...");
        DatabaseConnection.getInstance().getConnection();
        LOGGER.info("Database connection successful.");

        // 2. Khởi động trình quản lý thời gian đấu giá
        LOGGER.info("Starting Auction Time Manager...");
        auctionTimeManager.start();

        // 3. Đăng ký shutdown hook để đảm bảo tài nguyên được giải phóng khi server tắt
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Server is shutting down. Cleaning up resources...");
            auctionTimeManager.stop();
            LOGGER.info("Auction Time Manager stopped.");
        }));

        // 4. Khởi động server và lắng nghe kết nối
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            LOGGER.info("Server started. Listening on port: " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                LOGGER.info("New client connected: " + clientSocket.getInetAddress());

                // Tạo và khởi chạy một trình xử lý riêng cho mỗi client
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Server error: " + e.getMessage(), e);
        } finally {
            // Đảm bảo AuctionTimeManager luôn được dừng lại, ngay cả khi có lỗi nghiêm trọng
            if (auctionTimeManager != null) {
                auctionTimeManager.stop();
            }
        }
    }

    /**
     * Gửi một thông điệp tới tất cả các client đang kết nối.
     * @param message Đối tượng thông điệp cần gửi.
     */
    public static void broadcast(Object message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }
}
