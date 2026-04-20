package controller.sellerHub.new_item_page;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;

public class auctionInfo {
    @FXML
    private ChoiceBox<String> auction_choice;
    @FXML
    private TextField prdPrice;
    @FXML
    private TextField startTime;

    public void initialize() {
        auction_choice.getItems().addAll("1H", "5H", "10H");
    }

    public String getPrice() { return prdPrice.getText(); }
    public String getTime() { return startTime.getText(); }
    public String getChoice() { return auction_choice.getValue(); }
}
