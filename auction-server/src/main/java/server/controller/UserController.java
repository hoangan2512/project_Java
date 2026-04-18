package server.controller;

import message.Request;
import message.Response;
import model.User;
import server.repository.UserRepository;

public class UserController {
    private UserRepository userRepo;

    public UserController() {
        this.userRepo = new UserRepository();
    }

    public Response handleLogin(Request request) {
        Response response = new Response();
        User userFromClient = (User) request.getPayload();

        User authenticatedUser = userRepo.login(userFromClient.getName(), userFromClient.getPassword());

        if (authenticatedUser != null) {
            response.setStatus("SUCCESS");
            response.setMessage("Đăng nhập thành công!");
            response.setData(authenticatedUser);
            System.out.println("Đăng nhập thành công cho: " + authenticatedUser.getName());
        } else {
            response.setStatus("FAIL");
            response.setMessage("Sai tài khoản hoặc mật khẩu!");
            System.out.println("Đăng nhập thất bại cho: " + userFromClient.getName());
        }
        return response;
    }

    public Response handleRegister(Request request) {
        Response response = new Response();
        User newUser = (User) request.getPayload();

        boolean isRegistered = userRepo.addUser(newUser);

        if (isRegistered) {
            response.setStatus("SUCCESS");
            response.setMessage("Đăng ký thành công! Chào mừng " + newUser.getName());
            response.setData(newUser);
            System.out.println("Đã lưu User mới vào DB: " + newUser.getName());
        } else {
            response.setStatus("FAIL");
            response.setMessage("Đăng ký thất bại! Tên tài khoản đã tồn tại.");
            System.out.println("Lỗi khi đăng ký User!");
        }
        return response;
    }
}