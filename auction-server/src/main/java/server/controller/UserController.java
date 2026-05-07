package server.controller;

import message.Request;
import message.Response;
import model.ActionType;
import model.User;
import server.repository.UserRepository;

import java.util.logging.Level;
import java.util.logging.Logger;

public class UserController {
    private static final Logger LOGGER = Logger.getLogger(UserController.class.getName());
    private final UserRepository userRepo;

    public UserController() {
        this.userRepo = new UserRepository();
    }

    public Response handleLogin(Request request) {
        Response response = new Response();
        User userFromClient = (User) request.getPayload();
        String username = userFromClient.getName();

        // Bước 1: Xác thực người dùng (Authentication)
        User authenticatedUser = userRepo.login(username, userFromClient.getPassword());

        // Bước 2: Kiểm tra quyền hạn (Authorization)
        if (authenticatedUser != null) {
            String role = authenticatedUser.getRole();
            ActionType action = request.getAction();

            // SỬA LỖI Ở ĐÂY: Cần kiểm tra xem User có cả 2 quyền hoặc chỉ cần login thành công là đủ
            // Trong nhiều hệ thống, người dùng vừa có thể là Bidder, vừa có thể là Seller
            // Tạm thời, ta có thể bỏ kiểm tra strict này, hoặc cho phép nếu vai trò khớp với yêu cầu
            boolean isLoginRequestValid = false;
            if (action == ActionType.LOGIN_BIDDER) {
                 isLoginRequestValid = "BIDDER".equalsIgnoreCase(role) || "BOTH".equalsIgnoreCase(role);
            } else if (action == ActionType.LOGIN_SELLER) {
                 isLoginRequestValid = "SELLER".equalsIgnoreCase(role) || "BOTH".equalsIgnoreCase(role);
            }

            // Nếu hệ thống chỉ cho phép 1 role duy nhất và bạn gặp lỗi khi Seller cố login vào BidHub
            // Chúng ta có thể cung cấp lỗi rõ ràng hơn, hoặc tạm thời bypass nếu đang test
            if (isLoginRequestValid) {
                // Đăng nhập thành công và đúng vai trò
                LOGGER.log(Level.INFO, "Authorization successful for user ''{0}''. Granting access.", username);
                response.setStatus("SUCCESS");
                response.setMessage("Đăng nhập thành công!");
                response.setData(authenticatedUser);
            } else {
                // Đăng nhập thành công nhưng sai vai trò (Seller cố login vào BidHub hoặc ngược lại)
                LOGGER.log(Level.WARNING,
                        "Authorization failed for user ''{0}''. Role mismatch. Expected login type: {1}, but user role is: {2}",
                        new Object[]{username, action, role});
                response.setStatus("FAIL");
                // Thông báo chi tiết hơn để Client biết
                response.setMessage("Bạn không có quyền truy cập vào khu vực này."); 
            }
        } else {
            // Xác thực thất bại (sai username/password)
            response.setStatus("FAIL");
            response.setMessage("Sai tài khoản hoặc mật khẩu!");
        }
        return response;
    }

    public Response handleRegister(Request request) {
        Response response = new Response();
        User newUser = (User) request.getPayload();

        // Thêm bước kiểm tra tài khoản đã tồn tại hay chưa
        if (userRepo.isUserExists(newUser.getName())) {
            LOGGER.log(Level.WARNING, "Registration failed: Username ''{0}'' already exists.", newUser.getName());
            response.setStatus("FAIL");
            response.setMessage("Tài khoản đã tồn tại.");
            return response;
        }

        boolean isRegistered = userRepo.addUser(newUser);

        if (isRegistered) {
            LOGGER.log(Level.INFO, "New user registered successfully: ''{0}'' with role: {1}", new Object[]{newUser.getName(), newUser.getRole()});
            response.setStatus("SUCCESS");
            response.setMessage("Đăng ký thành công!");
            response.setData(newUser);
        } else {
            LOGGER.log(Level.SEVERE, "Registration failed for username: ''{0}'' due to a database error.", newUser.getName());
            response.setStatus("FAIL");
            response.setMessage("Có lỗi xảy ra trong quá trình đăng ký.");
        }
        return response;
    }
}
