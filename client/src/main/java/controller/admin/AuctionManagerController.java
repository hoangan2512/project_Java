package controller.admin;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import message.Request;
import message.Response;
import model.ActionType;
import model.Auction;
import model.Item;
import network.ClientSocket;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class AuctionManagerController {
    @FXML
    private TextArea reasonArea;
    @FXML
    private ImageView prdImage;
    @FXML
    private Button suspend_btn;
    @FXML
    private Label currentTime, startingPrice, startDate, startTime, duration, prdName, prd_description;

    private Runnable onBackAction;
    private Auction currentAuction;
    private Timeline liveClockTimeline;

    public void initialize() {}

    /**
     * Hàm chính nhận dữ liệu từ homepageController truyền sang khi click vào dòng Auction
     */
    public void setAuctionData(Auction auction) {
        if (auction == null) return;

        // Reset UI components to their default state
        if (reasonArea != null) {
            reasonArea.clear();
        }
        if (suspend_btn != null) {
            // Re-enable the button by default, then disable based on auction status
            suspend_btn.setDisable(false);
        }

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
        
        // Disable suspend button if auction is already suspended or finished
        if (suspend_btn != null && ("SUSPENDED".equals(auction.getStatus()) || "FINISHED".equals(auction.getStatus()))) {
            suspend_btn.setDisable(true);
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

    @FXML
    private void handleSuspendAuction(MouseEvent event) {
        if (currentAuction == null) return;

        String reason = (reasonArea != null) ? reasonArea.getText().trim() : "";

        // Bắt buộc Admin phải điền lý do vi phạm trước khi dừng phiên để đảm bảo tính minh bạch
        if (reason.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập lý do đình chỉ phiên đấu giá này!");
            return;
        }

        // Đóng gói dữ liệu gửi đi dưới dạng Object[] như Server đang mong đợi
        // [auctionId, reasonText]
        Object[] payload = new Object[]{currentAuction.getId(), reason};

        // Chạy Thread riêng để thực hiện Network Request tránh block UI đóng băng ứng dụng
        new Thread(() -> {
            Request request = new Request(payload, ActionType.ADMIN_STOP_AUCTION);
            Response response = ClientSocket.sendRequest(request);

            Platform.runLater(() -> {
                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã đình chỉ thành công phiên đấu giá ID: " + currentAuction.getId());

                    // Vô hiệu hóa nút bấm ngay lập tức sau khi dừng thành công
                    if (suspend_btn != null) suspend_btn.setDisable(true);

                    // Quay về danh sách quản lý chung của homepage sau khi thực hiện
                    if (onBackAction != null) onBackAction.run();
                } else {
                    String message = (response != null) ? response.getMessage() : "Mất kết nối tới Server.";
                    showAlert(Alert.AlertType.ERROR, "Thất bại", "Không thể dừng phiên: " + message);
                }
            });
        }).start();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}