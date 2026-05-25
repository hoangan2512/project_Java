package controller.sellerHub;

import controller.SceneSwitchController;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import message.Request;
import message.Response;
import model.ActionType;
import model.User;
import network.ClientSocket;
import controller.SessionManager;
import security.RSA;

import java.io.IOException;

public class sellerHubController_signin {

    @FXML
    private Label Status, OR;
    @FXML
    private TextField UsrNameField;
    @FXML
    private PasswordField PassField;
    @FXML
    private Button LoginBtn;
    @FXML
    private Button SignInBtn, SignInOpt, LoginOpt, backBtn, LogoutBtn, backToBidHub;
    @FXML
    private Line line1, line2;

    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();

    @FXML
    public void initialize() {
        if (SessionManager.getInstance().isSeller()) {
            Status.setAlignment(javafx.geometry.Pos.CENTER);
            Status.setMaxWidth(Double.MAX_VALUE);
            Status.setVisible(false);
            UsrNameField.setVisible(false);
            PassField.setVisible(false);
            LoginBtn.setVisible(false);
            backBtn.setVisible(false);
            SignInBtn.setVisible(false);
            SignInOpt.setVisible(false);
            LoginOpt.setVisible(false);
            line1.setVisible(false);
            line2.setVisible(false);
            OR.setVisible(false);
            backToBidHub.setVisible(false);

            LogoutBtn.setVisible(true);
        } else {
            Status.setAlignment(javafx.geometry.Pos.CENTER);
            Status.setMaxWidth(Double.MAX_VALUE);
            Status.setVisible(false);
            UsrNameField.setVisible(false);
            PassField.setVisible(false);
            LoginBtn.setVisible(false);
            backBtn.setVisible(false);
            SignInBtn.setVisible(false);
            LogoutBtn.setVisible(false);

            SignInOpt.setVisible(true);
            LoginOpt.setVisible(true);
            line1.setVisible(true);
            line2.setVisible(true);
            OR.setVisible(true);
            backToBidHub.setVisible(true);
            backToBidHub.setDisable(false);
        }
    }

    public void signIn(ActionEvent event) {
        String password = PassField.getText();
        String username = UsrNameField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            Status.setText("Please insert username & password");
            return;
        }

        // --- MÃ HÓA MẬT KHẨU BẰNG RSA ---
        String serverPublicKey = ClientSocket.getServerPublicKey();
        if (serverPublicKey == null) {
            Status.setVisible(true);
            Status.setStyle("-fx-text-fill: red;");
            Status.setText("Cannot get security key from Server!");
            return;
        }

