package server.network;
import server.repository.DatabaseConnection;

import java.io.IOException;
import java.util.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionServer {
    private static final int PORT=2810; //tạo 1 cổng
    public static List<ClientHandler> clients = new CopyOnWriteArrayList<>(); // Quản lí các kết nối của người dùng
    public static void main(String[] args){
        DatabaseConnection.getInstance().getConnection();
        System.out.println("[HỆ THỐNG] Đã mở Database thành công!");
        try (ServerSocket serverSocket = new ServerSocket(PORT)){
            System.out.println("Server đấu giá tại cổng: "+PORT);
            while (true) {
                //Đợi client kết nối tới Server
                Socket socket = serverSocket.accept();
                System.out.println("Người dùng mới đã kết nối tới hệ thống: " + socket.getInetAddress());
                //Tạo 1 luồng mới với mỗi client
                ClientHandler clientHandler = new ClientHandler(socket);
                //Thêm dữ liệu của khách hàng vào hệ thống
                clients.add(clientHandler);
                //Chạy từng luồng cho mỗi client
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            throw new RuntimeException("Lỗi Server: "+e.getMessage());
        }
    }
    // Hàm để bắn tin nhắn cho tất cả mọi người (Dùng cho tính năng Real-time sau này)
    public static void broadcast(Object message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }
}
