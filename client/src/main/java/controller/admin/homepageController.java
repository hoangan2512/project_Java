package controller.admin;

import controller.SceneSwitchController;
import controller.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
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
    
    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();

    public void initialize() {
        ToggleGroup group = new ToggleGroup();
        
        if (Seller != null) Seller.setToggleGroup(group);
        if (Item != null) Item.setToggleGroup(group);
        if (Auction != null) Auction.setToggleGroup(group);

        group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true);
            } else {
                boolean isSeller = (newVal == Seller);
                boolean isItem = (newVal == Item);
                boolean isAuction = (newVal == Auction);

                if (seller_head != null) { seller_head.setVisible(isSeller); seller_head.setManaged(isSeller); }
                if (item_head != null) { item_head.setVisible(isItem); item_head.setManaged(isItem); }
                if (auction_head != null) { auction_head.setVisible(isAuction); auction_head.setManaged(isAuction); }

                if (isSeller) fetchSellers();
                else if (isItem) fetchItems();
                else if (isAuction) fetchAuctions();
            }
        });

        if (Seller != null) Seller.setSelected(true);
    }

    private void fetchSellers() {
        new Thread(() -> {
            Request req = new Request(null, ActionType.ADMIN_GET_ALL_USERS);
            Response res = ClientSocket.sendRequest(req);
            Platform.runLater(() -> {
                productGrid.getChildren().clear();
                if (res != null && "SUCCESS".equals(res.getStatus()) && res.getData() != null) {
                    try {
                        List<Map<String, Object>> usersData = (List<Map<String, Object>>) res.getData();
                        for (Map<String, Object> userDataMap : usersData) {
                            try {
                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin/list.fxml"));
                                AnchorPane listNode = loader.load();
                                listController controller = loader.getController();

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
            Request req = new Request(null, ActionType.ADMIN_GET_ITEMS);
            Response res = ClientSocket.sendRequest(req);
            Platform.runLater(() -> {
                productGrid.getChildren().clear();
                if (res != null && "SUCCESS".equals(res.getStatus()) && res.getData() != null) {
                    try {
                        List<Auction> auctionList = (List<Auction>) res.getData();
                        List<model.Auction> auctions = (List<model.Auction>) res.getData();
                        Locale localeVN = new Locale("vi", "VN");
                        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeVN);
                        for (Auction auc : auctionList) {
                            try {
                                Item item = auc.getItem();
                                if (item == null) continue;

                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin/list.fxml"));
                                AnchorPane listNode = loader.load();
                                listController controller = loader.getController();
                                String formattedPrice = currencyFormatter.format(item.getStarting_price());
                                String[] rowData = {
                                        String.valueOf(item.getId()),
                                        item.getName(),
                                        item.getUser_prdID(),
                                        String.valueOf(item.getSeller_id()),
                                        String.valueOf(auc.getId()),
                                        formattedPrice,
                                        String.valueOf(item.getModeration_status())
                                };
                                controller.setRowData(rowData);
                                productGrid.getChildren().add(listNode);
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            });
        }).start();
    }

    private void fetchAuctions() {
        new Thread(() -> {
            Request req = new Request(null, ActionType.GET_LIST); 
            Response res = ClientSocket.sendRequest(req);
            Platform.runLater(() -> {
                productGrid.getChildren().clear();
                if (res != null && "SUCCESS".equals(res.getStatus()) && res.getData() != null) {
                    try {
                        List<model.Auction> auctions = (List<model.Auction>) res.getData();
                        Locale localeVN = new Locale("vi", "VN");
                        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeVN);
                        for (model.Auction auction : auctions) {
                            try {
                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/admin/list.fxml"));
                                AnchorPane listNode = loader.load();
                                listController controller = loader.getController();
                                String formattedPrice = currencyFormatter.format(auction.getCurrent_price());
                                String[] rowData = {
                                        String.valueOf(auction.getId()), 
                                        auction.getItem() != null ? auction.getItem().getName() : "Unknown",
                                        String.valueOf(auction.getItem_id()), 
                                        auction.getItem() != null ? String.valueOf(auction.getItem().getSeller_id()) : "Unknown", 
                                        auction.getEnd_time() != null ? auction.getEnd_time().toString() : "N/A",
                                        formattedPrice,
                                        auction.getStatus() != null ? auction.getStatus() : "WAITING"
                                };
                                controller.setRowData(rowData);
                                productGrid.getChildren().add(listNode);
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            });
        }).start();
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
}
