package server.controller;

import message.Request;
import message.Response;
import model.User;
import server.repository.UserRepository;

public class UserController {
    private final UserRepository userRepo; // Dùng final cho an toàn

    public UserController() {
        this.userRepo = new UserRepository();
    }

    // 1. XỬ LÝ ĐĂNG NHẬP
    public Response handleLogin(Request request) {
        // Khởi tạo Response trống để tránh lỗi "Cannot resolve symbol data"
        Response response = new Response();
        User userFromClient = (User) request.getPayload();

        User authenticatedUser = userRepo.login(userFromClient.getName(), userFromClient.getPassword());

        if (authenticatedUser != null) {
            response.setStatus("SUCCESS");
            response.setMessage("Đăng nhập thành công!");
            response.setData(authenticatedUser); // Trả về object User để Client lưu session
        } else {
            response.setStatus("FAIL");
            response.setMessage("Sai tài khoản hoặc mật khẩu!");
        }
        return response;
    }

    // 2. XỬ LÝ ĐĂNG KÝ
    public Response handleRegister(Request request) {
        Response response = new Response();
        User newUser = (User) request.getPayload();

        boolean isRegistered = userRepo.addUser(newUser);

        if (isRegistered) {
            response.setStatus("SUCCESS");
            response.setMessage("Đăng ký thành công!");
            response.setData(newUser);
        } else {
            response.setStatus("FAIL");
            response.setMessage("Tên tài khoản đã tồn tại hoặc lỗi DB.");
        }
        return response;
    }

    // 3. XỬ LÝ KIỂM TRA SỐ DƯ
    public Response handleBalance(Request request) {
        Response response = new Response();
        // Kiểm tra payload để tránh crash (NullPointerException)
        if (request.getPayload() instanceof Integer) {
            int userId = (Integer) request.getPayload();
            double balance = userRepo.getBalance(userId);

            response.setStatus("SUCCESS");
            response.setData(balance); // Gửi số dư về
            response.setMessage("Lấy số dư thành công");
        } else {
            response.setStatus("FAIL");
            response.setMessage("Dữ liệu yêu cầu không hợp lệ.");
        }
        return response;
    }

    // 4. XỬ LÝ NẠP TIỀN
    public Response handleDeposit(Request request) {
        Response response = new Response();
        // Giả sử Client gửi mảng [userId, amount]
        if (request.getPayload() instanceof Object[]) {
            Object[] payloadData = (Object[]) request.getPayload();
            int userId = (Integer) payloadData[0];
            double amount = (double) payloadData[1];

            boolean success = userRepo.updateBalance(userId, amount);
            if (success) {
                response.setStatus("SUCCESS");
                // Sau khi nạp, trả về số dư mới nhất luôn cho tiện hiển thị
                response.setData(userRepo.getBalance(userId));
                response.setMessage("Nạp tiền thành công!");
            } else {
                response.setStatus("FAIL");
                response.setMessage("Lỗi khi cập nhật số dư!");
            }
        }
        return response;
    }
}