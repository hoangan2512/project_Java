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
        // 1. ĐẨY DỮ LIỆU TRẠNG THÁI VÀ BỘ ĐẾM THỜI GIAN
        // ========================================================
        if (Status != null) {
            Status.setStyle("-fx-text-fill: #FFC107;");
            Status.setText(currentStatus);
        }

        if (reasonArea != null) {
            if ("SUSPENDED".equals(currentStatus)) {
                reasonArea.setVisible(true);
                reasonArea.setManaged(true);
                reasonArea.setText("Phiên đấu giá này đã bị đình chỉ bởi Quản trị viên.");
                reasonArea.setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
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

            if (duration != null) {
                duration.setText(String.format("%02d:%02d", totalHours, minutes));
            }
        }

        startLiveClock(auction.getStart_time(), auction.getEnd_time());

        // ========================================================
        // 2. BÓC TÁCH VÀ ĐẨY DỮ LIỆU SẢN PHẨM
        // ========================================================
        if (item != null) {
            if (prdName != null) prdName.setText(item.getName() != null ? item.getName() : "Chưa xác định");

            if (prdIDbySeller != null) {
                prdIDbySeller.setText(item.getUser_prdID() != null ? item.getUser_prdID() : "Không có ID");
            }

            if (startingPrice != null) {
                Locale localeVN = new Locale("vi", "VN");
                NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeVN);
                startingPrice.setText(currencyFormatter.format(item.getStarting_price()));
            }

            if (prd_description != null) {
                // Lưu lại đoạn text gốc vào biến để đối chiếu về sau
                originalDescription = item.getDescription() != null ? item.getDescription() : "";
                prd_description.setText(originalDescription);
            }

            if (prdImage != null) {
                if (item.getImageBytes() != null && item.getImageBytes().length > 0) {
                    try (ByteArrayInputStream bis = new ByteArrayInputStream(item.getImageBytes())) {
                        Image img = new Image(bis);
                        prdImage.setImage(img);
                    } catch (Exception e) {
                        System.err.println("[AUCTION MANAGER] Lỗi khi dựng luồng hiển thị mảng byte ảnh sản phẩm!");
                        e.printStackTrace();
                        prdImage.setImage(null);
                    }
                } else {
                    prdImage.setImage(null);
                }
            }
        }

        // ========================================================
        // 3. KIỂM TRA VÀ CẬP NHẬT TRẠNG THÁI CÁC NÚT ĐIỀU KHIỂN
        // ========================================================
        updateButtonStates(currentStatus);
    }

    /**
     * Quản lý việc ĐẶT TÊN và BẬT/TẮT (Enable/Disable) các nút bấm
     */
    private void updateButtonStates(String currentStatus) {
        if (currentAuction == null) {
            if (propose_changes != null) propose_changes.setDisable(true);
            if (propose_delete != null) propose_delete.setDisable(true);
            return;
        }

        // ==========================================
        // QUY TẮC ĐẶT TÊN NÚT (Tuyệt đối tuân thủ)
        // ==========================================
        if ("PENDING_APPROVAL".equals(currentStatus)) {
            if (propose_changes != null) propose_changes.setText("Save");
            if (propose_delete != null) propose_delete.setText("Delete Auction");
        } else if ("WAITING".equals(currentStatus)) {
            if (propose_changes != null) propose_changes.setText("Propose Changes");
            if (propose_delete != null) propose_delete.setText("Propose Delete");
        }

        // ==========================================
        // QUY TẮC BẬT/TẮT NÚT PROPOSE CHANGES
        // ==========================================
        boolean isUpdatableStatus = "WAITING".equals(currentStatus) || "PENDING_APPROVAL".equals(currentStatus);

        // Kiểm tra mô tả có bị thay đổi không
        String currentDescText = prd_description != null ? prd_description.getText() : "";
        boolean isDescriptionChanged = !currentDescText.equals(originalDescription);

        // Chỉ bật khi trạng thái cho phép VÀ người dùng đã sửa đổi chữ trong Description
        if (propose_changes != null) {
            propose_changes.setDisable(!(isUpdatableStatus && isDescriptionChanged));
        }

        // ==========================================
        // QUY TẮC BẬT/TẮT NÚT PROPOSE DELETE
        // ==========================================
        // Vô hiệu hóa (disable = true) nếu trạng thái là 1 trong 3 trạng thái dưới đây
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

                if (currentTime != null) {
                    currentTime.setText(String.format("- %02d:%02d:%02d", totalHours, minutes, seconds));
                }

            } else if (now.isBefore(endTime)) {
                java.time.Duration durationLeft = java.time.Duration.between(now, endTime);
                long totalHours = durationLeft.toHours();
                long minutes = durationLeft.toMinutes() % 60;
                long seconds = durationLeft.toSeconds() % 60;

                if (currentTime != null) {
                    currentTime.setText(String.format("%02d:%02d:%02d", totalHours, minutes, seconds));
                }

            } else {
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

        String currentStatus = currentAuction.getStatus() != null ? currentAuction.getStatus().toUpperCase() : "";

        // Chặn luồng nếu ở trạng thái không cho phép tương tác xóa/hủy
        if ("FINISHED".equals(currentStatus) || "RUNNING".equals(currentStatus) || "SUSPENDED".equals(currentStatus)) {
            return;
        }

        // Vô hiệu hóa nút trong lúc chờ Server phản hồi để tránh Spam click
        if (propose_delete != null) propose_delete.setDisable(true);
        if (propose_changes != null) propose_changes.setDisable(true);

        new Thread(() -> {
            Request request;

            // KIỂM TRA TRẠNG THÁI VÀ GÁN LỆNH TƯƠNG ỨNG
            if ("PENDING_APPROVAL".equals(currentStatus)) {
                // Nếu đang chờ duyệt -> Gửi lệnh xóa hoàn toàn khỏi cơ sở dữ liệu
                int itemId = currentAuction.getItem_id(); // Hoặc currentAuction.getItem().getId()
                request = new Request(itemId, ActionType.SELLER_DELETE_ITEM);
            } else {
                // Nếu là WAITING -> Gửi lệnh tự hủy (đình chỉ)
                Object[] payload = new Object[]{currentAuction.getId(), "Người bán tự hủy phiên"};
                request = new Request(payload, ActionType.ADMIN_STOP_AUCTION);
            }

            Response response = ClientSocket.sendRequest(request);

            Platform.runLater(() -> {
                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    if (onBackAction != null) onBackAction.run();
                } else {
                    // Thất bại -> Bật lại các nút điều khiển dựa trên trạng thái cũ, KHÔNG đổi Text của nút
                    updateButtonStates(currentStatus);
                }
            });
        }).start();
    }

    @FXML
    private void handleProposeChanges(ActionEvent event) {
        if (currentAuction == null) return;

        String currentStatus = currentAuction.getStatus() != null ? currentAuction.getStatus().toUpperCase() : "";

        // CHỈ GỌI SERVER KHI TRẠNG THÁI LÀ PENDING_APPROVAL
        if ("PENDING_APPROVAL".equals(currentStatus)) {
            String newDescription = prd_description != null ? prd_description.getText() : "";

            // Vô hiệu hóa nút trong lúc chờ Server để tránh spam click
            if (propose_changes != null) propose_changes.setDisable(true);
            if (propose_delete != null) propose_delete.setDisable(true);

            new Thread(() -> {
                // Tạo Payload mảng [itemId, newDescription] theo đúng cấu trúc của ItemController
                int itemId = currentAuction.getItem_id();
                Object[] payload = new Object[]{itemId, newDescription};
                Request request = new Request(payload, ActionType.UPDATE_ITEM_DESCRIPTION);

                Response response = ClientSocket.sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        // Cập nhật thành công: Đặt lại mốc mô tả gốc
                        originalDescription = newDescription;
                        System.out.println("[AUCTION MANAGER] Đã cập nhật mô tả thành công.");
                    } else {
                        System.err.println("[AUCTION MANAGER] Cập nhật mô tả thất bại.");
                    }

                    // Dù thành công hay thất bại cũng gọi lại hàm này để refresh trạng thái nút
                    // Nếu thành công -> Nút Save sẽ bị mờ đi (do text hiện tại == text gốc)
                    // Nếu thất bại -> Nút Save sẽ sáng lại để user có thể bấm thử lại
                    updateButtonStates(currentStatus);
                });
            }).start();

        } else if ("WAITING".equals(currentStatus)) {
            // TODO: Xử lý logic cho việc "Đề xuất thay đổi" khi phiên đã được duyệt (nếu cần sau này)
            System.out.println("Tính năng Đề xuất thay đổi khi phiên đã lên sàn (WAITING) chưa được xử lý.");
        }
    }
}