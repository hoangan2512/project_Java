package controller.admin;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import message.Request;
import message.Response;
import model.ActionType;
import model.Auction;
import model.Item;
import network.ClientSocket;

import java.io.ByteArrayInputStream;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

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
        // 1. Đưa approve và not_approve vào chung một ToggleGroup để chỉ chọn được tối đa 1 trong 2
        ToggleGroup actionGroup = new ToggleGroup();
        if (approve != null) approve.setToggleGroup(actionGroup);
        if (not_approve != null) not_approve.setToggleGroup(actionGroup);

        // 2. Gán sự kiện click cho duy nhất nút confirm_btn để thực thi hành động gửi mạng
        if (confirm_btn != null) {
            confirm_btn.setOnAction(this::handleConfirmAction);
        }
    }

    /**
     * HÀM XỬ LÝ TRUNG TÂM: Kích hoạt khi nhấn confirm_btn
     */
    @FXML
    private void handleConfirmAction(ActionEvent event) {
        if (currentAuction == null) return;

        // Kiểm tra xem Admin đang chọn hành động nào
        boolean isApproveSelected = (approve != null && approve.isSelected());
        boolean isRejectSelected = (not_approve != null && not_approve.isSelected());

        if (!isApproveSelected && !isRejectSelected) {
            showAlert(Alert.AlertType.WARNING, "Yêu cầu hành động", "Vui lòng chọn Phê duyệt hoặc Từ chối trước khi xác nhận!");
            return;
        }

        // Rẽ nhánh gọi luồng xử lý tương ứng
        if (isApproveSelected) {
            executeApproveItem();
        } else if (isRejectSelected) {
            executeRejectItem();
        }
    }

    /**
     * Logic gửi request Phê duyệt sản phẩm lên sàn
     * Payload yêu cầu từ Server: Integer (auctionId)
     */
    private void executeApproveItem() {
        new Thread(() -> {
            // Server nhận Integer auctionId cho nhánh APPROVE
            Request req = new Request(Integer.valueOf(currentAuction.getId()), ActionType.ADMIN_APPROVE_ITEM);
            Response res = ClientSocket.sendRequest(req);

            Platform.runLater(() -> {
                if (res != null && "SUCCESS".equals(res.getStatus())) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã phê duyệt sản phẩm lên sàn thành công!");
                    resetActionComponents();
                    if (onBackAction != null) onBackAction.run(); // Quay xe về danh sách chính
                } else {
                    String msg = (res != null) ? res.getMessage() : "Mất kết nối tới máy chủ.";
                    showAlert(Alert.AlertType.ERROR, "Thất bại", "Không thể phê duyệt: " + msg);
                }
            });
        }).start();
    }

    /**
     * Logic gửi request Từ chối sản phẩm kèm lý do vi phạm
     * Payload yêu cầu từ Server: Mảng Object[] { auctionId, reasonText }
     */
    private void executeRejectItem() {
        String reasonText = (reasonArea != null) ? reasonArea.getText().trim() : "";

        // Bắt buộc nhập lý do từ chối để đồng bộ vào reasonRepo trên Server
        if (reasonText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Yêu cầu dữ liệu", "Vui lòng nhập lý do từ chối kiểm duyệt sản phẩm này!");
            return;
        }

        new Thread(() -> {
            // Đóng gói mảng Object[] chứa chính xác 2 phần tử: [Number, String] khớp hoàn toàn Server
            Object[] payloadToSend = new Object[] { Integer.valueOf(currentAuction.getId()), reasonText };

            Request req = new Request(payloadToSend, ActionType.ADMIN_REJECT_ITEM);
            Response res = ClientSocket.sendRequest(req);

            Platform.runLater(() -> {
                if (res != null && "SUCCESS".equals(res.getStatus())) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã từ chối kiểm duyệt sản phẩm thành công.");
                    resetActionComponents();
                    if (onBackAction != null) onBackAction.run(); // Quay xe về danh sách chính
                } else {
                    String msg = (res != null) ? res.getMessage() : "Mất kết nối tới máy chủ.";
                    showAlert(Alert.AlertType.ERROR, "Thất bại", "Không thể từ chối sản phẩm: " + msg);
                }
            });
        }).start();
    }

    /**
     * Hàm dọn dẹp trạng thái các linh kiện giao diện sau khi hoàn tất tác vụ
     */
    private void resetActionComponents() {
        if (approve != null) approve.setSelected(false);
        if (not_approve != null) not_approve.setSelected(false);
        if (reasonArea != null) reasonArea.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void setItemData(Auction auction) {
        if (auction == null) return;

        this.currentAuction = auction;
        Item item = auction.getItem();

        // Làm sạch form xử lý cũ
        resetActionComponents();

        // 3. XỬ LÝ BÓC TÁCH THỜI GIAN (START_TIME & DURATION) TỪ AUCTION
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

        // 4. BÓC TÁCH DỮ LIỆU TỪ ITEM
        if (item != null) {
            if (prdName != null) prdName.setText(item.getName());

            if (startingPrice != null) {
                startingPrice.setText(String.format("%,.0f đ", item.getStarting_price()));
            }

            if (prd_description != null) {
                prd_description.setText(item.getDescription() != null ? item.getDescription() : "Không có mô tả sản phẩm.");
            }

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
        }
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