package controller;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.util.Duration;
import message.Request;
import message.Response;
import model.ActionType;
import model.User;
import network.ClientSocket;
import controller.SessionManager;

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
    private Button SignInBtn, SignInOpt, LoginOpt, backBtn;
    @FXML
    private Line line1, line2;

    @FXML
    public void initialize() {
        Status.setAlignment(javafx.geometry.Pos.CENTER);
        Status.setMaxWidth(Double.MAX_VALUE);
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

        Request req = new Request(newUser, ActionType.REGISTER);

        Response res = ClientSocket.sendRequest(req);

        if (res != null && "SUCCESS".equals(res.getStatus())) {
            Status.setVisible(true);
            Status.setStyle("-fx-text-fill: green;");
            Status.setText("Account created successfully");
            PauseTransition pause1 = new PauseTransition(Duration.seconds(1));
            SessionManager.getInstance().setCurrentUser(newUser);

            pause1.setOnFinished(e -> {
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.close();
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

        Request req = new Request(loginUser, ActionType.LOGIN);

        Response res = ClientSocket.sendRequest(req);

        if (res != null && "SUCCESS".equals(res.getStatus())) {
            Status.setVisible(true);
            Status.setStyle("-fx-text-fill: green;");
            Status.setText("Login successful!");
            SessionManager.getInstance().setCurrentUser(loginUser);

            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(e -> {
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.close();
            });
            pause.play();

        } else {
            Status.setVisible(true);
            Status.setStyle("-fx-text-fill: red;");
            Status.setText("Error: Invalid username or password!");
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

}
