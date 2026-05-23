package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;

public class policy_popupController {

    @FXML
    private Button backBtn;
    @FXML
    private AnchorPane privatePolicy, paymentPolicy, contact, work;

    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();

    @FXML
    public void initialize() {
    }

    @FXML
    private void handleBackBtn(MouseEvent event) {
        try {
            // Gọi hàm chuyển về màn hình Welcome từ sceneSwitcher của bạn
            sceneSwitcher.switchToWelcome(event);
        } catch (IOException e) {
            System.err.println("Lỗi chuyển màn hình Welcome: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void showSection(String sectionName) {
        // Bước 1: Ẩn TOÀN BỘ 4 khối nội dung trước để reset giao diện
        work.setVisible(false);
        privatePolicy.setVisible(false);
        paymentPolicy.setVisible(false);
        contact.setVisible(false);

        // Bước 2: Chỉ bật visible cho duy nhất khối được yêu cầu
        switch (sectionName.toLowerCase()) {
            case "work":
                work.setVisible(true);
                break;
            case "policy":
            case "privatepolicy":
                privatePolicy.setVisible(true);
                break;
            case "payment":
            case "paymentpolicy":
                paymentPolicy.setVisible(true);
                break;
            case "contact":
                contact.setVisible(true);
                break;
            default:
                contact.setVisible(true);
                break;
        }
    }

    @FXML
    private void handleEmailClick(MouseEvent event) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI("mailto:ontaphochanhnguyenquanganh@gmail.com"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}