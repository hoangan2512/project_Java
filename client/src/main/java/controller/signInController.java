package controller;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;
import message.Request;
import message.Response;
import model.ActionType;
import model.User;
import network.ClientSocket;

public class signInController {

    @FXML
    private Label Status;
    @FXML
    private TextField UsrNameField;
    @FXML
    private PasswordField PassField;
    @FXML
    private Button LoginBtn;
    @FXML
    private Button SignInBtn;

    @FXML
    public void initialize() {
        Status.setAlignment(javafx.geometry.Pos.CENTER);
        Status.setMaxWidth(Double.MAX_VALUE);
        SignInBtn.setVisible(false);
        Status.setVisible(false);
    }

    public void signIn(ActionEvent event) {
        String password = PassField.getText();
        String username = UsrNameField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            Status.setText("Vui lòng nhập đầy đủ!");
            return;
        }

        User newUser = new User();
        newUser.setName(username);
        newUser.setPassword(password);

        // Đổi ACTION sang REGISTER
        Request req = new Request(newUser, ActionType.REGISTER);

        ClientSocket clientService = new ClientSocket();
        Response res = clientService.sendRequest(req);

        if (res != null && "SUCCESS".equals(res.getStatus())) {
            Status.setText("Tạo tài khoản thành công!");
        } else {
            Status.setText("Lỗi: Không thể tạo người dùng!");
        }
    }

    public void UserNameInput(ActionEvent event) {
        String username = UsrNameField.getText();

        if (username == null || username.trim().isEmpty()) {
            Status.setText("Please enter username!");
            Status.setStyle("-fx-text-fill: red;");
            Status.setVisible(true);
        } else {
            LoginBtn.setVisible(false);
            LoginBtn.setManaged(false);

            SignInBtn.setVisible(true);
            SignInBtn.setManaged(true);
            Status.setVisible(false);
        }
    }


}
