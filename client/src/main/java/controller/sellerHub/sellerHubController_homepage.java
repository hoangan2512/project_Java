package controller.sellerHub;

import controller.SceneSwitchController;
import controller.SessionManager;
import controller.sellerHub.auction_managerController;
import controller.sellerHub.listController;
import controller.sellerHub.new_item_page.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
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
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

public class sellerHubController_homepage {
    @FXML
    private Circle userAvatar;
    @FXML
    private Circle searchBtn;
    @FXML
    private TextField searchBar;
    @FXML
    private Button BidHub, backBtn;
    @FXML
    private StackPane contentArea, auction_stack;
    @FXML
    private Button NextBtn;
    @FXML
    private Label Status;
    @FXML
    private TilePane productGrid;
    @FXML
    private ToggleButton list_new_item, auction_manager;
    @FXML
    private ScrollPane auction_scroll;
    @FXML
    private Node auction_manager_pane;
    @FXML
    private auction_managerController auction_manager_paneController;

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

        // =========================================================
        // ---> THÊM MỚI 1: GÁN SỰ KIỆN CHO NÚT BACK CỦA BẢNG CHI TIẾT
        // =========================================================
        if (auction_manager_paneController != null) {
            auction_manager_paneController.setOnBack(() -> {
                // Tắt bảng chi tiết
                if (auction_manager_pane != null) {
                    auction_manager_pane.setVisible(false);
                    auction_manager_pane.setManaged(false);
                }
                // Làm mới lại danh sách
                fetchAuctions();
            });
        }

        // =========================================================
        // CẤU HÌNH TOGGLE GROUP CHO 2 NÚT CHUYỂN TAB
        // =========================================================
        ToggleGroup actionGroup = new ToggleGroup();

        if (list_new_item != null) {
            list_new_item.setToggleGroup(actionGroup);
            list_new_item.setSelected(true); // Đặt trạng thái mặc định được chọn
        }

        if (auction_manager != null) {
            auction_manager.setToggleGroup(actionGroup);
        }

        // Khởi tạo giao diện ẩn phần auction ban đầu
        if (auction_scroll != null && auction_stack != null) {
            auction_scroll.setVisible(false);
            auction_scroll.setManaged(false);
            auction_stack.setVisible(false);
            auction_stack.setManaged(false);
        }

