package controller;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
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
    @FXML
    private Pane transitionPane;

    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();
    private static mainPageController instance;
    
    private customSearchController currentCustomSearchController;
    private prdPageController currentPrdPageController;
    
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

        // Load giao diện tìm kiếm mặc định
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/customSearch.fxml"));
            Parent customSearchNode = loader.load();

            currentCustomSearchController = loader.getController();
            if (currentCustomSearchController != null) {
                currentCustomSearchController.hideFilterButton();
                currentCustomSearchController.setSearchCriteria(null); 
            }

            currentPrdPageController = null; 
            prdPagePane.getChildren().setAll(customSearchNode);
            prdPagePane.setVisible(true);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi: Không load được file customSearch.fxml làm mặc định");
        }

        transitionPane.setVisible(false);
    }

    public void loadCustomSearchPane(SearchCriteria criteria) {
        this.lastSearchCriteria = criteria;
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/customSearch.fxml"));
            Parent customSearchNode = loader.load();

            currentCustomSearchController = loader.getController();
            currentPrdPageController = null;

            if (currentCustomSearchController != null) {
                currentCustomSearchController.setSearchCriteria(criteria);
            }

            prdPagePane.setStyle("-fx-background-color:  #1E1E1E;");
            customSearchNode.setOpacity(0.0);
            prdPagePane.getChildren().setAll(customSearchNode);
            prdPagePane.setVisible(true);
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(150), customSearchNode);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.setDelay(Duration.millis(50));
            fadeIn.play();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi: Không load được file customSearch.fxml");
        }
    }

    public void fillProductPage(Auction auction) {
        // --- HIỆU ỨNG GẠT MÀU MỚI ---
        
        // Khôi phục lại độ mờ về 100% (Rất quan trọng cho lần click thứ 2 trở đi vì trước đó nó bị Fade về 0)
        transitionPane.setOpacity(1.0);
        
        // Bật và đặt màu cho lớp hiệu ứng
        transitionPane.setVisible(true);
        transitionPane.toFront(); // Đảm bảo lớp gạt màu nằm trên cùng

        // Đặt vị trí bắt đầu ở ngoài màn hình bên trái
        double paneWidth = prdPagePane.getWidth() > 0 ? prdPagePane.getWidth() : 1500;
        transitionPane.setTranslateX(-paneWidth);

        // 1. Tạo hiệu ứng gạt vào (Slide-in)
        // Kéo dài thời gian ra một chút (450ms) để nhìn thấy rõ hiệu ứng Rất Chậm - Nhanh - Rất Chậm
        TranslateTransition wipeIn = new TranslateTransition(Duration.millis(450), transitionPane);
        wipeIn.setToX(0);

        // CUSTOM SPLINE: Điểm x1, y1, x2, y2 -> Tạo độ võng cực gắt (Bắt đầu rất chậm -> Phóng cực nhanh ở giữa -> Kết thúc rất chậm)
        wipeIn.setInterpolator(Interpolator.SPLINE(0.65, 0.0, 0.35, 1.0));

        // 2. Gán hành động sau khi gạt xong
        wipeIn.setOnFinished(e -> {
            // BẮT ĐẦU LOAD DỮ LIỆU MỚI (SAU KHI MÀN HÌNH ĐÃ BỊ CHE)
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/productPage.fxml"));
                Parent prdPageNode = loader.load();
                currentPrdPageController = loader.getController();
                currentCustomSearchController = null;

                if (currentPrdPageController != null) {
                    currentPrdPageController.setData(auction);
                }
                // Thay thế nội dung bên dưới lớp hiệu ứng
                prdPagePane.getChildren().setAll(prdPageNode);

            } catch (IOException ioException) {
                ioException.printStackTrace();
            }

            // 3. Tạo độ trễ "ngâm" màu cam
            PauseTransition hold = new PauseTransition(Duration.millis(200));
            hold.setOnFinished(ev -> {
                // 4. Tạo hiệu ứng mờ dần lớp hiệu ứng để lộ nội dung mới
                FadeTransition fadeOut = new FadeTransition(Duration.millis(400), transitionPane);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);

                fadeOut.setOnFinished(evt -> {
                    transitionPane.setVisible(false); // Tắt đi sau khi xong
                    transitionPane.setTranslateX(-paneWidth); // Đẩy lại về vị trí chờ
                });
                fadeOut.play();
            });
            hold.play();
        });

        // Chạy hiệu ứng gạt vào
        wipeIn.play();
    }

    public void refreshData(Response res) {
        if (currentCustomSearchController != null) {
            System.out.println("Đang làm mới danh sách sản phẩm...");
            currentCustomSearchController.refresh();
        } else if (currentPrdPageController != null) {
            System.out.println("Đang làm mới trang chi tiết sản phẩm...");
            currentPrdPageController.handleBroadcast(res);
        }
    }

    public void refreshData() {
        refreshData(null);
    }

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
        if (SessionManager.getInstance().isBidder()) {
            userAvatar.setStroke(Color.GREEN);
            User user = SessionManager.getInstance().getCurrentUser();
            System.out.println("Logged In: " + user.getUsername());
        } else {
            System.out.println("Status: Waiting for login");
            userAvatar.setStroke(Color.RED);
        }
    }

    public void handleBidHub(MouseEvent event) {
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

            prdPagePane.setStyle("-fx-background-color:  #1E1E1E;");
            customSearchNode.setOpacity(0.0);
            prdPagePane.getChildren().setAll(customSearchNode);
            prdPagePane.setVisible(true);
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(150), customSearchNode);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.setDelay(Duration.millis(50));
            fadeIn.play();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void goBackToSearch() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/customSearch.fxml"));
            Parent customSearchNode = loader.load();

            currentCustomSearchController = loader.getController();
            currentPrdPageController = null;

            if (currentCustomSearchController != null) {
                currentCustomSearchController.setSearchCriteria(lastSearchCriteria);
            }

            prdPagePane.setStyle("-fx-background-color:  #1E1E1E;");
            customSearchNode.setOpacity(0.0);
            prdPagePane.getChildren().setAll(customSearchNode);
            prdPagePane.setVisible(true);
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(150), customSearchNode);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.setDelay(Duration.millis(50));
            fadeIn.play();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleCustomSearch(ActionEvent event) {
        try {
            sceneSwitcher.openFilter(null);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Lỗi chuyển cảnh");
        }
    }
}
