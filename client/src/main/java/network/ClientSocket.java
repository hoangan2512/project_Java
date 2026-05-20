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
            while (true) {
                try (Socket browserSocket = serverSocket.accept();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(browserSocket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
                     PrintWriter writer = new PrintWriter(new OutputStreamWriter(browserSocket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), true)) {

                    String requestLine = reader.readLine();
                    if (requestLine == null || requestLine.isEmpty()) continue;

                    if (requestLine.contains("code=")) {
                        String codeParam = requestLine.split("code=")[1].split(" ")[0];
                        if (codeParam.contains("&")) codeParam = codeParam.split("&")[0];
                        String authorizationCode = java.net.URLDecoder.decode(codeParam, "UTF-8");

                        System.out.println("[OAUTH2] Đã bóc tách mã Code sạch thành công: " + authorizationCode);

                        writer.println("HTTP/1.1 200 OK");
                        writer.println("Content-Type: text/html; charset=UTF-8");
                        writer.println();

                        String htmlResponse = """
                    <!DOCTYPE html>
                    <html lang="vi">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>BidHub - Google Auth</title>
                        <style>
                            body, html {
                                margin: 0;
                                padding: 0;
                                width: 100%;
                                height: 100%;
                                overflow: hidden;
                                background-color: #0b0b12;
                            }

                            /* Container chính bao phủ toàn màn hình, dùng Flexbox căn giữa */
                            .frame {
                                display: flex;
                                flex-direction: column;
                                align-items: center;
                                justify-content: center;
                                width: 100vw;
                                height: 100vh;
                                background: url('https://i.postimg.cc/jj4vbsW9/background.png') no-repeat center center;
                                background-size: cover;
                                box-sizing: border-box;
                            }

                            /* Khối bọc Logo và Nội dung để quản lý khoảng cách bằng Flexbox, KHÔNG DÙNG absolute độc lập */
                            .main-container {
                                display: flex;
                                flex-direction: column;
                                align-items: center;
                                justify-content: center;
                                width: 90%;
                                max-width: 900px;
                                text-align: center;
                                margin-bottom: 60px; /* Chừa không gian cho footer ở đáy */
                            }

                            /* Logo co giãn theo tỉ lệ */
                            .logo {
                                width: 15vw;
                                min-width: 180px;
                                max-width: 275px;
                                height: auto;
                                aspect-ratio: 275/124; /* Giữ nguyên tỉ lệ ảnh gốc */
                                background: url('https://i.postimg.cc/VNW48Yrf/logo-project-2-removebg.png') no-repeat center center;
                                background-size: contain;
                                margin-bottom: 5vh; /* Khoảng cách động từ logo xuống chữ */
                            }

                            /* Khối text nhóm riêng để không bao giờ đè lên nhau */
                            .text-group {
                                display: flex;
                                flex-direction: column;
                                gap: 2vh; /* Tạo khoảng cách giãn tự động giữa các dòng */
                            }

                            .text-style {
                                font-family: 'Google Sans Flex', 'Segoe UI', system-ui, sans-serif;
                                font-style: normal;
                                color: #FFFFFF;
                                text-shadow: 0 2px 10px rgba(0,0,0,0.6);
                                margin: 0;
                            }

                            /* Tiêu đề lớn co giãn linh hoạt theo chiều rộng màn hình */
                            .success-text {
                                font-weight: 700;
                                font-size: calc(18px + 1vw); /* Tự động phóng to/thu nhỏ mượt mà */
                                line-height: 1.3;
                                word-break: break-word;
                            }

                            /* Dòng chữ phụ */
                            .redirect-text {
                                font-weight: 300;
                                font-size: calc(14px + 0.3vw);
                                line-height: 1.5;
                                color: rgba(255, 255, 255, 0.8);
                            }

                            /* Footer cố định ở đáy */
                            .footer-text {
                                position: absolute;
                                bottom: 25px;
                                left: 0;
                                width: 100%;
                                text-align: center;
                                font-family: 'Google Sans Flex', sans-serif;
                                font-weight: 300;
                                font-size: 14px;
                                color: rgba(255, 255, 255, 0.5);
                                text-shadow: 0 1px 5px rgba(0,0,0,0.3);
                            }
                        </style>
                    </head>
                    <body>
                        <div class="frame">
                            <div class="main-container">
                                <div class="logo"></div>
                                <div class="text-group">
                                    <h1 class="text-style success-text">Successfully linked Google account to BidHub (‾◡◝)</h1>
                                    <p class="text-style redirect-text">Redirecting to the app, you can close this page in a few seconds.</p>
                                </div>
                            </div>
                            <div class="footer-text">A Product of Team 14</div>
                        </div>
                    </body>
                    </html>
                    """;

                        writer.print(htmlResponse);
                        writer.flush();

                        return authorizationCode;
                    } else {
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
