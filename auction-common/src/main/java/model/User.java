package model;

//User.java (Abstract Class): Chứa các thuộc tính chung như username, password, role.
public abstract class User extends entity {
    private String password;
    public String role;
    public User(int id, String name, String password, String role){
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
}

