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
import server.controller.AuctionController;
import server.controller.BidController;
import server.controller.ItemController;
import server.controller.UserController;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    // --- HÀNG ĐỢI VÀ LUỒNG GỬI TIN BẤT ĐỒNG BỘ (WRITE THREAD) ---
    private final BlockingQueue<Object> responseQueue = new LinkedBlockingQueue<>();
    private Thread writeThread;
    private volatile boolean isRunning = true;

    // --- BIẾN QUẢN LÝ SESSION (PHIÊN ĐĂNG NHẬP) ---
    private User loggedInUser = null;

    // Gọi các Controller ra để làm việc
    private final UserController userController = new UserController();
    private final ItemController itemController = new ItemController();
    private final BidController bidController = new BidController();
    private final AuctionController auctionController = new AuctionController();

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

    /**
     * Kích hoạt luồng chuyên trách gửi dữ liệu (Write Thread) độc lập
     */
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

        // --- GIẢI MÃ MẬT KHẨU TRƯỚC KHI XỬ LÝ ---
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

        switch (type) {
            case LOGIN_BIDDER:
            case LOGIN_SELLER:
            case LOGIN_ADMIN:
                Response loginResponse = userController.handleLogin(request);
                if ("SUCCESS".equals(loginResponse.getStatus()) && loginResponse.getData() instanceof User) {
                    this.loggedInUser = (User) loginResponse.getData();
                    System.out.println("=> Đã ghi nhận Session cho user: " + loggedInUser.getName());
                }
                return loginResponse;

            case REGISTER:
                Response registerResponse = userController.handleRegister(request);
                if ("SUCCESS".equals(registerResponse.getStatus()) && registerResponse.getData() instanceof User) {
                    this.loggedInUser = (User) registerResponse.getData();
                    System.out.println("=> Đã tự động ghi nhận Session sau khi đăng ký cho user: " + loggedInUser.getName());
                }
                return registerResponse;

            case LOGOUT:
                if (this.loggedInUser != null) {
                    System.out.println("=> Client ngắt Session (Logout): " + this.loggedInUser.getName());
                } else {
                    System.out.println("=> Một Client ẩn danh vừa gửi yêu cầu Logout.");
                }
                this.loggedInUser = null;
                return new Response("SUCCESS", null, "Đăng xuất thành công.");

            case GOOGLE_LOGIN:
                String codeReceived = (String) request.getPayload();
                String clientId = "887547914295-i912u5c51mm9ur6s7kd1pr5kipmpcgka.apps.googleusercontent.com";
                String clientSecret = "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";
                String redirectUri = "http://localhost:8080";

                try {
                    com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse tokenResponse =
                            new com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest(
                                    new com.google.api.client.http.javanet.NetHttpTransport(),
                                    com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
                                    "https://oauth2.googleapis.com/token",
                                    clientId,
                                    clientSecret,
                                    codeReceived,
                                    redirectUri
                            ).execute();

                    com.google.api.client.googleapis.auth.oauth2.GoogleIdToken idToken = tokenResponse.parseIdToken();
                    com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload payload = idToken.getPayload();

                    String email = payload.getEmail();
                    String name = (String) payload.get("name");

                    System.out.println("[SERVER] User đăng nhập Google: " + name + " (" + email + ")");

                    Response authResponse = userController.handleGoogleLoginAuth(email, name);

                    if ("SUCCESS".equals(authResponse.getStatus()) && authResponse.getData() instanceof User) {
                        this.loggedInUser = (User) authResponse.getData();
                        System.out.println("=> Đã ghi nhận Session qua Google cho user: " + loggedInUser.getName());
                    }

                    return authResponse;

                } catch (com.google.api.client.auth.oauth2.TokenResponseException e) {
                    System.err.println("[SERVER] Google OAuth API trả về lỗi cấu hình:");
                    if (e.getDetails() != null) {
                        System.err.println("  - Error: " + e.getDetails().getError());
                        System.err.println("  - Description: " + e.getDetails().getErrorDescription());
                    } else {
                        System.err.println("  - Raw Content: " + e.getContent());
                    }
                    String errorMsg = (e.getDetails() != null) ? e.getDetails().getErrorDescription() : e.getMessage();
                    return new Response("FAILED", null, "Google từ chối cấp Token: " + errorMsg);

                } catch (Exception e) {
                    System.err.println("Lỗi xác thực Google OAuth tại Server: " + e.getMessage());
                    return new Response("FAILED", null, "Lỗi kết nối hệ thống Server: " + e.getMessage());
                }

                // ======================================================
                // CÁC HÀNH ĐỘNG DÀNH RIÊNG CHO ADMIN
                // ======================================================
            case ADMIN_GET_ALL_USERS:
                if (checkAuthorization("ADMIN")) return userController.handleGetAllUsers(request);
                return unauthResponse();
            case ADMIN_BAN_USER:
                if (checkAuthorization("ADMIN")) return userController.handleBanUser(request);
                return unauthResponse();

            case ADMIN_UNBAN_USER:
                if (checkAuthorization("ADMIN")) return userController.handleUnbanUser(request);
                return unauthResponse();
            case ADMIN_APPROVE_ITEM:
                if (checkAuthorization("ADMIN")) return itemController.handleApproveItem(request);
                return unauthResponse();

            case ADMIN_REJECT_ITEM:
                if (checkAuthorization("ADMIN")) return itemController.handleRejectItem(request);
                return unauthResponse();

            case ADMIN_STOP_AUCTION:
                if (checkAuthorization("ADMIN")) return auctionController.handleAdminStopAuction(request);
                return unauthResponse();

            // ======================================================
            // CÁC HÀNH ĐỘNG CẦN KIỂM TRA QUYỀN (AUTHORIZATION)
            // ======================================================
            case CREATE_ITEM:
                if (checkAuthorization("SELLER")) return itemController.handleCreateItem(request);
                return new Response("FAIL", null, "Bạn chưa đăng nhập hoặc không phải là Người bán!");

            case CHECK_DUPLICATE_NAME:
                if (checkAuthorization("SELLER")) return itemController.handleCheckDuplicateName(request);
                return new Response("FAIL", null, "Bạn chưa đăng nhập hoặc không phải là Người bán!");

            case BID:
                if (checkAuthorization("BIDDER")) return bidController.handleBid(request);
                return new Response("FAIL", null, "Bạn chưa đăng nhập hoặc không có quyền đấu giá!");

            case REGISTER_AUTO_BID:
                if (checkAuthorization("BIDDER")) return bidController.handleRegisterAutoBid(request);
                return new Response("FAIL", null, "Bạn chưa đăng nhập hoặc không có quyền đấu giá tự động!");

            // ======================================================
            // CÁC HÀNH ĐỘNG CÔNG KHAI
            // ======================================================
            case GET_BID_HISTORY:
                return bidController.handleGetBidHistory(request);
            case GET_LIST:
                return auctionController.handleGetList(request);
            case GET_ITEM_DETAIL:
                return auctionController.handleGetItemDetail(request);
            case CUSTOM_SEARCH:
                return auctionController.handleCustomSearch(request);

            case AUCTION_END:
                return new Response("FAIL", null, "Client không có quyền kết thúc phiên đấu giá.");

            default:
                return new Response("ERROR", null, "Hành động không xác định: " + type);
        }
    }

    private boolean checkAuthorization(String expectedRole) {
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

    private Response unauthResponse() {
        return new Response("FAIL", null, "Bạn không có quyền thực hiện chức năng này!");
    }

    /**
     * NÂNG CẤP BẤT ĐỒNG BỘ: Đẩy gói tin vào hàng đợi an toàn Thread-safe.
     * Hàm này phản hồi ngay lập tức, giải phóng luồng xử lý chính.
     */
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