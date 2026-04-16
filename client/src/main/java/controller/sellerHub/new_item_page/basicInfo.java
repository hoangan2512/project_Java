package controller.sellerHub.new_item_page;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class basicInfo {

    @FXML
    private Button NextBtn;
    @FXML
    private TextField prdName;
    @FXML
    private TextField ID;
    @FXML
    private TextField discription;
    @FXML
    private ChoiceBox<String> categories;
    @FXML
    private Label Status;

    public void initialize() {
        categories.getItems().addAll("Art", "Electronics", "Vehicle");
    }

    public String getPrdName() { return prdName.getText(); }
    public String getPrdId() { return ID.getText(); }
    public String getCategories() { return categories.getValue(); }
    public String getDescription() { return discription.getText(); }
}
