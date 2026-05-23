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

public class AuctionManagerController {
    @FXML
    private TextArea reasonArea;
    @FXML
    private ImageView prdImage;
    @FXML
    private Button suspend_btn, delete;
    @FXML
    private Label currentTime, startingPrice, startDate, startTime, duration, prdName, prd_description;

    private Runnable onBackAction;
    private Auction currentAuction;
    private Timeline liveClockTimeline;

    public void initialize() {
        // ========================================================
        // GẮN LISTENER ĐỂ CẬP NHẬT TRẠNG THÁI NÚT SUSPEND THEO THỜI GIAN THỰC
        // ========================================================
        if (reasonArea != null) {
            reasonArea.textProperty().addListener((observable, oldValue, newValue) -> updateButtonStates());
        }
    }

    /**
     * Thuật toán bật/tắt các nút điều khiển dựa trên trạng thái và dữ liệu đầu vào
     */
    private void updateButtonStates() {
        if (currentAuction == null) return;

        String aucStatus = currentAuction.getStatus() != null ? currentAuction.getStatus().toUpperCase() : "";

        // ==========================================
        // LOGIC CHO NÚT SUSPEND (ĐÌNH CHỈ)
        // ==========================================
        if (suspend_btn != null) {
            if ("SUSPENDED".equals(aucStatus) || "FINISHED".equals(aucStatus)) {
                // Đã kết thúc hoặc đình chỉ thì khóa vĩnh viễn
                suspend_btn.setDisable(true);
            } else {
                // Đang diễn ra hoặc chờ thì chỉ bật khi đã nhập lý do
                String currentReason = reasonArea != null ? reasonArea.getText().trim() : "";
                suspend_btn.setDisable(currentReason.isEmpty());
            }
        }

        // ==========================================
        // LOGIC CHO NÚT DELETE (XÓA)
        // ==========================================
        if (delete != null) {
            Item item = currentAuction.getItem();
            String modStatus = (item != null && item.getModeration_status() != null) ? item.getModeration_status().toUpperCase() : "";

            // Chỉ được phép xóa khi Item hoặc Auction bị REJECTED
            boolean isRejected = "REJECTED".equals(modStatus) || "REJECTED".equals(aucStatus);
            delete.setDisable(!isRejected);
        }
    }

    /**
     * Hàm chính nhận dữ liệu từ homepageController truyền sang khi click vào dòng Auction
     */
    public void setAuctionData(Auction auction) {
        if (auction == null) return;
        this.currentAuction = auction;
        Item item = auction.getItem();

        // 1. Dọn dẹp & Đặt lại UI mặc định
        if (reasonArea != null) {
            reasonArea.clear();
            reasonArea.setEditable(true);
            reasonArea.setDisable(false);
        }
        if (suspend_btn != null) {
            suspend_btn.setText("Suspend"); // Tên mặc định của nút
        }
        if (delete != null) {
            delete.setText("Delete"); // Tên mặc định của nút
        }

        // 2. Bóc tách thời gian
        if (auction.getStart_time() != null) {
            LocalDateTime startDateTime = auction.getStart_time();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

            if (startDate != null) startDate.setText(startDateTime.format(dateFormatter));
            if (startTime != null) startTime.setText(startDateTime.format(timeFormatter));
        }

        if (auction.getStart_time() != null && auction.getEnd_time() != null) {
            java.time.Duration diff = java.time.Duration.between(auction.getStart_time(), auction.getEnd_time());
            long totalHours = diff.toHours();
            long minutes = diff.toMinutes() % 60;
            if (duration != null) duration.setText(String.format("%02d:%02d", totalHours, minutes));
        }

        startLiveClock(auction.getStart_time(), auction.getEnd_time());

        // 3. Bóc tách dữ liệu vật phẩm
        if (item != null) {
            if (prdName != null) prdName.setText(item.getName());
            if (startingPrice != null) startingPrice.setText(String.format("%,.0f đ", item.getStarting_price()));
            if (prd_description != null) prd_description.setText(item.getDescription() != null ? item.getDescription() : "Không có mô tả sản phẩm.");

            if (prdImage != null && item.getImageBytes() != null) {
                try (ByteArrayInputStream bis = new ByteArrayInputStream(item.getImageBytes())) {
                    Image img = new Image(bis);
                    prdImage.setImage(img);
                } catch (Exception e) {
                    System.err.println("[AUCTION MANAGER] Lỗi hiển thị hình ảnh sản phẩm!");
                    e.printStackTrace();
                }
            } else if (prdImage != null) prdImage.setImage(null);
        }

        // 4. KIỂM TRA TRẠNG THÁI ĐÌNH CHỈ
        String aucStatus = auction.getStatus() != null ? auction.getStatus().toUpperCase() : "";
        if ("SUSPENDED".equals(aucStatus)) {
            if (reasonArea != null) {
                reasonArea.setEditable(false); // Khóa chỉ đọc
                reasonArea.setText("Đang tải lý do đình chỉ...");
            }
            fetchSuspendReason(auction.getId());
        }

        // 5. Cập nhật nút bấm lần cuối
        updateButtonStates();
    }

