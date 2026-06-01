package controller.admin;

import controller.SceneSwitchController;
import controller.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
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
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class homepageController {
    @FXML
    private Circle userAvatar, searchBtn;
    @FXML
    private TextField searchBar; // Thêm search bar
    @FXML
    private ToggleButton Seller, Item, Auction;
    @FXML
    private HBox seller_head, item_head, auction_head;
    @FXML
    private TilePane productGrid;
    @FXML
    private Node auction_manager_pane, item_manager_pane, seller_manager_pane;
    @FXML
    private AuctionManagerController auction_manager_paneController;
    @FXML
    private ItemManagerController item_manager_paneController;
    @FXML
    private SellerManagerController seller_manager_paneController;

    // FXML MenuItems cho User/Seller
    @FXML private MenuItem user_col1_increase, user_col1_decrease;
    @FXML private MenuItem user_col2_increase, user_col2_decrease;
    @FXML private MenuItem user_col3_seller, user_col3_bidder, user_col3_admin, user_col3_all;
    @FXML private MenuItem user_col4_increase, user_col4_decrease;
    @FXML private MenuItem user_col5_increase, user_col5_decrease;
    @FXML private MenuItem user_col6_increase, user_col6_decrease;
    @FXML private MenuItem user_col7_active, user_col7_banned, user_col7_all;

    // FXML MenuItems cho Item
    @FXML private MenuItem item_col1_increase, item_col1_decrease;
    @FXML private MenuItem item_col2_increase, item_col2_decrease;
    @FXML private MenuItem item_col6_increase, item_col6_decrease;
    @FXML private MenuItem item_col7_pending_approval, item_col7_approved, item_col7_rejected, item_col7_all;

    // FXML MenuItems cho Auction
    @FXML private MenuItem auction_col1_increase, auction_col1_decrease;
    @FXML private MenuItem auction_col2_increase, auction_col2_decrease;
    @FXML private MenuItem auction_col5_increase, auction_col5_decrease;
    @FXML private MenuItem auction_col6_increase, auction_col6_decrease;
    @FXML private MenuItem auction_col7_waiting, auction_col7_running, auction_col7_finished, auction_col7_suspended, auction_col7_all;

    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();

    // Bộ nhớ đệm lưu trữ danh sách thô gốc phục vụ Filter/Sort cục bộ
    private List<Map<String, Object>> originalUsersData = new ArrayList<>();
    private List<Auction> originalItemsData = new ArrayList<>();
    private List<Auction> originalAuctionsData = new ArrayList<>();

    // Các biến trạng thái Filter hiện tại để kết hợp khi Sort
    private String currentUserRoleFilter = "ALL";
    private String currentUserStatusFilter = "ALL";
    private String currentItemStatusFilter = "ALL";
    private String currentAuctionStatusFilter = "ALL";
    private String currentSearchKeyword = "";

    public void initialize() {
        item_manager_pane.setManaged(false);
        item_manager_pane.setVisible(false);
        seller_manager_pane.setManaged(false);
        seller_manager_pane.setVisible(false);
        auction_manager_pane.setManaged(false);
        auction_manager_pane.setVisible(false);

        if (searchBar != null) {
            searchBar.setOnAction(e -> handleSearch());
        }
        if (searchBtn != null) {
            searchBtn.setOnMouseClicked(e -> handleSearch());
        }

        try {
            Image usr_img = new Image(getClass().getResourceAsStream("../../image/avatar1.png"));
            userAvatar.setFill(new ImagePattern(usr_img));

            Image search_img = new Image(getClass().getResourceAsStream("../../image/search_icon1.png"));
            searchBtn.setFill(new ImagePattern(search_img));
        } catch (Exception e) {
            System.out.println("Không tìm thấy ảnh avatar, kiểm tra lại đường dẫn!");
        }

        if (item_manager_paneController != null) {
            item_manager_paneController.setOnBack(() -> {
                item_manager_pane.setVisible(false);
                item_manager_pane.setManaged(false);
                fetchItems();
            });
        }
        if (auction_manager_paneController != null) {
            auction_manager_paneController.setOnBack(() -> {
                auction_manager_pane.setVisible(false);
                auction_manager_pane.setManaged(false);
                fetchAuctions();
            });
        }
        if (seller_manager_paneController != null) {
            seller_manager_paneController.setOnBack(() -> {
                seller_manager_pane.setVisible(false);
                seller_manager_pane.setManaged(false);
                fetchSellers();
            });
        }

        ToggleGroup group = new ToggleGroup();
        if (Seller != null) Seller.setToggleGroup(group);
        if (Item != null) Item.setToggleGroup(group);
        if (Auction != null) Auction.setToggleGroup(group);

        group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true);
            } else {
                clearProductGridAndReleaseResources();

                boolean isSeller = (newVal == Seller);
                boolean isItem = (newVal == Item);
                boolean isAuction = (newVal == Auction);

                if (seller_head != null) { seller_head.setVisible(isSeller); seller_head.setManaged(isSeller); }
                if (item_head != null) { item_head.setVisible(isItem); item_head.setManaged(isItem); }
                if (auction_head != null) { auction_head.setVisible(isAuction); auction_head.setManaged(isAuction); }

                if (isSeller) fetchSellers();
                else if (isItem) fetchItems();
                else if (isAuction) fetchAuctions();

                item_manager_pane.setManaged(false);
                item_manager_pane.setVisible(false);
                seller_manager_pane.setManaged(false);
                seller_manager_pane.setVisible(false);
                auction_manager_pane.setManaged(false);
                auction_manager_pane.setVisible(false);
            }
        });

        // Thiết lập sự kiện xử lý click cho các MenuItem
        setupMenuActionListeners();

        if (Seller != null) Seller.setSelected(true);
    }

    private void handleSearch() {
        if (searchBar != null) {
            currentSearchKeyword = searchBar.getText().trim();
        }
        // Trigger re-rendering based on the current tab
        if (Seller.isSelected()) {
            renderSellers(filterSellers());
        } else if (Item.isSelected()) {
            renderItems(filterItems());
        } else if (Auction.isSelected()) {
            renderAuctions(filterAuctions());
        }
    }

    /**
     * Giải phóng tài nguyên và ĐỒNG THỜI đặt lại toàn bộ các bộ lọc,
     * tiêu chí tìm kiếm về trạng thái mặc định ban đầu.
     */
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
        // RESET FILTER & SORT TRẠNG THÁI MẶC ĐỊNH
        currentUserRoleFilter = "ALL";
        currentUserStatusFilter = "ALL";
        currentItemStatusFilter = "ALL";
        currentAuctionStatusFilter = "ALL";
        currentSearchKeyword = "";
        if (searchBar != null) {
            searchBar.clear();
        }
    }

    // =========================================================================
    // LOGIC THIẾT LẬP CÁC SỰ KIỆN CLICK CHO MENU ITEM (SORT / FILTER)
    // =========================================================================
    private void setupMenuActionListeners() {
        // --- 1. USER / SELLER ACTIONS ---
        if (user_col1_increase != null) user_col1_increase.setOnAction(e -> sortSellers((m1, m2) -> Long.compare(getUser(m1).getID(), getUser(m2).getID())));
        if (user_col1_decrease != null) user_col1_decrease.setOnAction(e -> sortSellers((m1, m2) -> Long.compare(getUser(m2).getID(), getUser(m1).getID())));

        if (user_col2_increase != null) user_col2_increase.setOnAction(e -> sortSellers((m1, m2) -> getUser(m1).getUsername().compareToIgnoreCase(getUser(m2).getUsername())));
        if (user_col2_decrease != null) user_col2_decrease.setOnAction(e -> sortSellers((m1, m2) -> getUser(m2).getUsername().compareToIgnoreCase(getUser(m1).getUsername())));

        if (user_col3_seller != null) user_col3_seller.setOnAction(e -> { currentUserRoleFilter = "SELLER"; renderSellers(filterSellers()); });
        if (user_col3_bidder != null) user_col3_bidder.setOnAction(e -> { currentUserRoleFilter = "BIDDER"; renderSellers(filterSellers()); });
        if (user_col3_admin != null) user_col3_admin.setOnAction(e -> { currentUserRoleFilter = "ADMIN"; renderSellers(filterSellers()); });
        if (user_col3_all != null) user_col3_all.setOnAction(e -> { currentUserRoleFilter = "ALL"; renderSellers(filterSellers()); });

        if (user_col4_increase != null) user_col4_increase.setOnAction(e -> sortSellers((m1, m2) -> Integer.compare(((Number)m1.getOrDefault("itemsCount", 0)).intValue(), ((Number)m2.getOrDefault("itemsCount", 0)).intValue())));
        if (user_col4_decrease != null) user_col4_decrease.setOnAction(e -> sortSellers((m1, m2) -> Integer.compare(((Number)m2.getOrDefault("itemsCount", 0)).intValue(), ((Number)m1.getOrDefault("itemsCount", 0)).intValue())));

        if (user_col5_increase != null) user_col5_increase.setOnAction(e -> sortSellers((m1, m2) -> Integer.compare(((Number)m1.getOrDefault("auctionsCount", 0)).intValue(), ((Number)m2.getOrDefault("auctionsCount", 0)).intValue())));
        if (user_col5_decrease != null) user_col5_decrease.setOnAction(e -> sortSellers((m1, m2) -> Integer.compare(((Number)m2.getOrDefault("auctionsCount", 0)).intValue(), ((Number)m1.getOrDefault("auctionsCount", 0)).intValue())));

        if (user_col6_increase != null) user_col6_increase.setOnAction(e -> sortSellers((m1, m2) -> Integer.compare(((Number)m1.getOrDefault("warningsCount", 0)).intValue(), ((Number)m2.getOrDefault("warningsCount", 0)).intValue())));
        if (user_col6_decrease != null) user_col6_decrease.setOnAction(e -> sortSellers((m1, m2) -> Integer.compare(((Number)m2.getOrDefault("warningsCount", 0)).intValue(), ((Number)m1.getOrDefault("warningsCount", 0)).intValue())));

        if (user_col7_active != null) user_col7_active.setOnAction(e -> { currentUserStatusFilter = "ACTIVE"; renderSellers(filterSellers()); });
        if (user_col7_banned != null) user_col7_banned.setOnAction(e -> { currentUserStatusFilter = "BANNED"; renderSellers(filterSellers()); });
        if (user_col7_all != null) user_col7_all.setOnAction(e -> { currentUserStatusFilter = "ALL"; renderSellers(filterSellers()); });

        // --- 2. ITEM ACTIONS ---
        if (item_col1_increase != null) item_col1_increase.setOnAction(e -> sortItems((a1, a2) -> Integer.compare(a1.getItem().getId(), a2.getItem().getId())));
        if (item_col1_decrease != null) item_col1_decrease.setOnAction(e -> sortItems((a1, a2) -> Integer.compare(a2.getItem().getId(), a1.getItem().getId())));

        if (item_col2_increase != null) item_col2_increase.setOnAction(e -> sortItems((a1, a2) -> a1.getItem().getName().compareToIgnoreCase(a2.getItem().getName())));
        if (item_col2_decrease != null) item_col2_decrease.setOnAction(e -> sortItems((a1, a2) -> a2.getItem().getName().compareToIgnoreCase(a1.getItem().getName())));

        if (item_col6_increase != null) item_col6_increase.setOnAction(e -> sortItems((a1, a2) -> Double.compare(a1.getItem().getStarting_price(), a2.getItem().getStarting_price())));
        if (item_col6_decrease != null) item_col6_decrease.setOnAction(e -> sortItems((a1, a2) -> Double.compare(a2.getItem().getStarting_price(), a1.getItem().getStarting_price())));

        if (item_col7_pending_approval != null) item_col7_pending_approval.setOnAction(e -> { currentItemStatusFilter = "PENDING"; renderItems(filterItems()); });
        if (item_col7_approved != null) item_col7_approved.setOnAction(e -> { currentItemStatusFilter = "APPROVE"; renderItems(filterItems()); });
        if (item_col7_rejected != null) item_col7_rejected.setOnAction(e -> { currentItemStatusFilter = "REJECT"; renderItems(filterItems()); });
        if (item_col7_all != null) item_col7_all.setOnAction(e -> { currentItemStatusFilter = "ALL"; renderItems(filterItems()); });

        // --- 3. AUCTION ACTIONS ---
        if (auction_col1_increase != null) auction_col1_increase.setOnAction(e -> sortAuctions((a1, a2) -> Integer.compare(a1.getId(), a2.getId())));
        if (auction_col1_decrease != null) auction_col1_decrease.setOnAction(e -> sortAuctions((a1, a2) -> Integer.compare(a2.getId(), a1.getId())));

        if (auction_col2_increase != null) auction_col2_increase.setOnAction(e -> sortAuctions((a1, a2) -> getAuctionName(a1).compareToIgnoreCase(getAuctionName(a2))));
        if (auction_col2_decrease != null) auction_col2_decrease.setOnAction(e -> sortAuctions((a1, a2) -> getAuctionName(a2).compareToIgnoreCase(getAuctionName(a1))));

        if (auction_col5_increase != null) auction_col5_increase.setOnAction(e -> sortAuctions((a1, a2) -> compareTimes(a1.getStart_time(), a2.getStart_time())));
        if (auction_col5_decrease != null) auction_col5_decrease.setOnAction(e -> sortAuctions((a1, a2) -> compareTimes(a2.getStart_time(), a1.getStart_time())));

        if (auction_col6_increase != null) auction_col6_increase.setOnAction(e -> sortAuctions((a1, a2) -> Double.compare(a1.getCurrent_price(), a2.getCurrent_price())));
        if (auction_col6_decrease != null) auction_col6_decrease.setOnAction(e -> sortAuctions((a1, a2) -> Double.compare(a2.getCurrent_price(), a1.getCurrent_price())));

        if (auction_col7_waiting != null) auction_col7_waiting.setOnAction(e -> { currentAuctionStatusFilter = "WAITING"; renderAuctions(filterAuctions()); });
        if (auction_col7_running != null) auction_col7_running.setOnAction(e -> { currentAuctionStatusFilter = "RUNNING"; renderAuctions(filterAuctions()); });
        if (auction_col7_finished != null) auction_col7_finished.setOnAction(e -> { currentAuctionStatusFilter = "FINISHED"; renderAuctions(filterAuctions()); });
        if (auction_col7_suspended != null) auction_col7_suspended.setOnAction(e -> { currentAuctionStatusFilter = "SUSPENDED"; renderAuctions(filterAuctions()); });
        if (auction_col7_all != null) auction_col7_all.setOnAction(e -> { currentAuctionStatusFilter = "ALL"; renderAuctions(filterAuctions()); });
    }

    private User getUser(Map<String, Object> map) { return (User) map.get("user"); }
    private String getAuctionName(Auction a) { return a.getItem() != null ? a.getItem().getName() : "Unknown"; }
    private int compareTimes(LocalDateTime t1, LocalDateTime t2) {
        if (t1 == null && t2 == null) return 0;
        if (t1 == null) return 1;
        if (t2 == null) return -1;
        return t1.compareTo(t2);
    }

    // ==========================================
    // LOGIC LIÊN QUAN TỚI FETCH VÀ RENDER SELLER
    // ==========================================
    private void fetchSellers() {
        new Thread(() -> {
            Request req = new Request(null, ActionType.ADMIN_GET_ALL_USERS);
            Response res = ClientSocket.sendRequest(req);
            Platform.runLater(() -> {
                if (res != null && "SUCCESS".equals(res.getStatus()) && res.getData() != null) {
                    originalUsersData = (List<Map<String, Object>>) res.getData();
                    renderSellers(filterSellers());
                }
            });
        }).start();
    }

    private List<Map<String, Object>> filterSellers() {
        return originalUsersData.stream().filter(map -> {
            User u = getUser(map);
            if (u == null) return false;
            boolean matchesRole = "ALL".equals(currentUserRoleFilter) || currentUserRoleFilter.equalsIgnoreCase(u.getRole());
            String status = u.getStatus() != null ? u.getStatus() : "ACTIVE";
            boolean matchesStatus = "ALL".equals(currentUserStatusFilter) || currentUserStatusFilter.equalsIgnoreCase(status);
            boolean matchesSearch = currentSearchKeyword.isEmpty() ||
                    (u.getUsername() != null && u.getUsername().toLowerCase().contains(currentSearchKeyword.toLowerCase())) ||
                    String.valueOf(u.getID()).contains(currentSearchKeyword);
            return matchesRole && matchesStatus && matchesSearch;
        }).collect(Collectors.toList());
    }

    private void sortSellers(java.util.Comparator<Map<String, Object>> comparator) {
        List<Map<String, Object>> filtered = filterSellers();
        filtered.sort(comparator);
        renderSellers(filtered);
    }

    private void renderSellers(List<Map<String, Object>> usersData) {
        clearProductGridAndReleaseResources();
        for (Map<String, Object> userDataMap : usersData) {
            try {
                User user = getUser(userDataMap);
                if (user == null) continue;

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin/list.fxml"));
                AnchorPane listNode = loader.load();
                listController controller = loader.getController();
                listNode.setUserData(controller);

                int itemsCount = ((Number) userDataMap.getOrDefault("itemsCount", 0)).intValue();
                int auctionsCount = ((Number) userDataMap.getOrDefault("auctionsCount", 0)).intValue();
                int warningsCount = ((Number) userDataMap.getOrDefault("warningsCount", 0)).intValue();

                String[] rowData = {
                        String.valueOf(user.getID()), user.getUsername(), user.getRole(),
                        String.valueOf(itemsCount), String.valueOf(auctionsCount), String.valueOf(warningsCount),
                        user.getStatus() != null ? user.getStatus() : "ACTIVE"
                };
                controller.setRowData(rowData);
                controller.setOnRowClick(() -> openUserManager(userDataMap));
                productGrid.getChildren().add(listNode);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // ==========================================
    // LOGIC LIÊN QUAN TỚI FETCH VÀ RENDER ITEMS
    // ==========================================
    private void fetchItems() {
        new Thread(() -> {
            Request req = new Request(null, ActionType.GET_LIST);
            Response res = ClientSocket.sendRequest(req);
            Platform.runLater(() -> {
                if (res != null && "SUCCESS".equals(res.getStatus()) && res.getData() != null) {
                    originalItemsData = (List<Auction>) res.getData();
                    // Sắp xếp mặc định theo trọng số duyệt tin và thời gian
                    originalItemsData.sort((a1, a2) -> {
                        int w1 = getModerationWeight(a1.getItem());
                        int w2 = getModerationWeight(a2.getItem());
                        if (w1 != w2) return Integer.compare(w1, w2);
                        return compareTimes(a1.getStart_time(), a2.getStart_time());
                    });
                    renderItems(filterItems());
                }
            });
        }).start();
    }

    private List<Auction> filterItems() {
        return originalItemsData.stream().filter(auc -> {
            Item item = auc.getItem();
            if (item == null) return false;
            boolean statusMatch = "ALL".equals(currentItemStatusFilter) ||
                    (item.getModeration_status() != null && String.valueOf(item.getModeration_status()).toUpperCase().contains(currentItemStatusFilter));
            boolean searchMatch = currentSearchKeyword.isEmpty() ||
                    (item.getName() != null && item.getName().toLowerCase().contains(currentSearchKeyword.toLowerCase())) ||
                    (item.getUser_prdID() != null && item.getUser_prdID().toLowerCase().contains(currentSearchKeyword.toLowerCase())) ||
                    String.valueOf(item.getId()).contains(currentSearchKeyword);
            return statusMatch && searchMatch;
        }).collect(Collectors.toList());
    }

    private void sortItems(java.util.Comparator<Auction> comparator) {
        List<Auction> filtered = filterItems();
        filtered.sort(comparator);
        renderItems(filtered);
    }

    private void renderItems(List<Auction> auctionList) {
        clearProductGridAndReleaseResources();
        Locale localeVN = new Locale("vi", "VN");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeVN);

        for (Auction auc : auctionList) {
            try {
                Item item = auc.getItem();
                if (item == null) continue;

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin/list.fxml"));
                AnchorPane listNode = loader.load();
                listController controller = loader.getController();
                listNode.setUserData(controller);

                String formattedPrice = currencyFormatter.format(item.getStarting_price());
                String[] rowData = {
                        String.valueOf(item.getId()), item.getName(),
                        item.getUser_prdID() != null ? item.getUser_prdID() : "N/A",
                        String.valueOf(item.getSeller_id()), String.valueOf(auc.getId()),
                        formattedPrice, String.valueOf(item.getModeration_status())
                };
                controller.setRowData(rowData);
                controller.setOnRowClick(() -> openItemManager(auc));
                productGrid.getChildren().add(listNode);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private int getModerationWeight(Item item) {
        if (item == null || item.getModeration_status() == null) return 4;
        String status = String.valueOf(item.getModeration_status()).toUpperCase();
        if (status.contains("PENDING")) return 1;
        if (status.contains("APPROVE")) return 2;
        if (status.contains("REJECT")) return 3;
        return 4;
    }

    // ==========================================
    // LOGIC LIÊN QUAN TỚI FETCH VÀ RENDER AUCTIONS
    // ==========================================
    private void fetchAuctions() {
        new Thread(() -> {
            Request req = new Request(null, ActionType.GET_LIST);
            Response res = ClientSocket.sendRequest(req);
            Platform.runLater(() -> {
                if (res != null && "SUCCESS".equals(res.getStatus()) && res.getData() != null) {
                    originalAuctionsData = (List<Auction>) res.getData();
                    // Sắp xếp mặc định ban đầu theo trạng thái đấu giá
                    originalAuctionsData.sort((a1, a2) -> {
                        int w1 = getAuctionStatusWeight(a1);
                        int w2 = getAuctionStatusWeight(a2);
                        if (w1 != w2) return Integer.compare(w1, w2);
                        return compareTimes(a1.getStart_time(), a2.getStart_time());
                    });
                    renderAuctions(filterAuctions());
                }
            });
        }).start();
    }

    private List<Auction> filterAuctions() {
        return originalAuctionsData.stream().filter(auc -> {
            if (auc == null) return false;
            boolean statusMatch = "ALL".equals(currentAuctionStatusFilter) ||
                    (auc.getStatus() != null && auc.getStatus().toUpperCase().contains(currentAuctionStatusFilter));
            boolean searchMatch = currentSearchKeyword.isEmpty() ||
                    (getAuctionName(auc) != null && getAuctionName(auc).toLowerCase().contains(currentSearchKeyword.toLowerCase())) ||
                    String.valueOf(auc.getId()).contains(currentSearchKeyword);
            return statusMatch && searchMatch;
        }).collect(Collectors.toList());
    }

    private void sortAuctions(java.util.Comparator<Auction> comparator) {
        List<Auction> filtered = filterAuctions();
        filtered.sort(comparator);
        renderAuctions(filtered);
    }

    private void renderAuctions(List<Auction> auctions) {
        clearProductGridAndReleaseResources();
        Locale localeVN = new Locale("vi", "VN");
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeVN);

        for (Auction auction : auctions) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin/list.fxml"));
                AnchorPane listNode = loader.load();
                listController controller = loader.getController();
                listNode.setUserData(controller);

                String formattedPrice = currencyFormatter.format(auction.getCurrent_price());
                String[] rowData = {
                        String.valueOf(auction.getId()), getAuctionName(auction),
                        String.valueOf(auction.getItem_id()),
                        auction.getItem() != null ? String.valueOf(auction.getItem().getSeller_id()) : "Unknown",
                        "", formattedPrice, auction.getStatus() != null ? auction.getStatus() : "WAITING"
                };
                controller.setRowData(rowData);
                controller.startCountdown(auction.getStart_time(), auction.getEnd_time());
                controller.setOnRowClick(() -> openAuctionManager(auction));
                productGrid.getChildren().add(listNode);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private int getAuctionStatusWeight(Auction auction) {
        if (auction == null || auction.getStatus() == null) return 6;
        String status = String.valueOf(auction.getStatus()).toUpperCase();
        if (status.contains("PENDING_APPROVAL")) return 1;
        if (status.contains("WAITING")) return 2;
        if (status.contains("RUNNING")) return 3;
        if (status.contains("FINISHED")) return 4;
        if (status.contains("SUSPENDED")) return 5;
        return 6;
    }

    // ==========================================
    // CÁC PHƯƠNG THỨC CHUYỂN SCENE & HIỂN THỊ KHÁC
    // ==========================================
    public void handleGoToBidHub(MouseEvent event) {
        clearProductGridAndReleaseResources();
        Request logoutReq = new Request(null, ActionType.LOGOUT);
        ClientSocket.sendRequest(logoutReq);
        SessionManager.getInstance().logout();
        try {
            sceneSwitcher.switchToWelcome(event);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void handleBidHub(MouseEvent event) {
        if (Seller != null) {
            Seller.setSelected(true);
        }
    }

    private void openItemManager(Auction auction) {
        if (item_manager_paneController != null) item_manager_paneController.setItemData(auction);
        item_manager_pane.setManaged(true); item_manager_pane.setVisible(true);
        seller_manager_pane.setManaged(false); seller_manager_pane.setVisible(false);
        auction_manager_pane.setManaged(false); auction_manager_pane.setVisible(false);
    }

    private void openUserManager(Map<String, Object> userDataMap) {
        if (seller_manager_paneController != null) seller_manager_paneController.setUserData(userDataMap);
        seller_manager_pane.setManaged(true); seller_manager_pane.setVisible(true);
        item_manager_pane.setManaged(false); item_manager_pane.setVisible(false);
        auction_manager_pane.setManaged(false); auction_manager_pane.setVisible(false);
    }

    private void openAuctionManager(Auction auction) {
        if (auction_manager_paneController != null) auction_manager_paneController.setAuctionData(auction);
        auction_manager_pane.setManaged(true); auction_manager_pane.setVisible(true);
        item_manager_pane.setManaged(false); item_manager_pane.setVisible(false);
        seller_manager_pane.setManaged(false); seller_manager_pane.setVisible(false);
    }
}