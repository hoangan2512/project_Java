package controller;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;

public class signInController {

    private Stage stage;
    private Scene scene;
    private Parent root;

    @FXML
    private Label Status;

    public void signIn(ActionEvent event) {
        Status.setVisible(true);
        System.out.println("Singning In");

        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> {
            Status.setVisible(false);
        });
        pause.play();
    }
}
