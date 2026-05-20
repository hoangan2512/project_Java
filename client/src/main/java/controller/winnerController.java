package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class winnerController {
    @FXML
    private Label prdname;

    public void setProductName(String productName) {
        if (prdname != null && productName != null) {
            prdname.setText(productName);
        }
    }

    public void handleBackBtn(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}