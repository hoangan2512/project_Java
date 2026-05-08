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
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;

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
    @FXML
    private Button backBtn;
    
    // Các nút Toggle chuyển tab
    @FXML
    private ToggleButton description_btn;
    @FXML
    private ToggleButton price_chart_btn;
    @FXML
    private ToggleButton auto_bid_btn;
    
    // Nút trạng thái Auto-bid bên trong tab Auto-bid
    @FXML
    private ToggleButton auto_bid_status_btn;
    
    // Các Pane hiển thị nội dung tương ứng
    @FXML
    private AnchorPane priceC;
    @FXML
    private AnchorPane auto_bid;

    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();
    private Timeline countdownTimer;
    private long remainingSeconds;

    public void initialize() {
        addCurrencyFormat(bidAmount);
        
        // --- LOGIC CHUYỂN TAB (MÔ TẢ / BIỂU ĐỒ / AUTO-BID) ---
        // Thiết lập mặc định ban đầu: Bật tab Mô tả
        if (description_btn != null) description_btn.setSelected(true);
        if (price_chart_btn != null) price_chart_btn.setSelected(false);
        if (auto_bid_btn != null) auto_bid_btn.setSelected(false);
        updatePanelsVisibility();

        // Gán sự kiện khi click vào các nút tab
        if (description_btn != null) {
            description_btn.setOnAction(e -> {
                description_btn.setSelected(true); // Ép luôn bật nếu bị click
                if (price_chart_btn != null) price_chart_btn.setSelected(false);
                if (auto_bid_btn != null) auto_bid_btn.setSelected(false);
                updatePanelsVisibility();
            });
        }
        
        if (price_chart_btn != null) {
            price_chart_btn.setOnAction(e -> {
                price_chart_btn.setSelected(true);
                if (description_btn != null) description_btn.setSelected(false);
                if (auto_bid_btn != null) auto_bid_btn.setSelected(false);
                updatePanelsVisibility();
            });
        }
        
        if (auto_bid_btn != null) {
            auto_bid_btn.setOnAction(e -> {
                auto_bid_btn.setSelected(true);
                if (description_btn != null) description_btn.setSelected(false);
                if (price_chart_btn != null) price_chart_btn.setSelected(false);
                updatePanelsVisibility();
            });
        }

        // --- LOGIC ĐỔI MÀU NÚT TRẠNG THÁI AUTO-BID ---
        if (auto_bid_status_btn != null) {
            if (auto_bid_status_btn.isSelected()) {
                auto_bid_btn.setStyle("-fx-border-width:2; -fx-border-radius:5; -fx-border-color: #3dd35b;"); // Xanh lá cây
                auto_bid_status_btn.setText("Active");
            } else {
                auto_bid_btn.setStyle("-fx-border-width:2; -fx-border-radius:5; -fx-border-color: grey;"); // Xám
                auto_bid_status_btn.setText("Inactive");
            }
            
            auto_bid_status_btn.setOnAction(e -> {
                if (auto_bid_status_btn.isSelected()) {
                    auto_bid_btn.setStyle("-fx-border-width:2; -fx-border-radius:5; -fx-border-color: #3dd35b;"); // Xanh lá cây
                    auto_bid_status_btn.setText("Active");
                } else {
                    auto_bid_btn.setStyle("-fx-border-width:2; -fx-border-radius:5; -fx-border-color: grey;"); // Xám
                    auto_bid_status_btn.setText("Inactive");
                }
            });
        }
    }

    private void updatePanelsVisibility() {
        boolean showDesc = description_btn != null && description_btn.isSelected();
        boolean showChart = price_chart_btn != null && price_chart_btn.isSelected();
        boolean showAutoBid = auto_bid_btn != null && auto_bid_btn.isSelected();

        if (prd_description != null) prd_description.setVisible(showDesc);
        if (priceC != null) priceC.setVisible(showChart);
        if (auto_bid != null) auto_bid.setVisible(showAutoBid);
    }

    @FXML
    public void handleBackBtn(MouseEvent event) {
        if (mainPageController.getInstance() != null) {
            mainPageController.getInstance().goBackToSearch();
        }
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

        if (text == null || text.trim().isEmpty()) {
            return 0;
        }

        String cleanString = text.replaceAll("\\.", "");

        try {
            return Long.parseLong(cleanString);
        } catch (NumberFormatException e) {
            System.err.println("Lỗi ép kiểu số: " + cleanString);
            return 0; 
        }
    }

    private void addCurrencyFormat(TextField textField) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);

        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                return;
            }

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

    public void setData(String name, long price, long time, String imagePath, String descriptionText, String status) {
        if (prdName != null) {
            prdName.setText(name);
        }

        if (currentPrice != null) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
            symbols.setGroupingSeparator('.');
            DecimalFormat formatter = new DecimalFormat("###,###", symbols);
            currentPrice.setText(formatter.format(price) + " ₫");
        }

        this.remainingSeconds = time;
        if (countdownTimer != null) countdownTimer.stop();

        if ("WAITING".equals(status) || "UPCOMING".equals(status)) {
            updateUpcomingTimeLabel();
            if (remainingSeconds > 0) {
                countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                    remainingSeconds--;
                    updateUpcomingTimeLabel();
                    if (remainingSeconds <= 0) {
                        countdownTimer.stop();
                        if (timeLeft != null) timeLeft.setText("Started - Refreshing...");
                        if (Bid != null) {
                            Bid.setDisable(false);
                            Bid.setText("Bid");
                        }
                        if (bidAmount != null) bidAmount.setDisable(false);
                    }
                }));
                countdownTimer.setCycleCount(Timeline.INDEFINITE);
                countdownTimer.play();
            } else {
                if (timeLeft != null) timeLeft.setText("Started - Refreshing...");
                if (Bid != null) {
                    Bid.setDisable(false);
                    Bid.setText("Bid");
                }
                if (bidAmount != null) bidAmount.setDisable(false);
            }
            
            if (Bid != null) {
                Bid.setDisable(true);
                Bid.setText("Upcoming");
            }
            if (bidAmount != null) bidAmount.setDisable(true);

        } else if ("RUNNING".equals(status)) {
            updateTimeLabel();
            if (Bid != null) {
                Bid.setDisable(false);
                Bid.setText("Bid");
            }
            if (bidAmount != null) bidAmount.setDisable(false);

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
        } else {
            handleAuctionEnd();
        }

        if (imagePath != null && !imagePath.trim().isEmpty() && prdImage != null) {
            try {
                File imgFile = new File("auction-server/src/main/resources" + imagePath);

                if (imgFile.exists()) {
                    Image img = new Image(imgFile.toURI().toString());
                    prdImage.setPreserveRatio(true);
                    prdImage.setSmooth(true);
                    prdImage.setImage(img);
                } else {
                    java.io.InputStream is = getClass().getResourceAsStream(imagePath);
                    if (is != null) {
                        prdImage.setPreserveRatio(true);
                        prdImage.setSmooth(true);
                        prdImage.setImage(new Image(is));
                    }
                }
            } catch (Exception e) {
                System.out.println("Cannot find image: " + imagePath);
            }
        }
        
        if (prd_description != null && descriptionText != null) {
            prd_description.setText(descriptionText);
        }

        System.out.println("Displaying: " + name + " - Auction Status: " + status);
    }
    
    private void updateTimeLabel() {
        if (timeLeft != null && remainingSeconds >= 0) {
            long hours = remainingSeconds / 3600;
            long minutes = (remainingSeconds % 3600) / 60;
            long seconds = remainingSeconds % 60;
            String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            timeLeft.setText(timeString);
        }
    }

    private void updateUpcomingTimeLabel() {
        if (timeLeft != null && remainingSeconds >= 0) {
            long hours = remainingSeconds / 3600;
            long minutes = (remainingSeconds % 3600) / 60;
            long seconds = remainingSeconds % 60;
            String timeString = String.format("Upcoming in: %02d:%02d:%02d", hours, minutes, seconds);
            timeLeft.setText(timeString);
        }
    }
    
    private void handleAuctionEnd() {
        if (timeLeft != null) {
            timeLeft.setText("Ended");
        }
        if (Bid != null) {
            Bid.setDisable(true);
            Bid.setText("Ended");
        }
        if (bidAmount != null) {
            bidAmount.setDisable(true);
        }
    }
}
