package controller.admin;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import model.Auction;
import model.Item;

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


    public void setItemData(Auction auction) {
        if (auction == null) return;

        // 1. Lưu lại thực thể vào biến toàn cục của Class
        this.currentAuction = auction;

        // 2. Trích xuất đối tượng Item nằm bên trong Auction
        Item item = auction.getItem();

        // 3. XỬ LÝ BÓC TÁCH THỜI GIAN (START_TIME & DURATION) TỪ AUCTION
        if (auction.getStart_time() != null) {
            LocalDateTime startDateTime = auction.getStart_time();

            // Tạo bộ định dạng ngày và giờ
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

            // Đổ dữ liệu tách biệt vào startDate và startTime
            if (startDate != null) {
                startDate.setText(startDateTime.format(dateFormatter));
            }
            if (startTime != null) {
                startTime.setText(startDateTime.format(timeFormatter));
            }
        }

        // Tính toán Duration diễn ra phiên (End_time - Start_time)
        if (auction.getStart_time() != null && auction.getEnd_time() != null) {
            Duration diff = Duration.between(auction.getStart_time(), auction.getEnd_time());

            long totalHours = diff.toHours();
            long minutes = diff.toMinutes() % 60;

            // %02d: Đảm bảo luôn hiển thị đủ 2 chữ số, tự động thêm số 0 ở trước nếu < 10
            String durationStr = String.format("%02d:%02d", totalHours, minutes);

            if (duration != null) {
                duration.setText(durationStr);
            }
        }

        // 4. BÓC TÁCH DỮ LIỆU TỪ ITEM
        if (item != null) {
            if (prdName != null) prdName.setText(item.getName());

            if (startingPrice != null) {
                startingPrice.setText(String.format("%,.0f đ", item.getStarting_price()));
            }

            // Đổ dữ liệu vào phần mô tả sản phẩm
            if (prd_description != null) {
                prd_description.setText(item.getDescription() != null ? item.getDescription() : "Không có mô tả sản phẩm.");
            }

            // 5. Xử lý bóc tách mảng byte ảnh (imageBytes) hiển thị lên ImageView
            if (prdImage != null && item.getImageBytes() != null) {
                try (ByteArrayInputStream bis = new ByteArrayInputStream(item.getImageBytes())) {
                    Image img = new Image(bis);
                    prdImage.setImage(img);
                } catch (Exception e) {
                    System.err.println("Lỗi bóc tách hiển thị hình ảnh sản phẩm!");
                    e.printStackTrace();
                }
            }
        }
    }

    public void setOnBack(Runnable onBackAction) {
        this.onBackAction = onBackAction;
    }

    @FXML
    private void handleBackBtn(ActionEvent event) {
        if (onBackAction != null) {
            onBackAction.run(); // Kích hoạt tín hiệu báo về cho Cha
        }
    }
}
