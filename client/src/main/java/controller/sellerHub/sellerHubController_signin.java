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
import javafx.util.Duration;
import message.Request;
import message.Response;
import model.ActionType;
import model.User;
import network.ClientSocket;
import controller.SessionManager;

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

        User newUser = new User();
        newUser.setName(username);
        newUser.setPassword(password);
        newUser.setRole("SELLER");

        Request req = new Request(newUser, ActionType.REGISTER);

        Response res = ClientSocket.sendRequest(req);

        if (res != null && "SUCCESS".equals(res.getStatus())) {
            Status.setVisible(true);
            Status.setStyle("-fx-text-fill: green;");
            Status.setText("Account created successfully");
            PauseTransition pause1 = new PauseTransition(Duration.seconds(1));
            SessionManager.getInstance().setCurrentUser(newUser);

            pause1.setOnFinished(e -> {
                try {
                    sceneSwitcher.switchToSellerHub(event);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
            pause1.play();
        } else {
            Status.setVisible(true);
            Status.setStyle("-fx-text-fill: red;");
            Status.setText("Username existed");
        }
    }

    public void logIn(ActionEvent event) {
        String password = PassField.getText();
        String username = UsrNameField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            Status.setText("Please insert username & password");
            return;
        }

        User loginUser = new User();
        loginUser.setName(username);
        loginUser.setPassword(password);

        Request req = new Request(loginUser, ActionType.LOGIN_SELLER);

        Response res = ClientSocket.sendRequest(req);

        if (res != null && "SUCCESS".equals(res.getStatus())) {
            User userFromServer = (User) res.getData();
            if ("SELLER".equalsIgnoreCase(userFromServer.getRole())) {
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
                Status.setVisible(true);
                Status.setStyle("-fx-text-fill: red;");
                Status.setText("This is not a seller account");
                SessionManager.getInstance().logout();
            }
        } else {
            Status.setVisible(true);
            Status.setStyle("-fx-text-fill: red;");
            Status.setText("This is not a seller account");
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
                    sceneSwitcher.switchToMainPage(event);
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
        } else {
            backToBidHub.setVisible(true);
            backToBidHub.setDisable(false);
            try {
                sceneSwitcher.switchToMainPage(event);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
