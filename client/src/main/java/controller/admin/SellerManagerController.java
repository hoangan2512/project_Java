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

        // 2. Nhóm biện pháp xử lý tài khoản (Activate / Ban)
        ToggleGroup actionGroup = new ToggleGroup();
        if (activate != null) activate.setToggleGroup(actionGroup);
        if (ban != null) ban.setToggleGroup(actionGroup);

        if (confirm_btn != null) {
            confirm_btn.setOnAction(this::handleConfirmAction);
        }

        // ========================================================
        // 3. GẮN LISTENER ĐỂ TỰ ĐỘNG BẬT/TẮT NÚT CONFIRM
        // ========================================================
        if (reasonArea != null) {
            reasonArea.textProperty().addListener((observable, oldValue, newValue) -> updateButtonStates());
        }
        if (activate != null) {
            activate.selectedProperty().addListener((observable, oldValue, newValue) -> updateButtonStates());
        }
        if (ban != null) {
            ban.selectedProperty().addListener((observable, oldValue, newValue) -> updateButtonStates());
        }
    }

    /**
     * Thuật toán cập nhật trạng thái nút Confirm
     */
    private void updateButtonStates() {
        if (currentSellerId == -1 || confirm_btn == null) return;

        boolean isActivateSelected = activate != null && activate.isSelected();
        boolean isBanSelected = ban != null && ban.isSelected();
        String currentReason = reasonArea != null ? reasonArea.getText().trim() : "";

        if (isActivateSelected) {
            // Đang chọn mở khóa -> Không cần lý do -> Mở Confirm
            confirm_btn.setDisable(false);
        } else if (isBanSelected) {
            // Đang chọn khóa -> Bắt buộc phải nhập lý do mới cho Confirm
            confirm_btn.setDisable(currentReason.isEmpty());
        } else {
            // Chưa chọn hành động nào -> Khóa nút Confirm
            confirm_btn.setDisable(true);
        }
    }

    /**
     * Bật/Tắt các nút khi đang chờ Server phản hồi
     */
    private void setUIDisabled(boolean disabled) {
        if (activate != null) activate.setDisable(disabled);
        if (ban != null) ban.setDisable(disabled);
        if (reasonArea != null) reasonArea.setDisable(disabled);
        if (confirm_btn != null) confirm_btn.setDisable(disabled);
    }

    @FXML
    private void handleConfirmAction(ActionEvent event) {
        if (currentSellerId == -1) return;

        boolean isActivateSelected = (activate != null && activate.isSelected());
        boolean isBanSelected = (ban != null && ban.isSelected());

        if (isActivateSelected) {
            executeActivateUser();
        } else if (isBanSelected) {
            executeBanUser();
        }
    }

    private void executeActivateUser() {
        setUIDisabled(true);
        if (confirm_btn != null) confirm_btn.setText("Processing...");

        new Thread(() -> {
            Request req = new Request(Integer.valueOf(currentSellerId), ActionType.ADMIN_UNBAN_USER);
            Response res = ClientSocket.sendRequest(req);

            Platform.runLater(() -> {
                if (res != null && "SUCCESS".equals(res.getStatus())) {
                    if (onBackAction != null) onBackAction.run();
                } else {
                    if (confirm_btn != null) confirm_btn.setText("Confirm");
                    setUIDisabled(false);
                    updateButtonStates();
                }
            });
        }).start();
    }

    private void executeBanUser() {
        String reasonText = (reasonArea != null) ? reasonArea.getText().trim() : "";
        if (reasonText.isEmpty()) return;

        setUIDisabled(true);
        if (confirm_btn != null) confirm_btn.setText("Processing...");

        new Thread(() -> {
            Object[] payloadToSend = new Object[] { Integer.valueOf(currentSellerId), reasonText };
            Request req = new Request(payloadToSend, ActionType.ADMIN_BAN_USER);
            Response res = ClientSocket.sendRequest(req);

            Platform.runLater(() -> {
                if (res != null && "SUCCESS".equals(res.getStatus())) {
                    if (onBackAction != null) onBackAction.run();
                } else {
                    if (confirm_btn != null) confirm_btn.setText("Confirm");
                    setUIDisabled(false);
                    updateButtonStates();
                }
            });
        }).start();
    }

    /**
     * Lấy lý do User bị Ban từ DB
     */
    private void fetchBanReason(int userId) {
        new Thread(() -> {
            Request req = new Request(userId, ActionType.ADMIN_GET_USER_REASON);
            Response res = ClientSocket.sendRequest(req);

            Platform.runLater(() -> {
                if (reasonArea != null) {
                    if (res != null && "SUCCESS".equals(res.getStatus()) && res.getData() != null) {
                        reasonArea.setText((String) res.getData());
                    } else {
                        reasonArea.setText("BANNED do vi phạm chính sách của hệ thống.");
                    }
                }
            });
        }).start();
    }

    private void resetActionButtons() {
        if (activate != null) {
            activate.setSelected(false);
            activate.setDisable(false);
        }
        if (ban != null) {
            ban.setSelected(false);
            ban.setDisable(false);
        }
        if (reasonArea != null) {
            reasonArea.clear();
            reasonArea.setEditable(true);
            reasonArea.setDisable(false);
        }
        if (confirm_btn != null) {
            confirm_btn.setText("Confirm");
            confirm_btn.setDisable(true); // Khóa nút lúc mới vào
        }
    }

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

        String status = user.getStatus() != null ? user.getStatus().toUpperCase() : "ACTIVE";
        if (user_status != null) {
            user_status.setText(status);
            if ("ACTIVE".equals(status)) {
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

        // Dọn dẹp form thao tác cũ
        resetActionButtons();

        // ========================================================
        // NẾU USER BỊ BANNED -> LẤY LÝ DO VÀ KHÓA LOGIC BAN
        // ========================================================
        if ("BANNED".equals(status)) {
            if (ban != null) ban.setDisable(true); // Không cho Ban lại
            if (activate != null) activate.setDisable(false); // Cho phép mở khóa

            if (reasonArea != null) {
                reasonArea.setEditable(false);
                reasonArea.setText("Đang tải lý do khóa tài khoản...");
            }
            fetchBanReason(currentSellerId);
        } else {
            // User đang ACTIVE
            if (activate != null) activate.setDisable(true); // Không cần mở khóa lại
            if (ban != null) ban.setDisable(false); // Cho phép Ban
        }

        updateButtonStates();

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