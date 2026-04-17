package controller; // Hoặc package chứa logic client của bạn

import model.User;

public class SessionManager {
    // 1. Biến static lưu trữ instance duy nhất
    private static SessionManager instance;

    // 2. Biến lưu trữ người dùng hiện tại
    private User currentUser;

    // Chặn không cho dùng từ khóa 'new' ở ngoài
    private SessionManager() {}

    // 3. Cánh cửa duy nhất để gọi cái túi này ra
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // 4. Các hàm thao tác (Cất vào, lấy ra, kiểm tra)
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null; // Có data nghĩa là đã đăng nhập
    }

    public void logout() {
        this.currentUser = null; // Xóa data khi đăng xuất
    }
}