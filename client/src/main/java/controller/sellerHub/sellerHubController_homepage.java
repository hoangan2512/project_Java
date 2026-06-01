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
import javafx.stage.Stage;
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
    @FXML
    private MenuItem col1_increase, col1_decrease, col2_increase, col2_decrease, col5_increase, col5_decrease, col6_increase, col6_decrease, col7_waiting, col7_running, col7_finished, col7_suspended, col7_pending_approval, col7_all;

    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();
    private Object currentSubController;
    private ProductDraftDTO currentDraft = new ProductDraftDTO();

    // --- BIẾN LƯU TRẠNG THÁI SẮP XẾP VÀ LỌC DANH SÁCH ---
    private List<Auction> masterAuctionList = new java.util.ArrayList<>(); // Lưu bản gốc từ Server về để sort/filter offline không cần kéo lại mạng
    private String currentSortField = "STATUS_DEFAULT"; // ID, NAME, TIME, PRICE, STATUS_DEFAULT
    private boolean isAscending = true;
    private String currentStatusFilter = "ALL";
    private String currentSearchKeyword = ""; // Thêm biến lưu từ khóa search

    // --- BIẾN GHI NHỚ ĐỂ BYPASS CẢNH BÁO ---
    private String lastWarnedName = "";
    private String lastWarnedId = "";
    private String lastWarnedCategory = "";

    @FXML
    public void initialize() {
        // --- Cột 1: ID ---
        if (col1_increase != null) col1_increase.setOnAction(e -> { currentSortField = "ID"; isAscending = true; applySortAndFilter(); });
        if (col1_decrease != null) col1_decrease.setOnAction(e -> { currentSortField = "ID"; isAscending = false; applySortAndFilter(); });

        // --- Cột 2: Product Name ---
        if (col2_increase != null) col2_increase.setOnAction(e -> { currentSortField = "NAME"; isAscending = true; applySortAndFilter(); });
        if (col2_decrease != null) col2_decrease.setOnAction(e -> { currentSortField = "NAME"; isAscending = false; applySortAndFilter(); });

        // --- Cột 5: Current Time (Thời gian bắt đầu) ---
        if (col5_increase != null) col5_increase.setOnAction(e -> { currentSortField = "TIME"; isAscending = true; applySortAndFilter(); });
        if (col5_decrease != null) col5_decrease.setOnAction(e -> { currentSortField = "TIME"; isAscending = false; applySortAndFilter(); });

        // --- Cột 6: Current Price ---
        if (col6_increase != null) col6_increase.setOnAction(e -> { currentSortField = "PRICE"; isAscending = true; applySortAndFilter(); });
        if (col6_decrease != null) col6_decrease.setOnAction(e -> { currentSortField = "PRICE"; isAscending = false; applySortAndFilter(); });

        // --- Cột 7: Lọc theo trạng thái (Filter Status) [ĐÃ BỔ SUNG ALL & PENDING_APPROVAL] ---
        if (col7_all != null) col7_all.setOnAction(e -> { currentStatusFilter = "ALL"; currentSortField = "STATUS_DEFAULT"; applySortAndFilter(); });
        if (col7_pending_approval != null) col7_pending_approval.setOnAction(e -> { currentStatusFilter = "PENDING_APPROVAL"; currentSortField = "STATUS_DEFAULT"; applySortAndFilter(); });
        if (col7_waiting != null) col7_waiting.setOnAction(e -> { currentStatusFilter = "WAITING"; currentSortField = "STATUS_DEFAULT"; applySortAndFilter(); });
        if (col7_running != null) col7_running.setOnAction(e -> { currentStatusFilter = "RUNNING"; currentSortField = "STATUS_DEFAULT"; applySortAndFilter(); });
        if (col7_finished != null) col7_finished.setOnAction(e -> { currentStatusFilter = "FINISHED"; currentSortField = "STATUS_DEFAULT"; applySortAndFilter(); });
        if (col7_suspended != null) col7_suspended.setOnAction(e -> { currentStatusFilter = "SUSPENDED"; currentSortField = "STATUS_DEFAULT"; applySortAndFilter(); });

        // Event listener cho Search Bar (ấn Enter)
        if (searchBar != null) {
            searchBar.setOnAction(e -> handleSearchBtnClick(null));
        }

        try {
            Image usr_img = new Image(getClass().getResourceAsStream("../../image/avatar1.png"));
            userAvatar.setFill(new ImagePattern(usr_img));

            Image search_img = new Image(getClass().getResourceAsStream("../../image/search_icon1.png"));
            searchBtn.setFill(new ImagePattern(search_img));
            
            // Allow clicking the search icon to search as well
            searchBtn.setOnMouseClicked(this::handleSearchBtnClick);
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

        if (auction_manager_paneController != null) {
            auction_manager_paneController.setOnBack(() -> {
                if (auction_manager_pane != null) {
                    auction_manager_pane.setVisible(false);
                    auction_manager_pane.setManaged(false);
                }
                fetchAuctions();
            });
        }

        ToggleGroup actionGroup = new ToggleGroup();

        if (list_new_item != null) {
            list_new_item.setToggleGroup(actionGroup);
            list_new_item.setSelected(true);
        }

        if (auction_manager != null) {
            auction_manager.setToggleGroup(actionGroup);
        }

        if (auction_scroll != null && auction_stack != null) {
            auction_scroll.setVisible(false);
            auction_scroll.setManaged(false);
            auction_stack.setVisible(false);
            auction_stack.setManaged(false);
        }

        actionGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                oldValue.setSelected(true);
                return;
            }

            if (newValue == list_new_item) {
                contentArea.setVisible(true);
                contentArea.setManaged(true);
                if (NextBtn != null) { NextBtn.setVisible(true); NextBtn.setManaged(true); }
                if (backBtn != null) { backBtn.setVisible(true); backBtn.setManaged(true); }

                if (auction_scroll != null) { auction_scroll.setVisible(false); auction_scroll.setManaged(false); }
                if (auction_stack != null) { auction_stack.setVisible(false); auction_stack.setManaged(false); }

            } else if (newValue == auction_manager) {
                contentArea.setVisible(false);
                contentArea.setManaged(false);
                if (NextBtn != null) { NextBtn.setVisible(false); NextBtn.setManaged(false); }
                if (backBtn != null) { backBtn.setVisible(false); backBtn.setManaged(false); }
                Status.setVisible(false);

                if (auction_scroll != null) { auction_scroll.setVisible(true); auction_scroll.setManaged(true); }
                if (auction_stack != null) { auction_stack.setVisible(true); auction_stack.setManaged(true); }

                if (auction_manager_pane != null) {
                    auction_manager_pane.setVisible(false);
                    auction_manager_pane.setManaged(false);
                }

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
                        if (auction_manager != null) {
                            auction_manager.setSelected(true);
                        }
                    });
                }
                NextBtn.setText("List new item");
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
                int dDay = Integer.parseInt(durParts[0]);
                int dHour = Integer.parseInt(durParts[2]);
                int dMinute = Integer.parseInt(durParts[4]);

                endDateTime = startDateTime.plusDays(dDay).plusHours(dHour).plusMinutes(dMinute);
            } catch (Exception ex) {
                System.err.println("Lỗi khi parse thời gian: " + ex.getMessage());
                return false;
            }

            newAuction.setStart_time(startDateTime);
            newAuction.setEnd_time(endDateTime);

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
        if (searchBar != null) {
            String searchText = searchBar.getText();
            if (searchText == null || searchText.trim().isEmpty()) {
                currentSearchKeyword = "";
                searchBar.clear();
            } else {
                currentSearchKeyword = searchText.trim();
            }
            applySortAndFilter();
        }
    }

    public void handleBidHub(MouseEvent event) {
        Request logoutReq = new Request(null, ActionType.LOGOUT);
        ClientSocket.sendRequest(logoutReq);
        SessionManager.getInstance().logout();
        Stage popupStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        try {
            sceneSwitcher.switchToWelcome(event);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fetchAuctions() {
        new Thread(() -> {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) return;

            Request req = new Request(null, ActionType.GET_LIST);
            Response res = ClientSocket.sendRequest(req);

            Platform.runLater(() -> {
                // KHI NẠP MỚI HOÀN TOÀN TỪ SERVER: Reset bộ lọc tổng thể
                clearProductGridAndReleaseResources();

                if (res != null && "SUCCESS".equals(res.getStatus()) && res.getData() != null) {
                    try {
                        masterAuctionList = (List<Auction>) res.getData();
                        applySortAndFilter();
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

    private void applySortAndFilter() {
        if (masterAuctionList == null || masterAuctionList.isEmpty()) return;

        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;
        int currentSellerId = currentUser.getId();

        // CHỈ XOÁ UI CŨ TRÊN GRID, Không reset biến trạng thái để tính năng Sắp xếp/Lọc hoạt động tốt
        clearGridUIOnly();

        // LỌC 1: Chỉ lấy sản phẩm của Seller đang đăng nhập và lọc theo Status được chọn (Bao gồm ALL và PENDING_APPROVAL)
        List<Auction> filteredList = masterAuctionList.stream()
                .filter(a -> a.getItem() != null && a.getItem().getSeller_id() == currentSellerId)
                .filter(a -> "ALL".equalsIgnoreCase(currentStatusFilter) ||
                        (a.getStatus() != null && a.getStatus().equalsIgnoreCase(currentStatusFilter)))
                .filter(a -> currentSearchKeyword.isEmpty() ||
                        (a.getItem().getName() != null && a.getItem().getName().toLowerCase().contains(currentSearchKeyword.toLowerCase())) ||
                        (a.getItem().getUser_prdID() != null && a.getItem().getUser_prdID().toLowerCase().contains(currentSearchKeyword.toLowerCase())))
                .collect(java.util.stream.Collectors.toList());

        // LỌC 2: Tiến hành Sắp xếp (Sort) dữ liệu dựa trên thuộc tính được chọn
        filteredList.sort((a1, a2) -> {
            int result = 0;
            switch (currentSortField) {
                case "ID":
                    result = Integer.compare(a1.getId(), a2.getId());
                    break;
                case "NAME":
                    String name1 = a1.getItem() != null && a1.getItem().getName() != null ? a1.getItem().getName() : "";
                    String name2 = a2.getItem() != null && a2.getItem().getName() != null ? a2.getItem().getName() : "";
                    result = name1.compareToIgnoreCase(name2);
                    break;
                case "TIME":
                    LocalDateTime time1 = a1.getStart_time();
                    LocalDateTime time2 = a2.getStart_time();
                    if (time1 == null) return 1;
                    if (time2 == null) return -1;
                    result = time1.compareTo(time2);
                    break;
                case "PRICE":
                    result = Double.compare(a1.getCurrent_price(), a2.getCurrent_price());
                    break;
                case "STATUS_DEFAULT":
                default:
                    int weight1 = getAuctionStatusWeight(a1);
                    int weight2 = getAuctionStatusWeight(a2);
                    if (weight1 != weight2) {
                        result = Integer.compare(weight1, weight2);
                    } else {
                        LocalDateTime t1 = a1.getStart_time();
                        LocalDateTime t2 = a2.getStart_time();
                        result = (t1 != null && t2 != null) ? t1.compareTo(t2) : 0;
                    }
                    return result;
            }
            return isAscending ? result : -result;
        });

        // HIỂN THỊ LÊN GIAO DIỆN
        Locale localeVN = new Locale("vi", "VN");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeVN);

        for (Auction auction : filteredList) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/sellerHub/list.fxml"));
                AnchorPane listNode = loader.load();
                listController controller = loader.getController();
                listNode.setUserData(controller);

                String formattedPrice = currencyFormatter.format(auction.getCurrent_price());
                String prdId = (auction.getItem().getUser_prdID() != null && !auction.getItem().getUser_prdID().isEmpty())
                        ? auction.getItem().getUser_prdID()
                        : String.valueOf(auction.getItem_id());

                String[] rowData = {
                        String.valueOf(auction.getId()),
                        auction.getItem().getName() != null ? auction.getItem().getName() : "Unknown",
                        auction.getItem().getUser_prdID() != null ? auction.getItem().getUser_prdID() : "Unknown",
                        prdId,
                        "",
                        formattedPrice,
                        auction.getStatus() != null ? auction.getStatus() : "WAITING"
                };

                controller.setRowData(rowData);
                controller.startCountdown(auction.getStart_time(), auction.getEnd_time());
                controller.setOnRowClick(() -> openAuctionManager(auction));

                productGrid.getChildren().add(listNode);
            } catch (Exception e) {
                System.err.println("Lỗi hiển thị dòng sau khi sort: " + e.getMessage());
            }
        }
    }

    // Hàm dọn dẹp UI thuần túy để chuẩn bị vẽ các Grid đã được sắp xếp hoặc lọc
    private void clearGridUIOnly() {
        if (productGrid != null) {
            for (Node node : productGrid.getChildren()) {
                Object controller = node.getUserData();
                if (controller instanceof listController) {
                    ((listController) controller).stopTimeline();
                }
            }
            productGrid.getChildren().clear();
        }
    }

    // Hàm dọn dẹp tổng lực: Vừa xóa UI vừa reset cứng bộ lọc về mặc định ban đầu
    private void clearProductGridAndReleaseResources() {
        // 1. Reset các biến trạng thái Sort và Filter về mặc định
        this.currentSortField = "STATUS_DEFAULT";
        this.isAscending = true;
        this.currentStatusFilter = "ALL";
        this.currentSearchKeyword = "";
        if (searchBar != null) {
            searchBar.clear();
        }

        // 2. Dọn sạch UI
        clearGridUIOnly();
        System.out.println("[HOMEPAGE] Đã giải phóng bộ đếm và làm sạch toàn bộ bộ lọc (Filter/Sort).");
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