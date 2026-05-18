package controller.admin;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
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
    @FXML
    private Button confirm_btn;

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

        // 2. Nhóm biện pháp xử lý tài khoản (Activate / Ban) - Đảm bảo chỉ chọn được 1 trong 2
        ToggleGroup actionGroup = new ToggleGroup();
        if (activate != null) activate.setToggleGroup(actionGroup);
        if (ban != null) ban.setToggleGroup(actionGroup);

        // ĐỔI LOGIC: Gán sự kiện click cho duy nhất nút confirm_btn để thực thi hành động công việc
        if (confirm_btn != null) {
            confirm_btn.setOnAction(this::handleConfirmAction);
        }
    }

    /**
     * HÀM XỬ LÝ TRUNG TÂM: Kích hoạt khi nhấn confirm_btn
     */
    @FXML
    private void handleConfirmAction(ActionEvent event) {
        if (currentSellerId == -1) return;

        // 1. Kiểm tra xem Admin đang chọn hành động nào
        boolean isActivateSelected = (activate != null && activate.isSelected());
        boolean isBanSelected = (ban != null && ban.isSelected());

        if (!isActivateSelected && !isBanSelected) {
            showAlert(Alert.AlertType.WARNING, "Yêu cầu", "Vui lòng chọn một hành động (Mở khóa hoặc Khóa tài khoản) trước khi xác nhận!");
            return;
        }

        // 2. Rẽ nhánh xử lý dựa trên nút ToggleButton được lựa chọn
        if (isActivateSelected) {
            executeActivateUser();
        } else if (isBanSelected) {
            executeBanUser();
        }
    }

    /**
     * Logic gửi request Mở khóa tài khoản (chạy ngầm sau khi confirm)
     */
    private void executeActivateUser() {
        new Thread(() -> {
            Request req = new Request(Integer.valueOf(currentSellerId), ActionType.ADMIN_UNBAN_USER);
            Response res = ClientSocket.sendRequest(req);

            Platform.runLater(() -> {
                if (res != null && "SUCCESS".equals(res.getStatus())) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã mở khóa tài khoản thành công!");
                    if (user_status != null) {
                        user_status.setText("ACTIVE");
                        user_status.setStyle("-fx-text-fill: #4CAF50;");
                    }
                    resetActionButtons();
                } else {
                    String msg = (res != null) ? res.getMessage() : "Mất kết nối tới máy chủ.";
                    showAlert(Alert.AlertType.ERROR, "Thất bại", "Không thể mở khóa tài khoản: " + msg);
                }
            });
        }).start();
    }

    /**
     * Logic gửi request Khóa tài khoản kèm lý do (chạy ngầm sau khi confirm)
     */
    private void executeBanUser() {
        String reasonText = (reasonArea != null) ? reasonArea.getText().trim() : "";

        if (reasonText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Yêu cầu dữ liệu", "Vui lòng nhập lý do khóa tài khoản vào ô văn bản!");
            return;
        }

        new Thread(() -> {
            Object[] payloadToSend = new Object[] { Integer.valueOf(currentSellerId), reasonText };
            Request req = new Request(payloadToSend, ActionType.ADMIN_BAN_USER);
            Response res = ClientSocket.sendRequest(req);

            Platform.runLater(() -> {
                if (res != null && "SUCCESS".equals(res.getStatus())) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã khóa tài khoản người dùng thành công.");
                    if (user_status != null) {
                        user_status.setText("BANNED");
                        user_status.setStyle("-fx-text-fill: #F44336;");
                    }
                    resetActionButtons();
                } else {
                    String msg = (res != null) ? res.getMessage() : "Mất kết nối tới máy chủ.";
                    showAlert(Alert.AlertType.ERROR, "Thất bại", "Không thể khóa tài khoản: " + msg);
                }
            });
        }).start();
    }

    /**
     * Hàm phụ trợ dọn dẹp trạng thái các nút và ô nhập liệu sau khi xử lý thành công
     */
    private void resetActionButtons() {
        if (activate != null) activate.setSelected(false);
        if (ban != null) ban.setSelected(false);
        if (reasonArea != null) reasonArea.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
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

        resetActionButtons();

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