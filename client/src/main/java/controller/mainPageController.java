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
import model.SearchCriteria;
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
    
    // Lưu lại controller đang được hiển thị để gọi hàm refresh khi có broadcast
    private customSearchController currentCustomSearchController;
    private prdPageController currentPrdPageController;

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

        // --------------------------------------------------------
        // LOAD GIAO DIỆN TÌM KIẾM MẶC ĐỊNH VÀO prdPagePane
        // VÀ ẨN NÚT FILTER
        // --------------------------------------------------------
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/customSearch.fxml"));
            Parent customSearchNode = loader.load();

            currentCustomSearchController = loader.getController();
            if (currentCustomSearchController != null) {
                currentCustomSearchController.hideFilterButton(); // Ẩn nút filter theo yêu cầu
                // Load dữ liệu mặc định (tất cả sản phẩm) từ Database
                currentCustomSearchController.setSearchCriteria(null); 
            }

            // Đánh dấu là đang mở bảng tìm kiếm
            currentPrdPageController = null; 

            prdPagePane.getChildren().setAll(customSearchNode);
            prdPagePane.setVisible(true);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi: Không load được file customSearch.fxml làm mặc định");
        }

        // --------------------------------------------------------
        // Đã bỏ việc load các sản phẩm fix cứng vào prd1, prd2... 
        // Vì giờ đây danh sách sẽ được load tự động từ Database vào prdPagePane.
        // --------------------------------------------------------
    }

    public void fillProductCard(StackPane container, String fxmlPath, String name, long price, long time, String imgPath, String description, String status) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent node = loader.load();

            prd_previewController controller = loader.getController();

            if (controller != null) {
                controller.setData(name, price, time, imgPath, status);

                controller.setOnBidAction(() -> {
                    fillProductPage(name, price, time, imgPath, description, status);
                });
            }

            container.getChildren().setAll(node);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi: Không tìm thấy file FXML tại " + fxmlPath);
        }
    }

    // Hàm mới: Load giao diện kết quả tìm kiếm vào prdPagePane
    // Thêm tham số SearchCriteria vào hàm
    public void loadCustomSearchPane(SearchCriteria criteria) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/customSearch.fxml"));
            Parent customSearchNode = loader.load();

            // Lấy controller của trang customSearch
            currentCustomSearchController = loader.getController();
            currentPrdPageController = null; // Đánh dấu không ở trang chi tiết

            // TRUYỀN DỮ LIỆU SANG TRANG CUSTOM SEARCH
            if (currentCustomSearchController != null && criteria != null) {
                currentCustomSearchController.setSearchCriteria(criteria);
            }

            // Nhét giao diện vào StackPane và hiển thị
            prdPagePane.getChildren().setAll(customSearchNode);
            prdPagePane.setVisible(true);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi: Không load được file customSearch.fxml");
        }
    }

    public void fillProductPage(String name, long price, long time, String imgPath, String description, String status) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/productPage.fxml"));
            Parent prdPageNode = loader.load();

            // Lấy controller của trang chi tiết
            currentPrdPageController = loader.getController();
            currentCustomSearchController = null; // Đánh dấu không ở trang tìm kiếm

            if (currentPrdPageController != null) {
                // Đẩy dữ liệu sang trang chi tiết (kèm theo ảnh, mô tả và trạng thái)
                currentPrdPageController.setData(name, price, time, imgPath, description, status);
            }

            // Hiển thị trang chi tiết lên (đè lên hoặc thay thế nội dung)
            prdPagePane.getChildren().setAll(prdPageNode);
            prdPagePane.setVisible(true);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Hàm gọi khi nhận được tín hiệu Broadcast từ mạng (tự động load lại dữ liệu)
    public void refreshData() {
        if (currentCustomSearchController != null) {
            currentCustomSearchController.refresh();
        } else if (currentPrdPageController != null) {
            // Tương lai: có thể thiết kế để lấy lại thông tin 1 sản phẩm cụ thể
            System.out.println("Đang làm mới trang chi tiết sản phẩm...");
            // Về cơ bản cần ID sản phẩm để kéo lại từ Server, hiện tại ta có thể quay lại trang search
            handleBidHub(null);
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
            System.out.println("searching: " + searchText);
            SearchCriteria criteria = new SearchCriteria();
            criteria.setKeyword(searchText.trim());
            loadCustomSearchPane(criteria);
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
        // Thay vì ẩn prdPagePane như cũ, ta load lại danh sách mặc định
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/customSearch.fxml"));
            Parent customSearchNode = loader.load();

            currentCustomSearchController = loader.getController();
            currentPrdPageController = null;

            if (currentCustomSearchController != null) {
                currentCustomSearchController.hideFilterButton();
                currentCustomSearchController.setSearchCriteria(null);
            }

            prdPagePane.getChildren().setAll(customSearchNode);
            prdPagePane.setVisible(true);

        } catch (IOException e) {
            e.printStackTrace();
        }
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