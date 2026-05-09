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

    // PHƯƠNG THỨC MỚI: Hỗ trợ nạp ảnh qua mảng byte (truyền qua mạng)
    public void setData(String name, long price, long time, String imagePath, String status, String sellerNameStr, byte[] imageBytes) {
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

        // --- BẮT ĐẦU ĐỊNH DẠNG TIỀN TỆ ---
        if (currentPrice != null) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
            symbols.setGroupingSeparator('.'); // Thiết lập dấu phân cách là dấu chấm

            // Mẫu định dạng: ###,### (ngăn cách mỗi 3 chữ số)
            DecimalFormat formatter = new DecimalFormat("###,###", symbols);
            String formattedPrice = formatter.format(price);

            currentPrice.setText(formattedPrice + " ₫");
        }
        // --- KẾT THÚC ĐỊNH DẠNG ---

        // --- BẮT ĐẦU BỘ ĐẾM THỜI GIAN & TRẠNG THÁI ---
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
                        if (auctionStatus != null) {
                            auctionStatus.setStyle("-fx-background-color: rgba(61, 211, 91, 0.6); -fx-background-radius: 10px; -fx-text-fill: white;");
                            auctionStatus.setText("Started - refreshing...");
                        }
                        if (Bid != null) {
                            Bid.setDisable(false);
                            Bid.setText("Start Biddding");
                        }
                    }
                }));
                countdownTimer.setCycleCount(Timeline.INDEFINITE);
                countdownTimer.play();
            } else {
                if (auctionStatus != null) {
                    auctionStatus.setStyle("-fx-background-color: rgba(61, 211, 91, 0.6); -fx-background-radius: 10px; -fx-text-fill: white;");
                    auctionStatus.setText("Started - refreshing...");
                }
                if (Bid != null) {
                    Bid.setDisable(false);
                    Bid.setText("Start Biddding");
                }
            }
            
            if (Bid != null) {
                Bid.setText("Upcoming");
                // Tùy chọn: có thể khóa nút hoặc vẫn cho người dùng vào xem trang chi tiết
                // Bid.setDisable(true); 
            }

        } else if ("RUNNING".equals(status)) {
            updateTimeLabel();
            if (Bid != null) {
                Bid.setDisable(false);
                Bid.setText("Start Bidding");
            }

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
            // FINISHED
            handleAuctionEnd();
        }
        // --- KẾT THÚC BỘ ĐẾM THỜI GIAN ---

        // --- BẮT ĐẦU LOAD ẢNH ---
        if (prdImage != null) {
            if (imageBytes != null && imageBytes.length > 0) {
                // Cách mới: Dựng ảnh từ dữ liệu nhị phân do Server truyền sang
                try {
                    ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
                    Image image = new Image(bis);
                    prdImage.setImage(image);
                } catch (Exception e) {
                    System.out.println("Lỗi khi load ảnh từ byte array: " + e.getMessage());
                }
            } else if (imagePath != null && !imagePath.trim().isEmpty()) {
                // Cách cũ: Fallback dự phòng đọc thẳng từ đĩa (local)
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

    // Giữ lại hàm cũ để tránh lỗi tương thích ở những chỗ khác chưa truyền mảng byte
    public void setData(String name, long price, long time, String imagePath, String status, String sellerNameStr) {
        setData(name, price, time, imagePath, status, sellerNameStr, null);
    }

    public void setData(String name, long price, long time, String imagePath, String status) {
        setData(name, price, time, imagePath, status, "Unknown", null);
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
            // Vẫn cho phép vào xem trang chi tiết để xem ai thắng, nhưng nút Bid ở trong sẽ bị khóa
        }
    }

    // Hàm này để MainPage truyền lệnh vào
    public void setOnBidAction(Runnable action) {
        this.toPrdPage = action;
    }

    public void handleBidBtn(MouseEvent event) {
        if (toPrdPage != null) {
            toPrdPage.run(); // Kích hoạt lệnh mà MainPage đã giao
        }
    }
}