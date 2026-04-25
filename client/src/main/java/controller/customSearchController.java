package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.TilePane;
import java.io.IOException;
import java.util.List;

import model.SearchCriteria;
import model.Auction;
import model.ActionType;
import message.Request;
import message.Response;
import network.ClientSocket;

public class customSearchController {

    @FXML
    private TilePane productGrid;

    private SearchCriteria currentCriteria;
    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();

    @FXML
    public void initialize() {
    }

    public void setSearchCriteria(SearchCriteria criteria) {
        this.currentCriteria = criteria;
        fetchProductsFromDatabase();
    }

    private void fetchProductsFromDatabase() {
        productGrid.getChildren().clear();

        // 1. Tạo Request với ActionType.CUSTOM_SEARCH [cite: 53]
        Request req = new Request(currentCriteria, ActionType.CUSTOM_SEARCH);

        // 2. Gửi request qua ClientSocket [cite: 1, 12]
        Response res = ClientSocket.sendRequest(req);

        if (res != null && "SUCCESS".equals(res.getStatus())) {
            // Ép kiểu về List<Auction> vì Server đã đổi sang dùng AuctionRepository [cite: 13, 47]
            List<Auction> resultList = (List<Auction>) res.getData();

            if (resultList != null && !resultList.isEmpty()) {
                for (Auction auc : resultList) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/prd_preview.fxml"));
                        Node productCard = loader.load();
                        prd_previewController cardController = loader.getController();

                        // ==========================================
                        // TẠM THỜI BỎ QUA TIMELEFT
                        // ==========================================
                        // Lấy tên và ảnh từ Item nằm trong Auction (kết quả của lệnh JOIN) [cite: 47, 50]
                        String name = (auc.getItem() != null) ? auc.getItem().getName() : "Không tên";
                        String imgPath = (auc.getItem() != null) ? auc.getItem().getImgPath() : null;

                        // Lấy giá hiện tại từ Auction [cite: 22, 48]
                        long currentPrice = (long) auc.getCurrent_price();

                        // Truyền 0 vào vị trí timeLeft để UI vẫn hiển thị nhưng không đếm ngược
                        cardController.setData(name, currentPrice, 0, imgPath);

                        productGrid.getChildren().add(productCard);

                    } catch (IOException e) {
                        System.err.println("Lỗi load card giao diện!");
                    }
                }
            } else {
                System.out.println("Không có kết quả phù hợp.");
            }
        } else {
            System.out.println("Lỗi: " + (res != null ? res.getMessage() : "Mất kết nối"));
        }
    }

    @FXML
    private void handleFilter(ActionEvent event) {
        try {
            sceneSwitcher.openFilter();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}