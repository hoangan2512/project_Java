package controller.admin;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import message.Request;
import message.Response;
import model.ActionType;
import model.Auction;
import model.Item;
import model.User;
import network.ClientSocket;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SellerManagerController {
    @FXML
    private Label username, role, item_number, auction_number, user_status, reject_item_number, suspend_auction_number, total;
    @FXML
    private ToggleButton activate, ban, item_opt, auction_opt;
    @FXML
    private TextArea reasonArea;
    @FXML
    private HBox item_head, auction_head;
    @FXML
    private TilePane productGrid;

    private Runnable onBackAction;

    // Biến lưu trữ ID người bán hiện tại đang được chọn điều khiển
    private int currentSellerId = -1;

    public void initialize() {
        // 1. Nhóm bộ lọc danh sách (Items / Auctions)
        ToggleGroup filterGroup = new ToggleGroup();
        if (item_opt != null) item_opt.setToggleGroup(filterGroup);
        if (auction_opt != null) auction_opt.setToggleGroup(filterGroup);

        filterGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true);
            } else {
                clearProductGridAndReleaseResources();

                boolean isItem = (newVal == item_opt);
                boolean isAuction = (newVal == auction_opt);

                if (item_head != null) { item_head.setVisible(isItem); item_head.setManaged(isItem); }
                if (auction_head != null) { auction_head.setVisible(isAuction); auction_head.setManaged(isAuction); }

                if (currentSellerId != -1) {
                    if (isItem) fetchItems();
                    else if (isAuction) fetchAuctions();
                }
            }
        });

        if (item_opt != null) item_opt.setSelected(true);

        // ========================================================
        // LOGIC MỚI: NHÓM BIỆN PHÁP XỬ LÝ TÀI KHOẢN (ACTIVATE / BAN)
        // ========================================================
        ToggleGroup actionGroup = new ToggleGroup();
        if (activate != null) activate.setToggleGroup(actionGroup);
        if (ban != null) ban.setToggleGroup(actionGroup);

        // Mặc định ban đầu không chọn cái nào (Đã được đảm bảo bằng cách không set selected cho nút nào trong hành động này)
    }

    /**
     * Nhận gói dữ liệu Map từ homepageController truyền sang khi click vào dòng Seller
     */
    public void setUserData(Map<String, Object> userDataMap) {
        if (userDataMap == null) return;

        User user = (User) userDataMap.get("user");
        if (user == null) return;

        this.currentSellerId = (int) user.getID();

        int itemsCount = ((Number) userDataMap.getOrDefault("itemsCount", 0)).intValue();
        int auctionsCount = ((Number) userDataMap.getOrDefault("auctionsCount", 0)).intValue();
        int warningsCount = ((Number) userDataMap.getOrDefault("warningsCount", 0)).intValue();

        int rejectedItems = ((Number) userDataMap.getOrDefault("rejectedItemsCount", 0)).intValue();
        int suspendedAuctions = ((Number) userDataMap.getOrDefault("suspendedAuctionsCount", 0)).intValue();

        if (username != null) username.setText(user.getUsername());
        if (role != null) role.setText(user.getRole() != null ? user.getRole().toUpperCase() : "SELLER");
        if (user_status != null) {
            user_status.setText(user.getStatus() != null ? user.getStatus().toUpperCase() : "ACTIVE");
            if ("ACTIVE".equalsIgnoreCase(user.getStatus())) {
                user_status.setStyle("-fx-text-fill: #4CAF50;");
            } else {
                user_status.setStyle("-fx-text-fill: #F44336;");
            }
        }

        if (item_number != null) item_number.setText(String.valueOf(itemsCount));
        if (auction_number != null) auction_number.setText(String.valueOf(auctionsCount));
        if (reject_item_number != null) reject_item_number.setText(String.valueOf(rejectedItems));
        if (suspend_auction_number != null) suspend_auction_number.setText(String.valueOf(suspendedAuctions));
        if (total != null) total.setText(String.valueOf(warningsCount));

        // ========================================================
        // SỬA ĐỒI TẠI ĐÂY: KHÔNG GÁN TRẠNG THÁI MẶC ĐỊNH CHO NÚT BẤM NỮA
        // Để trống hoàn toàn để giữ nguyên trạng thái nhả (không được chọn) ban đầu của cả 2 nút.
        // ========================================================
        if (activate != null) activate.setSelected(false);
        if (ban != null) ban.setSelected(false);

        clearProductGridAndReleaseResources();
        if (item_opt != null && item_opt.isSelected()) {
            fetchItems();
        } else if (auction_opt != null && auction_opt.isSelected()) {
            fetchAuctions();
        }
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
        }
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

                        Locale localeVN = new Locale("vi", "VN");
                        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeVN);

                        for (Auction auc : auctionList) {
                            try {
                                Item item = auc.getItem();
                                if (item == null) continue;

                                if (item.getSeller_id() != currentSellerId) continue;

                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin/list.fxml"));
                                AnchorPane listNode = loader.load();
                                listController controller = loader.getController();

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

    private void fetchAuctions() {
        new Thread(() -> {
            Request req = new Request(null, ActionType.GET_LIST);
            Response res = ClientSocket.sendRequest(req);
            Platform.runLater(() -> {
                clearProductGridAndReleaseResources();

                if (res != null && "SUCCESS".equals(res.getStatus()) && res.getData() != null) {
                    try {
                        List<model.Auction> auctions = (List<model.Auction>) res.getData();
                        Locale localeVN = new Locale("vi", "VN");
                        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeVN);

                        for (model.Auction auction : auctions) {
                            try {
                                if (auction.getItem() == null || auction.getItem().getSeller_id() != currentSellerId) continue;

                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin/list.fxml"));
                                AnchorPane listNode = loader.load();
                                listController controller = loader.getController();

                                listNode.setUserData(controller);

                                String formattedPrice = currencyFormatter.format(auction.getCurrent_price());

                                String[] rowData = {
                                        String.valueOf(auction.getId()),
                                        auction.getItem() != null ? auction.getItem().getName() : "Unknown",
                                        String.valueOf(auction.getItem_id()),
                                        String.valueOf(auction.getItem().getSeller_id()),
                                        "",
                                        formattedPrice,
                                        auction.getStatus() != null ? auction.getStatus() : "WAITING"
                                };
                                controller.setRowData(rowData);

                                controller.startCountdown(auction.getStart_time(), auction.getEnd_time());
                                productGrid.getChildren().add(listNode);
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            });
        }).start();
    }

    public void setOnBack(Runnable onBackAction) {
        this.onBackAction = onBackAction;
    }

    @FXML
    private void handleBackBtn(ActionEvent event) {
        if (onBackAction != null) {
            onBackAction.run();
        }
    }
}