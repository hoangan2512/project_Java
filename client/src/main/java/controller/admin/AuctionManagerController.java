package controller.admin;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import model.Auction;
import model.Item;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuctionManagerController {
    @FXML
    private TextArea reasonArea;
    @FXML
    private ImageView prdImage;
    @FXML
    private Button confirm_btn, backBtn;
    @FXML
    private Label currentTime, startingPrice, startDate, startTime, duration, prdName, prd_description;

    private Runnable onBackAction;
    private Auction currentAuction;
    private Timeline liveClockTimeline;

    public void initialize() {
        // Khởi tạo mặc định nếu cần
    }

    /**
     * Hàm chính nhận dữ liệu từ homepageController truyền sang khi click vào dòng Auction
     */
    public void setAuctionData(Auction auction) {
        if (auction == null) return;

        // Lưu lại thực thể để xử lý nghiệp vụ nút bấm bấm sau này (ví dụ: Stop/Cancel phiên)
        this.currentAuction = auction;

        // Trích xuất đối tượng Item nằm trong Auction
        Item item = auction.getItem();

        // ========================================================
        // 1. BÓC TÁCH DỮ LIỆU THỜI GIAN CẤU HÌNH PHIÊN (START_TIME)
        // ========================================================
        if (auction.getStart_time() != null) {
            LocalDateTime startDateTime = auction.getStart_time();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

            if (startDate != null) startDate.setText(startDateTime.format(dateFormatter));
            if (startTime != null) startTime.setText(startDateTime.format(timeFormatter));
        }

        // ========================================================
        // 2. TÍNH TOÁN TỔNG THỜI GIAN DIỄN RA PHIÊN (DURATION DẠNG hh:mm)
        // ========================================================
        if (auction.getStart_time() != null && auction.getEnd_time() != null) {
            java.time.Duration diff = java.time.Duration.between(auction.getStart_time(), auction.getEnd_time());
            long totalHours = diff.toHours();
            long minutes = diff.toMinutes() % 60;

            if (duration != null) {
                duration.setText(String.format("%02d:%02d", totalHours, minutes));
            }
        }

        // ========================================================
        // 3. KÍCH HOẠT ĐẾM NGƯỢC THỜI GIAN THỰC (CURRENT TIME PANE)
        // ========================================================
        startLiveClock(auction.getStart_time(), auction.getEnd_time());

        // ========================================================
        // 4. BÓC TÁCH DỮ LIỆU VẬT PHẨM (ITEM)
        // ========================================================
        if (item != null) {
            if (prdName != null) prdName.setText(item.getName());

            if (startingPrice != null) {
                startingPrice.setText(String.format("%,.0f đ", item.getStarting_price()));
            }

            if (prd_description != null) {
                prd_description.setText(item.getDescription() != null ? item.getDescription() : "Không có mô tả sản phẩm.");
            }

            // Giải mã mảng byte ảnh truyền trực tiếp lên ImageView
            if (prdImage != null && item.getImageBytes() != null) {
                try (ByteArrayInputStream bis = new ByteArrayInputStream(item.getImageBytes())) {
                    Image img = new Image(bis);
                    prdImage.setImage(img);
                } catch (Exception e) {
                    System.err.println("[AUCTION MANAGER] Lỗi hiển thị hình ảnh sản phẩm!");
                    e.printStackTrace();
                }
            } else if (prdImage != null) {
                prdImage.setImage(null); // Xóa ảnh cũ nếu sản phẩm này không có ảnh
            }
        }
    }

    /**
     * Bộ đếm thời gian động cập nhật từng giây lên nhãn currentTime của khung điều khiển
     */
    private void startLiveClock(LocalDateTime startTime, LocalDateTime endTime) {
        if (liveClockTimeline != null) {
            liveClockTimeline.stop();
        }

        if (startTime == null || endTime == null) {
            if (currentTime != null) currentTime.setText("N/A");
            return;
        }

        liveClockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            LocalDateTime now = LocalDateTime.now();

            if (now.isBefore(startTime)) {
                // Chưa đến giờ mở: Hiện dạng "- hh:mm:ss" màu vàng cảnh báo
                java.time.Duration durationToStart = java.time.Duration.between(now, startTime);
                long totalHours = durationToStart.toHours();
                long minutes = durationToStart.toMinutes() % 60;
                long seconds = durationToStart.toSeconds() % 60;

                if (currentTime != null) {
                    currentTime.setText(String.format("- %02d:%02d:%02d", totalHours, minutes, seconds));
                }

            } else if (now.isBefore(endTime)) {
                // Đang diễn ra: Hiện thời gian còn lại màu xanh lá cây
                java.time.Duration durationLeft = java.time.Duration.between(now, endTime);
                long totalHours = durationLeft.toHours();
                long minutes = durationLeft.toMinutes() % 60;
                long seconds = durationLeft.toSeconds() % 60;

                if (currentTime != null) {
                    currentTime.setText(String.format("%02d:%02d:%02d", totalHours, minutes, seconds));
                }

            } else {
                // Đã kết thúc: Hiện 00:00:00 màu xám
                if (currentTime != null) {
                    currentTime.setText("00:00:00");
                }
                liveClockTimeline.stop();
            }
        }));

        liveClockTimeline.setCycleCount(Animation.INDEFINITE);
        liveClockTimeline.play();
    }

    public void setOnBack(Runnable onBackAction) {
        this.onBackAction = onBackAction;
    }

    @FXML
    private void handleBackBtn(ActionEvent event) {
        // Hủy bộ đếm thời gian khi đóng hoặc ẩn panel quản lý để tránh rò rỉ RAM
        if (liveClockTimeline != null) {
            liveClockTimeline.stop();
        }

        if (onBackAction != null) {
            onBackAction.run();
        }
    }
}