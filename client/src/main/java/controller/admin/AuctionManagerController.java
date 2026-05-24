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
                suspend_btn.setDisable(true);
            } else {
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

            boolean isRejected = "REJECTED".equals(modStatus) || "REJECTED".equals(aucStatus);
            delete.setDisable(!isRejected);
        }
    }

    /**
     * Hàm chính nhận dữ liệu thô từ homepageController truyền sang khi click vào dòng Auction
     */
    public void setAuctionData(Auction auction) {
        if (auction == null) return;
        this.currentAuction = auction;
        Item item = auction.getItem();

        // 1. Dọn dẹp & Đặt lại UI mặc định về trạng thái chờ nạp dữ liệu sâu
        if (reasonArea != null) {
            reasonArea.clear();
            reasonArea.setEditable(true);
            reasonArea.setDisable(false);
        }
        if (suspend_btn != null) suspend_btn.setText("Suspend");
        if (delete != null) delete.setText("Delete");
        if (prdImage != null) prdImage.setImage(null); // Xóa ảnh cũ tránh hiện tượng nháy ảnh sản phẩm trước

        // 2. Bóc tách thời gian thô (Hiển thị ngay lập tức)
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

        // 3. ĐỔ DỮ LIỆU CÓ SẴN LÊN UI (TÊN VÀ GIÁ HIỆN TẠI NGUYÊN BẢN CHỮ SỐ)
        if (item != null) {
            if (prdName != null) prdName.setText(item.getName());
        }

        if (startingPrice != null) {
            // CHỈ FILL MỖI SỐ GIÁ HIỆN TẠI (Đã format tiền tệ giống hệt logic cũ của bạn)
            startingPrice.setText(String.format("%,.0f đ", auction.getCurrent_price()));
        }

        if (prd_description != null) {
            prd_description.setText("Đang tải mô tả chi tiết từ hệ thống...");
        }

        // 4. KIỂM TRA TRẠNG THÁI ĐÌNH CHỈ KHÓA KHUNG NHẬP
        String aucStatus = auction.getStatus() != null ? auction.getStatus().toUpperCase() : "";
        if ("SUSPENDED".equals(aucStatus)) {
            if (reasonArea != null) {
                reasonArea.setEditable(false);
                reasonArea.setText("Đang tải lý do đình chỉ...");
            }
            fetchSuspendReason(auction.getId());
        }

        // 5. Cập nhật nút bấm lần đầu theo thông tin thô ban đầu
        updateButtonStates();

        // =======================================================================
        // KÍCH HOẠT LAZY LOADING: Tải bất đồng bộ các trường còn thiếu và hình ảnh
        // =======================================================================
        fetchDetailedTextData(auction.getId());
        fetchProductImageLazy(auction.getId());
    }

    /**
     * LUỒNG NGẦM 1: Tải thông tin văn bản còn khuyết (Description, Moderation Status)
     */
    private void fetchDetailedTextData(int auctionId) {
        new Thread(() -> {
            Request req = new Request(auctionId, ActionType.GET_ITEM_DETAIL);
            Response res = ClientSocket.sendRequest(req);

            Platform.runLater(() -> {
                if (res != null && "SUCCESS".equals(res.getStatus()) && res.getData() instanceof Auction detailedAuction) {

                    // CẬP NHẬT THÔNG TIN: Đắp các trường chữ thiếu vào bộ nhớ, giữ nguyên giá hiện tại lấy từ list
                    if (this.currentAuction != null && detailedAuction.getItem() != null) {
                        Item fullItem = detailedAuction.getItem();
                        Item currentItem = this.currentAuction.getItem();

                        if (currentItem != null) {
                            currentItem.setCategories(fullItem.getCategories());
                            currentItem.setDescription(fullItem.getDescription());
                            currentItem.setStarting_price(fullItem.getStarting_price());
                            currentItem.setUser_prdID(fullItem.getUser_prdID());
                            currentItem.setModeration_status(fullItem.getModeration_status());
                        }
                    }

                    // Đổ dữ liệu mô tả chữ bổ sung lên UI (Bỏ qua, không can thiệp vào label startingPrice nữa)
                    Item item = this.currentAuction.getItem();
                    if (item != null) {
                        if (prd_description != null) {
                            prd_description.setText(item.getDescription() != null && !item.getDescription().trim().isEmpty()
                                    ? item.getDescription()
                                    : "Không có mô tả sản phẩm.");
                        }
                    }

                    // Cập nhật lại các nút bấm dựa trên chính xác cột moderation_status vừa đồng bộ
                    updateButtonStates();
                } else {
                    if (prd_description != null) prd_description.setText("Không thể kết nối để tải mô tả chi tiết.");
                }
            });
        }).start();
    }

    /**
     * LUỒNG NGẦM 2: Tải mảng byte hình ảnh (Lazy Loading qua mạng cực nhẹ)
     */
    private void fetchProductImageLazy(int auctionId) {
        new Thread(() -> {
            Request req = new Request(auctionId, ActionType.GET_IMAGE);
            Response res = ClientSocket.sendRequest(req);

            Platform.runLater(() -> {
                if (res != null && "SUCCESS".equals(res.getStatus()) && res.getData() != null) {
                    try {
                        byte[] imageBytes = (byte[]) res.getData();
                        if (imageBytes != null && imageBytes.length > 0 && prdImage != null) {
                            if (this.currentAuction != null && this.currentAuction.getItem() != null) {
                                this.currentAuction.getItem().setImageBytes(imageBytes);
                            }
                            try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
                                Image img = new Image(bis);
                                prdImage.setImage(img);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[LAZY IMAGE] Lỗi dựng hình ảnh lên ImageView tại client!");
                        e.printStackTrace();
                    }
                }
            });
        }).start();
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
                    if (delete != null) {
                        delete.setText("Delete");
                        updateButtonStates();
                    }
                }
            });
        }).start();
    }
}