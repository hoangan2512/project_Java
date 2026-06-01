package controller;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.application.Platform;

import model.SearchCriteria;

public class filterController {
    // categories
    @FXML
    private ToggleButton art;
    @FXML
    private ToggleButton electronics;
    @FXML
    private ToggleButton vehicle;
    @FXML
    private ToggleButton real_estate;

    // price range
    @FXML
    private ToggleButton price1; // Under 1M
    @FXML
    private ToggleButton price2; // 1M - 10M
    @FXML
    private ToggleButton price3; // Over 10M
    @FXML
    private TextField lowest;
    @FXML
    private TextField highest;

    // auction status
    @FXML
    private ToggleButton bidding;
    @FXML
    private ToggleButton newly_listed;
    @FXML
    private ToggleButton ending_soon;
    @FXML
    private ToggleButton upcoming;
    @FXML
    private ToggleButton ended;

    // auction id
    @FXML
    private TextField auctionID;

    // root
    @FXML
    private Button searchBtn;
    @FXML
    private Button backBtn;
    @FXML
    private Button clear_btn;
    @FXML
    private Label status;

    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();
    private SearchCriteria currentCriteria;

    @FXML
    public void initialize() {
        // Gắn hiệu ứng format tiền Việt cho 2 ô nhập giá
        addCurrencyFormat(lowest);
        addCurrencyFormat(highest);
    }
    
    // --- HÀM MỚI: TỰ ĐỘNG CHỌN LẠI CÁC TRƯỜNG ĐÃ LỌC TRƯỚC ĐÓ ---
    public void setInitialCriteria(SearchCriteria criteria) {
        this.currentCriteria = criteria;
        if (criteria == null) return;

        // Khôi phục Categories
        if (criteria.getCategories() != null) {
            if (criteria.getCategories().contains("Art")) art.setSelected(true);
            if (criteria.getCategories().contains("Electronics")) electronics.setSelected(true);
            if (criteria.getCategories().contains("Vehicle")) vehicle.setSelected(true);
            if (criteria.getCategories().contains("Real Estate")) real_estate.setSelected(true);
        }

        // Khôi phục Status
        if (criteria.getStatuses() != null) {
            if (criteria.getStatuses().contains("Bidding")) bidding.setSelected(true);
            if (criteria.getStatuses().contains("Newly Listed")) newly_listed.setSelected(true);
            if (criteria.getStatuses().contains("Ending Soon")) ending_soon.setSelected(true);
            if (criteria.getStatuses().contains("Upcoming")) upcoming.setSelected(true);
            if (criteria.getStatuses().contains("FINISHED")) ended.setSelected(true);
        }

        // Khôi phục ID
        if (criteria.getAuctionId() != null) {
            auctionID.setText(criteria.getAuctionId());
        }

        // Khôi phục Price
        if (criteria.getMinPrice() > 0) {
            lowest.setText(String.valueOf(criteria.getMinPrice())); 
        }
        if (criteria.getMaxPrice() > 0) {
            highest.setText(String.valueOf(criteria.getMaxPrice()));
        }
    }

    private void addCurrencyFormat(TextField textField) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);

        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;

            String numericString = newValue.replaceAll("[^\\d]", "");
            if (numericString.isEmpty()) {
                textField.setText("");
                return;
            }

            try {
                long value = Long.parseLong(numericString);
                String formattedString = formatter.format(value);

                if (!newValue.equals(formattedString)) {
                    textField.setText(formattedString);
                    Platform.runLater(() -> textField.positionCaret(formattedString.length()));
                }
            } catch (NumberFormatException e) {
                textField.setText(oldValue);
            }
        });
    }

    private long getRealPrice(TextField textField) {
        String text = textField.getText();
        if (text == null || text.trim().isEmpty()) return 0;

        String cleanString = text.replaceAll("\\.", "");
        try {
            return Long.parseLong(cleanString);
        } catch (NumberFormatException e) {
            System.err.println("Lỗi ép kiểu số: " + cleanString);
            return 0;
        }
    }

    @FXML
    public void handleBackBtn(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    public void handleClearBtn(ActionEvent event) {
        // Gom tất cả các nút và trường nhập liệu vào mảng
        ToggleButton[] allToggleButtons = {art, electronics, vehicle, real_estate, price1, price2, price3, bidding, newly_listed, ending_soon, upcoming, ended};
        TextField[] allTextFields = {lowest, highest, auctionID};

        // Bỏ chọn tất cả các ToggleButton
        for (ToggleButton btn : allToggleButtons) {
            if (btn != null) {
                btn.setSelected(false);
            }
        }

        // Xóa trắng tất cả các TextField
        for (TextField tf : allTextFields) {
            if (tf != null) {
                tf.clear();
            }
        }
    }

    @FXML
    public void handleCustomSearch(ActionEvent event) {
        // 1. Gom nhóm các nút
        ToggleButton[] categoryBtns = {art, electronics, vehicle, real_estate};
        ToggleButton[] priceBtns = {price1, price2, price3};
        ToggleButton[] statusBtns = {bidding, newly_listed, ending_soon, upcoming, ended};

        String lowPrice = lowest.getText().trim();
        String highPrice = highest.getText().trim();
        String id = auctionID.getText().trim();

        // TẠO DTO ĐÓNG GÓI DỮ LIỆU TÌM KIẾM
        SearchCriteria criteria = this.currentCriteria != null ? this.currentCriteria : new SearchCriteria();
        criteria.setCategories(getSelectedNames(categoryBtns));
        criteria.setMinPrice(getRealPrice(lowest));
        criteria.setMaxPrice(getRealPrice(highest));
        criteria.setStatuses(getSelectedNames(statusBtns));
        criteria.setAuctionId(id);

        // ĐÓNG POPUP
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();

        // GỌI MAIN PAGE VÀ TRUYỀN DỮ LIỆU SANG
        if (mainPageController.getInstance() != null) {
            mainPageController.getInstance().loadCustomSearchPane(criteria);
        } else {
            System.err.println("Lỗi: mainPageController chưa được khởi tạo!");
        }
    }

    // ========================================================
    // CÁC HÀM HỖ TRỢ (HELPERS)
    // ========================================================

    private boolean isAnySelected(ToggleButton[] buttons) {
        for (ToggleButton btn : buttons) {
            if (btn != null && btn.isSelected()) {
                return true;
            }
        }
        return false;
    }

    // Hàm mới: Trích xuất tên (text) của các nút đang được chọn để nhét vào List
    private List<String> getSelectedNames(ToggleButton[] buttons) {
        List<String> selected = new ArrayList<>();
        for (ToggleButton btn : buttons) {
            if (btn != null && btn.isSelected()) {
                if (btn == ended) {
                    selected.add("FINISHED");
                } else {
                    selected.add(btn.getText().trim());
                }
            }
        }
        return selected;
    }
}