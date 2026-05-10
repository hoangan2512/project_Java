package server.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.security.PublicKey;
import java.util.logging.Level;
import java.util.logging.Logger;

import message.Request;
import message.Response;
import model.ActionType;
import model.User;
import server.controller.AuctionController;
import server.controller.BidController;
import server.controller.ItemController;
import server.controller.UserController;

import javax.crypto.Cipher;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;

public class ClientHandler implements Runnable {
    // --- 1. KHAI BÁO CÁC THUỘC TÍNH ---
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());
    private final Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    // Các thuộc tính dành riêng cho phiên làm việc của client này
    private SecretKey sharedAesKey;
    private User loggedInUser = null;

    // Các controller để xử lý nghiệp vụ
    private final UserController userController = new UserController();
    private final ItemController itemController = new ItemController();
    private final BidController bidController = new BidController();
    private final AuctionController auctionController = new AuctionController();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    // --- 2. PHƯƠNG THỨC CHÍNH ĐIỀU KHIỂN LUỒNG ---
    @Override
    public void run() {
        try {
            // Khởi tạo stream
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Bước 1: Thiết lập kết nối an toàn
            performHandshake();

            // Bước 2: Vòng lặp xử lý yêu cầu
            Cipher aesCipher = Cipher.getInstance("AES"); // Tạo Cipher để tái sử dụng
            while (true) {
                // 2a. Nhận và giải mã yêu cầu
                SealedObject sealedRequest = (SealedObject) in.readObject();
                aesCipher.init(Cipher.DECRYPT_MODE, this.sharedAesKey);
                Request request = (Request) sealedRequest.getObject(aesCipher);

                LOGGER.info("Nhận yêu cầu: " + request.getAction() + " từ Client: " + (loggedInUser != null ? loggedInUser.getName() : "Khách ẩn danh"));

                // 2b. Xử lý logic nghiệp vụ
                Response response = handleBusinessLogic(request);

                // 2c. Mã hóa và gửi phản hồi
                sendMessage(response);
            }
        } catch (Exception e) {
            // Bắt tất cả các lỗi (IOException, ClassNotFound, Lỗi mã hóa,...)
            System.err.println("!!! SERVER ERROR !!!");
            e.printStackTrace();
            LOGGER.log(Level.WARNING, "Client đã ngắt kết nối hoặc có lỗi. User: " + (loggedInUser != null ? loggedInUser.getName() : "Khách ẩn danh"), e);
        } finally {
            // Luôn dọn dẹp tài nguyên khi kết thúc
            disconnect();
        }
    }

    // --- 3. CÁC PHƯƠNG THỨC HỖ TRỢ ---
    private void performHandshake() throws Exception {
        // 1. Gửi khóa công khai của server tới client
        PublicKey serverPublicKey = AuctionServer.getServerPublicKey();
        out.writeObject(serverPublicKey);
        out.flush();

        // 2. Nhận khóa AES đã được client mã hóa
        byte[] encryptedAesKey = (byte[]) in.readObject();

        // 3. Dùng private key để giải mã và lưu lại khóa AES
        Cipher rsaCipher = Cipher.getInstance("RSA");
        rsaCipher.init(Cipher.UNWRAP_MODE, AuctionServer.getServerPrivateKey());
        this.sharedAesKey = (SecretKey) rsaCipher.unwrap(encryptedAesKey, "AES", Cipher.SECRET_KEY);
        LOGGER.info("Handshake thành công, đã thiết lập khóa AES an toàn cho client " + socket.getInetAddress());
    }
    public void sendMessage(Object message) {
        try {
            if (this.sharedAesKey == null) {
                LOGGER.warning("Không thể gửi tin nhắn, khóa AES chưa được thiết lập.");
                return;
            }
            Cipher aesCipher = Cipher.getInstance("AES");
            aesCipher.init(Cipher.ENCRYPT_MODE, this.sharedAesKey); //khởi tạo
            SealedObject sealedMessage = new SealedObject((Serializable) message, aesCipher); //mã hoá tin nhắn bằng aesCipher
            out.writeObject(sealedMessage);
            out.flush();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi gửi tin nhắn mã hóa cho " + socket.getInetAddress(), e);
        }
    }
    private void disconnect() {
        AuctionServer.clients.remove(this);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            LOGGER.info("Đã đóng kết nối và dọn dẹp tài nguyên cho client " + socket.getInetAddress());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Lỗi khi đóng tài nguyên cho client.", e);
        }
    }

    //4. BỘ PHẬN ĐIỀU HƯỚNG (ROUTER)
    private Response handleBusinessLogic(Request request) {
        ActionType type = request.getAction();

        switch (type) {
            case LOGIN_BIDDER:
            case LOGIN_SELLER:
                Response loginResponse = userController.handleLogin(request);
                // Nếu đăng nhập thành công, lưu lại thông tin user vào ClientHandler
                if ("SUCCESS".equals(loginResponse.getStatus()) && loginResponse.getData() instanceof User) {
                    this.loggedInUser = (User) loginResponse.getData();
                    LOGGER.info("=> Đã ghi nhận Session cho user: " + loggedInUser.getName());
                }
                return loginResponse;

            case REGISTER:
                Response registerResponse = userController.handleRegister(request);
                // Nếu đăng ký thành công, hệ thống tự động đăng nhập (lưu Session) luôn cho User đó
                if ("SUCCESS".equals(registerResponse.getStatus()) && registerResponse.getData() instanceof User) {
                    this.loggedInUser = (User) registerResponse.getData();
                    LOGGER.info("=> Đã tự động ghi nhận Session sau khi đăng ký cho user: " + loggedInUser.getName());
                }
                return registerResponse;

            case LOGOUT:
                if (this.loggedInUser != null) {
                    LOGGER.info("=> Client ngắt Session (Logout): " + this.loggedInUser.getName());
                } else {
                    LOGGER.info("=> Một Client ẩn danh vừa gửi yêu cầu Logout.");
                }
                this.loggedInUser = null;
                return new Response("SUCCESS", null, "Đăng xuất thành công.");

            case CREATE_ITEM:
                if (!checkAuthorization("SELLER")) {
                    return new Response("FAIL", null, "Bạn chưa đăng nhập hoặc không phải là Người bán!");
                }
                return itemController.handleCreateItem(request);
                
            case CHECK_DUPLICATE_NAME:
                if (!checkAuthorization("SELLER")) {
                    return new Response("FAIL", null, "Bạn chưa đăng nhập hoặc không phải là Người bán!");
                }
                // Điều hướng đúng về hàm kiểm tra trùng lặp (tránh lỗi ClassCastException)
                return itemController.handleCheckDuplicateName(request);

            case BID:
                if (!checkAuthorization("BIDDER")) {
                    return new Response("FAIL", null, "Bạn chưa đăng nhập hoặc không có quyền đấu giá!");
                }
                return bidController.handleBid(request);

            case GET_BID_HISTORY:
                return bidController.handleGetBidHistory(request);
            case GET_LIST:
                return auctionController.handleGetList(request);
            case GET_ITEM_DETAIL:
                return auctionController.handleGetItemDetail(request);
            case CUSTOM_SEARCH:
                return auctionController.handleCustomSearch(request);
            case AUCTION_END: // Cái này nên chỉ cho hệ thống gọi (từ AuctionTimeManager), Client gọi sẽ bị chặn. Bạn nên chặn ở đây.
                return new Response("FAIL", null, "Client không có quyền kết thúc phiên đấu giá.");

            default:
                return new Response("ERROR", null, "hành động không xác định: " + type);
        }
    }

    private boolean checkAuthorization(String expectedRole) {
        if (this.loggedInUser == null) {
            return false; // Chưa đăng nhập
        }
        if (expectedRole != null && !expectedRole.equalsIgnoreCase(this.loggedInUser.getRole())) {
            return false; // Sai vai trò (Ví dụ: Seller cố gọi hàm Bid)
        }
        return true;
    }
}
