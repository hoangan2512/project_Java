package controller;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.util.Duration;
import message.Request;
import message.Response;
import model.ActionType;
import model.User;
import network.ClientSocket;
import controller.SessionManager;
import security.RSA;

import java.awt.*;
import java.net.URI;

public class signInController {

    @FXML
    private Label Status, OR;
    @FXML
    private TextField UsrNameField;
    @FXML
    private PasswordField PassField;
    @FXML
    private Button LoginBtn;
    @FXML
    private Button SignInBtn, SignInOpt, LoginOpt, backBtn, LogoutBtn;
    @FXML
    private Line line1, line2;
    @FXML
    private AnchorPane google_login_pane;

    @FXML
    public void initialize() {
        if (SessionManager.getInstance().isBidder()) {
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
            google_login_pane.setVisible(false);

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
            google_login_pane.setVisible(true);
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
        newUser.setRole("BIDDER");

        Request req = new Request(newUser, ActionType.REGISTER);

        Response res = ClientSocket.sendRequest(req);

        if (res != null && "SUCCESS".equals(res.getStatus())) {
            Status.setVisible(true);
            Status.setStyle("-fx-text-fill: green;");
            Status.setText("Account created successfully");
            PauseTransition pause1 = new PauseTransition(Duration.seconds(1));
            
            // Cập nhật session user với dữ liệu được Server trả về (đã bao gồm ID thực từ Database)
            User userFromServer = (User) res.getData();
            if (userFromServer != null) {
                SessionManager.getInstance().setCurrentUser(userFromServer);
            } else {
                SessionManager.getInstance().setCurrentUser(newUser);
            }

            pause1.setOnFinished(e -> {
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.close();
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

        Request req = new Request(loginUser, ActionType.LOGIN_BIDDER);

        Response res = ClientSocket.sendRequest(req);

        if (res != null && "SUCCESS".equals(res.getStatus())) {
            User userFromServer = (User) res.getData();

            // Sửa lỗi: Cập nhật kiểm tra vai trò để cho phép BOTH
            if ("BIDDER".equalsIgnoreCase(userFromServer.getRole()) || "BOTH".equalsIgnoreCase(userFromServer.getRole())) {
                SessionManager.getInstance().setCurrentUser(userFromServer);
                Status.setVisible(true);
                Status.setStyle("-fx-text-fill: green;");
                Status.setText("Login successful!");

                PauseTransition pause = new PauseTransition(Duration.seconds(1));
                pause.setOnFinished(e -> {
                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    stage.close();
                });
                pause.play();

            } else {
                Status.setVisible(true);
                Status.setStyle("-fx-text-fill: red;");
                Status.setText("This is not a bidder account");
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

        if (SessionManager.getInstance().isBidder()) {
            Status.setVisible(true);
            Status.setStyle("-fx-text-fill: red;");
            Status.setText("Error logging out, try again!");
        } else {
            Status.setVisible(true);
            Status.setStyle("-fx-text-fill: white;");
            Status.setText("Logged Out. See you later!");

            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(e -> {
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.close();
            });
            pause.play();
        }
    }

    @FXML
    private void handleGoogleLogin(ActionEvent event) {
        // Thay thế bằng Client ID thực tế của bạn tạo trên Google Cloud
        String clientId = "887547914295-i912u5c51mm9ur6s7kd1pr5kipmpcgka.apps.googleusercontent.com";
        String redirectUri = "http://localhost:8080";

        // Tạo URL dẫn tới trung tâm xác thực của Google
        String googleAuthUrl = "https://accounts.google.com/o/oauth2/v2/auth?"
                + "scope=email%20profile"
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&client_id=" + clientId;

        // 1. Dùng một Thread độc lập chạy ngầm mở trình duyệt và hứng mã để không gây đơ (Freeze) UI JavaFX
        new Thread(() -> {
            try {
                // Mở trình duyệt mặc định của hệ điều hành hiển thị trang đăng nhập Google
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(googleAuthUrl));
                }

                // 2. Kích hoạt bộ đón code (Hàm này sẽ treo luồng ngầm này lại đợi trình duyệt bắn code về)
                String code = ClientSocket.startLocalServerToGetCode();

                if (code != null) {
                    System.out.println("[CLIENT] Đã bắt được mã Code từ Google: " + code);

                    // 3. Đóng gói mã code vào Request gửi qua Socket chính về Server xử lý DB
                    Request req = new Request(code, ActionType.GOOGLE_LOGIN);
                    Response res = ClientSocket.sendRequest(req);

                    Platform.runLater(() -> {
                        if (res != null && "SUCCESS".equals(res.getStatus())) {
                            System.out.println("Đăng nhập Google hoàn tất! Chuyển trang thôi.");
                            
                            // 4. Ghi trạng thái đã đăng nhập vào SessionManager
                            User userFromServer = (User) res.getData();
                            SessionManager.getInstance().setCurrentUser(userFromServer);
                            
                            // 5. Cập nhật giao diện (Label Status)
                            Status.setVisible(true);
                            Status.setStyle("-fx-text-fill: green;");
                            Status.setText("Google Login successful!");

                            // 6. Đóng popup scene login/signin đang mở sau 1 khoảng trễ
                            PauseTransition pause = new PauseTransition(Duration.seconds(1));
                            pause.setOnFinished(e -> {
                                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                                stage.close();
                            });
                            pause.play();

                        } else {
                            Status.setVisible(true);
                            Status.setStyle("-fx-text-fill: red;");
                            Status.setText(res != null && res.getMessage() != null ? res.getMessage() : "Google Auth failed!");
                            System.err.println("Xác thực Google thất bại tại Server!");
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}