package controller;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.io.File;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
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
    private Label prdName, prd_description;
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
    private Timeline countdownTimer;
    private long remainingSeconds;

    public void initialize() {
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

    public void setData(String name, long price, long time, String imagePath, String description) {
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

        // 3. --- BẮT ĐẦU BỘ ĐẾM THỜI GIAN (COUNTDOWN) ---
        this.remainingSeconds = time;
        
        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        updateTimeLabel(); // Cập nhật hiển thị ngay lập tức

        if (remainingSeconds > 0) {
            countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                remainingSeconds--;
                updateTimeLabel();
                
                if (remainingSeconds <= 0) {
                    countdownTimer.stop();
                    handleAuctionEnd();
                }
            }));
            countdownTimer.setCycleCount(Timeline.INDEFINITE);
            countdownTimer.play();
        } else {
            handleAuctionEnd();
        }

        // 4. Tải ảnh sản phẩm từ DB (thông qua đường dẫn lưu ở server)
        if (imagePath != null && !imagePath.trim().isEmpty() && prdImage != null) {
            try {
                File imgFile = new File("auction-server/src/main/resources" + imagePath);
                
                if (imgFile.exists()) {
                    Image img = new Image(imgFile.toURI().toString());
                    prdImage.setPreserveRatio(true);
                    prdImage.setSmooth(true);
                    prdImage.setImage(img);
                } else {
                    // Dự phòng nếu ảnh ở trong resource client (các ảnh test cứng)
                    java.io.InputStream is = getClass().getResourceAsStream(imagePath);
                    if (is != null) {
                        prdImage.setPreserveRatio(true);
                        prdImage.setSmooth(true);
                        prdImage.setImage(new Image(is));
                    }
                }
            } catch (Exception e) {
                System.out.println("Không thể hiển thị ảnh chi tiết từ: " + imagePath);
            }
        }
        
        // 5. Hiển thị mô tả sản phẩm
        if (prd_description != null && description != null) {
            prd_description.setText(description);
        }

        System.out.println("Đã hiển thị chi tiết sản phẩm: " + name);
    }
    
    private void updateTimeLabel() {
        if (timeLeft != null && remainingSeconds > 0) {
            long hours = remainingSeconds / 3600;
            long minutes = (remainingSeconds % 3600) / 60;
            long seconds = remainingSeconds % 60;
            String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            timeLeft.setText(timeString);
        }
    }
    
    private void handleAuctionEnd() {
        if (timeLeft != null) {
            timeLeft.setText("This auction has ended.");
        }
        if (Bid != null) {
            Bid.setDisable(true); // Khóa nút đấu giá nếu đã kết thúc
            Bid.setText("Ended");
        }
        if (bidAmount != null) {
            bidAmount.setDisable(true); // Khóa ô nhập giá
        }
    }
}
