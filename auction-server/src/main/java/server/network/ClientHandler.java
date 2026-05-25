package server.network;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import message.Request;
import message.Response;
import model.ActionType;
import model.User;
import security.RSA;
import server.handler.ActionFactory;
import server.handler.ActionHandler;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
    }

    //HÀNG ĐỢI VÀ LUỒNG GỬI TIN BẤT ĐỒNG BỘ (WRITE THREAD)
    private final BlockingQueue<Object> responseQueue = new LinkedBlockingQueue<>();
    private Thread writeThread;
    private volatile boolean isRunning = true;
    private User loggedInUser = null;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    private void closeEverything() {
        isRunning = false;
        AuctionServer.clients.remove(this);
        if (writeThread != null) {
            writeThread.interrupt(); // Đánh thức Write Thread nếu đang bị kẹt ở hàng đợi trống
        }
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startWriteThread() {
        writeThread = new Thread(() -> {
            try {
                while (isRunning && !Thread.currentThread().isInterrupted()) {
                    // Chờ và lấy gói tin từ hàng đợi (Tự động block an toàn nếu hàng đợi trống)
                    Object message = responseQueue.take();
                    if (out != null) {
                        out.writeObject(message);
                        out.flush();
                        out.reset(); // Xóa bộ nhớ đệm Object để tránh lỗi lưu cache tuần tự hóa
                    }
                }
            } catch (InterruptedException e) {
                // Luồng bị ngắt khi closeEverything() được gọi
            } catch (IOException e) {
                System.err.println("Lỗi luồng gửi dữ liệu (Write Thread) của Client: " +
                        (loggedInUser != null ? loggedInUser.getName() : "Khách ẩn danh"));
            }
        });
        writeThread.setName("WriteThread-" + socket.getRemoteSocketAddress());
        writeThread.start();
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            // 1. Kích hoạt luồng gửi dữ liệu độc lập trước
            startWriteThread();

            // --- GỬI PUBLIC KEY CHO CLIENT KHI VỪA KẾT NỐI ---
            Response pubKeyResponse = new Response("PUBLIC_KEY", AuctionServer.serverPublicKeyStr, "Đây là khóa công khai của Server");
            sendMessage(pubKeyResponse);

            // Vòng lặp luồng đọc dữ liệu chính (Read Thread)
            while (isRunning && !socket.isClosed()) {
                Request request = (Request) in.readObject();
                System.out.println("Nhận yêu cầu: " + request.getAction() + " từ Client: " + (loggedInUser != null ? loggedInUser.getName() : "Khách ẩn danh"));

                // Giao việc cho hàm chia chọn
                Response response = handleBusinessLogic(request);

                // Đẩy gói tin phản hồi vào hàng đợi thay vì ghi trực tiếp vào Socket
                sendMessage(response);
            }
        } catch (EOFException | SocketException e) {
            System.err.println("Một Client đã ngắt kết nối. User: " + (loggedInUser != null ? loggedInUser.getName() : "Khách ẩn danh"));
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Lỗi kết nối từ Client. User: " + (loggedInUser != null ? loggedInUser.getName() : "Khách ẩn danh"));
            e.printStackTrace();
        } finally {
            closeEverything();
        }
    }

    // BỘ PHẬN ĐIỀU HƯỚNG (ROUTER)
    private Response handleBusinessLogic(Request request) {
        ActionType type = request.getAction();
        // giải mã password trước khi bắt đầu xử lí
        if (type == ActionType.LOGIN_BIDDER || type == ActionType.LOGIN_SELLER || type == ActionType.LOGIN_ADMIN || type == ActionType.REGISTER) {
            try {
                User userWithEncryptedPass = (User) request.getPayload();
                String encryptedPass = userWithEncryptedPass.getPassword();

                String decryptedPass = RSA.decrypt(encryptedPass, AuctionServer.serverPrivateKey);
                userWithEncryptedPass.setPassword(decryptedPass);
            } catch (Exception e) {
                System.err.println("Lỗi giải mã mật khẩu: " + e.getMessage());
                return new Response("FAIL", null, "Lỗi bảo mật: Không thể xác thực thông tin.");
            }
        }
        ActionHandler handler = ActionFactory.getHandler(type);
        if (handler != null) {
            return handler.execute(request, this);
        }

        return new Response("ERROR", null, "Hành động không xác định hoặc không được hỗ trợ: " + type);
    }

    public boolean checkAuthorization(String expectedRole) {
        if (this.loggedInUser == null) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(this.loggedInUser.getRole())) {
            return true;
        }
        if ("BOTH".equalsIgnoreCase(this.loggedInUser.getRole())) {
            return true;
        }
        return expectedRole == null || expectedRole.equalsIgnoreCase(this.loggedInUser.getRole());
    }

    public Response unauthResponse() {
        return new Response("FAIL", null, "Bạn không có quyền thực hiện chức năng này!");
    }

    public void sendMessage(Object msg) {
        if (msg == null) return;
        responseQueue.offer(msg); // Thêm vào queue (không gây nghẽn luồng gọi)
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }

    public void clearSession() {
        this.loggedInUser = null;
    }
}