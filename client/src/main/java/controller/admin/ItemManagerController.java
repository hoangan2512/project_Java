package controller.admin;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import message.Request;
import message.Response;
import model.ActionType;
import model.Auction;
import model.Item;
import network.ClientSocket;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ItemManagerController {
    @FXML
    private Label startingPrice, startDate, startTime, duration, prd_description, prdName;
    @FXML
    private ToggleButton approve, not_approve;
    @FXML
    private Button confirm_btn, backBtn;
    @FXML
    private TextArea reasonArea;
    @FXML
    private ImageView prdImage;

    private Auction currentAuction;
    private Runnable onBackAction;

    public void initialize() {
        ToggleGroup actionGroup = new ToggleGroup();
        if (approve != null) approve.setToggleGroup(actionGroup);
        if (not_approve != null) not_approve.setToggleGroup(actionGroup);

        if (confirm_btn != null) {
            confirm_btn.setOnAction(this::handleConfirmAction);
        }

        // ========================================================
        // GẮN LISTENER ĐỂ CẬP NHẬT TRẠNG THÁI NÚT CONFIRM (BỎ LOGIC SO SÁNH)
        // ========================================================
        if (reasonArea != null) {
            reasonArea.textProperty().addListener((observable, oldValue, newValue) -> updateConfirmButtonState());
        }
        if (approve != null) {
            approve.selectedProperty().addListener((observable, oldValue, newValue) -> updateConfirmButtonState());
        }
        if (not_approve != null) {
            not_approve.selectedProperty().addListener((observable, oldValue, newValue) -> updateConfirmButtonState());
        }
    }

    /**
     * Thuật toán bật/tắt nút Confirm dựa trên hành động
     */
    private void updateConfirmButtonState() {
        if (currentAuction == null || confirm_btn == null) return;

        Item item = currentAuction.getItem();

        // Nếu sản phẩm ĐÃ BỊ TỪ CHỐI TỪ TRƯỚC -> Khóa nút Confirm vĩnh viễn (không cho sửa)
        if (item != null && "REJECTED".equalsIgnoreCase(item.getModeration_status())) {
            confirm_btn.setDisable(true);
            return;
        }

        boolean isApprove = approve != null && approve.isSelected();
        boolean isReject = not_approve != null && not_approve.isSelected();
        String currentReason = reasonArea != null ? reasonArea.getText().trim() : "";

        if (isApprove) {
            // Nút Approve được chọn -> Luôn cho phép bấm Confirm
            confirm_btn.setDisable(false);
        } else if (isReject) {
            // Nút Reject được chọn -> Chỉ cho phép Confirm nếu đã nhập lý do
            confirm_btn.setDisable(currentReason.isEmpty());
        } else {
            // Chưa chọn gì -> Khóa nút Confirm
            confirm_btn.setDisable(true);
        }
    }

    /**
     * Bật/Tắt toàn bộ UI trong lúc chờ Server phản hồi (tránh spam)
     */
    private void setUIDisabled(boolean disabled) {
        if (approve != null) approve.setDisable(disabled);
        if (not_approve != null) not_approve.setDisable(disabled);
        if (reasonArea != null) reasonArea.setDisable(disabled);
        if (confirm_btn != null) confirm_btn.setDisable(disabled);
    }

    @FXML
    private void handleConfirmAction(ActionEvent event) {
        if (currentAuction == null) return;

        boolean isApproveSelected = (approve != null && approve.isSelected());
        boolean isRejectSelected = (not_approve != null && not_approve.isSelected());

        if (isApproveSelected) {
            executeApproveItem();
        } else if (isRejectSelected) {
            executeRejectItem();
        }
    }

    private void executeApproveItem() {
        setUIDisabled(true);
        if (confirm_btn != null) confirm_btn.setText("Processing...");

        new Thread(() -> {
            Request req = new Request(Integer.valueOf(currentAuction.getId()), ActionType.ADMIN_APPROVE_ITEM);
            Response res = ClientSocket.sendRequest(req);

            Platform.runLater(() -> {
                if (res != null && "SUCCESS".equals(res.getStatus())) {
                    if (onBackAction != null) onBackAction.run();
                } else {
                    if (confirm_btn != null) confirm_btn.setText("Confirm");
                    setUIDisabled(false);
                }
            });
        }).start();
    }

    private void executeRejectItem() {
        String reasonText = (reasonArea != null) ? reasonArea.getText().trim() : "";
        if (reasonText.isEmpty()) return;

        setUIDisabled(true);
        if (confirm_btn != null) confirm_btn.setText("Processing...");

        new Thread(() -> {
            Object[] payloadToSend = new Object[] { Integer.valueOf(currentAuction.getId()), reasonText };
            Request req = new Request(payloadToSend, ActionType.ADMIN_REJECT_ITEM);
            Response res = ClientSocket.sendRequest(req);

            Platform.runLater(() -> {
                if (res != null && "SUCCESS".equals(res.getStatus())) {
                    if (onBackAction != null) onBackAction.run();
                } else {
                    // Thất bại: Mở lại UI để thử lại
                    if (confirm_btn != null) confirm_btn.setText("Confirm");
                    setUIDisabled(false);
                }
            });
        }).start();
    }

    /**
     * Lấy lý do từ chối từ Server và fill vào reasonArea
     */
    private void fetchRejectReason(int auctionId) {
        new Thread(() -> {
            Request req = new Request(auctionId, ActionType.ADMIN_GET_ITEM_REASON);
            Response res = ClientSocket.sendRequest(req);

            Platform.runLater(() -> {
                if (reasonArea != null) {
                    if (res != null && "SUCCESS".equals(res.getStatus()) && res.getData() != null) {
                        reasonArea.setText((String) res.getData());
                    } else {
                        reasonArea.setText("Reject do hết thời gian duyệt");
                    }
                }
            });
        }).start();
    }

    /**
     * Hàm dọn dẹp trạng thái các linh kiện giao diện sau khi hoàn tất hoặc mở mới
     */
    private void resetActionComponents() {
        if (approve != null) {
            approve.setSelected(false);
            approve.setDisable(false);
        }
        if (not_approve != null) {
            not_approve.setSelected(false);
            not_approve.setDisable(false);
        }
        if (reasonArea != null) {
            reasonArea.clear();
            reasonArea.setEditable(true);
            reasonArea.setDisable(false);
        }
        if (confirm_btn != null) {
            confirm_btn.setText("Confirm");
            confirm_btn.setDisable(true); // Khóa nút Confirm theo mặc định
        }
    }

    public void setItemData(Auction auction) {
        if (auction == null) return;

        this.currentAuction = auction;
        Item item = auction.getItem();

        resetActionComponents();

        if (auction.getStart_time() != null) {
            LocalDateTime startDateTime = auction.getStart_time();

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

            if (startDate != null) startDate.setText(startDateTime.format(dateFormatter));
            if (startTime != null) startTime.setText(startDateTime.format(timeFormatter));
        }

        if (auction.getStart_time() != null && auction.getEnd_time() != null) {
            Duration diff = Duration.between(auction.getStart_time(), auction.getEnd_time());
            long totalHours = diff.toHours();
            long minutes = diff.toMinutes() % 60;
            String durationStr = String.format("%02d:%02d", totalHours, minutes);

            if (duration != null) duration.setText(durationStr);
        }

        if (item != null) {
            if (prdName != null) prdName.setText(item.getName());
            if (startingPrice != null) startingPrice.setText(String.format("%,.0f đ", item.getStarting_price()));
            if (prd_description != null) prd_description.setText(item.getDescription() != null ? item.getDescription() : "Không có mô tả sản phẩm.");

            if (prdImage != null && item.getImageBytes() != null) {
                try (ByteArrayInputStream bis = new ByteArrayInputStream(item.getImageBytes())) {
                    Image img = new Image(bis);
                    prdImage.setImage(img);
                } catch (Exception e) {
                    System.err.println("Lỗi bóc tách hiển thị hình ảnh sản phẩm!");
                    e.printStackTrace();
                }
            } else if (prdImage != null) {
                prdImage.setImage(null);
            }

            // ========================================================
            // NẾU SẢN PHẨM ĐÃ BỊ TỪ CHỐI TỪ TRƯỚC -> KHÓA UI VÀ ĐỌC LÝ DO
            // ========================================================
            if ("REJECTED".equalsIgnoreCase(item.getModeration_status())) {
                if (approve != null) approve.setDisable(true);

                if (not_approve != null) {
                    not_approve.setSelected(true);
                    not_approve.setDisable(true); // Khóa nút Reject không cho bấm bỏ chọn
                }

                if (reasonArea != null) {
                    reasonArea.setText("Đang tải lý do từ chối...");
                    reasonArea.setEditable(false); // Chỉ đọc
                }

                if (confirm_btn != null) {
                    confirm_btn.setDisable(true); // Khóa hoàn toàn nút Confirm
                }

                // Gọi Server lấy lý do cũ
                fetchRejectReason(auction.getId());
            }
        }

        updateConfirmButtonState();
    }

    public void setOnBack(Runnable onBackAction) {
        this.onBackAction = onBackAction;
    }

    @FXML
    private void handleBackBtn(ActionEvent event) {
        if (onBackAction != null) {
            onBackAction.run();
        }
    }
}