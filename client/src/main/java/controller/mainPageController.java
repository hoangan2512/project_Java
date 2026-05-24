package controller;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
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
import javafx.stage.Stage;
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
    private Button SellerHub, works, paymentPolicy, privatePolicy, contact;
    @FXML
    private StackPane prdPagePane;
    @FXML
    private ImageView bidHub;
    @FXML
    private Pane transitionPane;

    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();
    private static mainPageController instance;

    // BIẾN MỚI: Bộ nhớ đệm giữ lại màn hình Search để không phải tạo mới mỗi khi bấm
    private Parent customSearchNodeCached;
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
            System.out.println("Không tìm thấy ảnh avatar!");
        }
        instance = this;
        updateAvatarUI();

        // CACHE MÀN HÌNH TÌM KIẾM ĐÚNG 1 LẦN DUY NHẤT VÀO RAM
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/customSearch.fxml"));
            customSearchNodeCached = loader.load();
            currentCustomSearchController = loader.getController();

            if (currentCustomSearchController != null) {
                currentCustomSearchController.hideFilterButton();
                currentCustomSearchController.setSearchCriteria(null);
            }
            currentPrdPageController = null;
            prdPagePane.getChildren().setAll(customSearchNodeCached);
            prdPagePane.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        transitionPane.setVisible(false);
    }

    // --- CÁC HÀM XỬ LÝ CHUYỂN TRANG ĐÃ ĐƯỢC TỐI ƯU HÓA (KHÔNG DÙNG FXML LOADER NỮA) ---

    public void loadCustomSearchPane(SearchCriteria criteria) {
        this.lastSearchCriteria = criteria;

        // Tái sử dụng node đã được cache ở initialize
        currentPrdPageController = null;
        if (currentCustomSearchController != null) {
            currentCustomSearchController.setSearchCriteria(criteria);
        }

        switchToCachedSearchNode();
    }

    public void handleBidHub(MouseEvent event) {
        this.lastSearchCriteria = null;

        currentPrdPageController = null;
        if (currentCustomSearchController != null) {
            currentCustomSearchController.hideFilterButton();
            currentCustomSearchController.setSearchCriteria(null);
        }

        switchToCachedSearchNode();
    }

    public void goBackToSearch() {
        currentPrdPageController = null;
        if (currentCustomSearchController != null) {
            currentCustomSearchController.setSearchCriteria(lastSearchCriteria);
        }

        switchToCachedSearchNode();
    }

    // Hàm phụ trợ dùng chung cho 3 nút trên để nhét Node vào Pane mượt mà
    private void switchToCachedSearchNode() {
        if (customSearchNodeCached == null) return;

        prdPagePane.setStyle("-fx-background-color:  #1E1E1E;");
        customSearchNodeCached.setOpacity(0.0);

        if (!prdPagePane.getChildren().contains(customSearchNodeCached)) {
            prdPagePane.getChildren().setAll(customSearchNodeCached);
        }
        prdPagePane.setVisible(true);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(150), customSearchNodeCached);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    public void fillProductPage(Auction auction) {
        transitionPane.setOpacity(1.0);
        transitionPane.setVisible(true);
        transitionPane.toFront();

        double paneWidth = prdPagePane.getWidth() > 0 ? prdPagePane.getWidth() : 1500;
        transitionPane.setTranslateX(-paneWidth);

        TranslateTransition wipeIn = new TranslateTransition(Duration.millis(450), transitionPane);
        wipeIn.setToX(0);
        wipeIn.setInterpolator(Interpolator.SPLINE(0.65, 0.0, 0.35, 1.0));

        wipeIn.setOnFinished(e -> {
            try {
                // Trang chi tiết thay đổi cục bộ rất nhiều nên ta vẫn load FXML bình thường
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/productPage.fxml"));
                Parent prdPageNode = loader.load();
                currentPrdPageController = loader.getController();

                if (currentPrdPageController != null) {
                    currentPrdPageController.setData(auction);
                }

                // Gỡ giao diện search ra, đưa giao diện sản phẩm vào
                prdPagePane.getChildren().setAll(prdPageNode);

            } catch (IOException ioException) {
                ioException.printStackTrace();
            }

            PauseTransition hold = new PauseTransition(Duration.millis(200));
            hold.setOnFinished(ev -> {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(400), transitionPane);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(evt -> {
                    transitionPane.setVisible(false);
                    transitionPane.setTranslateX(-paneWidth);
                });
                fadeOut.play();
            });
            hold.play();
        });

        wipeIn.play();
    }

    // --- HÀM BẮT SỰ KIỆN TỪ SERVER ĐÃ ĐƯỢC TỐI ƯU CỦA BẠN ---
    public void refreshData(Response res) {
        if (res == null) return;

        String status = res.getStatus();

        if (currentCustomSearchController != null && prdPagePane.getChildren().contains(customSearchNodeCached)) {
            if ("NOTIFY_NEW_PRICE".equals(status) || "AUCTION_START".equals(status) || "AUCTION_END".equals(status) || "AUCTION_EXTENDED".equals(status)) {
                if (res.getData() instanceof Auction updatedAuction) {
                    currentCustomSearchController.updateSingleAuction(updatedAuction);
                }
            }
            else {
                currentCustomSearchController.refresh();
            }
        }
        else if (currentPrdPageController != null) {
            currentPrdPageController.handleBroadcast(res);
        }
    }

    public void refreshData() {
        if (currentCustomSearchController != null) {
            currentCustomSearchController.refresh();
        }
    }

    // Các hàm xử lý click khác giữ nguyên...
    @FXML
    public void handleAvatarClick(MouseEvent event) {
        try {
            Stage welcomeStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Runnable callback = () -> {
                // Ép JavaFX xử lý trên giao diện để tránh bị xung đột luồng
                javafx.application.Platform.runLater(() -> {
                    try {
                        // CHUYỂN SCENE CỦA STAGE WELCOME CŨ SANG MAINPAGE
                        SceneSwitchController.switchToWelcome2(welcomeStage);
                    } catch (IOException e) {
                        System.err.println("Lỗi chuyển trang: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            };

            // 3. Truyền Runnable vào đúng theo thiết kế cũ của hàm
            sceneSwitcher.openSignInPopup();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleSearchBtnClick(MouseEvent event) {
        String searchText = searchBar.getText();
        if (searchText == null || searchText.trim().isEmpty()) {
            searchBar.requestFocus();
        } else {
            SearchCriteria criteria = new SearchCriteria();
            criteria.setKeyword(searchText.trim());
            loadCustomSearchPane(criteria);
        }
    }

    public void handleSellerHub(MouseEvent event) {
        try { sceneSwitcher.switchToSellerSignIn(event); } catch (IOException e) {}
    }

    public void updateAvatarUI() {
        if (SessionManager.getInstance().isBidder()) {
            userAvatar.setStroke(Color.GREEN);
        } else {
            userAvatar.setStroke(Color.RED);
        }
    }

    public void handleCustomSearch(ActionEvent event) {
        try { sceneSwitcher.openFilter(null); } catch (IOException e) {}
    }

    private void loadPolicyPane(String sectionType) {
        try {
            // Nạp FXML của policy
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/policy_popup.fxml"));
            Parent policyNode = loader.load();

            // Lấy controller và gọi hàm showSection với tham số truyền vào
            policy_popupController policyCtrl = loader.getController();
            if (policyCtrl != null) {
                policyCtrl.showSection(sectionType);
            }

            // Đặt vào Pane và làm mờ dần để xuất hiện mượt mà
            prdPagePane.getChildren().setAll(policyNode);
            prdPagePane.setVisible(true);

            // Xóa cache màn hình sản phẩm để tránh lỗi (nếu có)
            currentPrdPageController = null;

            FadeTransition fadeIn = new FadeTransition(Duration.millis(250), policyNode);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi khi tải file policy_popup.fxml");
        }
    }

    @FXML
    private void handleWorksClick(ActionEvent event) {
        loadPolicyPane("work");
    }

    @FXML
    private void handlePaymentPolicyClick(ActionEvent event) {
        loadPolicyPane("payment");
    }

    @FXML
    private void handlePrivatePolicyClick(ActionEvent event) {
        loadPolicyPane("policy");
    }

    @FXML
    private void handleContactClick(ActionEvent event) {
        loadPolicyPane("contact");
    }
}