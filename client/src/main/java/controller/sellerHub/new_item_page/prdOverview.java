package controller.sellerHub.new_item_page;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class prdOverview {

    @FXML
    private Label InfoType;
    @FXML
    private Button previewBtn;

    // Biến lưu trữ hành động callback
    private Runnable onPreviewCallback;

    public void initialize() {
        previewBtn.setVisible(true);
        InfoType.setVisible(true);
    }

    // Hàm để Controller cha (sellerHubController_homepage) truyền hành động vào
    public void setOnPreviewCallback(Runnable onPreviewCallback) {
        this.onPreviewCallback = onPreviewCallback;
    }

    @FXML
    public void handlePreviewBtn(ActionEvent event) {
        // Nếu callback đã được cài đặt thì chạy nó
        if (onPreviewCallback != null) {
            onPreviewCallback.run();
        }
    }
}