package server.network;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

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
        AuctionServer.clients.remove(this);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // --- GỬI PUBLIC KEY CHO CLIENT KHI VỪA KẾT NỐI ---
            // Yêu cầu Client lưu Public Key này để mã hóa mật khẩu trước khi gửi lên
            Response pubKeyResponse = new Response("PUBLIC_KEY", AuctionServer.serverPublicKeyStr, "Đây là khóa công khai của Server");
            out.writeObject(pubKeyResponse);
            out.flush();

            // Sửa lỗi cảnh báo: 'while' statement cannot complete without throwing an exception
            // Thêm điều kiện kiểm tra isClosed để vòng lặp có thể kết thúc tự nhiên
            while (!socket.isClosed()) {
                Request request = (Request) in.readObject();
                System.out.println("Nhận yêu cầu: " + request.getAction() + " từ Client: " + (loggedInUser != null ? loggedInUser.getName() : "Khách ẩn danh"));

                // Giao việc cho hàm chia chọn
                Response response = handleBusinessLogic(request);

                out.writeObject(response);
                out.flush();
            }
        } catch (EOFException | SocketException e) {
            // SocketException và EOFException là bình thường khi client chủ động ngắt kết nối
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

                // Dùng Private Key của Server để giải mã
                String decryptedPass = RSA.decrypt(encryptedPass, AuctionServer.serverPrivateKey);

                // Cập nhật lại mật khẩu đã giải mã vào đối tượng User
                userWithEncryptedPass.setPassword(decryptedPass);

                // Request bây giờ đã chứa mật khẩu dạng plain-text, sẵn sàng để Controller xử lý
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
                this.loggedInUser = null; // Xóa session khi logout
                return new Response("SUCCESS", null, "Đăng xuất thành công.");

            case GOOGLE_LOGIN:
                String codeReceived = (String) request.getPayload();

                String clientId = "887547914295-i912u5c51mm9ur6s7kd1pr5kipmpcgka.apps.googleusercontent.com";
                String clientSecret = "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";
                String redirectUri = "http://localhost:8080";

                try {
                    // Gửi request POST trực tiếp lên Google Token API để đổi mã Code lấy thông tin payload
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

                    // Trích xuất gói payload chứa thông tin người dùng được mã hóa
                    com.google.api.client.googleapis.auth.oauth2.GoogleIdToken idToken = tokenResponse.parseIdToken();
                    com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload payload = idToken.getPayload();

                    String email = payload.getEmail();
                    String name = (String) payload.get("name");

                    System.out.println("[SERVER] User đăng nhập Google: " + name + " (" + email + ")");

                    // ========================================================
                    // 🌟 KẾT NỐI NGHIỆP VỤ DATABASE QUA USERCONTROLLER
                    // ========================================================
                    Response authResponse = userController.handleGoogleLoginAuth(email, name);

                    // Nếu đăng nhập/đăng ký thành công, lưu thông tin vào Session của Thread này
                    if ("SUCCESS".equals(authResponse.getStatus()) && authResponse.getData() instanceof User) {
                        this.loggedInUser = (User) authResponse.getData();
                        System.out.println("=> Đã ghi nhận Session qua Google cho user: " + loggedInUser.getName());
                    }

                    // ĐÚNG KIẾN TRÚC: Return kết quả về để luồng chính ở hàm run() tự đẩy ra Socket
                    return authResponse;

                } catch (com.google.api.client.auth.oauth2.TokenResponseException e) {
                    // ========================================================
                    // CRITICAL FIX: BẪY LỖI CHUYÊN SÂU TỪ ENDPOINT CỦA GOOGLE
                    // ========================================================
                    System.err.println("[SERVER] Google OAuth API trả về lỗi cấu hình:");
                    if (e.getDetails() != null) {
                        System.err.println("  - Error: " + e.getDetails().getError());
                        System.err.println("  - Description: " + e.getDetails().getErrorDescription());
                    } else {
                        System.err.println("  - Raw Content: " + e.getContent());
                    }
                    e.printStackTrace();

                    String errorMsg = (e.getDetails() != null) ? e.getDetails().getErrorDescription() : e.getMessage();
                    return new Response("FAILED", null, "Google từ chối cấp Token: " + errorMsg);

                } catch (Exception e) {
                    // Bắt các lỗi hệ thống khác (NullPointer, Network Timeout, IO,...)
                    System.err.println("Lỗi xác thực Google OAuth tại Server: " + e.getMessage());
                    e.printStackTrace();
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

            // Thêm route cho REGISTER_AUTO_BID
            case REGISTER_AUTO_BID:
                if (checkAuthorization("BIDDER")) return bidController.handleRegisterAutoBid(request);
                return new Response("FAIL", null, "Bạn chưa đăng nhập hoặc không có quyền đấu giá tự động!");

            // ======================================================
            // CÁC HÀNH ĐỘNG CÔNG KHAI (KHÔNG CẦN ĐĂNG NHẬP ĐỂ XEM)
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

    /**
     * Hàm phụ trợ để kiểm tra xem Client này đã đăng nhập chưa và có đúng vai trò yêu cầu không.
     * Sửa lỗi: Trả về false nếu KHÔNG có quyền, true nếu CÓ quyền để câu lệnh if bên trên xuôi theo tự nhiên.
     */
    private boolean checkAuthorization(String expectedRole) {
        if (this.loggedInUser == null) {
            return false; // Chưa đăng nhập
        }
        
        // Admin có mọi quyền (nếu cần thiết) hoặc chỉ check chính xác role
        if ("ADMIN".equalsIgnoreCase(this.loggedInUser.getRole())) {
             return true; 
        }
        
        if ("BOTH".equalsIgnoreCase(this.loggedInUser.getRole())) {
             return true; // Cho phép user có role BOTH thực hiện chức năng của cả Seller và Bidder
        }

        // Sửa lỗi logic if có thể simplified
        return expectedRole == null || expectedRole.equalsIgnoreCase(this.loggedInUser.getRole());
    }
    
    private Response unauthResponse() {
         return new Response("FAIL", null, "Bạn không có quyền thực hiện chức năng này!");
    }

    public void sendMessage(Object msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.err.println("Không thể gửi tin nhắn.");
        }
    }

    // Thêm các hàm phụ trợ cho quản lý session
    public User getLoggedInUser() {
        return loggedInUser;
    }

    public void clearSession() {
        this.loggedInUser = null;
    }
}
