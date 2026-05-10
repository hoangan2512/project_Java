package server.network;

import server.repository.DatabaseConnection;
import server.service.AuctionTimeManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionServer {
    private static final Logger LOGGER = Logger.getLogger(AuctionServer.class.getName());
    private static final int PORT = 2810;
    private static final int THREAD_POOL_SIZE = 50;

    public static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    // Trình quản lý thời gian của các phiên đấu giá
    private static final AuctionTimeManager auctionTimeManager = new AuctionTimeManager();

    // Cặp khóa RSA của server, dùng để trao đổi khóa AES an toàn
    private static PublicKey serverPublicKey;
    private static PrivateKey serverPrivateKey;

    public static PublicKey getServerPublicKey() {
        return serverPublicKey;
    }

    public static PrivateKey getServerPrivateKey() {
        return serverPrivateKey;
    }

    public static void main(String[] args) {
        //-1. TẠO CẶP KHOÁ RSA CHO SERVER
        try{
            KeyPairGenerator keyGen =KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048); //độ dài khoá
            KeyPair keyPair = keyGen.generateKeyPair();
            serverPublicKey = keyPair.getPublic();
            serverPrivateKey = keyPair.getPrivate();
            LOGGER.info("RSA KeyPair generated successfully.");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Không thể tạo cặp khóa RSA, server không thể khởi động.", e);
            return; // Dừng server nếu không tạo được khóa
        }

        // 0. KHỞI TẠO THREAD POOL
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        // 1. KHỞI TẠO DB
        LOGGER.info("Initializing database tables if not exist...");
        // Gọi hàm main của class Main để chạy các lệnh CREATE TABLE IF NOT EXISTS
        // Đây là cách fix nhanh, chuẩn nhất là tách phần khởi tạo bảng ra một hàm riêng
        server.Main.main(new String[]{});

        // 2. KẾT NỐI CƠ SỞ DỮ LIỆU
        LOGGER.info("Connecting to the database...");
        DatabaseConnection.getInstance().getConnection();
        LOGGER.info("Database connection successful.");

        // 3. KHỞI ĐỘNG TRÌNH QUẢN LÝ THỜI GIAN
        LOGGER.info("Starting Auction Time Manager...");
        auctionTimeManager.start();

        // 4. ĐĂNG KÝ SHUTDOWN HOOK
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Server is shutting down. Cleaning up resources...");
            auctionTimeManager.stop();
            LOGGER.info("Auction Time Manager stopped.");
            executorService.shutdown();
            LOGGER.info("Thread pool shut down.");
        }));

        // 5. KHỞI ĐỘNG SERVER VÀ LẮNG NGHE KẾT NỐI
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            LOGGER.info("Server started. Listening on port: " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                LOGGER.info("New client connected: " + clientSocket.getInetAddress());

                // Tạo và khởi chạy một trình xử lý riêng cho mỗi client
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                executorService.execute(clientHandler); // Sử dụng thread pool
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Server error: " + e.getMessage(), e);
        } finally {
            // Đảm bảo AuctionTimeManager luôn được dừng lại, ngay cả khi có lỗi nghiêm trọng
            if (auctionTimeManager != null) {
                auctionTimeManager.stop();
            }
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
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
