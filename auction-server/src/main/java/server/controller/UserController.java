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
}