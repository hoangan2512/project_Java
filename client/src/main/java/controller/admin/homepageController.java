package controller.admin;

import controller.SceneSwitchController;
import controller.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import message.Request;
import message.Response;
import model.ActionType;
import model.User;
import model.Item;
import model.Auction;
import network.ClientSocket;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class homepageController {
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

    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();

    public void initialize() {
        item_manager_pane.setManaged(false);
        item_manager_pane.setVisible(false);
        seller_manager_pane.setManaged(false);
        seller_manager_pane.setVisible(false);
        auction_manager_pane.setManaged(false);
        auction_manager_pane.setVisible(false);

        if (item_manager_paneController != null) {
            item_manager_paneController.setOnBack(() -> {
                item_manager_pane.setVisible(false);
                item_manager_pane.setManaged(false);
            });
        }
        if (auction_manager_paneController != null) {
            auction_manager_paneController.setOnBack(() -> {
                auction_manager_pane.setVisible(false);
                auction_manager_pane.setManaged(false);
            });
        }
        if (seller_manager_paneController != null) {
            seller_manager_paneController.setOnBack(() -> {
                seller_manager_pane.setVisible(false);
                seller_manager_pane.setManaged(false);
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

        if (Seller != null) Seller.setSelected(true);
    }

    /**
     * Hàm tiện ích xử lý việc dọn dẹp lưới và hủy các bộ đếm thời gian đang chạy
     */
    private void clearProductGridAndReleaseResources() {
        if (productGrid != null) {
            // Duyệt qua tất cả các dòng (Node) hiện tại trong lưới hiển thị
            for (Node node : productGrid.getChildren()) {
                // Trích xuất thuộc tính controller lưu trữ trong Node (nếu có)
                Object controller = node.getUserData();
                if (controller instanceof listController) {
                    // Ép kiểu và ra lệnh cho dòng đó dừng ngay lập tức Timeline chạy ẩn
                    ((listController) controller).stopTimeline();
                }
            }
            // Sau khi đã dập tắt toàn bộ luồng chạy ẩn, tiến hành xóa sạch các Node con
            productGrid.getChildren().clear();
            System.out.println("[HOMEPAGE] Đã giải phóng toàn bộ bộ đếm thời gian chạy ẩn thành công.");
        }
    }

    private void fetchSellers() {
        new Thread(() -> {
            Request req = new Request(null, ActionType.ADMIN_GET_ALL_USERS);
            Response res = ClientSocket.sendRequest(req);
            Platform.runLater(() -> {
                // Cần dọn dẹp an toàn luồng cũ (nếu có tác vụ chạy trùng lặp)
                clearProductGridAndReleaseResources();

                if (res != null && "SUCCESS".equals(res.getStatus()) && res.getData() != null) {
                    try {
                        List<Map<String, Object>> usersData = (List<Map<String, Object>>) res.getData();
                        for (Map<String, Object> userDataMap : usersData) {
                            try {
                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin/list.fxml"));
                                AnchorPane listNode = loader.load();
                                listController controller = loader.getController();

                                // Đính kèm controller vào Node để sau này hàm clear dữ liệu tìm thấy và hủy Timeline
                                listNode.setUserData(controller);

                                User user = (User) userDataMap.get("user");
                                if (user != null) {
                                    int itemsCount = ((Number) userDataMap.getOrDefault("itemsCount", 0)).intValue();
                                    int auctionsCount = ((Number) userDataMap.getOrDefault("auctionsCount", 0)).intValue();
                                    int warningsCount = ((Number) userDataMap.getOrDefault("warningsCount", 0)).intValue();

                                    String[] rowData = {
                                            String.valueOf(user.getID()),
                                            user.getUsername(),
                                            user.getRole(),
                                            String.valueOf(itemsCount),
                                            String.valueOf(auctionsCount),
                                            String.valueOf(warningsCount),
                                            user.getStatus() != null ? user.getStatus() : "ACTIVE"
                                    };
                                    controller.setRowData(rowData);
                                    controller.setOnRowClick(() -> {
                                        openUserManager(userDataMap);
                                    });
                                    productGrid.getChildren().add(listNode);
                                }
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            });
        }).start();
    }

    private void fetchItems() {
        new Thread(() -> {
            Request req = new Request(null, ActionType.GET_LIST);
            Response res = ClientSocket.sendRequest(req);

            Platform.runLater(() -> {
                clearProductGridAndReleaseResources();

                if (res != null && "SUCCESS".equals(res.getStatus()) && res.getData() != null) {
                    try {
                        List<Auction> auctionList = (List<Auction>) res.getData();

                        // SẮP XẾP DANH SÁCH THEO THỨ TỰ: PENDING -> APPROVED -> REJECTED
                        auctionList.sort((a1, a2) -> {
                            int weight1 = getModerationWeight(a1.getItem());
                            int weight2 = getModerationWeight(a2.getItem());
                            return Integer.compare(weight1, weight2);
                        });

                        Locale localeVN = new Locale("vi", "VN");
                        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeVN);

                        for (Auction auc : auctionList) {
                            try {
                                Item item = auc.getItem();
                                if (item == null) continue;

                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin/list.fxml"));
                                AnchorPane listNode = loader.load();
                                listController controller = loader.getController();

                                // Đính kèm controller vào Node để phục vụ việc giải phóng tài nguyên
                                listNode.setUserData(controller);

                                String formattedPrice = currencyFormatter.format(item.getStarting_price());

                                String[] rowData = {
                                        String.valueOf(item.getId()),
                                        item.getName(),
                                        item.getUser_prdID() != null ? item.getUser_prdID() : "N/A",
                                        String.valueOf(item.getSeller_id()),
                                        String.valueOf(auc.getId()),
                                        formattedPrice,
                                        String.valueOf(item.getModeration_status())
                                };
                                controller.setRowData(rowData);

                                controller.setOnRowClick(() -> {
                                    System.out.println("[ADMIN] Đang chọn quản lý Item ID: " + item.getId());
                                    openItemManager(auc);
                                });

                                productGrid.getChildren().add(listNode);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }).start();
    }

    private int getModerationWeight(Item item) {
        if (item == null || item.getModeration_status() == null) {
            return 4; // Nếu null hoặc không xác định thì ném xuống cuối
        }

        // Chuyển về viết hoa toàn bộ để dễ so sánh, tránh lỗi do sai khác chữ hoa/thường
        String status = String.valueOf(item.getModeration_status()).toUpperCase();

        if (status.contains("PENDING")) {
            return 1;
        } else if (status.contains("APPROVE")) {
            return 2;
        } else if (status.contains("REJECT")) {
            return 3;
        }

        return 4;
    }

    private void fetchAuctions() {
        new Thread(() -> {
            Request req = new Request(null, ActionType.GET_LIST);
            Response res = ClientSocket.sendRequest(req);
            Platform.runLater(() -> {
                clearProductGridAndReleaseResources();

                if (res != null && "SUCCESS".equals(res.getStatus()) && res.getData() != null) {
                    try {
                        List<model.Auction> auctions = (List<model.Auction>) res.getData();

                        // SẮP XẾP DANH SÁCH THEO THỨ TỰ: RUNNING -> WAITING -> FINISHED -> REJECTED
                        auctions.sort((a1, a2) -> {
                            int weight1 = getAuctionStatusWeight(a1);
                            int weight2 = getAuctionStatusWeight(a2);
                            return Integer.compare(weight1, weight2);
                        });

                        Locale localeVN = new Locale("vi", "VN");
                        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeVN);

                        for (model.Auction auction : auctions) {
                            try {
                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin/list.fxml"));
                                AnchorPane listNode = loader.load();
                                listController controller = loader.getController();

                                // Đính kèm controller vào Node cực kỳ quan trọng đối với luồng đếm ngược này
                                listNode.setUserData(controller);

                                String formattedPrice = currencyFormatter.format(auction.getCurrent_price());

                                String[] rowData = {
                                        String.valueOf(auction.getId()),
                                        auction.getItem() != null ? auction.getItem().getName() : "Unknown",
                                        String.valueOf(auction.getItem_id()),
                                        auction.getItem() != null ? String.valueOf(auction.getItem().getSeller_id()) : "Unknown",
                                        "",
                                        formattedPrice,
                                        auction.getStatus() != null ? auction.getStatus() : "WAITING"
                                };
                                controller.setRowData(rowData);

                                // Kích hoạt bộ đếm ngược thời gian thực
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

    public void handleBidHub(MouseEvent event) {
        // Trước khi chuyển scene chính thoát app, tắt toàn bộ Timeline
        clearProductGridAndReleaseResources();

        Request logoutReq = new Request(null, ActionType.LOGOUT);
        ClientSocket.sendRequest(logoutReq);
        SessionManager.getInstance().logout();
        try {
            sceneSwitcher.switchToMainPage(event);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openItemManager(Auction auction) {
        if (item_manager_paneController != null) {
            item_manager_paneController.setItemData(auction);
        }

        item_manager_pane.setManaged(true);
        item_manager_pane.setVisible(true);
        seller_manager_pane.setManaged(false);
        seller_manager_pane.setVisible(false);
        auction_manager_pane.setManaged(false);
        auction_manager_pane.setVisible(false);
    }

    private void openUserManager(Map<String, Object> userDataMap) {
        if (seller_manager_paneController != null) {
            seller_manager_paneController.setUserData(userDataMap);
        }

        seller_manager_pane.setManaged(true);
        seller_manager_pane.setVisible(true);
        item_manager_pane.setManaged(false);
        item_manager_pane.setVisible(false);
        auction_manager_pane.setManaged(false);
        auction_manager_pane.setVisible(false);
    }

    private void openAuctionManager(Auction auction) {
        if (auction_manager_paneController != null) {
            auction_manager_paneController.setAuctionData(auction);
        }

        auction_manager_pane.setManaged(true);
        auction_manager_pane.setVisible(true);

        item_manager_pane.setManaged(false);
        item_manager_pane.setVisible(false);
        seller_manager_pane.setManaged(false);
        seller_manager_pane.setVisible(false);
    }
}