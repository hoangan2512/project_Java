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
import message.Request;
import message.Response;
import model.ActionType;
import model.Auction;
import model.Item;
import model.User;
import network.ClientSocket;

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
    private ProductDraftDTO currentDraft = new ProductDraftDTO();

    // --- BIẾN GHI NHỚ ĐỂ BYPASS CẢNH BÁO ---
    private String lastWarnedName = "";
    private String lastWarnedId = "";
    private String lastWarnedCategory = "";

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

            contentArea.getChildren().setAll(node);
            currentSubController = loader.getController();

            if (currentSubController instanceof basicInfo) {
                ((basicInfo) currentSubController).setDraftData(currentDraft);
            } else if (currentSubController instanceof graphicInfo) {
                ((graphicInfo) currentSubController).setDraftData(currentDraft);
            } else if (currentSubController instanceof auctionInfo) {
                ((auctionInfo) currentSubController).setDraftData(currentDraft);
            }

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

            if (name.isBlank() || categories == null) {
                Status.setVisible(true);
                Status.setManaged(true);
                Status.setTextFill(Color.RED);
                Status.setText("Vui lòng nhập tên và chọn danh mục!");
                return;
            }

            String currentId = (id == null) ? "" : id.trim();

            // Cập nhật điều kiện Bypass: Phải giống y hệt Tên, ID VÀ Danh mục đã bị cảnh báo
            boolean shouldBypassWarning = name.equals(lastWarnedName) &&
                    categories.equals(lastWarnedCategory) &&
                    currentId.equals(lastWarnedId);

            if (!shouldBypassWarning) {
                User currentUser = SessionManager.getInstance().getCurrentUser();
                Item checkItem = new Item();
                checkItem.setName(name);
                checkItem.setCategories(categories); // Gửi thêm category để check
                checkItem.setUser_prdID(currentId);
                checkItem.setSeller_id(currentUser.getId());

                Request checkReq = new Request(checkItem, ActionType.CHECK_DUPLICATE_NAME);
                Response checkRes = ClientSocket.sendRequest(checkReq);

                if (checkRes != null) {
                    if ("DUPLICATE_ID".equals(checkRes.getStatus())) {
                        // LUẬT 1: TRÙNG ID -> CHẶN MỌI TRƯỜNG HỢP
                        Status.setVisible(true);
                        Status.setManaged(true);
                        Status.setTextFill(Color.RED);
                        Status.setText("Mã sản phẩm (ID) này đã được sử dụng! Vui lòng nhập mã khác.");

                        // Reset biến nhớ để không cho Bypass
                        lastWarnedName = ""; lastWarnedId = ""; lastWarnedCategory = "";
                        return;

                    } else if ("DUPLICATE_NAME_CAT".equals(checkRes.getStatus())) {
                        // LUẬT 2: TRÙNG TÊN VÀ DANH MỤC -> CẢNH BÁO CHO BYPASS
                        String existingId = (checkRes.getData() != null) ? (String) checkRes.getData() : "Chưa xác định";

                        Status.setVisible(true);
                        Status.setManaged(true);
                        Status.setTextFill(Color.web("#FFA500")); // Màu cam
                        Status.setText("Đã có sản phẩm cùng tên và danh mục (ID: " + existingId + "). Bấm Next để bỏ qua cảnh báo.");

                        // Ghi nhớ để lần bấm Next sau sẽ cho qua
                        lastWarnedName = name;
                        lastWarnedId = currentId;
                        lastWarnedCategory = categories;
                        return;
                    }
                    // LUẬT 3: Nếu Trùng Tên nhưng Khác Danh mục -> Server trả về OK -> Đi tiếp bình thường
                }
            }

            // Nếu đi tiếp thành công, xóa bộ nhớ
            lastWarnedName = ""; lastWarnedId = ""; lastWarnedCategory = "";

            currentDraft.setName(name);
            currentDraft.setId(currentId);
            currentDraft.setDescription(description);
            currentDraft.setCategories(categories);

            loadChildFXML("/view/sellerHub/new_item_page/graphicInfo.fxml");
            Status.setVisible(false);
            Status.setManaged(false);

        } else if (currentSubController instanceof graphicInfo) {
            graphicInfo gic = (graphicInfo) currentSubController;

            if (gic.getImgPath() == null || gic.getImgPath().isBlank()) {
                Status.setVisible(true);
                Status.setManaged(true);
                Status.setTextFill(Color.RED);
                Status.setText("Vui lòng tải lên ít nhất 1 ảnh sản phẩm");
                return;
            }

            currentDraft.setImgPath(gic.getImgPath());
            currentDraft.setImgPath1(gic.getImgPath1());
            currentDraft.setImgPath2(gic.getImgPath2());
            currentDraft.setImgPath3(gic.getImgPath3());
            currentDraft.setImgPath4(gic.getImgPath4());
            currentDraft.setImgPath5(gic.getImgPath5());
            currentDraft.setImgPath6(gic.getImgPath6());

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
                Status.setTextFill(Color.RED);
                Status.setText("Information missing");
                return;
            }

            currentDraft.setPrice(prdPrice);
            currentDraft.setStartTime(startTime);
            currentDraft.setAuctionChoice(auction_choice);

            boolean isSaved = pushToDatabase(currentDraft);

            if (isSaved) {
                loadChildFXML("/view/sellerHub/new_item_page/prdOverview.fxml");
                NextBtn.setText("Back to product list");
                Status.setVisible(false);
                Status.setManaged(false);
                currentDraft.clear();
            } else {
                Status.setVisible(true);
                Status.setManaged(true);
                Status.setTextFill(Color.RED);
                Status.setText("Lỗi kết nối cơ sở dữ liệu!");
            }
        } else if (currentSubController instanceof prdOverview) {
            loadChildFXML("/view/sellerHub/new_item_page/basicInfo.fxml");
        }
    }

    private boolean pushToDatabase(ProductDraftDTO draft) {
        try {
            Item newItem = new Item();
            newItem.setName(draft.getName());
            newItem.setDescription(draft.getDescription());
            newItem.setUser_prdID(draft.getId());
            newItem.setCategories(draft.getCategories());

            try {
                newItem.setStarting_price(Double.parseDouble(draft.getPrice()));
            } catch (NumberFormatException e) {
                System.err.println("Giá nhập vào không phải là số hợp lệ!");
                return false;
            }

            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                newItem.setSeller_id(currentUser.getId());
            } else {
                return false;
            }

            newItem.setImgPath(draft.getImgPath());
            newItem.setImgPath1(draft.getImgPath1());
            newItem.setImgPath2(draft.getImgPath2());
            newItem.setImgPath3(draft.getImgPath3());
            newItem.setImgPath4(draft.getImgPath4());
            newItem.setImgPath5(draft.getImgPath5());
            newItem.setImgPath6(draft.getImgPath6());

            Auction newAuction = new Auction();
            newAuction.setCurrent_price(newItem.getStarting_price());
            newAuction.setStatus("RUNNING");
            newAuction.setHighest_bidder_id(0);

            newAuction.setStart_time(java.time.LocalDateTime.now());
            newAuction.setEnd_time(java.time.LocalDateTime.now().plusDays(3));

            Object[] payload = new Object[]{ newItem, newAuction };

            Request req = new Request(payload, ActionType.CREATE_ITEM);
            Response res = ClientSocket.sendRequest(req);

            return res != null && "SUCCESS".equals(res.getStatus());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

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
        }
    }
}