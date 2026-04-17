package server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import message.Request;
import message.Response;
import model.ActionType;
import model.Item;
import model.User;
import server.repository.DatabaseConnection;
import server.repository.ItemRepository;
import server.repository.UserRepository;

public class ServerApplication {
    private static final int PORT = 12345;

    public static void main(String[] args) {
        System.out.println("Đang kiểm tra Database...");
        DatabaseConnection.getInstance();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server đã bật! Đang lắng nghe ở port: " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("\nCó một Client vừa kết nối: " + clientSocket.getInetAddress());

                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

                Request req = (Request) in.readObject();
                System.out.println("Client yêu cầu chức năng: " + req.getAction());

                Response res = new Response();

                // ---------------- LẮP NÃO CHO SERVER Ở ĐÂY ----------------
                if (req.getAction() == ActionType.CREATE_ITEM) {
                    Item newItem = (Item) req.getPayload();
                    ItemRepository itemRepo = new ItemRepository();

                    boolean isSaved = itemRepo.addItem(newItem);

                    if (isSaved) {
                        res.setStatus("SUCCESS");
                        System.out.println("Đã lưu vào DB sản phẩm: " + newItem.getName());
                    } else {
                        res.setStatus("FAIL");
                        System.out.println("Lỗi khi lưu sản phẩm vào DB!");
                    }
                }
                else if (req.getAction() == ActionType.REGISTER) {
                    User newUser = (User) req.getPayload();
                    UserRepository userRepo = new UserRepository();

                    boolean isSaved = userRepo.addUser(newUser);

                    if (isSaved) {
                        res.setStatus("SUCCESS");
                        // SỬA LỖI ĐĂNG KÝ: Trả cái newUser này về để Client cất vào Session
                        res.setData(newUser);
                        System.out.println("Đã lưu User mới vào DB: " + newUser.getName());
                    } else {
                        res.setStatus("FAIL");
                        System.out.println("Lỗi khi đăng ký User!");
                    }
                }
                else if (req.getAction() == ActionType.LOGIN) {
                    User userFromClient = (User) req.getPayload();
                    UserRepository userRepo = new UserRepository();

                    User authenticatedUser = userRepo.login(userFromClient.getName(), userFromClient.getPassword());

                    if (authenticatedUser != null) {
                        res.setStatus("SUCCESS");

                        // SỬA LỖI ĐĂNG NHẬP: Nhét "User xịn" vào response gửi về cho Client
                        res.setData(authenticatedUser);

                        System.out.println("Đăng nhập thành công cho: " + authenticatedUser.getName());
                    } else {
                        res.setStatus("FAIL");
                        System.out.println("Đăng nhập thất bại cho: " + userFromClient.getName());
                    }
                }
                // ----------------------------------------------------------

                out.writeObject(res);
                out.flush();

                clientSocket.close();
            }

        } catch (Exception e) {
            System.err.println("Lỗi Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}