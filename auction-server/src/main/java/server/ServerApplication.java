package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import server.network.ClientHandler;
import server.repository.DatabaseConnection;

public class ServerApplication {
    private static final int PORT = 2810;

    // Danh sách lưu các client đang online để sau này gọi hàm broadcast
    public static List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Đang kiểm tra Database...");
        DatabaseConnection.getInstance();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server đã bật! Đang lắng nghe ở port: " + PORT);

            // Vòng lặp chỉ làm nhiệm vụ ĐÓN KHÁCH
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("\n[+] Có Client vừa kết nối: " + clientSocket.getInetAddress());

                // Tạo nhân viên phục vụ
                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler);

                // Ném nhân viên vào một luồng riêng để tự chạy
                Thread thread = new Thread(handler);
                thread.start();
            }

        } catch (Exception e) {
            System.err.println("Lỗi Server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Hàm loa thông báo toàn máy chủ
    public static void broadcast(Object msg) {
        for (ClientHandler client : clients) {
            client.sendMessage(msg);
        }
    }
}