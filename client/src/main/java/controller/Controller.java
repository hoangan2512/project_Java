package controller;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class Controller {

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
