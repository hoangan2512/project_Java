package network;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javafx.application.Platform;

import message.Request;
import message.Response;
import controller.mainPageController;

public class ClientSocket {
    private static final String SERVER_IP = "localhost";
    private static final Integer SERVER_PORT = 2810;

    private static Socket socket;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;

    private static final BlockingQueue<Response> responseQueue = new LinkedBlockingQueue<>();
    
    // Biến lưu trữ Public Key của Server
    private static String serverPublicKey = null;

    //Hàm ngắt kết nối socket
    public static void disconnect() {
        try {
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // HÀM KIỂM TRA KẾT NỐI BAN ĐẦU
    public static boolean tryConnect() {
        if (socket != null && !socket.isClosed()) {
            return true;
        }
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            startListenerThread();
            return true;

        } catch (Exception e) {
            System.out.println("ERROR: Không thể kết nối với server.");
            e.printStackTrace();
            disconnect(); // Dọn dẹp tài nguyên nếu có lỗi
            return false;
        }
    }

    public static synchronized Response sendRequest(Request request) {
        if (socket == null || socket.isClosed()) {  //ktra kết nối socket
            if (!tryConnect()) {
                return null;
            }
        }

        try {
            out.writeObject(request);
            out.flush();

            return responseQueue.take();
        } catch (Exception e) {
            System.out.println("ERROR: Failed to send/receive request. " + e.getMessage());
            e.printStackTrace();
            return null;
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


    private static void handleBroadcast(Response res) {
        System.out.println("[Server message]: " + res.getMessage() + " | Dữ liệu: " + res.getData());

        // Tự động làm mới UI, truyền luôn Broadcast Response sang để Controller tự quyết định làm gì
        if (mainPageController.getInstance() != null) {
            mainPageController.getInstance().refreshData(res);
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

    public static String startLocalServerToGetCode() {
        int port = 8080;
        System.out.println("[OAUTH2] Đang mở Server ngầm tại port " + port + " để đợi Google...");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            // Vòng lặp liên tục để bỏ qua các request rác (như /favicon.ico) từ trình duyệt
            while (true) {
                try (Socket browserSocket = serverSocket.accept();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(browserSocket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
                     PrintWriter writer = new PrintWriter(new OutputStreamWriter(browserSocket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), true)) {

                    String requestLine = reader.readLine();
                    System.out.println("[OAUTH2] Trình duyệt gửi yêu cầu: " + requestLine);

                    if (requestLine == null || requestLine.isEmpty()) {
                        continue;
                    }

                    // Kiểm tra xem request này có thực sự chứa mã xác thực không
                    if (requestLine.contains("code=")) {
                        // 1. Tách lấy phần chuỗi nằm sau "code="
                        String codeParam = requestLine.split("code=")[1];

                        // 2. Cắt bỏ phần " HTTP/1.1" ở cuối dòng
                        codeParam = codeParam.split(" ")[0];

                        // 3. QUAN TRỌNG: Loại bỏ tất cả các tham số dư thừa đi kèm phía sau dấu &
                        if (codeParam.contains("&")) {
                            codeParam = codeParam.split("&")[0];
                        }

                        // 4. QUAN TRỌNG: Giải mã URL Encoding (Biến %2F ngược lại thành dấu / )
                        String authorizationCode = java.net.URLDecoder.decode(codeParam, "UTF-8");

                        System.out.println("[OAUTH2] Đã bóc tách mã Code sạch thành công: " + authorizationCode);

                        // Trả lời một trang HTML thân thiện hiển thị trên trình duyệt của User
                        writer.println("HTTP/1.1 200 OK");
                        writer.println("Content-Type: text/html; charset=UTF-8");
                        writer.println();
                        writer.println("<html><body style='font-family: sans-serif; text-align: center; padding-top: 50px; background-color: #121212; color: white;'>");
                        writer.println("<h2 style='color: #FF9800;'>BidHub</h2>");
                        writer.println("<h3>Đăng nhập bằng Google thành công!</h3>");
                        writer.println("<p style='color: #4CAF50;'>Bạn có thể đóng tab trình duyệt này và quay lại ứng dụng.</p>");
                        writer.println("</body></html>");
                        writer.flush();

                        // Trả mã code sạch về cho luồng xử lý chính gửi lên Server
                        return authorizationCode;
                    } else {
                        // Nếu là request rác (như favicon.ico), trả về 404 và tiếp tục vòng lặp đợi kết nối tiếp theo
                        writer.println("HTTP/1.1 404 Not Found");
                        writer.println();
                        writer.flush();
                    }
                } catch (Exception e) {
                    System.err.println("[OAUTH2] Lỗi xử lý một kết nối từ trình duyệt: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[OAUTH2] Lỗi khởi chạy Server ngầm tại cổng 8080: " + e.getMessage());
            return null;
        }
    }
}
