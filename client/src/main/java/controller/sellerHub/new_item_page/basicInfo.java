package controller.sellerHub.new_item_page;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;

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
    private MenuButton categories;
    @FXML
    private Label Status;

    public String getPrdName() { return prdName.getText(); }
    public String getPrdId() { return ID.getText(); }
    public String getDescription() { return discription.getText(); }
}
