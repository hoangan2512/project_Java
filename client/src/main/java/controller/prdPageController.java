package controller;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.List;
import java.io.ByteArrayInputStream;
import java.io.File;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
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
import model.AutoBidConfig;
import model.Auction;
import model.Bid;
import model.ActionType;
import message.Request;
import message.Response;
import network.ClientSocket;
import javafx.util.StringConverter;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;

import java.io.IOException;

public class prdPageController {

    @FXML
    private Label currentPrice, currentPrice2;
    @FXML
    private ImageView prdImage;
    @FXML
    private Label prdName, prd_description;
    @FXML
    private TextField bidAmount, ceilling_price, price_step;
    @FXML
    private StackPane imgPane;
    @FXML
    private ImageView BidHub;
    @FXML
    private Button Bid, bid_increase, bid_decrease;
    @FXML
    private Label timeLeft, hours_left, mins_left, seconds_left, auctiontime_status, auto_bid_status;
    @FXML
    private LineChart<Number, Number> prcieChart;
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

    // Biến định dạng số tiền chung để dùng lại
    private final DecimalFormat currencyFormatter;

    // Biến theo dõi số tiền đang được đề xuất ở Label currentPrice2
    private double proposedBidAmount = 0;
    
    // Series cho biểu đồ giá
    private XYChart.Series<Number, Number> priceSeries;

