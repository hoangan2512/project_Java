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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

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
        if (user != null) {
            System.out.println("Logged In: " + user.getUsername());
        }
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
                if (currentUser != null) {
                    checkItem.setSeller_id(currentUser.getId());
                }

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
            String duration = aic.getDuration();

            // Kiểm tra các trường trống (Đặc biệt là startTime do DatePicker có thể null)
            if (prdPrice == null || prdPrice.isBlank() || startTime == null || startTime.isBlank() || duration == null) {
                Status.setVisible(true);
                Status.setManaged(true);
                Status.setTextFill(Color.RED);
                Status.setText("Vui lòng điền đầy đủ giá khởi điểm và thời gian bắt đầu!");
                return;
            }
            
            // Kiểm tra xem thời lượng có hợp lệ không (phải lớn hơn 0)
            if (aic.isDurationZero()) {
                Status.setVisible(true);
                Status.setManaged(true);
                Status.setTextFill(Color.RED);
                Status.setText("Vui lòng chọn thời lượng đấu giá lớn hơn 0!");
                return;
            }

            currentDraft.setPrice(prdPrice);
            currentDraft.setStartTime(startTime);
            currentDraft.setDuration(duration);

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
                Status.setText("Lỗi: Không thể kết nối cơ sở dữ liệu hoặc thời gian không hợp lệ!");
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

            // =========================================================
            // ĐỌC DỮ LIỆU ẢNH TỪ ĐƯỜNG DẪN VÀ GÁN VÀO BYTE ARRAY
            // =========================================================
            try {
                if (draft.getImgPath() != null && !draft.getImgPath().isBlank()) {
                    newItem.setImgPath(new File(draft.getImgPath()).getName()); // Chỉ lấy tên file
                    Path path = Paths.get(draft.getImgPath());
                    newItem.setImageBytes(Files.readAllBytes(path));
                }
                if (draft.getImgPath1() != null && !draft.getImgPath1().isBlank()) {
                    newItem.setImgPath1(new File(draft.getImgPath1()).getName());
                    Path path = Paths.get(draft.getImgPath1());
                    newItem.setImageBytes1(Files.readAllBytes(path));
                }
                if (draft.getImgPath2() != null && !draft.getImgPath2().isBlank()) {
                    newItem.setImgPath2(new File(draft.getImgPath2()).getName());
                    Path path = Paths.get(draft.getImgPath2());
                    newItem.setImageBytes2(Files.readAllBytes(path));
                }
                if (draft.getImgPath3() != null && !draft.getImgPath3().isBlank()) {
                    newItem.setImgPath3(new File(draft.getImgPath3()).getName());
                    Path path = Paths.get(draft.getImgPath3());
                    newItem.setImageBytes3(Files.readAllBytes(path));
                }
                if (draft.getImgPath4() != null && !draft.getImgPath4().isBlank()) {
                    newItem.setImgPath4(new File(draft.getImgPath4()).getName());
                    Path path = Paths.get(draft.getImgPath4());
                    newItem.setImageBytes4(Files.readAllBytes(path));
                }
                if (draft.getImgPath5() != null && !draft.getImgPath5().isBlank()) {
                    newItem.setImgPath5(new File(draft.getImgPath5()).getName());
                    Path path = Paths.get(draft.getImgPath5());
                    newItem.setImageBytes5(Files.readAllBytes(path));
                }
                if (draft.getImgPath6() != null && !draft.getImgPath6().isBlank()) {
                    newItem.setImgPath6(new File(draft.getImgPath6()).getName());
                    Path path = Paths.get(draft.getImgPath6());
                    newItem.setImageBytes6(Files.readAllBytes(path));
                }
            } catch (IOException e) {
                System.err.println("Lỗi khi đọc file ảnh ở client!");
                e.printStackTrace();
                return false; // Dừng lại nếu không đọc được ảnh
            }

            Auction newAuction = new Auction();
            newAuction.setCurrent_price(newItem.getStarting_price());
            newAuction.setHighest_bidder_id(0);

            // =========================================================
            // CHUYỂN ĐỔI CHUỖI THỜI GIAN TỪ CLIENT THÀNH LOCALDATETIME
            // =========================================================
            LocalDateTime startDateTime = null;
            LocalDateTime endDateTime = null;

            try {
                // Parse startTime (Ví dụ: "2024-05-03 14:30")
                String[] parts = draft.getStartTime().split(" ");
                String[] dateParts = parts[0].split("-");
                String[] timeParts = parts[1].split(":");
                
                int sYear = Integer.parseInt(dateParts[0]);
                int sMonth = Integer.parseInt(dateParts[1]);
                int sDay = Integer.parseInt(dateParts[2]);
                int sHour = Integer.parseInt(timeParts[0]);
                int sMinute = Integer.parseInt(timeParts[1]);
                
                startDateTime = LocalDateTime.of(sYear, sMonth, sDay, sHour, sMinute);

                // Parse duration (Ví dụ: "3 hours 30 mins")
                String[] durParts = draft.getDuration().split(" ");
                int dHour = Integer.parseInt(durParts[0]);
                int dMinute = Integer.parseInt(durParts[2]);
                
                endDateTime = startDateTime.plusHours(dHour).plusMinutes(dMinute);
            } catch (Exception ex) {
                System.err.println("Lỗi khi parse thời gian: " + ex.getMessage());
                return false; // Hủy lưu nếu parse thời gian bị lỗi
            }

            newAuction.setStart_time(startDateTime);
            newAuction.setEnd_time(endDateTime);

            // Gán trạng thái ban đầu dựa vào thời gian bắt đầu
            if (startDateTime.isAfter(LocalDateTime.now())) {
                newAuction.setStatus("WAITING"); // Phiên chưa tới giờ
            } else {
                newAuction.setStatus("RUNNING"); // Phiên bắt đầu ngay lập tức
            }

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
        // --- THỰC HIỆN ĐĂNG XUẤT ---
        // 1. Gửi request LOGOUT lên Server
        Request logoutReq = new Request(null, ActionType.LOGOUT);
        ClientSocket.sendRequest(logoutReq); // Không cần chờ Response
        
        // 2. Xóa session ở phía Client
        SessionManager.getInstance().logout();

        // 3. Chuyển cảnh về trang chủ (BidHub)
        try {
            sceneSwitcher.switchToMainPage(event);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
