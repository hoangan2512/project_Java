package controller;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.util.Locale;
import java.io.ByteArrayInputStream;
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
import model.Auction;
import model.Bid;
import model.ActionType;
import message.Request;
import message.Response;
import network.ClientSocket;

import java.io.IOException;

public class prdPageController {

    @FXML
    private Label currentPrice, currentPrice2;
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
    private Label timeLeft, hours_left, mins_left, seconds_left, auctiontime_status;
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
    private Auction currentAuction;

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
            if (currentAuction == null) {
                System.err.println("Lỗi: Không có phiên đấu giá nào được chọn.");
                return;
            }

            long realBidAmount = getRealPrice(bidAmount);

            if (realBidAmount > currentAuction.getCurrent_price()) {
                // Tạo đối tượng Bid
                Bid newBid = new Bid();
                newBid.setAuction_id(currentAuction.getId());
                newBid.setBidder_id(SessionManager.getInstance().getCurrentUser().getId());
                newBid.setAmount(realBidAmount);
                newBid.setBid_time(LocalDateTime.now());

                // Tạo Request và gửi lên Server. SỬ DỤNG ActionType.BID
                Request request = new Request(newBid, ActionType.BID);
                Response response = ClientSocket.sendRequest(request);

                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    System.out.println("Đặt giá thành công: " + realBidAmount);
                    // Cập nhật giá hiện tại trên UI ngay lập tức (hoặc chờ broadcast)
                    Platform.runLater(() -> {
                        currentAuction.setCurrent_price(realBidAmount);
                        updateCurrentPriceLabel(realBidAmount);
                        bidAmount.clear();
                        try {
                            sceneSwitcher.openBidded(); // Mở popup thông báo thành công
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    String errorMessage = (response != null) ? response.getMessage() : "Lỗi không xác định từ Server.";
                    System.err.println("Đặt giá thất bại: " + errorMessage);
                    showErrorInBidAmount(errorMessage);
                }

            } else {
                showErrorInBidAmount("Giá đặt phải cao hơn giá hiện tại (" + currentAuction.getCurrent_price() + ").");
            }
        } else {
            sceneSwitcher.openSignInPopup(null);
        }
    }
    
    // --- HÀM MỚI: XỬ LÝ BROADCAST TỪ SERVER ---
    public void handleBroadcast(Response res) {
        if (res == null || currentAuction == null) return;
        
        String status = res.getStatus();
        
        if ("NOTIFY_NEW_PRICE".equals(status)) {
            if (res.getData() instanceof Bid) {
                Bid newBid = (Bid) res.getData();
                
                // Chỉ cập nhật nếu gói tin này là dành cho sản phẩm đang xem
                if (newBid.getAuction_id() == currentAuction.getId()) {
                    System.out.println("Cập nhật giá mới từ Server: " + newBid.getAmount());
                    Platform.runLater(() -> {
                        currentAuction.setCurrent_price(newBid.getAmount());
                        updateCurrentPriceLabel(newBid.getAmount());
                    });
                }
            }
        } else if ("AUCTION_END".equals(status)) {
            if (res.getData() instanceof Auction) {
                Auction endedAuction = (Auction) res.getData();
                if (endedAuction.getId() == currentAuction.getId()) {
                    Platform.runLater(() -> {
                        handleAuctionEnd();
                        if (countdownTimer != null) countdownTimer.stop();
                    });
                }
            }
        } else if ("AUCTION_START".equals(status)) {
             if (res.getData() instanceof Auction) {
                Auction startedAuction = (Auction) res.getData();
                if (startedAuction.getId() == currentAuction.getId()) {
                     Platform.runLater(() -> {
                        if (timeLeft != null) {
                            timeLeft.setText("Started - Refreshing...");
                            auctiontime_status.setText("Auction Started - Refreshing");
                        }
                        if (Bid != null) {
                            Bid.setDisable(false);
                            Bid.setText("Place Bid");
                        }
                        if (bidAmount != null) bidAmount.setDisable(false);
                    });
                }
            }
        }
    }

