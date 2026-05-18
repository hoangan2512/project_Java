package server.controller;

import message.Request;
import message.Response;
import model.ActionType;
import model.User;
import server.network.AuctionServer;
import server.repository.UserRepository;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;

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
            
            // Xử lý trường hợp tài khoản bị khóa
            if ("BANNED".equalsIgnoreCase(authenticatedUser.getStatus())) {
                 response.setStatus("FAIL");
                 response.setMessage("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Admin.");
                 return response;
            }

            String role = authenticatedUser.getRole();
            ActionType action = request.getAction();

            // Kiểm tra role dựa trên loại login request
            boolean isLoginRequestValid = isIsLoginRequestValid(action, role);

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
                response.setMessage("Bạn không có quyền truy cập vào khu vực này."); 
            }
        } else {
            // Xác thực thất bại (sai username/password)
            response.setStatus("FAIL");
            response.setMessage("Sai tài khoản hoặc mật khẩu!");
        }
        return response;
    }

    private static boolean isIsLoginRequestValid(ActionType action, String role) {
        boolean isLoginRequestValid = false;
        if (action == ActionType.LOGIN_BIDDER) {
             isLoginRequestValid = "BIDDER".equalsIgnoreCase(role) || "BOTH".equalsIgnoreCase(role);
        } else if (action == ActionType.LOGIN_SELLER) {
             isLoginRequestValid = "SELLER".equalsIgnoreCase(role) || "BOTH".equalsIgnoreCase(role);
        } else if (action == ActionType.LOGIN_ADMIN) {
             isLoginRequestValid = "ADMIN".equalsIgnoreCase(role);
        }
        return isLoginRequestValid;
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
    
    // ==========================================
    // CÁC HÀNH ĐỘNG DÀNH CHO ADMIN QUẢN LÝ USER
    // ==========================================
    
    public Response handleGetAllUsers(Request request) {
        Response response = new Response();
        List<User> users = userRepo.getAllUsers();
        
        response.setStatus("SUCCESS");
        response.setMessage("Lấy danh sách user thành công.");
        response.setData(users);
        return response;
    }
    
    public Response handleBanUser(Request request) {
         Response response = new Response();
         // Payload gửi lên có thể là ID của User (Integer)
         Object payload = request.getPayload();
         if (payload instanceof Integer userIdToBan) {

             boolean success = userRepo.updateUserStatus(userIdToBan, "BANNED");
             if (success) {
                 response.setStatus("SUCCESS");
                 response.setMessage("Đã khóa tài khoản thành công.");
                 
                 // Gửi yêu cầu FORCE_LOGOUT tới Client đang online
                 AuctionServer.forceLogoutUser(userIdToBan);
             } else {
                 response.setStatus("FAIL");
                 response.setMessage("Không thể khóa tài khoản, vui lòng thử lại.");
             }
         } else {
             response.setStatus("FAIL");
             response.setMessage("Dữ liệu không hợp lệ.");
         }
         return response;
    }
    
    public Response handleUnbanUser(Request request) {
         Response response = new Response();
         Object payload = request.getPayload();
         if (payload instanceof Integer userIdToUnban) {

             boolean success = userRepo.updateUserStatus(userIdToUnban, "ACTIVE");
             if (success) {
                 response.setStatus("SUCCESS");
                 response.setMessage("Đã mở khóa tài khoản thành công.");
             } else {
                 response.setStatus("FAIL");
                 response.setMessage("Không thể mở khóa tài khoản, vui lòng thử lại.");
             }
         } else {
             response.setStatus("FAIL");
             response.setMessage("Dữ liệu không hợp lệ.");
         }
         return response;
    }
}
