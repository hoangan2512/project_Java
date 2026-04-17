package server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

// Bắt buộc phải thêm các thư viện này để nhận diện được Item, User, ActionType...
import message.Request;
import message.Response;
import model.ActionType;
import model.Item;
import model.User;
import server.repository.DatabaseConnection;
import server.repository.ItemRepository;
import server.repository.UserRepository;

public class ServerApplication {
    // Đặt Port giống hệt Port bên ClientSocket.java
    private static final int PORT = 12345;

    public static void main(String[] args) {
        // Bước 1: Khởi tạo Database trước (gọi lại logic tạo bảng của bạn nếu cần)
        System.out.println("Đang kiểm tra Database...");
        DatabaseConnection.getInstance();

        // Bước 2: Mở cổng mạng
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✅ Server đã bật! Đang lắng nghe ở port: " + PORT);

            // Vòng lặp vô tận: Để Server luôn sống và chờ đợi Client
            while (true) {
                // Lệnh này sẽ làm Server "tạm dừng" cho đến khi có 1 Client kết nối vào
                Socket clientSocket = serverSocket.accept();
                System.out.println("\nCó một Client vừa kết nối: " + clientSocket.getInetAddress());

                // Khởi tạo luồng dữ liệu (Nhớ: Luôn tạo Output trước Input)
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

                // Đọc Request từ Client
                Request req = (Request) in.readObject();
                System.out.println("Client yêu cầu chức năng: " + req.getAction());

                Response res = new Response(); // Tạo sẵn gói phản hồi

                // ---------------- LẮP NÃO CHO SERVER Ở ĐÂY ----------------
                if (req.getAction() == ActionType.CREATE_ITEM) { // Nếu Client muốn THÊM SẢN PHẨM
                    Item newItem = (Item) req.getPayload();
                    ItemRepository itemRepo = new ItemRepository();

                    // Gọi hàm save() siêu xịn mà bạn vừa viết ban nãy
                    boolean isSaved = itemRepo.addItem(newItem);

                    if (isSaved) {
                        res.setStatus("SUCCESS");
                        System.out.println(" Đã lưu vào DB sản phẩm: " + newItem.getName());
                    } else {
                        res.setStatus("FAIL");
                        System.out.println(" Lỗi khi lưu sản phẩm vào DB!");
                    }
                }
                else if (req.getAction() == ActionType.REGISTER) { // Nếu Client muốn ĐĂNG KÝ
                    User newUser = (User) req.getPayload();
                    UserRepository userRepo = new UserRepository();

                    // Giả sử bạn đã viết hàm save() trong UserRepository
                    boolean isSaved = userRepo.addUser(newUser);

                    if (isSaved) {
                        res.setStatus("SUCCESS");
                        System.out.println("Đã lưu User mới vào DB: " + newUser.getName());
                    } else {
                        res.setStatus("FAIL");
                        System.out.println("Lỗi khi đăng ký User!");
                    }
                } else if (req.getAction() == ActionType.LOGIN) {
                    User userFromClient = (User) req.getPayload();
                    UserRepository userRepo = new UserRepository();

                    // Gọi hàm login bạn đã viết trong UserRepository
                    // Lưu ý: Kiểm tra xem class User dùng .getName() hay .getUsername() nhé!
                    User authenticatedUser = userRepo.login(userFromClient.getName(), userFromClient.getPassword());

                    if (authenticatedUser != null) {
                        res.setStatus("SUCCESS");
                        System.out.println("Đăng nhập thành công cho: " + authenticatedUser.getName());
                    } else {
                        res.setStatus("FAIL");
                        System.out.println("Đăng nhập thất bại cho: " + userFromClient.getName());
                    }
                }
                // Các chức năng khác như LOGIN, BID... viết tương tự ở đây
                // ----------------------------------------------------------

                // Gửi kết quả về cho Client
                out.writeObject(res);
                out.flush();

                // Đóng kết nối với Client này để đón Client khác
                clientSocket.close();
            }

        } catch (Exception e) {
            System.err.println("Lỗi Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}