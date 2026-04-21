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
    private StackPane imgPane;
    @FXML
    private Label timeLabel, prdDiscription, discriptionLabel, currentPrice, priceLabel, prdName, timeLeft, InfoType;
    @FXML
    private Button Bid, previewBtn;
    @FXML
    private TextField bidAmount;

    public void initialize() {
        previewBtn.setVisible(true);
        InfoType.setVisible(true);

        timeLabel.setVisible(false);
        prdDiscription.setVisible(false);
        discriptionLabel.setVisible(false);
        currentPrice.setVisible(false);
        priceLabel.setVisible(false);
        prdName.setVisible(false);
        timeLeft.setVisible(false);
        imgPane.setVisible(false);
        Bid.setVisible(false);
        bidAmount.setVisible(false);
    }

    public void handlePreviewBtn(ActionEvent event) {
        previewBtn.setVisible(false);
        InfoType.setVisible(false);

        timeLabel.setVisible(true);
        prdDiscription.setVisible(true);
        discriptionLabel.setVisible(true);
        currentPrice.setVisible(true);
        priceLabel.setVisible(true);
        prdName.setVisible(true);
        timeLeft.setVisible(true);
        imgPane.setVisible(true);
        Bid.setVisible(true);
        bidAmount.setVisible(true);
    }
}
