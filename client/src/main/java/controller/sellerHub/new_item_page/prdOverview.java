package controller.sellerHub.new_item_page;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

public class prdOverview {

    @FXML
    private Label InfoType;
    @FXML
    private Button previewBtn;

    public void initialize() {
        previewBtn.setVisible(true);
        InfoType.setVisible(true);

    }

    public void handlePreviewBtn(ActionEvent event) {

    }
}
