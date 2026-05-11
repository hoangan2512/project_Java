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

            // Ignore case when checking roles to prevent issues like "Bidder" vs "BIDDER"
            boolean isLoginRequestValid = (action == ActionType.LOGIN_BIDDER && "BIDDER".equalsIgnoreCase(role)) ||
                                          (action == ActionType.LOGIN_SELLER && "SELLER".equalsIgnoreCase(role));

            if (isLoginRequestValid) {
                // Đăng nhập thành công và đúng vai trò
                LOGGER.log(Level.INFO, "Authorization successful for user ''{0}''. Granting access.", username);
                response.setStatus("SUCCESS");
                response.setMessage("Đăng nhập thành công!");
                response.setData(authenticatedUser);
            } else {
                // Đăng nhập thành công nhưng sai vai trò
                LOGGER.log(Level.WARNING,
                        "Authorization failed for user ''{0}''. Role mismatch. Expected login type: {1}, but user role is: {2}",
                        new Object[]{username, action, role});
                response.setStatus("FAIL");
            }
        } else {
            // Xác thực thất bại (sai username/password)
            // UserRepository đã log việc này, ở đây chỉ cần trả về response
            response.setStatus("FAIL");
            response.setMessage("Sai tài khoản hoặc mật khẩu!");
        }
        return response;
    }

    public Response handleRegister(Request request) {
        Response response = new Response();
        User newUser = (User) request.getPayload();

        boolean isRegistered = userRepo.addUser(newUser);

        if (isRegistered) {
            LOGGER.log(Level.INFO, "New user registered successfully: ''{0}'' with role: {1}", new Object[]{newUser.getName(), newUser.getRole()});
            response.setStatus("SUCCESS");
            response.setMessage("Đăng ký thành công!");
            response.setData(newUser);
        } else {
            LOGGER.log(Level.WARNING, "Registration failed for username: ''{0}'' (likely a duplicate or DB error).", newUser.getName());
            response.setStatus("FAIL");
            response.setMessage("Tên tài khoản đã tồn tại hoặc có lỗi xảy ra.");
        }
        return response;
    }
}