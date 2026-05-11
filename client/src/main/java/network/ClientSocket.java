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
}
