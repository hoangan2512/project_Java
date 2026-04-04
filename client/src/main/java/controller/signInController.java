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

public class signInController {

    @FXML
    private Label Status;
    @FXML
    private TextField UsrNameField;
    @FXML
    private PasswordField PassField;
    @FXML
    private Button NextBtn;
    @FXML
    private Button SignInBtn;

    @FXML
    public void initialize() {
        Status.setAlignment(javafx.geometry.Pos.CENTER);
        Status.setMaxWidth(Double.MAX_VALUE);
        PassField.setVisible(false);
        PassField.setManaged(false);
        SignInBtn.setVisible(false);
        SignInBtn.setManaged(false);
        Status.setVisible(false);
    }

    public void signIn(ActionEvent event) {
        String password = PassField.getText();

        if ("123".equals(password)) {
            Status.setText("Signing In...");
            Status.setVisible(true);

            PauseTransition pause1 = new PauseTransition(Duration.seconds(1));

            pause1.setOnFinished(e -> {

                Status.setText("Sign In Successfully!");
                Status.setStyle("-fx-text-fill: #4CAF50;");

                PauseTransition pause2 = new PauseTransition(Duration.seconds(0.3));

                pause2.setOnFinished(closeEvent -> {
                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    stage.close();
                });
                pause2.play();
            });
            pause1.play();
        } else {
            Status.setText("Wrong password!");
            Status.setStyle("-fx-text-fill: red; -fx-font-weight: bold");
            Status.setVisible(true);
            PauseTransition pause3 = new PauseTransition(Duration.seconds(2));

            pause3.setOnFinished(e -> {
                Status.setVisible(false);
            });
            pause3.play();
        }
    }

    public void UserNameInput(ActionEvent event) {
        String username = UsrNameField.getText();

        if (username == null || username.trim().isEmpty()) {
            Status.setText("Please enter username!");
            Status.setStyle("-fx-text-fill: red;");
            Status.setVisible(true);
        } else {
            UsrNameField.setVisible(false);
            UsrNameField.setManaged(false);
            NextBtn.setVisible(false);
            NextBtn.setManaged(false);

            PassField.setVisible(true);
            PassField.setManaged(true);
            SignInBtn.setVisible(true);
            SignInBtn.setManaged(true);
            Status.setVisible(false);
        }
    }


}
