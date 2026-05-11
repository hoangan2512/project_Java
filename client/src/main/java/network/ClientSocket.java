package network;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javafx.application.Platform;

import message.Request;
import message.Response;
import controller.mainPageController;

public class ClientSocket {
    private static final String SERVER_IP = "localhost";
    private static final Integer SERVER_PORT = 2810;     // tạo port

    private static Socket socket;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;

    // Hàng đợi để giao tiếp giữa luồng đọc (Listener) và luồng chính gọi sendRequest
    private static final BlockingQueue<Response> responseQueue = new LinkedBlockingQueue<>();
    
    // Biến lưu trữ Public Key của Server
    private static String serverPublicKey = null;

    // --- HÀM KIỂM TRA KẾT NỐI BAN ĐẦU ---
    public static boolean tryConnect() {
        if (socket != null && !socket.isClosed()) {
            return true; // Đã kết nối rồi
        }
        try {
            // Thử tạo một kết nối mới

            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Nếu thành công, khởi chạy luồng Listener
            startListenerThread();
            return true;

        } catch (IOException e) {
            socket = null;
            out = null;
            in = null;
            return false;
        }
    }

    private static void startListenerThread() {
        Thread listenerThread = new Thread(() -> {
            try {
                while (true) {
                    Response res = (Response) in.readObject();

                    String status = res.getStatus();
                    if ("PUBLIC_KEY".equals(status)) {
                        // Nhận Public Key từ Server ngay khi kết nối
                        serverPublicKey = (String) res.getData();
                        System.out.println("Đã nhận Public Key từ Server.");
                    } else if ("NOTIFY_NEW_PRICE".equals(status) || "AUCTION_END".equals(status) || "AUCTION_START".equals(status)) {
                        Platform.runLater(() -> handleBroadcast(res));
                    } else {
                        responseQueue.put(res);
                    }
                }
            } catch (EOFException e) {
                System.out.println("INFO: Server đã đóng kết nối.");
            } catch (Exception e) {
                // Không in stack trace nếu socket đã bị đóng chủ động
                if (socket != null && !socket.isClosed()) {
                    System.out.println("ERROR: Mất kết nối tới Server hoặc lỗi đọc dữ liệu!");
                    e.printStackTrace();
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public static synchronized Response sendRequest(Request request) {
        // Nếu chưa kết nối, thử kết nối lại. Nếu vẫn thất bại thì trả về null.
        if (socket == null || socket.isClosed()) {
            if (!tryConnect()) {
                return null;
            }
        }

        try {
            out.writeObject(request);
            out.flush();
            return responseQueue.take();
        } catch (Exception e) {
            System.out.println("ERROR: Lỗi khi gửi/nhận Request: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static void handleBroadcast(Response res) {
        System.out.println("[Server message]: " + res.getMessage() + " | Dữ liệu: " + res.getData());
        
        // Tự động làm mới UI
        if (mainPageController.getInstance() != null) {
            mainPageController.getInstance().refreshData();
        }
    }

    public static void disconnect() {
        try {
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Hàm cung cấp Public Key cho các Controller (ví dụ lúc đăng nhập/đăng ký)
    public static String getServerPublicKey() {
        // Đợi một chút nếu chưa có (trường hợp vừa connect xong nhưng luồng Listener chưa kịp nhận)
        int retries = 0;
        while (serverPublicKey == null && retries < 10) {
            try {
                Thread.sleep(100);
                retries++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return serverPublicKey;
    }
}
