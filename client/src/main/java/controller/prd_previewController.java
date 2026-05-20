package controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class prd_previewController {
    @FXML
    private Label prdName;
    @FXML
    private Label currentPrice;
    @FXML
    private Button Bid;
    @FXML
    private ImageView prdImage;
    @FXML
    private Label auctionStatus;
    @FXML
    private Label sellerName;

    private Runnable toPrdPage;
    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();

    private Timeline countdownTimer;
    private long remainingSeconds;

    // BIẾN MỚI: Lưu thời gian kết thúc để dùng khi tự động chuyển trạng thái
    private long timeToEndSeconds;

    // PHƯƠNG THỨC ĐÃ CẬP NHẬT: Thêm tham số 'long timeToEnd'
    public void setData(String name, long price, long timeToStart, long timeToEnd, String imagePath, String status, String sellerNameStr, byte[] imageBytes) {
        if (prdName != null) {
            prdName.setText(name);
        }

        if (sellerName != null) {
            if (sellerNameStr != null && !sellerNameStr.isEmpty()) {
                sellerName.setText("by " + sellerNameStr);
            } else {
                sellerName.setText("by Unknown");
            }
        }

        if (currentPrice != null) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
            symbols.setGroupingSeparator('.');
            DecimalFormat formatter = new DecimalFormat("###,###", symbols);
            currentPrice.setText(formatter.format(price) + " ₫");
        }

        // --- BẮT ĐẦU BỘ ĐẾM THỜI GIAN & TRẠNG THÁI ---
        this.remainingSeconds = timeToStart;
        this.timeToEndSeconds = timeToEnd; // Lưu lại để xài sau

        if (countdownTimer != null) countdownTimer.stop();

        if ("WAITING".equals(status) || "UPCOMING".equals(status)) {
            updateUpcomingTimeLabel();

            if (remainingSeconds > 0) {
                countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                    remainingSeconds--;
                    updateUpcomingTimeLabel();
                    if (remainingSeconds <= 0) {
                        countdownTimer.stop();
                        switchToRunningState(); // TỰ ĐỘNG CHUYỂN TRẠNG THÁI
                    }
                }));
                countdownTimer.setCycleCount(Timeline.INDEFINITE);
                countdownTimer.play();
            } else {
                switchToRunningState();
            }

            if (Bid != null) {
                Bid.setText("Upcoming");
                Bid.setDisable(true); // Nên khóa nút lúc chờ
            }

        } else if ("RUNNING".equals(status)) {
            // Nếu vừa vào đã là RUNNING, time truyền vào thực chất là thời gian kết thúc
            this.remainingSeconds = (timeToEnd > 0) ? timeToEnd : timeToStart;
            startRunningCountdown();
        } else {
            handleAuctionEnd();
        }

        // --- BẮT ĐẦU LOAD ẢNH ---
        if (prdImage != null) {
            if (imageBytes != null && imageBytes.length > 0) {
                try {
                    ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
                    Image image = new Image(bis);
                    prdImage.setImage(image);
                } catch (Exception e) {
                    System.out.println("Lỗi khi load ảnh từ byte array: " + e.getMessage());
                }
            } else if (imagePath != null && !imagePath.trim().isEmpty()) {
                try {
                    File imgFile = new File("auction-server/src/main/resources" + imagePath);
                    if (imgFile.exists()) {
                        Image image = new Image(imgFile.toURI().toString());
                        prdImage.setImage(image);
                    } else {
                        java.io.InputStream is = getClass().getResourceAsStream(imagePath);
                        if (is != null) {
                            prdImage.setImage(new Image(is));
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Không thể hiển thị ảnh từ path: " + imagePath);
                }
            }
        }
    }

    // HÀM MỚI: Xử lý tự động chuyển sang chế độ đếm ngược kết thúc
    private void switchToRunningState() {
        if (Bid != null) {
            Bid.setDisable(false);
            Bid.setText("Start Bidding");
        }
        // Gán thời gian hiện tại bằng tổng thời gian phiên đấu giá diễn ra
        this.remainingSeconds = this.timeToEndSeconds;
        startRunningCountdown();
    }

    // HÀM MỚI: Chạy đồng hồ đếm ngược chờ kết thúc
    private void startRunningCountdown() {
        updateTimeLabel();
        if (Bid != null) {
            Bid.setDisable(false);
            Bid.setText("Start Bidding");
        }

        if (countdownTimer != null) countdownTimer.stop();

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

    // Các hàm Overload cũ giữ nguyên phòng hờ lỗi
    public void setData(String name, long price, long timeToStart, long timeToEnd, String imagePath, String status, String sellerNameStr) {
        setData(name, price, timeToStart, timeToEnd, imagePath, status, sellerNameStr, null);
    }
    public void setData(String name, long price, long time, String imagePath, String status) {
        setData(name, price, time, 0, imagePath, status, "Unknown", null);
    }

    private void updateTimeLabel() {
        if (auctionStatus != null && remainingSeconds >= 0) {
            long hours = remainingSeconds / 3600;
            long minutes = (remainingSeconds % 3600) / 60;
            long seconds = remainingSeconds % 60;
            String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            auctionStatus.setStyle("-fx-background-color: rgba(61, 211, 91, 0.6); -fx-background-radius: 10px; -fx-text-fill: white;");
            auctionStatus.setText(timeString);
        }
    }

    private void updateUpcomingTimeLabel() {
        if (auctionStatus != null && remainingSeconds >= 0) {
            long hours = remainingSeconds / 3600;
            long minutes = (remainingSeconds % 3600) / 60;
            long seconds = remainingSeconds % 60;
            String timeString = String.format("Upcoming: %02d:%02d:%02d", hours, minutes, seconds);
            auctionStatus.setStyle("-fx-background-color: rgba(128, 128, 128, 0.6); -fx-background-radius: 10px; -fx-text-fill: white;");
            auctionStatus.setText(timeString);
        }
    }

    private void handleAuctionEnd() {
        if (auctionStatus != null) {
            auctionStatus.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6); -fx-background-radius: 10px;  -fx-text-fill: #b5b4b4;");
            auctionStatus.setText("Ended");
        }
        if (Bid != null) {
            Bid.setText("Ended");
        }
    }

    public void setOnBidAction(Runnable action) {
        this.toPrdPage = action;
    }

    public void handleBidBtn(MouseEvent event) {
        if (toPrdPage != null) {
            toPrdPage.run();
        }
    }

    public void stopTimer() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
    }
}