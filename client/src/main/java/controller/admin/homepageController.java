package controller.admin;

import controller.SceneSwitchController;
import controller.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import message.Request;
import model.ActionType;
import network.ClientSocket;

import java.io.IOException;

public class homepageController {
    @FXML
    private ToggleButton Seller, Item, Auction;
    @FXML
    private StackPane contentArea;
    @FXML
    private HBox seller_head, item_head, auction_head;
    @FXML
    private Node auction_manager_pane, item_manager_pane, seller_manager_pane;
    
    @FXML
    private AuctionManagerController auction_manager_paneController;
    @FXML
    private ItemManagerController item_manager_paneController;
    @FXML
    private SellerManagerController seller_manager_paneController;

    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();

    public void initialize() {
        ToggleGroup group = new ToggleGroup();
        auction_manager_pane.setVisible(false);
        auction_manager_pane.setManaged(false);
        item_manager_pane.setVisible(false);
        item_manager_pane.setManaged(false);
        seller_manager_pane.setVisible(false);
        seller_manager_pane.setManaged(false);
        Seller.setToggleGroup(group);
        Item.setToggleGroup(group);
        Auction.setToggleGroup(group);

        group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                // Không cho phép bỏ chọn hoàn toàn (bắt buộc phải có 1 nút được chọn)
                oldVal.setSelected(true);
            } else {
                boolean isSeller = (newVal == Seller);
                boolean isItem = (newVal == Item);
                boolean isAuction = (newVal == Auction);

                // Cập nhật trạng thái hiển thị cho Header
                seller_head.setVisible(isSeller);
                seller_head.setManaged(isSeller);
                
                item_head.setVisible(isItem);
                item_head.setManaged(isItem);
                
                auction_head.setVisible(isAuction);
                auction_head.setManaged(isAuction);
            }
        });

        // Mặc định chọn Seller khi khởi tạo (sẽ trigger listener ở trên)
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
