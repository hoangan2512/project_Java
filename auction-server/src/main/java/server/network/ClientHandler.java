package server.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import message.Request;
import message.Response;
import model.ActionType;
import model.User;
import server.controller.AuctionController;
import server.controller.BidController;
import server.controller.ItemController;
import server.controller.UserController;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    // --- BIẾN QUẢN LÝ SESSION (PHIÊN ĐĂNG NHẬP) ---
    private User loggedInUser = null;

    // Gọi các Controller ra để làm việc
    private UserController userController = new UserController();
    private ItemController itemController = new ItemController();
    private BidController bidController = new BidController();
    private AuctionController auctionController = new AuctionController();

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

            while (true) {
                Request request = (Request) in.readObject();
                System.out.println("Nhận yêu cầu: " + request.getAction() + " từ Client: " + (loggedInUser != null ? loggedInUser.getName() : "Khách ẩn danh"));

                // Giao việc cho hàm chia chọn
                Response response = handleBusinessLogic(request);

                out.writeObject(response);
                out.flush();
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Một Client đã ngắt kết nối. User: " + (loggedInUser != null ? loggedInUser.getName() : "Khách ẩn danh"));
        } finally {
            closeEverything();
        }
    }

    // BỘ PHẬN ĐIỀU HƯỚNG (ROUTER)
    private Response handleBusinessLogic(Request request) {
        ActionType type = request.getAction();

        switch (type) {
            case LOGIN_BIDDER:
            case LOGIN_SELLER:
                Response loginResponse = userController.handleLogin(request);
                // Nếu đăng nhập thành công, lưu lại thông tin user vào ClientHandler
                if ("SUCCESS".equals(loginResponse.getStatus()) && loginResponse.getData() instanceof User) {
                    this.loggedInUser = (User) loginResponse.getData();
                    System.out.println("=> Đã ghi nhận Session cho user: " + loggedInUser.getName());
                }
                return loginResponse;

            case REGISTER:
                Response registerResponse = userController.handleRegister(request);
                // Nếu đăng ký thành công, hệ thống tự động đăng nhập (lưu Session) luôn cho User đó
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

            // --- CÁC HÀNH ĐỘNG CẦN KIỂM TRA QUYỀN (AUTHORIZATION) ---
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
                // Tại đây, bạn có thể (và nên) ép buộc Request lấy ID của loggedInUser để đảm bảo an toàn, thay vì tin tưởng ID mà client gửi lên
                return bidController.handleBid(request);

            // --- CÁC HÀNH ĐỘNG CÔNG KHAI (KHÔNG CẦN ĐĂNG NHẬP ĐỂ XEM) ---
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

    /**
     * Hàm phụ trợ để kiểm tra xem Client này đã đăng nhập chưa và có đúng vai trò yêu cầu không.
     */
    private boolean checkAuthorization(String expectedRole) {
        if (this.loggedInUser == null) {
            return false; // Chưa đăng nhập
        }
        if (expectedRole != null && !expectedRole.equalsIgnoreCase(this.loggedInUser.getRole())) {
            return false; // Sai vai trò (Ví dụ: Seller cố gọi hàm Bid)
        }
        return true;
    }

    public void sendMessage(Object msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.err.println("Không thể gửi tin nhắn.");
        }
    }
}