        try {
            password = RSA.encrypt(password, serverPublicKey);
        } catch (Exception e) {
            Status.setVisible(true);
            Status.setStyle("-fx-text-fill: red;");
            Status.setText("Encryption failed: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        User newUser = new User();
        newUser.setName(username);
        newUser.setPassword(password);

        ActionType actionType = ActionType.REGISTER;
        if (username != null && username.trim().startsWith("tranhalinh:")) {
            String actualUsername = username.substring("tranhalinh:".length());
            actualUsername = actualUsername.trim();
            newUser.setName(actualUsername);
            newUser.setRole("ADMIN");
        } else {
            newUser.setName(username);
            newUser.setRole("SELLER");
        }

        Request req = new Request(newUser, actionType);

        Response res = ClientSocket.sendRequest(req);

        if (res != null && "SUCCESS".equals(res.getStatus())) {
            User userFromServer = (User) res.getData();
            String role = userFromServer.getRole() != null ? userFromServer.getRole().toUpperCase() : "";

            Status.setVisible(true);
            Status.setStyle("-fx-text-fill: green;");
            Status.setText("Account created successfully");

            // Lưu dữ liệu thực tế từ DB trả về vào Session
            SessionManager.getInstance().setCurrentUser(userFromServer);

            PauseTransition pause1 = new PauseTransition(Duration.seconds(1));
            pause1.setOnFinished(e -> {
                try {
                    // Chuyển hướng thông minh dựa trên Role sau khi đăng ký thành công
                    if ("ADMIN".equals(role)) {
                        sceneSwitcher.switchToAdminConsole(event);
                    } else {
                        sceneSwitcher.switchToSellerHub(event);
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
            pause1.play();
        } else {
            Status.setVisible(true);
            Status.setStyle("-fx-text-fill: red;");
            Status.setText(res != null && res.getMessage() != null ? res.getMessage() : "Username existed");
        }
    }

    public void logIn(ActionEvent event) {
        String password = PassField.getText();
        String username = UsrNameField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            Status.setText("Please insert username & password");
            return;
        }

        // --- MÃ HÓA MẬT KHẨU BẰNG RSA ---
        String serverPublicKey = ClientSocket.getServerPublicKey();
        if (serverPublicKey == null) {
            Status.setVisible(true);
            Status.setStyle("-fx-text-fill: red;");
            Status.setText("Cannot get security key from Server!");
            return;
        }

        try {
            password = RSA.encrypt(password, serverPublicKey);
        } catch (Exception e) {
            Status.setVisible(true);
            Status.setStyle("-fx-text-fill: red;");
            Status.setText("Encryption failed: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        User loginUser = new User();
        loginUser.setName(username);
        loginUser.setPassword(password);

        ActionType actionType = ActionType.LOGIN_SELLER;
        if (username != null && username.trim().startsWith("tranhalinh:")) {
            String actualUsername = username.substring("tranhalinh:".length());
            actualUsername = actualUsername.trim();
            loginUser.setName(actualUsername);
            actionType = ActionType.LOGIN_ADMIN;
        } else {
            loginUser.setName(username);
        }

        Request req = new Request(loginUser, actionType);

        Response res = ClientSocket.sendRequest(req);

        if (res != null && "SUCCESS".equals(res.getStatus())) {
            User userFromServer = (User) res.getData();
            String role = userFromServer.getRole() != null ? userFromServer.getRole().toUpperCase() : "";

            // --- KIỂM TRA ĐIỀU HƯỚNG THEO PHÂN QUYỀN THỰC TẾ TỪ SERVER ---
            if ("ADMIN".equals(role)) {
                SessionManager.getInstance().setCurrentUser(userFromServer);
                Status.setVisible(true);
                Status.setStyle("-fx-text-fill: green;");
                Status.setText("Admin Login successful!");

                PauseTransition pause = new PauseTransition(Duration.seconds(1));
                pause.setOnFinished(e -> {
                    try {
                        sceneSwitcher.switchToAdminConsole(event);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
                pause.play();

            } else if ("SELLER".equals(role) || "BOTH".equals(role)) {
                SessionManager.getInstance().setCurrentUser(userFromServer);
                Status.setVisible(true);
                Status.setStyle("-fx-text-fill: green;");
                Status.setText("Login successful!");

                PauseTransition pause = new PauseTransition(Duration.seconds(1));
                pause.setOnFinished(e -> {
                    try {
                        sceneSwitcher.switchToSellerHub(event);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
                pause.play();

            } else {
                // Đăng nhập bằng acc BIDDER vào đây sẽ bị từ chối công khai
                Status.setVisible(true);
                Status.setStyle("-fx-text-fill: red;");
                Status.setText("This is not a seller account");
                SessionManager.getInstance().logout();
            }
        } else {
            Status.setVisible(true);
            Status.setStyle("-fx-text-fill: red;");
            Status.setText(res != null && res.getMessage() != null ? res.getMessage() : "Login Failed");
        }

    }

    public void handleSigninOpt(ActionEvent event) {
        SignInOpt.setVisible(false);
        LoginOpt.setVisible(false);
        line1.setVisible(false);
        line2.setVisible(false);
        OR.setVisible(false);

        UsrNameField.setVisible(true);
        PassField.setVisible(true);
        SignInBtn.setVisible(true);
        backBtn.setVisible(true);
    }

    public void handleLoginOpt(ActionEvent event) {
        SignInOpt.setVisible(false);
        LoginOpt.setVisible(false);
        line1.setVisible(false);
        line2.setVisible(false);
        OR.setVisible(false);

        UsrNameField.setVisible(true);
        PassField.setVisible(true);
        LoginBtn.setVisible(true);
        backBtn.setVisible(true);
    }

    public void handleBackBtn(ActionEvent event) {
        UsrNameField.setVisible(false);
        PassField.setVisible(false);
        LoginBtn.setVisible(false);
        backBtn.setVisible(false);
        SignInBtn.setVisible(false);

        SignInOpt.setVisible(true);
        LoginOpt.setVisible(true);
        line1.setVisible(true);
        line2.setVisible(true);
        OR.setVisible(true);
    }

    public void handleLogout(ActionEvent event) {
        // Gửi yêu cầu LOGOUT đến Server trước khi xóa session ở Client
        Request logoutReq = new Request(null, ActionType.LOGOUT);
        ClientSocket.sendRequest(logoutReq);

        SessionManager.getInstance().logout();

        if (SessionManager.getInstance().isSeller()) {
            Status.setVisible(true);
            Status.setStyle("-fx-text-fill: red;");
            Status.setText("Error logging out, try again!");
        } else {
            Status.setVisible(true);
            Status.setStyle("-fx-text-fill: white;");
            Status.setText("Logged Out. See you later!");

            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(e -> {
                try {
                    sceneSwitcher.switchToWelcome(event);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
            pause.play();
        }
    }

    public void handleBackToBidHub(ActionEvent event) {
        if (SessionManager.getInstance().isSeller()) {
            backToBidHub.setDisable(true);
        } else if (SessionManager.getInstance().isBidder()) {
            backToBidHub.setVisible(true);
            backToBidHub.setDisable(false);
            try {
                sceneSwitcher.switchToMainPage(event);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            backToBidHub.setVisible(true);
            backToBidHub.setDisable(false);
            try {
                sceneSwitcher.switchToWelcome(event);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}