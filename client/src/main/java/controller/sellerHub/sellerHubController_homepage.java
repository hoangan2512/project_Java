package controller.sellerHub;

import controller.SceneSwitchController;
import controller.sellerHub.new_item_page.basicInfo;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

import java.io.IOException;

public class sellerHubController_homepage {
    @FXML
    private Circle userAvatar;
    @FXML
    private Circle searchBtn;
    @FXML
    private TextField searchBar;
    @FXML
    private Button BidHub;
    @FXML
    private StackPane contentArea;
    @FXML
    private Button NextBtn;
    @FXML
    private Label Status;

    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();

    private Object currentSubController;

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

        loadChildFXML("/view/sellerHub/new_item_page/basicInfo.fxml");

        Status.setVisible(false);
        Status.setManaged(false);
    }

    public void loadChildFXML(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent node = loader.load();

            // Xóa cái cũ, nạp cái mới
            contentArea.getChildren().setAll(node);
            currentSubController = loader.getController();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi: Không tìm thấy file FXML tại " + fxmlPath);
        }
    }

    public void handleNextBtn(MouseEvent event) {
        if (currentSubController instanceof basicInfo) {
            basicInfo bic = (basicInfo) currentSubController;

            String name = bic.getPrdName();
            String id = bic.getPrdId();
            String description = bic.getDescription();

            // Logic kiểm tra tập trung tại Homepage
            if (name.isBlank() || id.isBlank()) {
                Status.setVisible(true);
                Status.setManaged(true);
                Status.setText("Infomation missing");
                return;
            }

            // Nếu ok, load trang tiếp theo
            loadChildFXML("/view/sellerHub/new_item_page/graphicInfo.fxml");
            Status.setVisible(false);
            Status.setManaged(false);
        }
    }

    public void handleSearchBtnClick(MouseEvent event) {
        String searchText = searchBar.getText();
        if (searchText == null || searchText.trim().isEmpty()) {
            searchBar.requestFocus();
        } else {
            System.out.println("searching");
        }
    }
    public void handleBidHub(MouseEvent event) {
        try {
            sceneSwitcher.switchToMainPage(event);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Lỗi chuyển cảnh");
        }
    }


}
