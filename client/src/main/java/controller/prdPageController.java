package controller;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import java.util.Locale;

public class prdPageController {

    @FXML
    private Label currentPrice, currentPrice2;
    @FXML
    private ImageView prdImage;
    @FXML
    private Label prdName, prd_description, auction_id;
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
    @FXML
    private ToggleButton description_btn;
    @FXML
    private ToggleButton price_chart_btn;
    @FXML
    private ToggleButton auto_bid_btn;
    @FXML
    private ToggleButton auto_bid_status_btn;
    @FXML
    private AnchorPane priceC;
    @FXML
    private AnchorPane auto_bid;

    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();
    private Timeline countdownTimer;
    private long remainingSeconds;
    private Auction currentAuction;
    private int currentHighestBidderId = 0;

    // Biến định dạng số tiền chung để dùng lại
    private final DecimalFormat currencyFormatter;

    // Biến theo dõi số tiền đang được đề xuất ở Label currentPrice2
    private double proposedBidAmount = 0;

    // Series cho biểu đồ giá
    private XYChart.Series<Number, Number> priceSeries;

    // --- BIẾN PHỤC VỤ GIÃN BIỂU ĐỒ THÔNG MINH ---
    private final List<Double> fakeXList = new ArrayList<>();
    private final List<Long> realTimeList = new ArrayList<>();

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
                if ((bidAmount.getText() == null || bidAmount.getText().isEmpty()) && currentAuction != null) {
                    bidAmount.setText(currencyFormatter.format(proposedBidAmount));
                    Platform.runLater(() -> bidAmount.positionCaret(bidAmount.getText().length()));
                }
            });
        }

        // --- TÍNH NĂNG MỚI: RÀNG BUỘC DỮ LIỆU AUTOBID KHI USER RỜI Ô NHẬP (FOCUS LOST) ---
        if (price_step != null) {
            price_step.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) { // Khi mất focus (người dùng nhấn ra ngoài hoặc chuyển ô)
                    validateAndCorrectPriceStep();
                }
            });
        }

        if (ceilling_price != null) {
            ceilling_price.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal) { // Khi mất focus
                    validateAndCorrectCeilingPrice();
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

    // =========================================================
    // HÀM KIỂM TRA VÀ TỰ ĐỘNG SỬA DỮ LIỆU AUTO-BID
    // =========================================================

    /**
     * Kiểm tra và tự động sửa nếu bước giá nhập vào thấp hơn bước giá tối thiểu của hệ thống
     */
    private void validateAndCorrectPriceStep() {
        if (currentAuction == null || price_step == null || price_step.getText().isEmpty()) return;

        double currentPriceValue = currentAuction.getCurrent_price();
        long minAllowedIncrement = (long) calculateMinimumIncrement(currentPriceValue);
        long inputStep = getRealPrice(price_step);

        if (inputStep < minAllowedIncrement) {
            // Tự động đẩy lên bước giá tối thiểu hợp lệ và thông báo trực quan
            price_step.setText(currencyFormatter.format(minAllowedIncrement));
            showErrorFeedback(price_step, "Min step: " + currencyFormatter.format(minAllowedIncrement));

            // Sau khi sửa lại bước giá, kiểm tra luôn giá trần xem có còn hợp lệ không
            validateAndCorrectCeilingPrice();
        }
    }

    /**
     * Kiểm tra và sửa nếu giá trần thấp hơn mức: Giá hiện tại + Bước giá đang nhập
     */
    private void validateAndCorrectCeilingPrice() {
        if (currentAuction == null || ceilling_price == null || ceilling_price.getText().isEmpty()) return;

        double currentPriceValue = currentAuction.getCurrent_price();
        long inputStep = getRealPrice(price_step);

        // Nếu ô bước giá trống, lấy bước giá tối thiểu mặc định làm căn cứ
        if (inputStep <= 0) {
            inputStep = (long) calculateMinimumIncrement(currentPriceValue);
        }

        long minAllowedCeiling = (long) (currentPriceValue + inputStep);
        long inputCeiling = getRealPrice(ceilling_price);

        if (inputCeiling < minAllowedCeiling) {
            // Tự động điều chỉnh lên mức giá trần tối thiểu hợp lệ
            ceilling_price.setText(currencyFormatter.format(minAllowedCeiling));
            showErrorFeedback(ceilling_price, "Min ceiling: " + currencyFormatter.format(minAllowedCeiling));
        }
    }

    /**
     * Hiệu ứng nhấp nháy chữ đỏ cảnh báo lỗi tạm thời trên TextField khi tự động sửa dữ liệu
     */
    private void showErrorFeedback(TextField textField, String warningMessage) {
        String oldStyle = textField.getStyle();
        String currentText = textField.getText();

        textField.setText("");
        textField.setPromptText(warningMessage);
        textField.setStyle(oldStyle + "; -fx-prompt-text-fill: #ff4d4d; -fx-font-weight: bold;");

        PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
        pause.setOnFinished(e -> {
            textField.setStyle(oldStyle);
            textField.setPromptText("");
            textField.setText(currentText);
        });
        pause.play();
    }


    // --- HÀM HỖ TRỢ THUẬT TOÁN BIỂU ĐỒ ---
    private String formatTime(long epoch) {
        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.ofHours(7));
        return dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private XYChart.Data<Number, Number> createSmartPoint(long realTime, double price) {
        if (fakeXList.isEmpty()) {
            fakeXList.add(0.0);
            realTimeList.add(realTime);
            return new XYChart.Data<>(0.0, price);
        }

        long lastReal = realTimeList.get(realTimeList.size() - 1);
        double lastFake = fakeXList.get(fakeXList.size() - 1);

        long deltaReal = realTime - lastReal;
        if (deltaReal < 0) deltaReal = 0;

        // Ép khoảng cách: Nhỏ nhất 60s, Lớn nhất 30 phút (1800s)
        double deltaFake = Math.max(60.0, Math.min(1800.0, deltaReal));
        double newFakeX = lastFake + deltaFake;

        fakeXList.add(newFakeX);
        realTimeList.add(realTime);

        return new XYChart.Data<>(newFakeX, price);
    }

    private void initPriceChart() {
        if (prcieChart != null) {
            prcieChart.setAnimated(false);
            prcieChart.setCreateSymbols(true);
            prcieChart.setLegendVisible(false);

            // Định dạng trục X với cơ chế dịch thông minh
            NumberAxis xAxis = (NumberAxis) prcieChart.getXAxis();
            xAxis.setAutoRanging(true);
            xAxis.setForceZeroInRange(false);
            xAxis.setMinorTickVisible(false); // Ẩn vạch chia phụ

            xAxis.setTickLabelFormatter(new StringConverter<>() {
                @Override
                public String toString(Number object) {
                    double x_tick = object.doubleValue();
                    if (fakeXList.isEmpty()) return "";

                    if (x_tick <= fakeXList.get(0)) return formatTime(realTimeList.get(0));
                    if (x_tick >= fakeXList.get(fakeXList.size() - 1)) return formatTime(realTimeList.get(realTimeList.size() - 1));

                    for (int i = 0; i < fakeXList.size() - 1; i++) {
                        double x1 = fakeXList.get(i);
                        double x2 = fakeXList.get(i + 1);
                        if (x_tick >= x1 && x_tick <= x2) {
                            long t1 = realTimeList.get(i);
                            long t2 = realTimeList.get(i + 1);
                            double ratio = (x_tick - x1) / (x2 - x1);
                            long interp = t1 + (long)(ratio * (t2 - t1));
                            return formatTime(interp);
                        }
                    }
                    return "";
                }

                @Override
                public Number fromString(String string) {
                    return 0;
                }
            });

            // Định dạng trục Y
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

            priceSeries = new XYChart.Series<>();
            prcieChart.getData().add(priceSeries);
        }
    }

    private void updatePanelsVisibility() {
        boolean showDesc = description_btn != null && description_btn.isSelected();
        boolean showChart = price_chart_btn != null && price_chart_btn.isSelected();
        boolean showAutoBid = auto_bid_btn != null && auto_bid_btn.isSelected();

        if (prd_description != null) {
            prd_description.setVisible(showDesc);
            auction_id.setVisible(showDesc);
        }
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

    private void checkAutoBidStatus() {
        if (!SessionManager.getInstance().isBidder() || currentAuction == null) {
            return;
        }
        int bidderId = SessionManager.getInstance().getCurrentUser().getId();
        Object[] payload = new Object[]{currentAuction.getId(), bidderId};
        Request req = new Request(payload, ActionType.CHECK_AUTOBID_STATUS);

        new Thread(() -> {
            try {
                Response res = ClientSocket.sendRequest(req);
                Platform.runLater(() -> {
                    if (res != null && "SUCCESS".equals(res.getStatus())) {
                        // Server trả về AutoBidConfig nếu đã đăng ký, hoặc null nếu chưa
                        if (res.getData() instanceof AutoBidConfig config) {
                            // Đã đăng ký -> Cập nhật trạng thái và điền thông tin
                            updateAutoBidState(true, "Auto-bid active.");
                            if (ceilling_price != null) {
                                ceilling_price.setText(currencyFormatter.format(config.getMaxBid()));
                            }
                            if (price_step != null) {
                                price_step.setText(currencyFormatter.format(config.getIncrement()));
                            }
                        } else {
                            // Chưa đăng ký -> Chỉ cập nhật trạng thái
                            updateAutoBidState(false, "Auto-bid inactive.");
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void registerAutoBid() {
        if (!SessionManager.getInstance().isBidder()) {
            updateAutoBidState(false, "Sign in as bidder first.");
            try {
                sceneSwitcher.openSignInPopup();
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

    private double getBidStep(double currentPrice) {
        if (currentPrice >= 1_000_000_000) {
            return 10_000_000;
        } else if (currentPrice >= 100_000_000) {
            return 1_000_000;
        } else if (currentPrice >= 10_000_000) {
            return 100_000;
        } else {
            return 10_000;
        }
    }

    private int fetchHighestBidderId() {
        if (currentAuction == null) {
            return 0;
        }

        return fetchHighestBidderId(currentAuction.getId());
    }

    private int fetchHighestBidderId(int auctionId) {
        Response response = ClientSocket.sendRequest(new Request(auctionId, ActionType.GET_HIGHEST_BIDDER_ID));
        if (response != null && "SUCCESS".equals(response.getStatus()) && response.getData() instanceof Integer bidderId) {
            return bidderId;
        }

        return currentHighestBidderId;
    }

    private void refreshHighestBidderIdAsync() {
        if (currentAuction == null || !SessionManager.getInstance().isBidder()) {
            return;
        }

        int requestedAuctionId = currentAuction.getId();
        new Thread(() -> {
            int bidderId = fetchHighestBidderId(requestedAuctionId);
            Platform.runLater(() -> {
                if (currentAuction == null || currentAuction.getId() != requestedAuctionId) {
                    return;
                }
                currentHighestBidderId = bidderId;
                currentAuction.setHighest_bidder_id(bidderId);
                updateHighestBidderState();
            });
        }).start();
    }

    private boolean isCurrentUserHighestBidder() {
        return SessionManager.getInstance().isBidder()
                && currentHighestBidderId > 0
                && SessionManager.getInstance().getCurrentUser().getId() == currentHighestBidderId;
    }

    private boolean isCurrentAuctionRunning() {
        return currentAuction != null
                && "RUNNING".equals(currentAuction.getStatus())
                && currentAuction.getEnd_time() != null
                && LocalDateTime.now().isBefore(currentAuction.getEnd_time());
    }

    private void updateHighestBidderState() {
        if (currentAuction == null || !isCurrentAuctionRunning()) {
            return;
        }

        if (isCurrentUserHighestBidder()) {
            if (Bid != null) {
                Bid.setDisable(true);
                Bid.setText("Leading");
            }
            if (bidAmount != null) {
                bidAmount.clear();
                bidAmount.setDisable(true);
                bidAmount.setPromptText("You are holding the highest bid.");
            }
            if (bid_increase != null) bid_increase.setDisable(true);
            if (bid_decrease != null) bid_decrease.setDisable(true);
        } else {
            if (Bid != null) {
                Bid.setDisable(false);
                Bid.setText("Place Bid");
            }
            if (bidAmount != null) {
                bidAmount.setDisable(false);
                updateBidPrompt();
            }
            if (bid_increase != null) bid_increase.setDisable(false);
            if (bid_decrease != null) bid_decrease.setDisable(false);
        }
    }

    private void handleBidIncrease() {
        if (currentAuction == null) return;
        if (isCurrentUserHighestBidder()) {
            updateHighestBidderState();
            return;
        }
        double step = getBidStep(currentAuction.getCurrent_price());
        proposedBidAmount += step;

        if (currentPrice2 != null) currentPrice2.setText(currencyFormatter.format(proposedBidAmount) + " ₫");
        if (bidAmount != null) bidAmount.setText(currencyFormatter.format(proposedBidAmount));
    }

    private void handleBidDecrease() {
        if (currentAuction == null) return;
        if (isCurrentUserHighestBidder()) {
            updateHighestBidderState();
            return;
        }
        double minAllowed = currentAuction.getCurrent_price() + calculateMinimumIncrement(currentAuction.getCurrent_price());
        double step = getBidStep(currentAuction.getCurrent_price());

        proposedBidAmount -= step;

        if (proposedBidAmount < minAllowed) {
            proposedBidAmount = minAllowed;
        }

        if (currentPrice2 != null) currentPrice2.setText(currencyFormatter.format(proposedBidAmount) + " ₫");
        if (bidAmount != null) bidAmount.setText(currencyFormatter.format(proposedBidAmount));
    }

    private void updateBidPrompt() {
        if (currentAuction != null && bidAmount != null) {
            if (isCurrentUserHighestBidder()) {
                bidAmount.setPromptText("You are holding the highest bid.");
                return;
            }

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

            currentHighestBidderId = fetchHighestBidderId();
            currentAuction.setHighest_bidder_id(currentHighestBidderId);
            if (isCurrentUserHighestBidder()) {
                updateHighestBidderState();
                return;
            }

            long realBidAmount = getRealPrice(bidAmount);
            double currentPriceValue = currentAuction.getCurrent_price();
            double minInc = calculateMinimumIncrement(currentPriceValue);
            double minAllowedBid = currentPriceValue + minInc;

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
                    int latestHighestBidderId = fetchHighestBidderId();
                    Platform.runLater(() -> {
                        currentHighestBidderId = latestHighestBidderId;
                        currentAuction.setHighest_bidder_id(latestHighestBidderId);
                        currentAuction.setCurrent_price(realBidAmount);
                        updateCurrentPriceLabel(realBidAmount);
                        bidAmount.clear();
                        updateHighestBidderState();
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
            sceneSwitcher.openSignInPopup();
        }
    }

    // --- XỬ LÝ BROADCAST TỪ SERVER ---
    public void handleBroadcast(Response res) {
        if (res == null || currentAuction == null) return;

        String status = res.getStatus();

        if ("SUCCESS".equals(status) && res.getData() instanceof List) {
            handleGetBidHistoryResponse(res);
            return;
        }

        if ("NOTIFY_NEW_PRICE".equals(status)) {
            if (res.getData() instanceof Bid newBid) {

                if (newBid.getAuction_id() == currentAuction.getId()) {
                    System.out.println("Cập nhật giá mới từ Server: " + newBid.getAmount());
                    Platform.runLater(() -> {
                        currentHighestBidderId = newBid.getBidder_id();
                        currentAuction.setCurrent_price(newBid.getAmount());
                        currentAuction.setHighest_bidder_id(newBid.getBidder_id());
                        updateCurrentPriceLabel(newBid.getAmount());

                        // Thêm điểm vào biểu đồ sử dụng Smart Point
                        if (priceSeries != null && newBid.getBid_time() != null) {
                            long timestamp = newBid.getBid_time().toEpochSecond(ZoneOffset.ofHours(7));
                            priceSeries.getData().add(createSmartPoint(timestamp, newBid.getAmount()));
                        }

                        updateBidPrompt();
                        updateHighestBidderState();

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
                    if (prcieChart != null && !rawList.isEmpty()) {

                        // Dọn dẹp bộ nhớ đệm thuật toán trước khi load lịch sử
                        fakeXList.clear();
                        realTimeList.clear();
                        List<XYChart.Data<Number, Number>> newChartData = new ArrayList<>();

                        // Thêm điểm giá khởi điểm
                        if (currentAuction != null && currentAuction.getStart_time() != null) {
                            long startTimestamp = currentAuction.getStart_time().toEpochSecond(ZoneOffset.ofHours(7));
                            newChartData.add(createSmartPoint(startTimestamp, currentAuction.getItem().getStarting_price()));
                        }

                        // Thêm lịch sử với Smart Point
                        for (Object obj : rawList) {
                            if (obj instanceof Bid b) {
                                if (b.getBid_time() != null) {
                                    long timestamp = b.getBid_time().toEpochSecond(ZoneOffset.ofHours(7));
                                    newChartData.add(createSmartPoint(timestamp, b.getAmount()));
                                }
                            }
                        }

                        XYChart.Series<Number, Number> newSeries = new XYChart.Series<>();
                        newSeries.getData().addAll(newChartData);

                        prcieChart.getData().clear();
                        prcieChart.getData().add(newSeries);

                        priceSeries = newSeries;

                        System.out.println("Đã vẽ lại biểu đồ với " + rawList.size() + " lượt đặt cũ.");
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

    private void updateCurrentPriceLabel(double price) {
        if (currentPrice != null) {
            currentPrice.setText(currencyFormatter.format(price) + " ₫");
        }

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
        this.currentHighestBidderId = auction.getHighest_bidder_id();
        String name = auction.getItem().getName();
        long price = (long) auction.getCurrent_price();
        String imagePath = auction.getItem().getImgPath();
        byte[] imageBytes = auction.getItem().getImageBytes();
        String descriptionText = auction.getItem().getDescription();
        String productId = auction.getItem().getUser_prdID();
        int auctionID = auction.getId();

        if (prdName != null) {
            prdName.setText(name);
        }

        updateCurrentPriceLabel(price);
        updateBidPrompt();

        if (countdownTimer != null) countdownTimer.stop();

        LocalDateTime now = LocalDateTime.now();
        String status = auction.getStatus();

        // Xác định chính xác số giây còn lại dựa trên thời gian thực tế
        if ("RUNNING".equals(status) && auction.getEnd_time() != null) {
            if (now.isBefore(auction.getEnd_time())) {
                this.remainingSeconds = java.time.Duration.between(now, auction.getEnd_time()).getSeconds();
            } else {
                status = "FINISHED";
            }
        } else if (("WAITING".equals(status) || "PROPOSAL".equals(status) || "DELETE_PROPOSAL".equals(status)) && auction.getStart_time() != null) {
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
        currentAuction.setStatus(status);

        // =======================================================================
        // XỬ LÝ ĐỒNG BỘ TRẠNG THÁI UI CHI TIẾT THEO TỪNG PHASE
        // =======================================================================
        if ("WAITING".equals(status) || "PROPOSAL".equals(status) || "DELETE_PROPOSAL".equals(status)) {
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

            // Khóa phần đặt giá vì phiên đấu giá chưa thực sự bắt đầu (Upcoming)
            if (Bid != null) {
                Bid.setDisable(true);
                Bid.setText("Upcoming");
            }
            if (bidAmount != null) bidAmount.setDisable(true);

            // ĐẢM BẢO: Các Tab hiển thị thông tin như mô tả vẫn phải mở để User có thể xem trước
            if (description_btn != null) description_btn.setDisable(false);
            if (prd_description != null) prd_description.setDisable(false);

        } else if ("RUNNING".equals(status)) {
            updateTimeLabel();
            if (Bid != null) {
                Bid.setDisable(false);
                Bid.setText("Place Bid");
            }
            if (bidAmount != null) bidAmount.setDisable(false);
            updateHighestBidderState();

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

        // Đổ hình ảnh lên giao diện
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

        // Đổ văn bản mô tả mới (Đã được cập nhật từ database sau khi Accept Changes)
        if (prd_description != null) {
            prd_description.setText(descriptionText != null && !descriptionText.trim().isEmpty()
                    ? descriptionText
                    : "No product description.");
        }
        if (auction_id != null) {
            String displayProductId = productId != null && !productId.trim().isEmpty() ? productId : "N/A";
            auction_id.setText("Auction ID: " + auctionID + " | Product ID: " + displayProductId);
        }

        // Đồng bộ lại cơ chế ẩn hiện các Tab để đảm bảo văn bản mới được hiển thị ngay lập tức
        updatePanelsVisibility();

        System.out.println("Displaying: " + name + " - Auction Status: " + status);

        if (auction.getId() > 0) {
            Request req = new Request(auction.getId(), ActionType.GET_BID_HISTORY);
            try {
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

            checkAutoBidStatus();
            refreshHighestBidderIdAsync();
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
        updateHighestBidderState();

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
        if (currentAuction != null) {
            String currentStatus = currentAuction.getStatus();
            if ("WAITING".equals(currentStatus) || "PROPOSAL".equals(currentStatus)) {
                currentAuction.setStatus("RUNNING");
            }
        }

        restartRunningCountdown();
    }
}
