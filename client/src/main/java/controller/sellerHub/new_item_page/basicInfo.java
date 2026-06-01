package controller.sellerHub.new_item_page;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class basicInfo {

    @FXML
    private TextField prdName;
    @FXML
    private TextField ID;
    @FXML
    private TextField discription;
    @FXML
    private ChoiceBox<String> categories;

    public void initialize() {
        categories.getItems().addAll("Art", "Electronics", "Vehicle", "Antiques", "Real Estate", "Exclusive Product");
        categories.setValue("Choose product categories");
    }

    public String getPrdName() { return prdName.getText(); }
    public String getPrdId() { return ID.getText(); }
    public String getCategories() { return categories.getValue(); }
    public String getDescription() { return discription.getText(); }

    public void setDraftData(ProductDraftDTO draft) {
        if (draft == null) return;

        // Gán lại dữ liệu vào các ô text (thay tên biến field cho đúng với code của bạn)
        if (draft.getName() != null) prdName.setText(draft.getName());
        if (draft.getId() != null) ID.setText(draft.getId());
        if (draft.getDescription() != null) discription.setText(draft.getDescription());
        if (draft.getCategories() != null) categories.setValue(draft.getCategories());
    }
}
