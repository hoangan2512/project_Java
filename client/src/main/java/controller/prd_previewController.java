package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.io.IOException;

public class prd_previewController {
    @FXML
    private Label prdName;
    @FXML
    private Label currentPrice;
    @FXML
    private Button Bid;
    @FXML
    private ImageView prdImage;
    @FXML
    private Label auctionStatus;

    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();

    public void setData(String name, String price, String auction_status, String imagePath) {
        prdName.setText(name);
        currentPrice.setText(price);
        auctionStatus.setText(auction_status);
        if (auction_status == null) return;

        switch (auction_status.toLowerCase()) {
            case "bidding":
                auctionStatus.setTextFill(Color.GREEN);
                break;
            case "preparing":
                auctionStatus.setTextFill(Color.RED);
                break;
            case "ended":
                auctionStatus.setTextFill(Color.WHITE);
                break;
            default:
                auctionStatus.setTextFill(Color.BLACK);
                break;
        }
        // imgProduct.setImage(new Image(getClass().getResourceAsStream(imagePath)));
    }

    public void handleBidBtn(MouseEvent event) {
        try {
            sceneSwitcher.switchToPrdPage(event);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Lỗi chuyển cảnh");
        }
    }
}
