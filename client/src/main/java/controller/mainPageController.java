package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import model.User;
import controller.prdPageController;

import java.io.IOException;

public class mainPageController {

    @FXML
    private Circle userAvatar;
    @FXML
    private Circle searchBtn;
    @FXML
    private TextField searchBar;
    @FXML
    private Button CustomSearch;
    @FXML
    private Button SellerHub;
    @FXML
    private StackPane prd1, prd2, prd3, prd4, prd5, prd6;
    @FXML
    private StackPane prdPagePane;
    @FXML
    private ImageView bidHub;


    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();
    private static mainPageController instance;
    public static mainPageController getInstance() {
        return instance;
    }

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
        instance = this;
        updateAvatarUI();
        prdPagePane.setVisible(false);

        //load prd_card
        String fxmlPath = "/view/prd_preview.fxml";
        fillProductCard(prd1, fxmlPath, "iPhone 15 Pro Max", 1000000, 3666);
        fillProductCard(prd2, fxmlPath, "Bàn phím cơ Custom", 1000000,  3665);
        fillProductCard(prd3, fxmlPath, "Chuột Logitech G Pro", 1000000, 3665);
        fillProductCard(prd4, fxmlPath, "Màn hình Dell Ultrasharp", 1000000, 3665);
        fillProductCard(prd5, fxmlPath, "Tai nghe Sony WH-1000XM5", 1000000, 3665);
        fillProductCard(prd6, fxmlPath, "Card đồ họa RTX 4090", 1000000, 3665);
    }

    public void fillProductCard(StackPane container, String fxmlPath, String name, long price, long time) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent node = loader.load();

            prd_previewController controller = loader.getController();

            if (controller != null) {
                controller.setData(name, price, time, null);

                controller.setOnBidAction(() -> {
                    fillProductPage(name, price, time);
                });
            }

            container.getChildren().setAll(node);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi: Không tìm thấy file FXML tại " + fxmlPath);
        }
    }

    public void fillProductPage(String name, long price, long time) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/productPage.fxml"));
            Parent prdPageNode = loader.load();

            // Lấy controller của trang chi tiết
            prdPageController controller = loader.getController();

            if (controller != null) {
                // Đẩy dữ liệu sang trang chi tiết
                controller.setData(name, price, time, null);
            }

            // Hiển thị trang chi tiết lên (đè lên hoặc thay thế nội dung)
            // Giả sử bạn muốn dùng chính cái prd1 để hiển thị hoặc một vùng lớn hơn
            prdPagePane.getChildren().setAll(prdPageNode);
            prdPagePane.setVisible(true);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Hàm xử lý khi bấm vào nút tròn hình người (Avatar)
    @FXML
    public void handleAvatarClick(MouseEvent event) {
        try {
            sceneSwitcher.openSignInPopup(null);
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

    public void updateAvatarUI() {
        // Hỏi "bộ nhớ" xem hiện tại có ai đang đăng nhập không?
        if (SessionManager.getInstance().isBidder()) {
            // Đã đăng nhập: Viền xanh lá
            // Hoặc nếu bạn dùng CSS: avatarBorder.setStyle("-fx-border-color: green;");
            userAvatar.setStroke(Color.GREEN);
            User user = SessionManager.getInstance().getCurrentUser();
            System.out.println("Logged In: " + user.getUsername());
        } else {
            // Chưa đăng nhập (Bấm tắt popup mà không login): Viền đỏ
            System.out.println("Status: Waiting for login");
            userAvatar.setStroke(Color.RED);
        }
    }

    public void handleBidHub(MouseEvent event) {
        prdPagePane.setVisible(false);
    }

    public void handleCustomSearch(ActionEvent event) {
        try {
            sceneSwitcher.openFilter();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Lỗi chuyển cảnh");
        }
    }
}