        // Lắng nghe sự kiện chuyển đổi giữa 2 nút
        actionGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                // Ép buộc luôn phải có 1 nút được chọn
                oldValue.setSelected(true);
                return;
            }

            if (newValue == list_new_item) {
                // Hiện giao diện "Thêm sản phẩm mới"
                contentArea.setVisible(true);
                contentArea.setManaged(true);
                if (NextBtn != null) { NextBtn.setVisible(true); NextBtn.setManaged(true); }
                if (backBtn != null) { backBtn.setVisible(true); backBtn.setManaged(true); }

                // Ẩn giao diện "Quản lý đấu giá"
                if (auction_scroll != null) { auction_scroll.setVisible(false); auction_scroll.setManaged(false); }
                if (auction_stack != null) { auction_stack.setVisible(false); auction_stack.setManaged(false); }

            } else if (newValue == auction_manager) {
                // Ẩn giao diện "Thêm sản phẩm mới"
                contentArea.setVisible(false);
                contentArea.setManaged(false);
                if (NextBtn != null) { NextBtn.setVisible(false); NextBtn.setManaged(false); }
                if (backBtn != null) { backBtn.setVisible(false); backBtn.setManaged(false); }
                Status.setVisible(false);

                // HIỂN THỊ CẢ DANH SÁCH VÀ KHUNG CHI TIẾT THEO YÊU CẦU TRƯỚC ĐÓ
                if (auction_scroll != null) { auction_scroll.setVisible(true); auction_scroll.setManaged(true); }
                if (auction_stack != null) { auction_stack.setVisible(true); auction_stack.setManaged(true); }

                // ---> THÊM MỚI 2: ĐẢM BẢO BẢNG CHI TIẾT BỊ TẮT KHI BẤM CHUYỂN TAB
                if (auction_manager_pane != null) {
                    auction_manager_pane.setVisible(false);
                    auction_manager_pane.setManaged(false);
                }

                // Fetch danh sách các phiên đấu giá lên grid (Làm mới trang)
                fetchAuctions();
            }
        });

        if (auction_manager_pane != null) {
            auction_manager_pane.setManaged(false);
            auction_manager_pane.setVisible(false);
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

            boolean shouldBypassWarning = name.equals(lastWarnedName) &&
                    categories.equals(lastWarnedCategory) &&
                    currentId.equals(lastWarnedId);

            if (!shouldBypassWarning) {
                User currentUser = SessionManager.getInstance().getCurrentUser();
                Item checkItem = new Item();
                checkItem.setName(name);
                checkItem.setCategories(categories);
                checkItem.setUser_prdID(currentId);
                if (currentUser != null) {
                    checkItem.setSeller_id(currentUser.getId());
                }

                Request checkReq = new Request(checkItem, ActionType.CHECK_DUPLICATE_NAME);
                Response checkRes = ClientSocket.sendRequest(checkReq);

                if (checkRes != null) {
                    if ("DUPLICATE_ID".equals(checkRes.getStatus())) {
                        Status.setVisible(true);
                        Status.setManaged(true);
                        Status.setTextFill(Color.RED);
                        Status.setText("Mã sản phẩm (ID) này đã được sử dụng! Vui lòng nhập mã khác.");
                        lastWarnedName = ""; lastWarnedId = ""; lastWarnedCategory = "";
                        return;

                    } else if ("DUPLICATE_NAME_CAT".equals(checkRes.getStatus())) {
                        String existingId = (checkRes.getData() != null) ? (String) checkRes.getData() : "Chưa xác định";
                        Status.setVisible(true);
                        Status.setManaged(true);
                        Status.setTextFill(Color.web("#FFA500"));
                        Status.setText("Đã có sản phẩm cùng tên và danh mục (ID: " + existingId + "). Bấm Next để bỏ qua cảnh báo.");
                        lastWarnedName = name;
                        lastWarnedId = currentId;
                        lastWarnedCategory = categories;
                        return;
                    }
                }
            }

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

            if (prdPrice == null || prdPrice.isBlank() || startTime == null || startTime.isBlank() || duration == null) {
                Status.setVisible(true);
                Status.setManaged(true);
                Status.setTextFill(Color.RED);
                Status.setText("Vui lòng điền đầy đủ giá khởi điểm và thời gian bắt đầu!");
                return;
            }

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
                if (currentSubController instanceof prdOverview) {
                    ((prdOverview) currentSubController).setOnPreviewCallback(() -> {
                        // Khi bấm "Preview", kích hoạt nút "auction_manager" (Nút quản lý đấu giá)
                        // Lệnh này sẽ tự động kích hoạt listener trong initialize() để bật auction_scroll,
                        // auction_stack, ẩn các nút tạo mới và fetch dữ liệu từ Database.
                        if (auction_manager != null) {
                            auction_manager.setSelected(true);
                        }
                    });
                }
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
            NextBtn.setText("Next");
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

            try {
                if (draft.getImgPath() != null && !draft.getImgPath().isBlank()) {
                    newItem.setImgPath(new File(draft.getImgPath()).getName());
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
                return false;
            }

            Auction newAuction = new Auction();
            newAuction.setCurrent_price(newItem.getStarting_price());
            newAuction.setHighest_bidder_id(0);

            LocalDateTime startDateTime = null;
            LocalDateTime endDateTime = null;

            try {
                String[] parts = draft.getStartTime().split(" ");
                String[] dateParts = parts[0].split("-");
                String[] timeParts = parts[1].split(":");

                int sYear = Integer.parseInt(dateParts[0]);
                int sMonth = Integer.parseInt(dateParts[1]);
                int sDay = Integer.parseInt(dateParts[2]);
                int sHour = Integer.parseInt(timeParts[0]);
                int sMinute = Integer.parseInt(timeParts[1]);

                startDateTime = LocalDateTime.of(sYear, sMonth, sDay, sHour, sMinute);

                String[] durParts = draft.getDuration().split(" ");
                int dHour = Integer.parseInt(durParts[0]);
                int dMinute = Integer.parseInt(durParts[2]);

                endDateTime = startDateTime.plusHours(dHour).plusMinutes(dMinute);
            } catch (Exception ex) {
                System.err.println("Lỗi khi parse thời gian: " + ex.getMessage());
                return false;
            }

            newAuction.setStart_time(startDateTime);
            newAuction.setEnd_time(endDateTime);

            if (startDateTime.isAfter(LocalDateTime.now())) {
                newAuction.setStatus("WAITING");
            } else {
                newAuction.setStatus("RUNNING");
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
        Request logoutReq = new Request(null, ActionType.LOGOUT);
        ClientSocket.sendRequest(logoutReq);
        SessionManager.getInstance().logout();

        try {
            sceneSwitcher.switchToMainPage(event);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fetchAuctions() {
        new Thread(() -> {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) return;

            int currentSellerId = currentUser.getId();

            Request req = new Request(null, ActionType.GET_LIST); // Vẫn gọi lấy toàn bộ
            Response res = ClientSocket.sendRequest(req);

            Platform.runLater(() -> {
                clearProductGridAndReleaseResources();

                if (res != null && "SUCCESS".equals(res.getStatus()) && res.getData() != null) {
                    try {
                        List<Auction> auctions = (List<model.Auction>) res.getData();

                        auctions.sort((a1, a2) -> {
                            int weight1 = getAuctionStatusWeight(a1);
                            int weight2 = getAuctionStatusWeight(a2);
                            return Integer.compare(weight1, weight2);
                        });

                        Locale localeVN = new Locale("vi", "VN");
                        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeVN);

                        for (model.Auction auction : auctions) {
                            try {
                                // ---- ĐÂY LÀ ĐOẠN LỌC THỦ CÔNG ---
                                // Nếu ID người bán của phiên đấu giá KHÁC với ID đang đăng nhập thì bỏ qua
                                if (auction.getItem() == null || auction.getItem().getSeller_id() != currentSellerId) {
                                    continue;
                                }

                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/sellerHub/list.fxml"));
                                AnchorPane listNode = loader.load();
                                listController controller = loader.getController();

                                listNode.setUserData(controller);

                                String formattedPrice = currencyFormatter.format(auction.getCurrent_price());

                                String[] rowData = {
                                        String.valueOf(auction.getId()),
                                        auction.getItem().getName(),
                                        String.valueOf(auction.getItem_id()),
                                        String.valueOf(auction.getItem().getSeller_id()),
                                        "",
                                        formattedPrice,
                                        auction.getStatus() != null ? auction.getStatus() : "WAITING"
                                };
                                controller.setRowData(rowData);
                                controller.startCountdown(auction.getStart_time(), auction.getEnd_time());
                                controller.setOnRowClick(() -> {
                                    openAuctionManager(auction);
                                });

                                productGrid.getChildren().add(listNode);
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            });
        }).start();
    }

    private int getAuctionStatusWeight(Auction auction) {
        if (auction == null || auction.getStatus() == null) {
            return 6;
        }

        String status = String.valueOf(auction.getStatus()).toUpperCase();

        if (status.contains("PENDING_APPROVAL")) {
            return 1;
        } else if (status.contains("WAITING")) {
            return 2;
        } else if (status.contains("RUNNING")) {
            return 3;
        } else if (status.contains("FINISHED")) {
            return 4;
        } else if (status.contains("SUSPENDED")) {
            return 5;
        }
        return 6;
    }

    private void clearProductGridAndReleaseResources() {
        if (productGrid != null) {
            for (Node node : productGrid.getChildren()) {
                Object controller = node.getUserData();
                if (controller instanceof listController) {
                    ((listController) controller).stopTimeline();
                }
            }
            productGrid.getChildren().clear();
            System.out.println("[HOMEPAGE] Đã giải phóng toàn bộ bộ đếm thời gian chạy ẩn thành công.");
        }
    }

    private void openAuctionManager(Auction auction) {
        if (auction_manager_paneController != null) {
            auction_manager_paneController.setAuctionData(auction);
        }
        if (auction_manager_pane != null) {
            auction_manager_pane.setVisible(true);
            auction_manager_pane.setManaged(true);
        }
    }
}