package server.network;
import java.io.IOException;
import java.sql.ClientInfoStatus;
import java.util.*;
import java.net.Socket;
import java.net.ServerSocket;
public class AuctionServer {
    private static final int PORT=2810; //tạo 1 cổng
    public static List<ClientHandler> clients= new ArrayList<>(); // Quản lí các kết nối của người dùng
    public static void main(String[] args){
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