    private void showErrorInBidAmount(String message) {
        String oldStyle = bidAmount.getStyle();
        bidAmount.setPromptText(message);
        bidAmount.setStyle(oldStyle + "; -fx-prompt-text-fill: #ff4d4d; -fx-font-weight: bold;");

        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> {
            bidAmount.setStyle(oldStyle);
            bidAmount.setPromptText("Insert a price");
        });
        pause.play();
    }

    private void updateCurrentPriceLabel(double price) {
        if (currentPrice != null) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
            symbols.setGroupingSeparator('.');
            DecimalFormat formatter = new DecimalFormat("###,###", symbols);
            currentPrice.setText(formatter.format(price) + " ₫");
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

    public void setData(Auction auction) {
        this.currentAuction = auction;
        String name = auction.getItem().getName();
        long price = (long) auction.getCurrent_price();
        String imagePath = auction.getItem().getImgPath();
        byte[] imageBytes = auction.getItem().getImageBytes();
        String descriptionText = auction.getItem().getDescription();

        if (prdName != null) {
            prdName.setText(name);
        }

        updateCurrentPriceLabel(price);

        if (countdownTimer != null) countdownTimer.stop();

        LocalDateTime now = LocalDateTime.now();
        String status = auction.getStatus();

        // 1. Tự động tính toán lại trạng thái và thời gian còn lại (bảo vệ khỏi việc dữ liệu cũ)
        if ("RUNNING".equals(status) && auction.getEnd_time() != null) {
            if (now.isBefore(auction.getEnd_time())) {
                this.remainingSeconds = java.time.Duration.between(now, auction.getEnd_time()).getSeconds();
            } else {
                status = "FINISHED";
            }
        } else if ("WAITING".equals(status) && auction.getStart_time() != null) {
            if (now.isBefore(auction.getStart_time())) {
                this.remainingSeconds = java.time.Duration.between(now, auction.getStart_time()).getSeconds();
            } else {
                status = "RUNNING";
                if (auction.getEnd_time() != null && now.isBefore(auction.getEnd_time())) {
                    this.remainingSeconds = java.time.Duration.between(now, auction.getEnd_time()).getSeconds();
                } else {
                    status = "FINISHED";
                }
            }
        } else if ("FINISHED".equals(status)) {
            this.remainingSeconds = 0;
        }

        // 2. Cập nhật UI dựa trên trạng thái đã tính toán
        if ("WAITING".equals(status)) {
            updateUpcomingTimeLabel();
            if (remainingSeconds > 0) {
                countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                    remainingSeconds--;
                    updateUpcomingTimeLabel();
                    if (remainingSeconds <= 0) {
                        countdownTimer.stop();
                        // Chuyển trạng thái UI sang RUNNING (Chờ server xác nhận qua broadcast)
                        if (timeLeft != null) {
                            timeLeft.setText("Started - Refreshing...");
                            auctiontime_status.setText("Auction Started - Refreshing");
                        }
                        if (Bid != null) {
                            Bid.setDisable(false);
                            Bid.setText("Place Bid");
                        }
                        if (bidAmount != null) bidAmount.setDisable(false);
                    }
                }));
                countdownTimer.setCycleCount(Timeline.INDEFINITE);
                countdownTimer.play();
            } else {
                if (timeLeft != null) {
                    timeLeft.setText("Started - Refreshing...");
                    auctiontime_status.setText("Auction Started - Refreshing");

                }
                if (Bid != null) {
                    Bid.setDisable(false);
                    Bid.setText("Place Bid");
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
                Bid.setText("Place Bid");
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

        // 3. Load Ảnh và Mô tả
        if (prdImage != null) {
            if (imageBytes != null && imageBytes.length > 0) {
                // Ưu tiên load ảnh từ mảng byte do Server gửi
                try {
                    ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
                    Image img = new Image(bis);
                    prdImage.setPreserveRatio(true);
                    prdImage.setSmooth(true);
                    prdImage.setImage(img);
                } catch (Exception e) {
                    System.out.println("Lỗi khi hiển thị ảnh từ byte array: " + e.getMessage());
                }
            } else if (imagePath != null && !imagePath.trim().isEmpty()) {
                // Cách cũ: Load trực tiếp từ ổ cứng nếu chưa có byte array
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
            auctiontime_status.setText("Auction Ends In");
            hours_left.setText(String.format("%02d", hours));
            mins_left.setText(String.format("%02d", minutes));
            seconds_left.setText(String.format("%02d", seconds));
        }
    }

    private void updateUpcomingTimeLabel() {
        if (timeLeft != null && remainingSeconds >= 0) {
            long hours = remainingSeconds / 3600;
            long minutes = (remainingSeconds % 3600) / 60;
            long seconds = remainingSeconds % 60;
            String timeString = String.format("Upcoming in: %02d:%02d:%02d", hours, minutes, seconds);
            timeLeft.setText(timeString);
            auctiontime_status.setText("Auction Coming In");
            hours_left.setText(String.format("%02d", hours));
            mins_left.setText(String.format("%02d", minutes));
            seconds_left.setText(String.format("%02d", seconds));
        }
    }
    
    private void handleAuctionEnd() {
        if (timeLeft != null) {
            auctiontime_status.setText("Auction Ended");
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
