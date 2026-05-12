package controller.admin;

import controller.SceneSwitchController;
import controller.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import message.Request;
import model.ActionType;
import network.ClientSocket;

import java.io.IOException;

public class homepageController {
    @FXML
    private ToggleButton Seller, Item, Auction, BidHubM;
    @FXML
    private StackPane contentArea;

    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();

    public void initialize() {
        Seller.setSelected(true);
    }

    public void handleBidHub(MouseEvent event) {
        // --- THỰC HIỆN ĐĂNG XUẤT ---
        // 1. Gửi request LOGOUT lên Server
        Request logoutReq = new Request(null, ActionType.LOGOUT);
        ClientSocket.sendRequest(logoutReq);

        // 2. Xóa session ở phía Client
        SessionManager.getInstance().logout();

        // 3. Chuyển cảnh về trang chủ (BidHub)
        try {
            sceneSwitcher.switchToMainPage(event);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
