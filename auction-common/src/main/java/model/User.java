package model;

import java.io.Serial;
import java.io.Serializable;

public class User extends entity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L; // Giúp tránh lỗi khác phiên bản

    private String password;
    protected String role;
    private String status; // Thêm thuộc tính status (ACTIVE, BANNED)

    public User(int id, String name, String password, String role) {
        super(id, name);
        this.password = password;
        this.role = role;
        this.status = "ACTIVE"; // Mặc định là ACTIVE
    }

    public User() {
        super(0, "");
        this.status = "ACTIVE";
    }

    // =========================================================
    // THÊM 2 HÀM NÀY ĐỂ ĐỒNG BỘ VỚI CODE MẠNG (CLIENT/SERVER)
    // =========================================================
    public String getUsername() {
        return super.getName();
    }

    public void setUsername(String username) {
        super.setName(username);
    }
    // =========================================================

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
    
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getID() {return id; }

    public static User createUser(String role, int id, String username, String password) {
        if ("Bidder".equalsIgnoreCase(role)) {
            return new Bidder(id, username, password);
        } else if ("Seller".equalsIgnoreCase(role)) {
            return new Seller(id, username, password);
        } else if ("Admin".equalsIgnoreCase(role)) {
            return new Admin(id, username, password);
        } else {
            // Cho trường hợp role là BOTH hoặc các role khác
            return new User(id, username, password, role);
        }
    }

    // Lớp Bidder: Người tham gia đấu giá
    static class Bidder extends User implements Serializable {
        public Bidder(int id, String username, String password) {
            super(id, username, password, "Bidder");
        }
    }

    // Lớp Seller: Người đăng bán sản phẩm
    static class Seller extends User implements Serializable {
        public Seller(int id, String username, String password) {
            super(id, username, password, "Seller");
        }
    }

    // Lớp Admin: Quản lý hệ thống
    static class Admin extends User implements Serializable {
        public Admin(int id, String name, String password) {
            super(id, name, password, "Admin");
        }
    }
}