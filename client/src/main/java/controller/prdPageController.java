package controller;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import model.User;

import java.io.IOException;
import java.util.function.UnaryOperator;

public class prdPageController {

    @FXML
    private Label currentPrice;
    @FXML
    private ImageView prdImage;
    @FXML
    private Label prdName;
    @FXML
    private TextField bidAmount;
    @FXML
    private StackPane imgPane;
    @FXML
    private ImageView BidHub;
    @FXML
    private Button Bid;
    @FXML
    private Label timeLeft;
    @FXML
    private LineChart<Number, Number> priceChart;
    @FXML
    private NumberAxis xAxis;
    @FXML
    private NumberAxis yAxis;

    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();

    public void initialize() {
        // Tải ảnh sp (default)
        String ImgPath = "/image/prd/vinfast.jpg";
        try {
            Image prdImg = new Image(getClass().getResourceAsStream(ImgPath));
            prdImage.setPreserveRatio(true);
            prdImage.setSmooth(true);
            prdImage.setImage(prdImg);
        } catch (Exception e) {
            System.err.println("Không tìm thấy ảnh tại đường dẫn: " + ImgPath);
        }

        addCurrencyFormat(bidAmount);
        // price_chart
    }


    public void handleBidBtn(MouseEvent event) throws IOException {
        if (SessionManager.getInstance().isBidder()) {
            // Lấy SỐ THẬT thay vì lấy chữ
            long realBidAmount = getRealPrice(bidAmount);

            if (realBidAmount > 0) {
                System.out.println("Bidding: " + realBidAmount);
                bidAmount.clear();
                try {
                    sceneSwitcher.openBidded();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Lỗi chuyển cảnh");
                }

            } else {
                String oldStyle = bidAmount.getStyle();

                // 2. Đổi chữ thành báo lỗi và thêm CSS đổi màu đỏ, in đậm
                bidAmount.setPromptText("Please insert a price");
                bidAmount.setStyle(oldStyle + "; -fx-prompt-text-fill: #ff4d4d; -fx-font-weight: bold;");

                // 3. Đặt đồng hồ đếm ngược 2 giây
                PauseTransition pause = new PauseTransition(Duration.seconds(2));
                pause.setOnFinished(e -> {
                    // Hết 2 giây -> Trả lại style cũ và chữ mặc định
                    bidAmount.setStyle(oldStyle);
                    bidAmount.setPromptText("Insert a price");
                });
                pause.play();
            }
        } else {
            sceneSwitcher.openSignInPopup(null);
        }
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

    public void setData(String name, long price, long time, String imagePath) {
        // 1. Set tên sản phẩm
        if (prdName != null) {
            prdName.setText(name);
        }

        // 2. Định dạng và set giá tiền
        if (currentPrice != null) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
            symbols.setGroupingSeparator('.');
            DecimalFormat formatter = new DecimalFormat("###,###", symbols);
            currentPrice.setText(formatter.format(price) + " VNĐ");
        }

        // 3. Định dạng và set thời gian còn lại
        if (timeLeft != null) {
            long hours = time / 3600;
            long minutes = (time % 3600) / 60;
            long seconds = time % 60;
            String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            timeLeft.setText(timeString);
        }

        System.out.println("Đã hiển thị chi tiết sản phẩm: " + name);
    }
}