package controller;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.TilePane;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
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

    @FXML
    private Button filter;

    private SearchCriteria currentCriteria;
    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();

    private final Map<Integer, prd_previewController> cardMap = new HashMap<>();
    private final Map<Integer, Auction> auctionDataCache = new HashMap<>();
    private final Map<Integer, String> sellerNameCache = new HashMap<>();

    @FXML
    public void initialize() {
    }

    public void hideFilterButton() {
        if (filter != null) {
            filter.setVisible(false);
            filter.setManaged(false);
        }
    }

    public void setSearchCriteria(SearchCriteria criteria) {
        this.currentCriteria = criteria;
        fetchProductsFromDatabase();
    }

    public void refresh() {
        fetchProductsFromDatabase();
    }

    public void updateSingleAuction(Auction updatedAuction) {
        if (updatedAuction == null) return;

        int aucId = updatedAuction.getId();

        if (!auctionDataCache.containsKey(aucId) || !cardMap.containsKey(aucId)) {
            return;
        }

        Auction existingAuc = auctionDataCache.get(aucId);
        existingAuc.setCurrent_price(updatedAuction.getCurrent_price());

        if (updatedAuction.getStatus() != null) existingAuc.setStatus(updatedAuction.getStatus());
        if (updatedAuction.getEnd_time() != null) existingAuc.setEnd_time(updatedAuction.getEnd_time());
        if (updatedAuction.getStart_time() != null) existingAuc.setStart_time(updatedAuction.getStart_time());

        long timeToStartSeconds = 0;
        long timeToEndSeconds = 0;
        LocalDateTime now = LocalDateTime.now();

        if (existingAuc.getStart_time() != null) {
            timeToStartSeconds = Duration.between(now, existingAuc.getStart_time()).getSeconds();
            if (timeToStartSeconds < 0) timeToStartSeconds = 0;
        }

        if (existingAuc.getEnd_time() != null) {
            timeToEndSeconds = Duration.between(now, existingAuc.getEnd_time()).getSeconds();
            if (timeToEndSeconds < 0) timeToEndSeconds = 0;
        }

        String status = existingAuc.getStatus();
        if (("RUNNING".equals(status) || "PROPOSAL".equals(status) || "DELETE_PROPOSAL".equals(status)) && timeToEndSeconds <= 0) {
            status = "FINISHED";
        } else if ("WAITING".equals(status) && timeToStartSeconds <= 0) {
            if (timeToEndSeconds > 0) {
                status = "RUNNING";
            } else {
                status = "FINISHED";
            }
        }

        String name = (existingAuc.getItem() != null) ? existingAuc.getItem().getName() : "Không tên";
        String imgPath = (existingAuc.getItem() != null) ? existingAuc.getItem().getImgPath() : null;
        byte[] imageBytes = (existingAuc.getItem() != null) ? existingAuc.getItem().getImageBytes() : null;
        String sellerNameStr = "Unknown";
        if (existingAuc.getItem() != null) {
            sellerNameStr = sellerNameCache.getOrDefault(existingAuc.getItem().getSeller_id(), "Unknown");
        }

        prd_previewController cardController = cardMap.get(aucId);
        cardController.setData(name, (long) existingAuc.getCurrent_price(), timeToStartSeconds, timeToEndSeconds, imgPath, status, sellerNameStr, imageBytes);
    }

    @SuppressWarnings("unchecked")
    private void fetchProductsFromDatabase() {
        for (prd_previewController card : cardMap.values()) {
            card.stopTimer();
        }

        productGrid.getChildren().clear();
        cardMap.clear();
        auctionDataCache.clear();
        sellerNameCache.clear();

        if (currentCriteria == null) {
            currentCriteria = new SearchCriteria();
        }

        Request req = new Request(currentCriteria, ActionType.CUSTOM_SEARCH);
        Response res = ClientSocket.sendRequest(req);

        if (res != null && "SUCCESS".equals(res.getStatus())) {

            List<Auction> resultList = null;
            Map<Integer, String> sellerNames = null;

            if (res.getData() instanceof Object[]) {
                Object[] dataPackage = (Object[]) res.getData();
                if (dataPackage.length >= 2) {
                    resultList = (List<Auction>) dataPackage[0];
                    sellerNames = (Map<Integer, String>) dataPackage[1];
                }
            } else if (res.getData() instanceof List) {
                resultList = (List<Auction>) res.getData();
                sellerNames = new HashMap<>();
            }

            if (resultList != null && !resultList.isEmpty()) {
                int cardIndex = 0;

                // Lưu lại danh sách ID cần fetch ảnh để chạy Thread ngầm
                List<Integer> auctionIdsToFetchImage = new ArrayList<>();

                for (Auction auc : resultList) {
                    try {
                        String status = auc.getStatus();

                        if ("SUSPENDED".equals(status) || "PENDING_APPROVAL".equals(status)) {
                            continue;
                        }

                        String sellerNameStr = "Unknown";
                        if (auc.getItem() != null && sellerNames != null) {
                            int sellerId = auc.getItem().getSeller_id();
                            sellerNameStr = sellerNames.getOrDefault(sellerId, "Unknown");
                            sellerNameCache.put(sellerId, sellerNameStr);
                        }

                        long timeToStartSeconds = 0;
                        long timeToEndSeconds = 0;
                        LocalDateTime now = LocalDateTime.now();

                        if (auc.getStart_time() != null) {
                            timeToStartSeconds = Duration.between(now, auc.getStart_time()).getSeconds();
                            if (timeToStartSeconds < 0) timeToStartSeconds = 0;
                        }

                        if (auc.getEnd_time() != null) {
                            timeToEndSeconds = Duration.between(now, auc.getEnd_time()).getSeconds();
                            if (timeToEndSeconds < 0) timeToEndSeconds = 0;
                        }

                        if ("RUNNING".equals(status) && timeToEndSeconds <= 0) {
                            status = "FINISHED";
                        } else if (("WAITING".equals(status) || "PROPOSAL".equals(status) || "DELETE_PROPOSAL".equals(status)) && timeToStartSeconds <= 0) {
                            if (timeToEndSeconds > 0) {
                                status = "RUNNING";
                            } else {
                                status = "FINISHED";
                            }
                        }

                        String finalStatus = status;

                        if ("FINISHED".equals(finalStatus)) {
                            boolean isSearchingById = (currentCriteria.getAuctionId() != null && !currentCriteria.getAuctionId().trim().isEmpty());
                            if (!isSearchingById) {
                                List<String> selectedStatuses = currentCriteria.getStatuses();
                                if (selectedStatuses == null || !selectedStatuses.contains("FINISHED")) {
                                    continue;
                                }
                            }
                        }

                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/prd_preview.fxml"));
                        Node productCard = loader.load();
                        prd_previewController cardController = loader.getController();

                        cardMap.put(auc.getId(), cardController);
                        auctionDataCache.put(auc.getId(), auc);
                        auctionIdsToFetchImage.add(auc.getId()); // Thêm vào hàng đợi lấy ảnh

                        String name = (auc.getItem() != null) ? auc.getItem().getName() : "Không tên";
                        long currentPrice = (long) auc.getCurrent_price();

                        // LƯU Ý: Ở bước này truyền NULL cho ảnh để render Chữ siêu tốc
                        cardController.setData(name, currentPrice, timeToStartSeconds, timeToEndSeconds, null, finalStatus, sellerNameStr, null);

                        cardController.setOnBidAction(() -> {
                            if (mainPageController.getInstance() != null) {
                                mainPageController.getInstance().fillProductPage(auc);
                            }
                        });

                        productCard.setOpacity(0);
                        productCard.setTranslateY(-30);

                        FadeTransition fadeIn = new FadeTransition(javafx.util.Duration.millis(400), productCard);
                        fadeIn.setToValue(1.0);

                        TranslateTransition dropDown = new TranslateTransition(javafx.util.Duration.millis(400), productCard);
                        dropDown.setToY(0);

                        ParallelTransition combinedAnim = new ParallelTransition(fadeIn, dropDown);

                        // GIẢM LAG ANIMATION: Nếu quá nhiều card thì không nhân độ trễ nữa
                        long delayMillis = (cardIndex < 15) ? (cardIndex * 60L) : 0;
                        combinedAnim.setDelay(javafx.util.Duration.millis(delayMillis));

                        productGrid.getChildren().add(productCard);
                        combinedAnim.play();

                        cardIndex++;

                    } catch (IOException e) {
                        System.err.println("Lỗi load card giao diện: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                // =========================================================
                // TRUE LAZY LOADING: Mở đúng 1 luồng ngầm tuần tự đi đòi ảnh
                // =========================================================
                startImageLazyLoadThread(auctionIdsToFetchImage);

            } else {
                System.out.println("Không có kết quả phù hợp.");
            }
        } else {
            System.out.println("Lỗi: " + (res != null ? res.getMessage() : "Mất kết nối"));
        }
    }
    /**
     * Chạy duy nhất 1 Thread để xin từng ảnh một, bảo vệ Socket và giúp UI cực mượt
     */
    private void startImageLazyLoadThread(List<Integer> auctionIds) {
        new Thread(() -> {
            for (Integer aucId : auctionIds) {
                try {
                    Request imgReq = new Request(aucId, ActionType.GET_IMAGE);
                    Response imgRes = ClientSocket.sendRequest(imgReq);

                    if (imgRes != null && "SUCCESS".equals(imgRes.getStatus()) && imgRes.getData() != null) {
                        byte[] loadedBytes = (byte[]) imgRes.getData();

                        if (loadedBytes != null && loadedBytes.length > 0) {
                            Platform.runLater(() -> {
                                // 1. Nạp ảnh vào bộ nhớ đệm
                                Auction cached = auctionDataCache.get(aucId);
                                if (cached != null && cached.getItem() != null) {
                                    cached.getItem().setImageBytes(loadedBytes);
                                }

                                // 2. Update ĐỘC LẬP lên thẻ UI (Không làm reset bộ đếm giờ)
                                prd_previewController targetCard = cardMap.get(aucId);
                                if (targetCard != null) {
                                    targetCard.updateImage(loadedBytes);
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi nạp ảnh Lazy Load cho ID " + aucId + ": " + e.getMessage());
                }
            }
        }).start();
    }

    @FXML
    private void handleFilter(ActionEvent event) {
        try {
            sceneSwitcher.openFilter(currentCriteria);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}