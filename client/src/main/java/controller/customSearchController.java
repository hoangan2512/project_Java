package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.TilePane;
import java.io.IOException;
import java.util.List;
import java.time.LocalDateTime;
import java.time.Duration;

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

        // 1. Tạo Request với ActionType.CUSTOM_SEARCH
        Request req = new Request(currentCriteria, ActionType.CUSTOM_SEARCH);

        // 2. Gửi request qua ClientSocket
        Response res = ClientSocket.sendRequest(req);

        if (res != null && "SUCCESS".equals(res.getStatus())) {
            // Ép kiểu về List<Auction> vì Server đã đổi sang dùng AuctionRepository
            List<Auction> resultList = (List<Auction>) res.getData();

            if (resultList != null && !resultList.isEmpty()) {
                for (Auction auc : resultList) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/prd_preview.fxml"));
                        Node productCard = loader.load();
                        prd_previewController cardController = loader.getController();

                        // Lấy tên, ảnh và mô tả từ Item nằm trong Auction
                        String name = (auc.getItem() != null) ? auc.getItem().getName() : "Không tên";
                        String imgPath = (auc.getItem() != null) ? auc.getItem().getImgPath() : null;
                        String description = (auc.getItem() != null && auc.getItem().getDescription() != null) 
                                                ? auc.getItem().getDescription() : "Chưa có mô tả cho sản phẩm này.";

                        // Lấy giá hiện tại từ Auction
                        long currentPrice = (long) auc.getCurrent_price();

                        // --- TÍNH TOÁN THỜI GIAN CÒN LẠI THỰC TẾ ---
                        long timeLeftSeconds = 0;
                        if ("RUNNING".equals(auc.getStatus()) && auc.getEnd_time() != null) {
                            LocalDateTime now = LocalDateTime.now();
                            if (now.isBefore(auc.getEnd_time())) {
                                timeLeftSeconds = Duration.between(now, auc.getEnd_time()).getSeconds();
                            }
                        }
                        
                        // Cập nhật card với số giây còn lại thực tế để đếm ngược
                        cardController.setData(name, currentPrice, timeLeftSeconds, imgPath);

                        // Thêm hành động khi click vào card sẽ mở trang chi tiết sản phẩm
                        cardController.setOnBidAction(() -> {
                            if (mainPageController.getInstance() != null) {
                                // Tính lại lần nữa khi click để đảm bảo thời gian cập nhật nhất
                                long currentRemaining = 0;
                                if ("RUNNING".equals(auc.getStatus()) && auc.getEnd_time() != null) {
                                    LocalDateTime nowClick = LocalDateTime.now();
                                    if (nowClick.isBefore(auc.getEnd_time())) {
                                        currentRemaining = Duration.between(nowClick, auc.getEnd_time()).getSeconds();
                                    }
                                }
                                mainPageController.getInstance().fillProductPage(name, currentPrice, currentRemaining, imgPath, description);
                            }
                        });

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
