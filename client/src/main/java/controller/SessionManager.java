package controller;

import model.User;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public void logout() {
        this.currentUser = null;
    }

    // ==========================================
    // THÊM CÁC HÀM KIỂM TRA ROLE (QUYỀN HẠN) Ở ĐÂY
    // ==========================================

    // Kiểm tra xem có phải là Người bán không?
    public boolean isSeller() {
        // Trả về true NẾU đã đăng nhập VÀ role của người đó là "Seller"
        return isLoggedIn() && "Seller".equalsIgnoreCase(currentUser.getRole());
    }

    // Kiểm tra xem có phải là Người mua/Đấu giá không?
    public boolean isBidder() {
        return isLoggedIn() && "Bidder".equalsIgnoreCase(currentUser.getRole());
    }

    // Nếu sau này bạn có Admin thì thêm luôn:
    public boolean isAdmin() {
        return isLoggedIn() && "Admin".equalsIgnoreCase(currentUser.getRole());
    }
}