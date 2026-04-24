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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javafx.application.Platform;

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
    private Label status;

    @FXML
    public void initialize() {
        // Gắn hiệu ứng format tiền Việt cho 2 ô nhập giá
        addCurrencyFormat(lowest);
        addCurrencyFormat(highest);
    }

    private void addCurrencyFormat(TextField textField) {
        // Cấu hình format số kiểu Việt Nam (dấu chấm phân cách hàng nghìn)
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);

        // Lắng nghe mọi sự thay đổi text trong ô nhập
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            // Nếu ô trống thì bỏ qua
            if (newValue == null || newValue.isEmpty()) {
                return;
            }

            // 1. Xóa bỏ tất cả các ký tự không phải là số (bảo vệ khỏi việc nhập chữ)
            String numericString = newValue.replaceAll("[^\\d]", "");

            if (numericString.isEmpty()) {
                textField.setText("");
                return;
            }

            try {
                // 2. Ép kiểu thành số Long (Dùng Long để chứa được tiền Tỷ)
                long value = Long.parseLong(numericString);

                // 3. Format lại thành chuỗi có dấu chấm (VD: 1000000 -> 1.000.000)
                String formattedString = formatter.format(value);

                // 4. Nếu text mới khác với text đang hiển thị thì cập nhật lại
                if (!newValue.equals(formattedString)) {
                    textField.setText(formattedString);

                    // 5. Đẩy con trỏ chuột về cuối cùng để gõ liên tục không bị ngược
                    Platform.runLater(() -> textField.positionCaret(formattedString.length()));
                }
            } catch (NumberFormatException e) {
                // Nếu nhập số quá lớn (vượt quá giới hạn Long), chặn lại bằng cách giữ giá trị cũ
                textField.setText(oldValue);
            }
        });
    }

    private long getRealPrice(TextField textField) {
        String text = textField.getText();

        // Nếu ô trống thì mặc định trả về 0
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }

        // Xóa toàn bộ dấu chấm
        String cleanString = text.replaceAll("\\.", "");

        try {
            // Ép thành kiểu Long (Dùng Long thay vì Int để chứa được tiền Tỷ)
            return Long.parseLong(cleanString);
        } catch (NumberFormatException e) {
            System.err.println("Lỗi ép kiểu số: " + cleanString);
            return 0; // Trả về 0 nếu có lỗi bất ngờ
        }
    }

    @FXML
    public void handleBackBtn(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
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

        // 2. KIỂM TRA TÌNH TRẠNG CHỌN
        boolean hasCategory = isAnySelected(categoryBtns);
        boolean hasPrice = isAnySelected(priceBtns);
        boolean hasStatus = isAnySelected(statusBtns);
        boolean hasManualPrice = !lowPrice.isEmpty() || !highPrice.isEmpty();
        boolean hasId = !id.isEmpty();

        boolean hasAnySelection = hasCategory || hasPrice || hasStatus || hasManualPrice || hasId;

        // ==========================================
        // 3. XỬ LÝ NẾU KHÔNG CÓ GÌ ĐƯỢC CHỌN (BÁO LỖI)
        // ==========================================
        if (!hasAnySelection) {
            // Lấy lại màu cũ (trong trường hợp bạn có style CSS mặc định)
            String oldStyle = status.getStyle();

            // Chuyển sang bold đỏ
            status.setStyle("-fx-text-fill: #ff4d4d; -fx-font-weight: bold;");

            // Tạo đếm ngược 2 giây để trả về màu cũ
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(e -> status.setStyle(oldStyle)); // Đổi lại style cũ khi hết 2s
            pause.play();

            return; // Ngắt luôn hàm ở đây, không in ấn hay đóng cửa sổ gì cả
        }

        // ==========================================
        // 4. XỬ LÝ NẾU ĐÃ CHỌN (IN RA & ĐÓNG POPUP)
        // ==========================================
        System.out.println("\n========= KẾT QUẢ BỘ LỌC =========");

        printGroupSelection("Danh mục (Categories)", categoryBtns);
        printGroupSelection("Mức giá (Price Range)", priceBtns);
        printGroupSelection("Trạng thái (Status)", statusBtns);

        if (hasManualPrice) {
            String from = lowPrice.isEmpty() ? "0" : lowPrice;
            String to = highPrice.isEmpty() ? "Không giới hạn" : highPrice;
            System.out.println("[+] Giá nhập tay: Từ " + from + " -> " + to);
        }

        if (hasId) {
            System.out.println("[+] Tìm theo ID Phiên đấu giá: " + id);
        }

        System.out.println("==================================\n");

        // ĐÓNG POPUP SAU KHI IN XONG
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    // ========================================================
    // CÁC HÀM HỖ TRỢ (HELPERS)
    // ========================================================

    // Hàm mới: Kiểm tra xem mảng nút có nút nào đang được bấm không
    private boolean isAnySelected(ToggleButton[] buttons) {
        for (ToggleButton btn : buttons) {
            if (btn != null && btn.isSelected()) {
                return true;
            }
        }
        return false;
    }

    // Hàm in kết quả (Giữ nguyên như cũ)
    private void printGroupSelection(String groupName, ToggleButton[] buttons) {
        System.out.print("[+] " + groupName + ": ");
        boolean hasSelection = false;

        for (ToggleButton btn : buttons) {
            if (btn != null && btn.isSelected()) {
                System.out.print("[" + btn.getText() + "] ");
                hasSelection = true;
            }
        }

        if (!hasSelection) {
            System.out.print("(Bỏ qua / Chọn tất cả)");
        }
        System.out.println();
    }
}