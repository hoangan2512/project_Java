package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

import java.io.IOException;

public class mainPageController {

    @FXML
    private Circle userAvatar; // Đảm bảo fx:id trong FXML là "userAvatar"
    @FXML
    private Circle searchBtn;
    @FXML
    private TextField searchBar;
    @FXML
    private Button CustomSearch;
    @FXML
    private Button SellerHub;
    @FXML
    private StackPane prd1;
    @FXML
    private StackPane prd2;
    @FXML
    private StackPane prd3;
    @FXML
    private StackPane prd4;
    @FXML
    private StackPane prd5;
    @FXML
    private StackPane prd6;

    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();

    @FXML
    public void initialize() {
        try {
            Image usr_img = new Image(getClass().getResourceAsStream("/image/avatar1.png"));
            userAvatar.setFill(new ImagePattern(usr_img));

            Image search_img = new Image(getClass().getResourceAsStream("/image/search_icon1.png"));
            searchBtn.setFill(new ImagePattern(search_img));
        } catch (Exception e) {
            System.out.println("Không tìm thấy ảnh avatar, kiểm tra lại đường dẫn!");
        }

        //load prd_card
        String fxmlPath = "/view/prd_preview.fxml";
        fillProductCard(prd1, fxmlPath, "iPhone 15 Pro Max", "1200$", "Bidding");
        fillProductCard(prd2, fxmlPath, "Bàn phím cơ Custom", "350$",  "Bidding");
        fillProductCard(prd3, fxmlPath, "Chuột Logitech G Pro", "120$", "Bidding");
        fillProductCard(prd4, fxmlPath, "Màn hình Dell Ultrasharp", "500$", "Bidding");
        fillProductCard(prd5, fxmlPath, "Tai nghe Sony WH-1000XM5", "300$", "Bidding");
        fillProductCard(prd6, fxmlPath, "Card đồ họa RTX 4090", "1600$", "Bidding");
    }

    public void fillProductCard(StackPane container, String fxmlPath, String name, String price, String auction_status) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent node = loader.load();

            prd_previewController controller = loader.getController();

            if (controller != null) {
                controller.setData(name, price, auction_status, null);
            }

            container.getChildren().setAll(node);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi: Không tìm thấy file FXML tại " + fxmlPath);
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
            System.out.println("Lỗi chuyển cảnh");
        }
    }

    public void handleSearchBtnClick(MouseEvent event) {
        String searchText = searchBar.getText();
        if (searchText == null || searchText.trim().isEmpty()) {
            searchBar.requestFocus();
        } else {
            System.out.println("searching");
            try {
                sceneSwitcher.switchToPrdPage(event);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Lỗi chuyển cảnh");
            }
        }
    }

    public void handleSellerHub(MouseEvent event) {
        try {
            sceneSwitcher.switchToSellerSignIn(event);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Lỗi chuyển cảnh");
        }
    }


}