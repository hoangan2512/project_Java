package controller.sellerHub;

import controller.SceneSwitchController;
import controller.SessionManager;
import controller.sellerHub.new_item_page.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import jdk.jfr.Event;
import model.User;

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

        userAvatar.setStroke(Color.GREEN);
        User user = SessionManager.getInstance().getCurrentUser();
        System.out.println("Logged In: " + user.getUsername());
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

            String categories = bic.getCategories();

            if (name.isBlank() || id.isBlank() || categories == null) {
                Status.setVisible(true);
                Status.setManaged(true);
                Status.setText("Infomation missing");
                return;
            }

            currentDraft.setName(name);
            currentDraft.setId(id);
            currentDraft.setDescription(description);
            currentDraft.setCategories(categories);

            // fxml loader
            loadChildFXML("/view/sellerHub/new_item_page/graphicInfo.fxml");
            Status.setVisible(false);
            Status.setManaged(false);
        } else if (currentSubController instanceof graphicInfo) {
            graphicInfo gic = (graphicInfo) currentSubController;

            if (gic.getImg() == null) {
                Status.setVisible(true);
                Status.setManaged(true);
                Status.setText("Infomation missing");
                return;
            }

            currentDraft.setImage(gic.getImg());

            // Nếu ok, load trang tiếp theo
            loadChildFXML("/view/sellerHub/new_item_page/auctionInfo.fxml");
            Status.setVisible(false);
            Status.setManaged(false);
        } else if (currentSubController instanceof auctionInfo) {
            auctionInfo aic = (auctionInfo) currentSubController;

            String prdPrice = aic.getPrice();
            String startTime = aic.getTime();
            String auction_choice = aic.getChoice();

            if (prdPrice.isBlank() || startTime.isBlank() || auction_choice == null) {
                Status.setVisible(true);
                Status.setManaged(true);
                Status.setText("Infomation missing");
                return;
            }

            currentDraft.setPrice(prdPrice);
            currentDraft.setStartTime(startTime);
            currentDraft.setAuctionChoice(auction_choice);

            boolean isSaved = pushToDatabase(currentDraft);

            if (isSaved) {
                loadChildFXML("/view/sellerHub/new_item_page/prdOverview.fxml");
                // Truyền dữ liệu sang trang Overview để hiển thị (tùy chọn)
                if (currentSubController instanceof prdOverview) { // (Tên class bạn đã sửa)
                    // ((PrdOverviewController) currentSubController).setData(currentDraft);
                }

                NextBtn.setText("Back to product list");
                Status.setVisible(false);
                Status.setManaged(false);

                // Xóa draft để chuẩn bị cho sản phẩm tiếp theo
                currentDraft.clear();
            } else {
                Status.setVisible(true);
                Status.setManaged(true);
                Status.setText("Lỗi kết nối cơ sở dữ liệu!");
            }
        } else if (currentSubController instanceof prdOverview) {
            loadChildFXML("/view/sellerHub/new_item_page/basicInfo.fxml");
        }
    }

    private boolean pushToDatabase(ProductDraftDTO currentDraft) {
        return true;
    }

    // Nơi chứa các biến toàn cục của Parent Controller
    private ProductDraftDTO currentDraft = new ProductDraftDTO();

    public void handleBackBtn(MouseEvent event) {
        if (currentSubController instanceof graphicInfo) {
            loadChildFXML("/view/sellerHub/new_item_page/basicInfo.fxml");
        } else if (currentSubController instanceof auctionInfo) {
            loadChildFXML("/view/sellerHub/new_item_page/graphicInfo.fxml");
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