    /**
     * Lấy lý do đình chỉ từ Server và fill vào reasonArea
     */
    private void fetchSuspendReason(int auctionId) {
        new Thread(() -> {
            Request req = new Request(auctionId, ActionType.ADMIN_GET_AUCTION_REASON);
            Response res = ClientSocket.sendRequest(req);

            Platform.runLater(() -> {
                if (reasonArea != null) {
                    if (res != null && "SUCCESS".equals(res.getStatus()) && res.getData() != null) {
                        reasonArea.setText((String) res.getData());
                    } else {
                        // Nếu không tìm thấy trong Database thì dùng câu mặc định
                        reasonArea.setText("SUSPEND do hết thời gian duyệt");
                    }
                }
            });
        }).start();
    }

    private void startLiveClock(LocalDateTime startTime, LocalDateTime endTime) {
        if (liveClockTimeline != null) liveClockTimeline.stop();

        if (startTime == null || endTime == null) {
            if (currentTime != null) currentTime.setText("N/A");
            return;
        }

        liveClockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            LocalDateTime now = LocalDateTime.now();

            if (now.isBefore(startTime)) {
                java.time.Duration durationToStart = java.time.Duration.between(now, startTime);
                long totalHours = durationToStart.toHours();
                long minutes = durationToStart.toMinutes() % 60;
                long seconds = durationToStart.toSeconds() % 60;
                if (currentTime != null) currentTime.setText(String.format("- %02d:%02d:%02d", totalHours, minutes, seconds));
            } else if (now.isBefore(endTime)) {
                java.time.Duration durationLeft = java.time.Duration.between(now, endTime);
                long totalHours = durationLeft.toHours();
                long minutes = durationLeft.toMinutes() % 60;
                long seconds = durationLeft.toSeconds() % 60;
                if (currentTime != null) currentTime.setText(String.format("%02d:%02d:%02d", totalHours, minutes, seconds));
            } else {
                if (currentTime != null) currentTime.setText("00:00:00");
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
        if (liveClockTimeline != null) liveClockTimeline.stop();
        if (onBackAction != null) onBackAction.run();
    }

    @FXML
    private void handleSuspendAuction(MouseEvent event) {
        if (currentAuction == null) return;

        String reason = (reasonArea != null) ? reasonArea.getText().trim() : "";
        if (reason.isEmpty()) return;

        // Vô hiệu hóa giao diện để tránh click đúp (chờ mạng)
        if (suspend_btn != null) {
            suspend_btn.setDisable(true);
            suspend_btn.setText("Processing...");
        }
        if (reasonArea != null) reasonArea.setDisable(true);

        new Thread(() -> {
            Object[] payload = new Object[]{currentAuction.getId(), reason};
            Request request = new Request(payload, ActionType.ADMIN_STOP_AUCTION);
            Response response = ClientSocket.sendRequest(request);

            Platform.runLater(() -> {
                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    if (onBackAction != null) onBackAction.run();
                } else {
                    // Trả lại giao diện nếu thất bại
                    if (suspend_btn != null) suspend_btn.setText("Suspend");
                    if (reasonArea != null) reasonArea.setDisable(false);
                    updateButtonStates();
                }
            });
        }).start();
    }

    @FXML
    private void handleDeleteAuction(ActionEvent event) {
        if (currentAuction == null) return;

        if (delete != null) {
            delete.setDisable(true);
            delete.setText("Processing...");
        }

        int itemId = currentAuction.getItem_id();

        new Thread(() -> {
            Request request = new Request(itemId, ActionType.ADMIN_DELETE_ITEM);
            Response response = ClientSocket.sendRequest(request);

            Platform.runLater(() -> {
                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    if (onBackAction != null) onBackAction.run();
                } else {
                    // Bật lại nếu lỗi
                    if (delete != null) {
                        delete.setText("Delete");
                        updateButtonStates();
                    }
                }
            });
        }).start();
    }
}