package controller;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class welcomeController {

    @FXML
    private ImageView banner1, banner2, banner3, banner4, banner5, banner6;
    @FXML
    private ImageView login;
    @FXML
    private AnchorPane menu, menu_pane, transistion_pane;
    @FXML
    private Button next, prev, go_to_seller, explore1, info, explore2;

    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();

    // --- CÁC BIẾN QUẢN LÝ LOGIC SLIDE ---
    private ImageView[] banners;
    private int currentIndex = 0;
    private Timeline autoSlideTimeline;
    private final double BANNER_WIDTH = 1280.0;
    private boolean isAnimating = false;

    // --- CÁC BIẾN QUẢN LÝ LOGIC MENU ---
    private final double MENU_WIDTH = 400.0; // Chiều rộng của menu_pane theo FXML
    private boolean isMenuOpen = false;

    @FXML
    public void Login(MouseEvent event) {
        try {
            Stage welcomeStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Runnable Callback = () -> {
                javafx.application.Platform.runLater(() -> {
                    try {
                        // CHUYỂN SCENE CỦA STAGE WELCOME CŨ SANG MAINPAGE
                        sceneSwitcher.switchToMainPage(null);
                    } catch (IOException e) {
                        System.err.println("Lỗi chuyển trang: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            };
            sceneSwitcher.openSignInPopup();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        // ==========================================
        // 1. KHỞI TẠO LOGIC BANNER
        // ==========================================
        banners = new ImageView[]{banner1, banner2, banner3, banner4, banner5, banner6};

        for (int i = 0; i < banners.length; i++) {
            if (i == currentIndex) {
                banners[i].setTranslateX(0);
            } else {
                banners[i].setTranslateX(BANNER_WIDTH);
            }
        }

        autoSlideTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            handleNext();
        }));
        autoSlideTimeline.setCycleCount(Animation.INDEFINITE);
        autoSlideTimeline.play();

        next.setOnAction(e -> {
            resetAutoSlide();
            handleNext();
        });

        prev.setOnAction(e -> {
            resetAutoSlide();
            handlePrev();
        });

        // ==========================================
        // 2. KHỞI TẠO LOGIC MENU BÊN TRÁI
        // ==========================================
        // Giấu menu ra ngoài lề trái (-400)
        menu_pane.setTranslateX(-MENU_WIDTH);
        // Ẩn lớp nền tối và set opacity = 0
        transistion_pane.setOpacity(0.0);
        transistion_pane.setVisible(false);

        // Gắn sự kiện click vào nút Menu (icon 3 gạch)
        menu.setOnMouseClicked(e -> toggleMenu());

        // (UX) Gắn sự kiện: Bấm ra ngoài vùng tối sẽ tự động đóng menu
        transistion_pane.setOnMouseClicked(e -> {
            if (isMenuOpen) toggleMenu();
        });
    }

    // ==========================================
    // CÁC HÀM XỬ LÝ MENU
    // ==========================================
    private void toggleMenu() {
        isMenuOpen = !isMenuOpen; // Đảo trạng thái menu

        // 1. Hiệu ứng trượt menu (0.25s)
        TranslateTransition menuTransition = new TranslateTransition(Duration.millis(250), menu_pane);
        menuTransition.setToX(isMenuOpen ? 0 : -MENU_WIDTH);

        // 2. Hiệu ứng mờ nền tối (0.25s)
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(250), transistion_pane);

        if (isMenuOpen) {
            transistion_pane.setVisible(true);
            fadeTransition.setToValue(0.6);
        } else {
            fadeTransition.setToValue(0.0);
            fadeTransition.setOnFinished(e -> transistion_pane.setVisible(false));
        }

        menuTransition.play();
        fadeTransition.play();
    }

    // ==========================================
    // CÁC HÀM XỬ LÝ BANNER
    // ==========================================
    private void handleNext() {
        if (isAnimating) return;
        int nextIndex = (currentIndex + 1) % banners.length;
        slideAnimation(currentIndex, nextIndex, true);
        currentIndex = nextIndex;
    }

    private void handlePrev() {
        if (isAnimating) return;
        int prevIndex = (currentIndex - 1 + banners.length) % banners.length;
        slideAnimation(currentIndex, prevIndex, false);
        currentIndex = prevIndex;
    }

    private void slideAnimation(int oldIdx, int newIdx, boolean isNext) {
        isAnimating = true;

        ImageView oldBanner = banners[oldIdx];
        ImageView newBanner = banners[newIdx];

        int directionMultiplier = isNext ? 1 : -1;

        newBanner.setTranslateX(BANNER_WIDTH * directionMultiplier);

        TranslateTransition transitionOld = new TranslateTransition(Duration.millis(650), oldBanner);
        transitionOld.setToX(-BANNER_WIDTH * directionMultiplier);

        TranslateTransition transitionNew = new TranslateTransition(Duration.millis(650), newBanner);
        transitionNew.setToX(0);

        transitionOld.play();
        transitionNew.play();

        transitionNew.setOnFinished(e -> {
            isAnimating = false;
        });
    }

    private void resetAutoSlide() {
        if (autoSlideTimeline != null) {
            autoSlideTimeline.stop();
            autoSlideTimeline.play();
        }
    }

    // ==========================================
    // CÁC HÀM ĐIỀU HƯỚNG SCENE (THÊM @FXML)
    // ==========================================
    @FXML
    private void handleExplore1(MouseEvent event) {
        if (SessionManager.getInstance().isBidder()) {
            try {
                sceneSwitcher.switchToMainPage(event);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            try {
                // 1. Lấy Stage Welcome hiện tại
                Stage welcomeStage = (Stage) ((Node) event.getSource()).getScene().getWindow();

                // 2. Tạo một cái bẫy Callback (Runnable)
                Runnable callback = () -> {
                    // Ép JavaFX xử lý trên giao diện để tránh bị xung đột luồng
                    javafx.application.Platform.runLater(() -> {
                        try {
                            // CHUYỂN SCENE CỦA STAGE WELCOME CŨ SANG MAINPAGE
                            SceneSwitchController.switchToMainPage2(welcomeStage);
                        } catch (IOException e) {
                            System.err.println("Lỗi chuyển trang: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                };

                sceneSwitcher.openSignInPopup();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleGoToSeller(MouseEvent event) {
        try {
            sceneSwitcher.openSellerSignInPopup(event);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @FXML
    private void handleBackBtn(MouseEvent event) {
        // Kiểm tra nếu menu đang mở thì mới gọi hàm đóng
        if (isMenuOpen) {
            toggleMenu(); // Tái sử dụng lại hàm toggleMenu để có hiệu ứng trượt đóng mượt mà
        }
    }

    @FXML
    private void handleExit(MouseEvent event) {
        // Lệnh chuẩn của JavaFX để đóng toàn bộ ứng dụng một cách an toàn
        javafx.application.Platform.exit();

        // Hoặc dùng System.exit(0); nếu phần mềm của bạn có chạy ngầm luồng (Thread) khác cần ép đóng.
    }

    @FXML
    private void handleInfo(MouseEvent event) {
        try {
            // 1. Tải giao diện của trang policy
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/view/policy.fxml"));
            javafx.scene.Parent root = loader.load();

            // 2. Lấy Controller của trang policy ra để tương tác
            // Lưu ý: Đổi chữ 'policyController' thành 'policy' nếu file bên kia bạn vẫn đặt tên class là policy
            policyController policyCtrl = loader.getController();

            // 3. Gọi hàm truyền tham số để bật khối "How BidHub Works"
            policyCtrl.showSection("work");

            // 4. Thực hiện chuyển Scene
            javafx.stage.Stage stage = (javafx.stage.Stage)((javafx.scene.Node)event.getSource()).getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();

            // (Tùy chọn) Đóng menu lại nếu nó đang mở
            if (isMenuOpen) {
                toggleMenu();
            }

        } catch (IOException e) {
            System.err.println("Lỗi khi mở trang Policy: " + e.getMessage());
            e.printStackTrace();
        }
    }
}