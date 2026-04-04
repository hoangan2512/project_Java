package controller;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

import java.io.IOException;

public class mainPageController {

    @FXML
    private Circle userAvatar; // Đảm bảo fx:id trong FXML là "userAvatar"

    private SceneSwitchController sceneSwitcher = new SceneSwitchController();

    @FXML
    public void initialize() {
        try {
            Image img = new Image(getClass().getResourceAsStream("/image/avatar1.png"));
            userAvatar.setFill(new ImagePattern(img));
        } catch (Exception e) {
            System.out.println("Không tìm thấy ảnh avatar, kiểm tra lại đường dẫn!");
        }
    }

    // Hàm xử lý khi bấm vào nút tròn hình người (Avatar)
    @FXML
    public void handleAvatarClick(MouseEvent event) {
        try {
            // 2. Gọi hàm chuyển cảnh từ đối tượng đã tạo
            // Lưu ý: Đảm bảo bên SceneSwitchController bạn đã đổi tham số thành (Event e)
            sceneSwitcher.openSignInPopup();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Lỗi khi chuyển cảnh!");
        }
    }
}