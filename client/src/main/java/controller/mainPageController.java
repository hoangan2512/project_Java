package controller;

import javafx.animation.FadeTransition;
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
import javafx.util.Duration;
import model.SearchCriteria;
import model.User;
import model.Auction;
import message.Response;

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
    private StackPane prdPagePane;
    @FXML
    private ImageView bidHub;

    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();
    private static mainPageController instance;
    
    // Lưu lại controller đang được hiển thị để gọi hàm refresh khi có broadcast
    private customSearchController currentCustomSearchController;
    private prdPageController currentPrdPageController;
    
    // Lưu lại bộ lọc gần nhất để khi bấm quay về trang chủ (BidHub logo), nó không bị mất kết quả lọc
    private SearchCriteria lastSearchCriteria = null;

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

            // Cài đặt màu nền cho khung chứa để hiệu ứng fade đẹp hơn
            prdPagePane.setStyle("-fx-background-color:  #1E1E1E;");
            prdPagePane.getChildren().setAll(customSearchNode);
            prdPagePane.setVisible(true);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi: Không load được file customSearch.fxml làm mặc định");
        }
    }

    public void loadCustomSearchPane(SearchCriteria criteria) {
        this.lastSearchCriteria = criteria; // Lưu lại để dùng cho chức năng "Quay lại"
        
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

            // Cài màu nền cam lúc chuyển cảnh
            prdPagePane.setStyle("-fx-background-color:  #1E1E1E;");
            
            // Ép tàng hình ngay lập tức để lộ nền cam
            customSearchNode.setOpacity(0.0);
            
            // Nhét giao diện vào StackPane và hiển thị
            prdPagePane.getChildren().setAll(customSearchNode);
            prdPagePane.setVisible(true);
            
            // Rút ngắn thời gian fade (ví dụ: 150ms)
            FadeTransition fadeIn = new FadeTransition(Duration.millis(150), customSearchNode);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            
            // Ngâm màu nền 50ms
            fadeIn.setDelay(Duration.millis(50));
            fadeIn.play();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi: Không load được file customSearch.fxml");
        }
    }

    public void fillProductPage(Auction auction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/productPage.fxml"));
            Parent prdPageNode = loader.load();

            // Lấy controller của trang chi tiết
            currentPrdPageController = loader.getController();
            currentCustomSearchController = null; // Đánh dấu không ở trang tìm kiếm

            if (currentPrdPageController != null) {
                // Đẩy dữ liệu sang trang chi tiết
                currentPrdPageController.setData(auction);
            }

            // Đặt màu nền màu cam cho khung chứa
            prdPagePane.setStyle("-fx-background-color:  #1E1E1E;");
            
            // Tàng hình giao diện mới để lộ nền cam
            prdPageNode.setOpacity(0.0);
            
            // Hiển thị trang chi tiết lên
            prdPagePane.getChildren().setAll(prdPageNode);
            prdPagePane.setVisible(true);
            
            // Tốc độ mờ dần nhanh hơn (150ms)
            FadeTransition fadeIn = new FadeTransition(Duration.millis(150), prdPageNode);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            
            // Ngâm màu nền 250ms
            fadeIn.setDelay(Duration.millis(50));
            fadeIn.play();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Hàm gọi khi nhận được tín hiệu Broadcast từ mạng (tự động load lại dữ liệu)
    public void refreshData(Response res) {
        if (currentCustomSearchController != null) {
            System.out.println("Đang làm mới danh sách sản phẩm...");
            currentCustomSearchController.refresh();
        } else if (currentPrdPageController != null) {
            System.out.println("Đang làm mới trang chi tiết sản phẩm...");
            // Chuyển thông tin Broadcast sang cho Product Page xử lý (Cập nhật giá hoặc trạng thái)
            currentPrdPageController.handleBroadcast(res);
        }
    }

    // Giữ lại hàm cũ phòng trường hợp có nơi gọi
    public void refreshData() {
        refreshData(null);
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
        // Về trang chủ mặc định: XÓA TOÀN BỘ BỘ LỌC
        this.lastSearchCriteria = null;
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/customSearch.fxml"));
            Parent customSearchNode = loader.load();

            currentCustomSearchController = loader.getController();
            currentPrdPageController = null;

            if (currentCustomSearchController != null) {
                currentCustomSearchController.hideFilterButton();
                currentCustomSearchController.setSearchCriteria(null); 
            }

            // Đặt nền cam
            prdPagePane.setStyle("-fx-background-color:  #1E1E1E;");
            customSearchNode.setOpacity(0.0);
            
            prdPagePane.getChildren().setAll(customSearchNode);
            prdPagePane.setVisible(true);
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(150), customSearchNode);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            // Ngâm màu nền 50ms
            fadeIn.setDelay(Duration.millis(50));
            fadeIn.play();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Hàm gọi khi bấm nút Back (Nút <) từ trang chi tiết sản phẩm
    public void goBackToSearch() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/customSearch.fxml"));
            Parent customSearchNode = loader.load();

            currentCustomSearchController = loader.getController();
            currentPrdPageController = null;

            if (currentCustomSearchController != null) {
                // Khôi phục lại bộ lọc cũ
                currentCustomSearchController.setSearchCriteria(lastSearchCriteria);
            }

            // Đặt nền cam
            prdPagePane.setStyle("-fx-background-color:  #1E1E1E;");
            customSearchNode.setOpacity(0.0);
            
            prdPagePane.getChildren().setAll(customSearchNode);
            prdPagePane.setVisible(true);
            
            // TẠO HIỆU ỨNG FADE-IN KHI QUAY LẠI
            FadeTransition fadeIn = new FadeTransition(Duration.millis(150), customSearchNode);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            // Ngâm màu nền 50ms
            fadeIn.setDelay(Duration.millis(50));
            fadeIn.play();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleCustomSearch(ActionEvent event) {
        try {
            // Mở bộ lọc trống, không truyền lastSearchCriteria
            sceneSwitcher.openFilter(null);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Lỗi chuyển cảnh");
        }
    }
}
