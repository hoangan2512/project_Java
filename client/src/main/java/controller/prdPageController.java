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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import model.Auction;
import model.Bid;
import model.ActionType;
import model.AutoBidConfig;
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
    private Button bidButton;
    @FXML
    private Label timeLeft, hours_left, mins_left, seconds_left, auctiontime_status;
    
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
    
    // UI Elements cho Auto-bid
    @FXML
    private TextField ceilling_price;
    @FXML
    private TextField price_step;
    @FXML
    private Label auto_bid_status;
    
    // Các Pane hiển thị nội dung tương ứng
    @FXML
    private AnchorPane priceC;
    @FXML
    private AnchorPane auto_bid;

    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();
    private Timeline countdownTimer;
    private long remainingSeconds;
    private Auction currentAuction;

    @FXML
    private void initialize() {
        if (bidAmount != null) addCurrencyFormat(bidAmount);
        if (ceilling_price != null) addCurrencyFormat(ceilling_price);
        if (price_step != null) addCurrencyFormat(price_step);
        
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
            updateAutoBidUI();
            
            auto_bid_status_btn.setOnAction(e -> {
                updateAutoBidUI();
                
                // Khi bật Active, tiến hành đăng ký Auto-Bid
                if (auto_bid_status_btn.isSelected()) {
                    handleRegisterAutoBid();
                } else {
                    // TODO: Gửi request HỦY Auto-Bid nếu người dùng tắt (Nếu Server có hỗ trợ)
                    if (auto_bid_status != null) {
                        auto_bid_status.setText("Auto-bid cancelled.");
                        auto_bid_status.setStyle("-fx-text-fill: grey;");
                    }
                }
            });
        }
    }
    
    private void updateAutoBidUI() {
        if (auto_bid_status_btn.isSelected()) {
            if (auto_bid_btn != null) {
                auto_bid_btn.setStyle("-fx-border-width:2; -fx-border-radius:5; -fx-border-color: #3dd35b;");
            }
            auto_bid_status_btn.setText("Active");
            // Disable input fields when active
            if (ceilling_price != null) ceilling_price.setDisable(true);
            if (price_step != null) price_step.setDisable(true);
        } else {
            if (auto_bid_btn != null) {
                auto_bid_btn.setStyle("-fx-border-width:2; -fx-border-radius:5; -fx-border-color: grey;");
            }
            auto_bid_status_btn.setText("Inactive");
            // Enable input fields when inactive
            if (ceilling_price != null) ceilling_price.setDisable(false);
            if (price_step != null) price_step.setDisable(false);
        }
    }
    
    private void handleRegisterAutoBid() {
        if (!SessionManager.getInstance().isBidder()) {
            auto_bid_status_btn.setSelected(false);
            updateAutoBidUI();
            try {
                sceneSwitcher.openSignInPopup(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        if (currentAuction == null) return;

        long maxBid = getRealPrice(ceilling_price);
        long increment = getRealPrice(price_step);

        if (maxBid <= currentAuction.getCurrent_price()) {
            if (auto_bid_status != null) {
                auto_bid_status.setText("Lỗi: Ceiling Price phải lớn hơn giá hiện tại!");
                auto_bid_status.setStyle("-fx-text-fill: red;");
            }
            auto_bid_status_btn.setSelected(false);
            updateAutoBidUI();
            return;
        }

        if (increment <= 0) {
            if (auto_bid_status != null) {
                auto_bid_status.setText("Lỗi: Bid Increment phải lớn hơn 0!");
                auto_bid_status.setStyle("-fx-text-fill: red;");
            }
            auto_bid_status_btn.setSelected(false);
            updateAutoBidUI();
            return;
        }

        int auctionId = currentAuction.getId();
        int bidderId = SessionManager.getInstance().getCurrentUser().getId();

        AutoBidConfig config = new AutoBidConfig(auctionId, bidderId, maxBid, increment);
        Request req = new Request(config, ActionType.REGISTER_AUTO_BID);

        Response res = ClientSocket.sendRequest(req);

        if (res != null && "SUCCESS".equals(res.getStatus())) {
            if (auto_bid_status != null) {
                auto_bid_status.setText("Auto-bid is active and watching.");
                auto_bid_status.setStyle("-fx-text-fill: #3dd35b;"); // Xanh
            }
        } else {
            if (auto_bid_status != null) {
                auto_bid_status.setText("Lỗi: " + (res != null ? res.getMessage() : "Mất kết nối"));
                auto_bid_status.setStyle("-fx-text-fill: red;");
            }
            auto_bid_status_btn.setSelected(false);
            updateAutoBidUI();
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
    private void handleBackBtn() {
        if (mainPageController.getInstance() != null) {
            mainPageController.getInstance().goBackToSearch();
        }
    }

    @FXML
    private void handleBidBtn() throws IOException {
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
            if (res.getData() instanceof Bid newBid) {
                
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
            if (res.getData() instanceof Auction endedAuction) {
                if (endedAuction.getId() == currentAuction.getId()) {
                    Platform.runLater(() -> {
                        handleAuctionEnd();
                        if (countdownTimer != null) countdownTimer.stop();
                    });
                }
            }
        } else if ("AUCTION_START".equals(status)) {
             if (res.getData() instanceof Auction startedAuction) {
                if (startedAuction.getId() == currentAuction.getId()) {
                     Platform.runLater(() -> {
                        if (timeLeft != null) {
                            timeLeft.setText("Started - Refreshing...");
                            setLabelText(auctiontime_status, "Auction Started - Refreshing");
                        }
                        if (bidButton != null) {
                            bidButton.setDisable(false);
                            bidButton.setText("Place Bid");
                        }
                        if (bidAmount != null) bidAmount.setDisable(false);
                    });
                }
            }
        } else if ("AUCTION_EXTENDED".equals(status)) {
            if (res.getData() instanceof Auction extendedAuction) {
                if (extendedAuction.getId() == currentAuction.getId()) {
                    Platform.runLater(() -> {
                        // Cập nhật lại thời gian kết thúc mới vào biến cục bộ
                        currentAuction.setEnd_time(extendedAuction.getEnd_time());
                        // Tính toán lại số giây còn lại (remainingSeconds)
                        this.remainingSeconds = java.time.Duration.between(LocalDateTime.now(), extendedAuction.getEnd_time()).getSeconds();
                        // Buộc đồng hồ đếm ngược cập nhật ngay lập tức giao diện
                        updateTimeLabel();
                        System.out.println("Nhận thông báo gia hạn! Thời gian mới: " + remainingSeconds + " giây.");
                    });
                }
            }
        }
    }

    private void showErrorInBidAmount(String message) {
        if (bidAmount == null) {
            return;
        }

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
            String formattedPrice = formatter.format(price) + " ₫";
            currentPrice.setText(formattedPrice);
            if (currentPrice2 != null) {
                currentPrice2.setText(formattedPrice);
            }
        }
    }

    private long getRealPrice(TextField textField) {
        if (textField == null) {
            return 0;
        }

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
        if (textField == null) {
            return;
        }

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("vi-VN"));
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
        if (auction == null || auction.getItem() == null) {
            System.err.println("Lỗi: Không có dữ liệu đấu giá hoặc sản phẩm để hiển thị.");
            return;
        }

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
        switch (status) {
            case "PENDING_APPROVAL" -> {
                if (timeLeft != null) {
                    timeLeft.setText("Awaiting Approval...");
                    setLabelText(auctiontime_status, "Status: Pending Approval");
                }
                if (bidButton != null) {
                    bidButton.setDisable(true);
                    bidButton.setText("Pending");
                }
                if (bidAmount != null) bidAmount.setDisable(true);
                if (auto_bid_status_btn != null) auto_bid_status_btn.setDisable(true);
                if (ceilling_price != null) ceilling_price.setDisable(true);
                if (price_step != null) price_step.setDisable(true);
            }
            case "WAITING" -> {
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
                                setLabelText(auctiontime_status, "Auction Started - Refreshing");
                            }
                            if (bidButton != null) {
                                bidButton.setDisable(false);
                                bidButton.setText("Place Bid");
                            }
                            if (bidAmount != null) bidAmount.setDisable(false);
                            if (auto_bid_status_btn != null) auto_bid_status_btn.setDisable(false);
                            if (ceilling_price != null) ceilling_price.setDisable(false);
                            if (price_step != null) price_step.setDisable(false);
                        }
                    }));
                    countdownTimer.setCycleCount(Timeline.INDEFINITE);
                    countdownTimer.play();
                } else {
                    if (timeLeft != null) {
                        timeLeft.setText("Started - Refreshing...");
                        setLabelText(auctiontime_status, "Auction Started - Refreshing");

                    }
                    if (bidButton != null) {
                        bidButton.setDisable(false);
                        bidButton.setText("Place Bid");
                    }
                    if (bidAmount != null) bidAmount.setDisable(false);
                    if (auto_bid_status_btn != null) auto_bid_status_btn.setDisable(false);
                    if (ceilling_price != null) ceilling_price.setDisable(false);
                    if (price_step != null) price_step.setDisable(false);
                }

                if (bidButton != null) {
                    bidButton.setDisable(true);
                    bidButton.setText("Upcoming");
                }
                if (bidAmount != null) bidAmount.setDisable(true);
                if (auto_bid_status_btn != null) auto_bid_status_btn.setDisable(true);
                if (ceilling_price != null) ceilling_price.setDisable(true);
                if (price_step != null) price_step.setDisable(true);
            }
            case "RUNNING" -> {
                updateTimeLabel();
                if (bidButton != null) {
                    bidButton.setDisable(false);
                    bidButton.setText("Place Bid");
                }
                if (bidAmount != null) bidAmount.setDisable(false);
                if (auto_bid_status_btn != null) auto_bid_status_btn.setDisable(false);
                if (ceilling_price != null) ceilling_price.setDisable(false);
                if (price_step != null) price_step.setDisable(false);

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
            }
            case null, default -> handleAuctionEnd();
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
            setLabelText(auctiontime_status, "Auction Ends In");
            setLabelText(hours_left, String.format("%02d", hours));
            setLabelText(mins_left, String.format("%02d", minutes));
            setLabelText(seconds_left, String.format("%02d", seconds));
        }
    }

    private void updateUpcomingTimeLabel() {
        if (timeLeft != null && remainingSeconds >= 0) {
            long hours = remainingSeconds / 3600;
            long minutes = (remainingSeconds % 3600) / 60;
            long seconds = remainingSeconds % 60;
            String timeString = String.format("Upcoming in: %02d:%02d:%02d", hours, minutes, seconds);
            timeLeft.setText(timeString);
            setLabelText(auctiontime_status, "Auction Coming In");
            setLabelText(hours_left, String.format("%02d", hours));
            setLabelText(mins_left, String.format("%02d", minutes));
            setLabelText(seconds_left, String.format("%02d", seconds));
        }
    }
    
    private void handleAuctionEnd() {
        if (timeLeft != null) {
            setLabelText(auctiontime_status, "Auction Ended");
            timeLeft.setText("Ended");
        }
        if (bidButton != null) {
            bidButton.setDisable(true);
            bidButton.setText("Ended");
        }
        if (bidAmount != null) {
            bidAmount.setDisable(true);
        }
        if (auto_bid_status_btn != null) {
            auto_bid_status_btn.setDisable(true);
        }
        if (ceilling_price != null) {
            ceilling_price.setDisable(true);
        }
        if (price_step != null) {
            price_step.setDisable(true);
        }
    }

    private void setLabelText(Label label, String text) {
        if (label != null) {
            label.setText(text);
        }
    }
}
