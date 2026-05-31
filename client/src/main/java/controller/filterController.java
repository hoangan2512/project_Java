package controller;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
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

    // Tạo ToggleGroup cho các nút chọn khoảng giá nhanh
    private final ToggleGroup priceGroup = new ToggleGroup();

    @FXML
    public void initialize() {
        // Gắn hiệu ứng format tiền Việt cho 2 ô nhập giá
        addCurrencyFormat(lowest);
        addCurrencyFormat(highest);

        // Đưa các nút giá vào cùng một nhóm và thiết lập sự kiện lắng nghe
        setupPriceActionButtons();
    }

    /**
     * Hàm cấu hình ToggleGroup và xử lý tự động điền giá trị khi bấm nút chọn giá nhanh
     */
    private void setupPriceActionButtons() {
        price1.setToggleGroup(priceGroup);
        price2.setToggleGroup(priceGroup);
        price3.setToggleGroup(priceGroup);

        priceGroup.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> {
            if (newToggle == null) {
                // Nếu người dùng bỏ chọn nút hiện tại (hoặc bấm Clear), không tự động xóa text
                // để tránh đè lên trường hợp họ muốn tự nhập tay sau đó.
                return;
            }

            ToggleButton selectedBtn = (ToggleButton) newToggle;

            if (selectedBtn == price1) {          // Dưới 1 triệu
                lowest.setText("0");
                highest.setText("1.000.000");
            } else if (selectedBtn == price2) {   // Từ 1 triệu - 10 triệu
                lowest.setText("1.000.000");
                highest.setText("10.000.000");
            } else if (selectedBtn == price3) {   // Trên 10 triệu
                lowest.setText("10.000.000");
                highest.clear(); // Giá cao nhất để trống đại diện cho vô cực
            }
        });

        // Nếu người dùng chủ động gõ vào ô text, nhả nút chọn nhanh ra để tránh xung đột UI
        lowest.textProperty().addListener((obs, oldVal, newVal) -> {
            if (lowest.isFocused() && priceGroup.getSelectedToggle() != null) {
                priceGroup.selectToggle(null);
            }
        });
        highest.textProperty().addListener((obs, oldVal, newVal) -> {
            if (highest.isFocused() && priceGroup.getSelectedToggle() != null) {
                priceGroup.selectToggle(null);
            }
        });
    }

    // --- HÀM TỰ ĐỘNG CHỌN LẠI CÁC TRƯỜNG ĐÃ LỌC TRƯỚC ĐÓ ---
    public void setInitialCriteria(SearchCriteria criteria) {
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

        // Khôi phục Price (Ưu tiên điền text, các bộ lắng nghe listener sẽ tự xử lý)
        if (criteria.getMinPrice() > 0) {
            lowest.setText(String.valueOf(criteria.getMinPrice()));
        } else {
            lowest.clear();
        }

        if (criteria.getMaxPrice() > 0) {
            highest.setText(String.valueOf(criteria.getMaxPrice()));
        } else {
            highest.clear();
        }

        // Đồng bộ ngược lại trạng thái Nút bấm nhanh nếu khoảng giá khớp chính xác
        long min = criteria.getMinPrice();
        long max = criteria.getMaxPrice();
        if (min == 0 && max == 1000000) price1.setSelected(true);
        else if (min == 1000000 && max == 10000000) price2.setSelected(true);
        else if (min == 10000000 && max == 0) price3.setSelected(true);
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
        ToggleButton[] allToggleButtons = {art, electronics, vehicle, real_estate, bidding, newly_listed, ending_soon, upcoming, ended};
        TextField[] allTextFields = {lowest, highest, auctionID};

        // Bỏ chọn các nút khoảng giá thông qua ToggleGroup
        priceGroup.selectToggle(null);

        // Bỏ chọn tất cả các ToggleButton danh mục/trạng thái khác
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
        ToggleButton[] categoryBtns = {art, electronics, vehicle, real_estate};
        ToggleButton[] statusBtns = {bidding, newly_listed, ending_soon, upcoming, ended};

        String id = auctionID.getText().trim();

        // TẠO DTO ĐÓNG GÓI DỮ LIỆU TÌM KIẾM
        SearchCriteria criteria = new SearchCriteria();
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

    private boolean isAnySelected(ToggleButton[] buttons) {
        for (ToggleButton btn : buttons) {
            if (btn != null && btn.isSelected()) {
                return true;
            }
        }
        return false;
    }

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