package controller.sellerHub;

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
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class auction_managerController {
    @FXML
    private TextArea prd_description;
    @FXML
    private ImageView prdImage;
    @FXML
    private Button propose_changes, propose_delete;
    @FXML
    private Label currentTime, startingPrice, startDate, startTime, duration, prdName, Status, prdIDbySeller, reasonArea;

    private Runnable onBackAction;
    private Auction currentAuction;
    private Timeline liveClockTimeline;

    // Biến lưu trữ đoạn mô tả gốc để so sánh sự khác biệt
    private String originalDescription = "";

    public void initialize() {
        if (reasonArea != null) {
            reasonArea.setVisible(false);
            reasonArea.setManaged(false);
        }

        // Lắng nghe sự thay đổi của TextArea mô tả sản phẩm để bật/tắt nút propose_changes
        if (prd_description != null) {
            prd_description.textProperty().addListener((observable, oldValue, newValue) -> {
                String currentStatus = currentAuction != null && currentAuction.getStatus() != null
                        ? currentAuction.getStatus().toUpperCase() : "";
                updateButtonStates(currentStatus);
            });
        }
    }

    public void setAuctionData(Auction auction) {
        if (auction == null) return;

        this.currentAuction = auction;
        Item item = auction.getItem();
        String currentStatus = auction.getStatus() != null ? auction.getStatus().toUpperCase() : "WAITING";

        // ========================================================
        // 1. DỌN DẸP UI (XÓA ẢNH CŨ VÀ ĐẶT TRẠNG THÁI LOADING CHO MÔ TẢ)
        // ========================================================
        if (prdImage != null) prdImage.setImage(null);
        if (prd_description != null) {
            originalDescription = ""; // Xóa bộ nhớ gốc tạm thời
            prd_description.setText("Đang tải mô tả chi tiết từ hệ thống...");
        }

        // ========================================================
        // 2. ĐẨY DỮ LIỆU TRẠNG THÁI VÀ BỘ ĐẾM THỜI GIAN
        // ========================================================
        if (Status != null) {
            Status.setStyle("-fx-text-fill: #FFC107;");
            Status.setText(currentStatus);
        }

        if (reasonArea != null) {
            if ("SUSPENDED".equals(currentStatus)) {
                reasonArea.setVisible(true);
                reasonArea.setManaged(true);
                reasonArea.setText("Đang tải lý do đình chỉ...");
                fetchSuspendReason(auction.getId());
            } else {
                reasonArea.setVisible(false);
                reasonArea.setManaged(false);
            }
        }

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

        // ========================================================
        // 3. ĐẨY DỮ LIỆU SẢN PHẨM THÔ CÓ SẴN NGAY LẬP TỨC
        // ========================================================
        if (item != null) {
            if (prdName != null) prdName.setText(item.getName() != null ? item.getName() : "Chưa xác định");
            if (prdIDbySeller != null) prdIDbySeller.setText(item.getUser_prdID() != null ? item.getUser_prdID() : "Không có ID");

            if (startingPrice != null) {
                Locale localeVN = new Locale("vi", "VN");
                NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeVN);
                startingPrice.setText(currencyFormatter.format(item.getStarting_price()));
            }
        }

        updateButtonStates(currentStatus);

        // ========================================================
        // 4. KÍCH HOẠT LAZY LOADING
        // ========================================================
        fetchDetailedTextData(auction.getId(), currentStatus);
        fetchProductImageLazy(auction.getId());
    }

    /**
     * LUỒNG NGẦM 1: Lấy chi tiết Description để fill vào khung Text
     */
    private void fetchDetailedTextData(int auctionId, String currentStatus) {
        new Thread(() -> {
            Request req = new Request(auctionId, ActionType.GET_ITEM_DETAIL);
            Response res = ClientSocket.sendRequest(req);

            Platform.runLater(() -> {
                if (res != null && "SUCCESS".equals(res.getStatus()) && res.getData() instanceof Auction detailedAuction) {
                    if (this.currentAuction != null && detailedAuction.getItem() != null) {
                        Item fullItem = detailedAuction.getItem();
                        Item currentItem = this.currentAuction.getItem();

                        if (currentItem != null) {
                            currentItem.setDescription(fullItem.getDescription());
                            currentItem.setCategories(fullItem.getCategories());
                        }

                        // Cập nhật biến lưu gốc TRƯỚC KHI fill lên UI để tránh kích hoạt cờ thay đổi giả
                        originalDescription = fullItem.getDescription() != null ? fullItem.getDescription() : "";

                        if (prd_description != null) {
                            prd_description.setText(originalDescription.isEmpty() ? "" : originalDescription);
                        }
                    }
                    updateButtonStates(currentStatus);
                } else {
                    if (prd_description != null) prd_description.setText("Không thể kết nối để tải mô tả chi tiết.");
                }
            });
        }).start();
    }

    /**
     * LUỒNG NGẦM 2: Lấy mảng byte hình ảnh (Fullsize)
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
                                prdImage.setImage(new Image(bis));
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[SELLER HUB] Lỗi dựng hình ảnh lên ImageView tại client!");
                        e.printStackTrace();
                    }
                }
            });
        }).start();
    }

    private void fetchSuspendReason(int auctionId) {
        new Thread(() -> {
            Request req = new Request(auctionId, ActionType.SELLER_GET_REASON);
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

    private void updateButtonStates(String currentStatus) {
        if (currentAuction == null) {
            if (propose_changes != null) propose_changes.setDisable(true);
            if (propose_delete != null) propose_delete.setDisable(true);
            return;
        }

        if ("PENDING_APPROVAL".equals(currentStatus)) {
            if (propose_changes != null) propose_changes.setText("Save");
            if (propose_delete != null) propose_delete.setText("Delete Auction");
        } else if ("WAITING".equals(currentStatus)) {
            if (propose_changes != null) propose_changes.setText("Propose Changes");
            if (propose_delete != null) propose_delete.setText("Propose Delete");
        }

        boolean isUpdatableStatus = "WAITING".equals(currentStatus) || "PENDING_APPROVAL".equals(currentStatus);
        String currentDescText = prd_description != null ? prd_description.getText() : "";

        // Không bật nút Save nếu mô tả đang ở trạng thái loading
        boolean isLoading = currentDescText.equals("Đang tải mô tả chi tiết từ hệ thống...");
        boolean isDescriptionChanged = !currentDescText.equals(originalDescription) && !isLoading;

        if (propose_changes != null) {
            propose_changes.setDisable(!(isUpdatableStatus && isDescriptionChanged));
        }

        boolean isDeleteDisabled = "FINISHED".equals(currentStatus) || "RUNNING".equals(currentStatus) || "SUSPENDED".equals(currentStatus);
        if (propose_delete != null) {
            propose_delete.setDisable(isDeleteDisabled);
        }
    }

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

        String currentStatus = currentAuction.getStatus() != null ? currentAuction.getStatus().toUpperCase() : "";

        if ("FINISHED".equals(currentStatus) || "RUNNING".equals(currentStatus) || "SUSPENDED".equals(currentStatus)) {
            return;
        }

        if (propose_delete != null) propose_delete.setDisable(true);
        if (propose_changes != null) propose_changes.setDisable(true);

        new Thread(() -> {
            Request request;

            if ("PENDING_APPROVAL".equals(currentStatus)) {
                int itemId = currentAuction.getItem_id();
                request = new Request(itemId, ActionType.SELLER_DELETE_ITEM);
            } else {
                Object[] payload = new Object[]{currentAuction.getId(), "Người bán tự hủy phiên"};
                request = new Request(payload, ActionType.ADMIN_STOP_AUCTION);
            }

            Response response = ClientSocket.sendRequest(request);

            Platform.runLater(() -> {
                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    if (onBackAction != null) onBackAction.run();
                } else {
                    updateButtonStates(currentStatus);
                }
            });
        }).start();
    }

    @FXML
    private void handleProposeChanges(ActionEvent event) {
        if (currentAuction == null) return;

        String currentStatus = currentAuction.getStatus() != null ? currentAuction.getStatus().toUpperCase() : "";

        if ("PENDING_APPROVAL".equals(currentStatus)) {
            String newDescription = prd_description != null ? prd_description.getText() : "";

            if (propose_changes != null) propose_changes.setDisable(true);
            if (propose_delete != null) propose_delete.setDisable(true);

            new Thread(() -> {
                int itemId = currentAuction.getItem_id();
                Object[] payload = new Object[]{itemId, newDescription};
                Request request = new Request(payload, ActionType.UPDATE_ITEM_DESCRIPTION);

                Response response = ClientSocket.sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        originalDescription = newDescription;
                        System.out.println("[AUCTION MANAGER] Đã cập nhật mô tả thành công.");
                    } else {
                        System.err.println("[AUCTION MANAGER] Cập nhật mô tả thất bại.");
                    }

                    updateButtonStates(currentStatus);
                });
            }).start();

        } else if ("WAITING".equals(currentStatus)) {
            System.out.println("Tính năng Đề xuất thay đổi khi phiên đã lên sàn (WAITING) chưa được xử lý.");
        }
    }
}