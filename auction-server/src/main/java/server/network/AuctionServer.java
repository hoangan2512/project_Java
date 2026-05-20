package server.network;

import server.repository.DatabaseConnection;
import server.service.AuctionTimeManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import message.Response;

public class AuctionServer {
    private static final Logger LOGGER = Logger.getLogger(AuctionServer.class.getName());
    private static final int PORT = 2810;

    // Danh sách thread-safe để quản lý các client đang kết nối
    public static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    // Không khởi tạo tĩnh (static final) ở đây nữa để kiểm soát việc khởi tạo
    private static AuctionTimeManager auctionTimeManager;

    //Lưu trữ khóa RSA để dùng chung cho toàn bộ Server
    public static java.security.PrivateKey serverPrivateKey;
    public static String serverPublicKeyStr;

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
        // Khởi tạo thông qua Singleton thay vì biến cục bộ
        LOGGER.info("Starting Auction Time Manager...");
        auctionTimeManager = AuctionTimeManager.getInstance();
        auctionTimeManager.start();

        LOGGER.info("Generating RSA Key Pair for Security...");
        try {
            java.security.KeyPair keyPair = security.RSA.generateKeyPair();
            serverPrivateKey = keyPair.getPrivate();
            serverPublicKeyStr = security.RSA.keyToString(keyPair.getPublic());
            LOGGER.info("RSA Keys generated successfully.");
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi tạo khóa RSA: " + e.getMessage());
        }


        // 3. Đăng ký shutdown hook để đảm bảo tài nguyên được giải phóng khi server tắt
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Server is shutting down. Cleaning up resources...");
            if (auctionTimeManager != null) auctionTimeManager.stop();
            LOGGER.info("Auction Time Manager stopped.");
        }));

        // 4. Khởi động server và lắng nghe kết nối
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            LOGGER.info("Server started. Listening on port: " + PORT);

            // SỬA LỖI Ở ĐÂY: Xóa chữ "throws " thừa thãi sau vòng lặp while
            while (!serverSocket.isClosed()) {
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

    public static void sendMessageToUser(int userId, Object message) {
        boolean isOnline = false;

        for (ClientHandler client : clients) {
            // Kiểm tra client này đã đăng nhập chưa và có khớp ID không
            if (client.getLoggedInUser() != null && client.getLoggedInUser().getID() == userId) {
                client.sendMessage(message);
                isOnline = true;

                // Lưu ý: Nếu hệ thống cho phép 1 tài khoản đăng nhập trên nhiều máy cùng lúc,
                // hãy BỎ 'break;' để máy nào cũng nhận được thông báo.
                // Nếu chỉ 1 máy, giữ 'break;' để tối ưu hiệu năng.
                // break;
            }
        }

        if (!isOnline) {
            LOGGER.info("User ID " + userId + " hiện không online. Thông báo chưa được gửi trực tiếp qua socket.");
            // (Tùy chọn) Tại đây bạn có thể gọi hàm lưu thông báo vào Database (bảng notifications)
            // để lần tới khi user đăng nhập vào, họ sẽ đọc được.
        }
    }

    /**
     * Tìm ClientHandler của một User ID cụ thể và ép đăng xuất.
     */
    public static void forceLogoutUser(int userId) {
        for (ClientHandler client : clients) {
            if (client.getLoggedInUser() != null && client.getLoggedInUser().getID() == userId) {
                // Gửi thông báo ép đăng xuất tới Client này
                Response forceLogoutResponse = new Response("FORCE_LOGOUT", null, "Tài khoản của bạn đã bị khóa bởi Admin.");
                client.sendMessage(forceLogoutResponse);

                // Hủy session ở phía Server
                client.clearSession();
                LOGGER.info("Đã ép đăng xuất (Force Logout) đối với User ID: " + userId);
                break; // Thường mỗi user chỉ log in 1 nơi, nếu cho phép multi-login thì bỏ break
            }
        }
    }
}