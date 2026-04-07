package model;

//User.java (Abstract Class): Chứa các thuộc tính chung như username, password, role.

public abstract class User extends entity {
    private String password;
    protected String role;

    public User(int id, String name, String password, String role) {
        super(id, name);
        this.password = password;
        this.role = role;
    }


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

    public boolean authenticate(String inputPassword) {
        return this.password.equals(inputPassword);
    }

    public abstract void displayInfo();

    public static User createUser(String role, int id, String username, String password) {
        if ("Bidder".equalsIgnoreCase(role)) {
            return new Bidder(id, username, password);
        } else if ("Seller".equalsIgnoreCase(role)) {
            return new Seller(id, username, password);
        } else {
            return new Admin(id, username, password);
        }
    }
    // Lớp Bidder: Người tham gia đấu giá
    static class Bidder extends User {

        public Bidder(int id, String username, String password) {
            // Cần truyền 4 tham số: id, name, password, role
            super(id, username, password, "Bidder");

        }

        @Override
        public void displayInfo() { // Kiểm tra xem lớp cha dùng displayInfo hay dissplayInfo
            System.out.println("[Bidder] " + getName());
        }
    }

    // Lớp Seller: Người đăng bán sản phẩm
    static class Seller extends User {
        public Seller(int id, String username, String password) {
            super(id, username, password, "Seller");
        }

        @Override
        public void displayInfo() {
            System.out.println("[Seller] " + getName());
        }
    }


    // Lớp Admin: Quản lý hệ thống
    static class Admin extends User {

        public Admin(int id, String name, String password) {
            // Truyền đủ 4 tham số: id, name, password, role
            super(id, name, password, "Admin");
        }

        @Override
        public void displayInfo() {
            // Lưu ý: Dùng getName() thay vì getUsername() nếu lớp cha đặt là name
            System.out.println("[Admin] ID: " + id + " | Tên: " + getName() + " | Quyền: " + getRole());
        }


        // Chức năng riêng của Admin (Ví dụ: Khóa người dùng)
        public void blockUser(User user) {
            System.out.println("Admin " + getName() + " đã khóa người dùng: " + user.getName());
        }
    }


}