    public prdPageController() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        this.currencyFormatter = new DecimalFormat("###,###", symbols);
    }

    public void initialize() {
        addCurrencyFormat(bidAmount);
        addCurrencyFormat(ceilling_price);
        addCurrencyFormat(price_step);

        // Khởi tạo biểu đồ
        initPriceChart();

        // --- GÁN SỰ KIỆN CHO NÚT TĂNG GIẢM GIÁ (+/-) ---
        if (bid_increase != null) {
            bid_increase.setOnAction(e -> handleBidIncrease());
        }
        if (bid_decrease != null) {
            bid_decrease.setOnAction(e -> handleBidDecrease());
        }

        // --- TÍNH NĂNG MỚI: TỰ ĐỘNG ĐIỀN GIÁ TỐI THIỂU KHI BẤM VÀO Ô NHẬP ---
        if (bidAmount != null) {
            bidAmount.setOnMousePressed(event -> {
                // Chỉ điền tự động nếu ô đang trống và phiên đấu giá đang hợp lệ
                if ((bidAmount.getText() == null || bidAmount.getText().isEmpty()) && currentAuction != null) {
                    // Lấy luôn số tiền đang được hiển thị ở currentPrice2
                    bidAmount.setText(currencyFormatter.format(proposedBidAmount));

                    // Chuyển con trỏ chuột về cuối chuỗi để người dùng dễ gõ thêm
                    Platform.runLater(() -> bidAmount.positionCaret(bidAmount.getText().length()));
                }
            });
        }

        // --- LOGIC CHUYỂN TAB ---
        if (description_btn != null) description_btn.setSelected(true);
        if (price_chart_btn != null) price_chart_btn.setSelected(false);
        if (auto_bid_btn != null) auto_bid_btn.setSelected(false);
        updatePanelsVisibility();

        if (description_btn != null) {
            description_btn.setOnAction(e -> {
                description_btn.setSelected(true);
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
            updateAutoBidState(false, null);
            auto_bid_status_btn.setOnAction(e -> handleAutoBidToggle());
        }
    }
    
    private void initPriceChart() {
        if (prcieChart != null) {
            prcieChart.setAnimated(false); // Tắt animation để vẽ nhanh hơn
            prcieChart.setCreateSymbols(true); // Hiển thị các chấm trên đường
            prcieChart.setLegendVisible(false); // Ẩn chú thích
            
            // Định dạng trục X (Thời gian)
            NumberAxis xAxis = (NumberAxis) prcieChart.getXAxis();
            xAxis.setAutoRanging(true);
            xAxis.setForceZeroInRange(false);
            xAxis.setTickLabelFormatter(new StringConverter<>() {
                @Override
                public String toString(Number object) {
                    long timestamp = object.longValue();
                    LocalDateTime dateTime = LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.ofHours(7));
                    return dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                }

                @Override
                public Number fromString(String string) {
                    return 0;
                }
            });
            xAxis.setTickLabelFill(javafx.scene.paint.Color.WHITE);

            // Định dạng trục Y (Giá)
            NumberAxis yAxis = (NumberAxis) prcieChart.getYAxis();
            yAxis.setAutoRanging(true);
            yAxis.setForceZeroInRange(false);
            yAxis.setTickLabelFormatter(new StringConverter<>() {
                @Override
                public String toString(Number object) {
                    if (object.doubleValue() >= 1000000) {
                        return String.format("%.1fM", object.doubleValue() / 1000000);
                    } else if (object.doubleValue() >= 1000) {
                        return String.format("%.0fK", object.doubleValue() / 1000);
                    }
                    return currencyFormatter.format(object.doubleValue());
                }

                @Override
                public Number fromString(String string) {
                    return 0;
                }
            });
            yAxis.setTickLabelFill(javafx.scene.paint.Color.WHITE);

            priceSeries = new XYChart.Series<>();
            prcieChart.getData().add(priceSeries);
            
            // Styling cho đường line và chấm
            prcieChart.lookup(".chart-series-line").setStyle("-fx-stroke: #ff8e1f; -fx-stroke-width: 2px;");
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

    private void handleAutoBidToggle() {
        if (auto_bid_status_btn != null && auto_bid_status_btn.isSelected()) {
            registerAutoBid();
        } else {
            unregisterAutoBid();
        }
    }

    private void registerAutoBid() {
        if (!SessionManager.getInstance().isBidder()) {
            updateAutoBidState(false, "Sign in as bidder first.");
            try {
                sceneSwitcher.openSignInPopup(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        if (currentAuction == null) {
            updateAutoBidState(false, "No auction selected.");
            return;
        }

        long maxBid = getRealPrice(ceilling_price);
        long increment = getRealPrice(price_step);
        double minAllowedBid = currentAuction.getCurrent_price() + calculateMinimumIncrement(currentAuction.getCurrent_price());

        if (increment <= 0) {
            updateAutoBidState(false, "Insert bid increment.");
            return;
        }
        if (maxBid < minAllowedBid) {
            updateAutoBidState(false, "Ceiling must be at least " + currencyFormatter.format(minAllowedBid));
            return;
        }

        int bidderId = SessionManager.getInstance().getCurrentUser().getId();
        AutoBidConfig config = new AutoBidConfig(currentAuction.getId(), bidderId, maxBid, increment);
        Response response = ClientSocket.sendRequest(new Request(config, ActionType.REGISTER_AUTOBID));

        if (response != null && "SUCCESS".equals(response.getStatus())) {
            updateAutoBidState(true, response.getMessage());
        } else {
            String message = response != null ? response.getMessage() : "Cannot register auto-bid.";
            updateAutoBidState(false, message);
        }
    }

    private void unregisterAutoBid() {
        if (currentAuction == null || !SessionManager.getInstance().isBidder()) {
            updateAutoBidState(false, null);
            return;
        }

        int bidderId = SessionManager.getInstance().getCurrentUser().getId();
        AutoBidConfig config = new AutoBidConfig(currentAuction.getId(), bidderId, 0, 0);
        Response response = ClientSocket.sendRequest(new Request(config, ActionType.UNREGISTER_AUTOBID));

        if (response == null || !"SUCCESS".equals(response.getStatus())) {
            updateAutoBidState(true, response != null ? response.getMessage() : "Cannot disable auto-bid.");
            return;
        }

        updateAutoBidState(false, response.getMessage());
    }

    private void updateAutoBidState(boolean active, String statusMessage) {
        if (auto_bid_status_btn != null) {
            auto_bid_status_btn.setSelected(active);
            auto_bid_status_btn.setText(active ? "Active" : "Inactive");
        }
        if (auto_bid_btn != null) {
            String color = active ? "#3dd35b" : "grey";
            auto_bid_btn.setStyle("-fx-border-width:2; -fx-border-radius:5; -fx-border-color: " + color + ";");
        }
        if (auto_bid_status != null) {
            auto_bid_status.setText(statusMessage != null ? statusMessage : (active ? "Auto-bid active." : "Auto-bid inactive."));
        }
    }

    @FXML
    public void handleBackBtn(MouseEvent event) {
        if (mainPageController.getInstance() != null) {
            mainPageController.getInstance().goBackToSearch();
        }
    }

    // ==========================================
    // LOGIC BƯỚC GIÁ TỐI THIỂU & TĂNG GIẢM
    // ==========================================

    private double calculateMinimumIncrement(double currentPrice) {
        if (currentPrice < 100000) {
            return 10000;
        } else if (currentPrice < 1000000) {
            return 50000;
        } else if (currentPrice < 10000000) {
            return 200000;
        } else if (currentPrice < 50000000) {
            return 500000;
        } else {
            return 1000000;
        }
    }

    /**
     * Xác định mức nhảy (step) cho 2 nút +/- tùy thuộc vào giá trị sản phẩm hiện tại
     */
    private double getBidStep(double currentPrice) {
        if (currentPrice >= 1_000_000_000) {
            return 10_000_000; // >= 1 Tỷ -> Nhảy 10 Triệu
        } else if (currentPrice >= 100_000_000) {
            return 1_000_000;  // >= 100 Triệu -> Nhảy 1 Triệu (Đã chuẩn hóa lỗi typo)
        } else if (currentPrice >= 10_000_000) {
            return 100_000;    // >= 10 Triệu -> Nhảy 100k
        } else {
            return 10_000;     // Dưới 10 Triệu -> Nhảy 10k
        }
    }

    /**
     * Nút [+] Tăng giá đề xuất
     */
    private void handleBidIncrease() {
        if (currentAuction == null) return;
        double step = getBidStep(currentAuction.getCurrent_price());
        proposedBidAmount += step;

        // Cập nhật lên UI
        if (currentPrice2 != null) currentPrice2.setText(currencyFormatter.format(proposedBidAmount) + " ₫");
        if (bidAmount != null) bidAmount.setText(currencyFormatter.format(proposedBidAmount));
    }

    /**
     * Nút [-] Giảm giá đề xuất (Không bao giờ giảm dưới mức giá sàn tối thiểu)
     */
    private void handleBidDecrease() {
        if (currentAuction == null) return;
        double minAllowed = currentAuction.getCurrent_price() + calculateMinimumIncrement(currentAuction.getCurrent_price());
        double step = getBidStep(currentAuction.getCurrent_price());

        proposedBidAmount -= step;

        // Khóa giới hạn (Clamp): Không cho giảm dưới giá bid tối thiểu
        if (proposedBidAmount < minAllowed) {
            proposedBidAmount = minAllowed;
        }

        // Cập nhật lên UI
        if (currentPrice2 != null) currentPrice2.setText(currencyFormatter.format(proposedBidAmount) + " ₫");
        if (bidAmount != null) bidAmount.setText(currencyFormatter.format(proposedBidAmount));
    }

    /**
     * Cập nhật chữ chìm (PromptText) hướng dẫn số tiền tối thiểu cần nhập
     */
    private void updateBidPrompt() {
        if (currentAuction != null && bidAmount != null) {
            double current = currentAuction.getCurrent_price();
            double minInc = calculateMinimumIncrement(current);
            double minAllowedBid = current + minInc;

            bidAmount.setPromptText("Min bid: " + currencyFormatter.format(minAllowedBid) + " ₫");
        }
    }

    public void handleBidBtn(MouseEvent event) throws IOException {
        if (SessionManager.getInstance().isBidder()) {
            if (currentAuction == null) {
                System.err.println("Lỗi: Không có phiên đấu giá nào được chọn.");
                return;
            }

            long realBidAmount = getRealPrice(bidAmount);
            double currentPriceValue = currentAuction.getCurrent_price();
            double minInc = calculateMinimumIncrement(currentPriceValue);
            double minAllowedBid = currentPriceValue + minInc;

            // KIỂM TRA BƯỚC GIÁ HỢP LỆ
            if (realBidAmount >= minAllowedBid) {
                Bid newBid = new Bid();
                newBid.setAuction_id(currentAuction.getId());
                newBid.setBidder_id(SessionManager.getInstance().getCurrentUser().getId());
                newBid.setAmount(realBidAmount);
                newBid.setBid_time(LocalDateTime.now());

                Request request = new Request(newBid, ActionType.BID);
                Response response = ClientSocket.sendRequest(request);

                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    System.out.println("Đặt giá thành công: " + realBidAmount);
                    Platform.runLater(() -> {
                        currentAuction.setCurrent_price(realBidAmount);
                        updateCurrentPriceLabel(realBidAmount);
                        bidAmount.clear();
                        try {
                            sceneSwitcher.openBidded();
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
                showErrorInBidAmount("Minimum bid not reached: " + currencyFormatter.format(minAllowedBid) + " ₫");
            }
        } else {
            sceneSwitcher.openSignInPopup(null);
        }
    }

    // --- XỬ LÝ BROADCAST TỪ SERVER ---
    public void handleBroadcast(Response res) {
        if (res == null || currentAuction == null) return;

        String status = res.getStatus();

        if ("NOTIFY_NEW_PRICE".equals(status)) {
            if (res.getData() instanceof Bid newBid) {

                if (newBid.getAuction_id() == currentAuction.getId()) {
                    System.out.println("Cập nhật giá mới từ Server: " + newBid.getAmount());
                    Platform.runLater(() -> {
                        currentAuction.setCurrent_price(newBid.getAmount());
                        updateCurrentPriceLabel(newBid.getAmount());

                        // Thêm điểm dữ liệu mới vào biểu đồ
                        if (priceSeries != null && newBid.getBid_time() != null) {
                            long timestamp = newBid.getBid_time().toEpochSecond(ZoneOffset.ofHours(7));
                            XYChart.Data<Number, Number> newData = new XYChart.Data<>(timestamp, newBid.getAmount());
                            priceSeries.getData().add(newData);
                        }

                        // Cập nhật lại Prompt text bước giá mới
                        updateBidPrompt();

                        // Nếu user đang gõ dở giá cũ mà bị báo giá mới đè lên, tự động dọn sạch ô nhập
                        if (bidAmount.getText() != null && !bidAmount.getText().isEmpty()) {
                            long typedAmount = getRealPrice(bidAmount);
                            double newMinAllowed = newBid.getAmount() + calculateMinimumIncrement(newBid.getAmount());

                            if (typedAmount < newMinAllowed) {
                                bidAmount.clear();
                                showErrorInBidAmount("Price updated by another user!");
                            }
                        }
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
                    Platform.runLater(this::run);
                }
            }
        } else if ("AUCTION_EXTENDED".equals(status)) {
            if (res.getData() instanceof Auction extendedAuction) {
                if (extendedAuction.getId() == currentAuction.getId() && extendedAuction.getEnd_time() != null) {
                    Platform.runLater(() -> {
                        currentAuction.setEnd_time(extendedAuction.getEnd_time());
                        restartRunningCountdown();
                    });
                }
            }
        }
    }
    
    // --- XỬ LÝ NHẬN LỊCH SỬ ĐẤU GIÁ (GET_BID_HISTORY) ---
    public void handleGetBidHistoryResponse(Response res) {
        if (res != null && "SUCCESS".equals(res.getStatus())) {
            if (res.getData() instanceof List<?> rawList) {
                Platform.runLater(() -> {
                    if (priceSeries != null) {
                        priceSeries.getData().clear();
                        
                        // Thêm điểm giá khởi điểm (Start Price)
                        if (currentAuction != null && currentAuction.getStart_time() != null) {
                            long startTimestamp = currentAuction.getStart_time().toEpochSecond(ZoneOffset.ofHours(7));
                            priceSeries.getData().add(new XYChart.Data<>(startTimestamp, currentAuction.getItem().getStarting_price()));
                        }
                        
                        // Thêm các điểm giá từ lịch sử
                        for (Object obj : rawList) {
                            if (obj instanceof Bid b) {
                                if (b.getBid_time() != null) {
                                    long timestamp = b.getBid_time().toEpochSecond(ZoneOffset.ofHours(7));
                                    XYChart.Data<Number, Number> data = new XYChart.Data<>(timestamp, b.getAmount());
                                    priceSeries.getData().add(data);
                                }
                            }
                        }
                        
                        System.out.println("Đã tải lịch sử biểu đồ giá với " + rawList.size() + " lượt đặt.");
                    }
                });
            }
        } else {
             System.err.println("Không thể tải lịch sử đấu giá: " + (res != null ? res.getMessage() : "null"));
        }
    }

    private void showErrorInBidAmount(String message) {
        String oldStyle = bidAmount.getStyle();

        bidAmount.clear();
        bidAmount.setPromptText(message);

        bidAmount.setStyle(oldStyle + "; -fx-prompt-text-fill: #ff4d4d; -fx-font-weight: normal;");

        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(e -> {
            bidAmount.setStyle(oldStyle);
            updateBidPrompt();
        });
        pause.play();
    }

    /**
     * SỬA ĐỔI: Đồng bộ hóa luôn biến proposedBidAmount và giao diện currentPrice2
     */
    private void updateCurrentPriceLabel(double price) {
        if (currentPrice != null) {
            currentPrice.setText(currencyFormatter.format(price) + " ₫");
        }

        // Khi giá Server thay đổi, ép currentPrice2 bằng đúng giá trị sàn tối thiểu mới
        if (currentAuction != null) {
            double minInc = calculateMinimumIncrement(price);
            proposedBidAmount = price + minInc;
            if (currentPrice2 != null) {
                currentPrice2.setText(currencyFormatter.format(proposedBidAmount) + " ₫");
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

        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                return;
            }

            String numericString = newValue.replaceAll("\\D", "");

            if (numericString.isEmpty()) {
                textField.setText("");
                return;
            }

            try {
                long value = Long.parseLong(numericString);
                String formattedString = currencyFormatter.format(value);

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

        // Hiển thị bước giá tối thiểu lần đầu tiên khi load giao diện
        updateBidPrompt();

        if (countdownTimer != null) countdownTimer.stop();

        LocalDateTime now = LocalDateTime.now();
        String status = auction.getStatus();

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

        if ("WAITING".equals(status)) {
            updateUpcomingTimeLabel();
            if (remainingSeconds > 0) {
                countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                    remainingSeconds--;
                    updateUpcomingTimeLabel();
                    if (remainingSeconds <= 0) {
                        countdownTimer.stop();
                        run();
                    }
                }));
                countdownTimer.setCycleCount(Timeline.INDEFINITE);
                countdownTimer.play();
            } else {
                run();
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

        if (prdImage != null) {
            if (imageBytes != null && imageBytes.length > 0) {
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
        
        // --- YÊU CẦU LỊCH SỬ ĐẤU GIÁ (GET_BID_HISTORY) TỪ SERVER ĐỂ VẼ BIỂU ĐỒ ---
        if (auction.getId() > 0) {
             Request req = new Request(auction.getId(), ActionType.GET_BID_HISTORY);
             try {
                 // Gửi request bất đồng bộ hoặc đồng bộ tùy hệ thống, ở đây tôi dùng luồng mới 
                 // hoặc ClientSocket nếu nó đã hỗ trợ xử lý sau
                 // Tạm thời dùng luồng mới để tránh block UI
                 new Thread(() -> {
                     try {
                         Response res = ClientSocket.sendRequest(req);
                         if (res != null) {
                             handleGetBidHistoryResponse(res);
                         }
                     } catch (Exception e) {
                         System.err.println("Lỗi khi yêu cầu lịch sử đấu giá: " + e.getMessage());
                     }
                 }).start();
             } catch (Exception e) {
                 e.printStackTrace();
             }
        }
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
            bidAmount.setPromptText("");
            bidAmount.setDisable(true);
        }
    }

    private void restartRunningCountdown() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        if (currentAuction == null || currentAuction.getEnd_time() == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(currentAuction.getEnd_time())) {
            handleAuctionEnd();
            return;
        }

        remainingSeconds = java.time.Duration.between(now, currentAuction.getEnd_time()).getSeconds();
        updateTimeLabel();

        if (Bid != null) {
            Bid.setDisable(false);
            Bid.setText("Place Bid");
        }
        if (bidAmount != null) {
            bidAmount.setDisable(false);
        }

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
    }

    private void run() {
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
}
