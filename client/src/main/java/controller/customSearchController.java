package controller;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
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
    
    // Hàm gọi để load lại dữ liệu (dùng khi có tín hiệu broadcast)
    public void refresh() {
        fetchProductsFromDatabase();
    }

    @SuppressWarnings("unchecked")
    private void fetchProductsFromDatabase() {
        productGrid.getChildren().clear();
        
        // Nếu không có criteria (ví dụ: load mặc định khi mở app), thì tạo criteria trống để lấy tất cả
        if (currentCriteria == null) {
            currentCriteria = new SearchCriteria();
        }

        // 1. Tạo Request với ActionType.CUSTOM_SEARCH
        Request req = new Request(currentCriteria, ActionType.CUSTOM_SEARCH);

        // 2. Gửi request qua ClientSocket
        Response res = ClientSocket.sendRequest(req);

        if (res != null && "SUCCESS".equals(res.getStatus())) {
            
            List<Auction> resultList = null;
            Map<Integer, String> sellerNames = null;

            // Xử lý an toàn để tránh lỗi ClassCastException nếu Server chưa kịp cập nhật (hoặc trả về sai kiểu)
            if (res.getData() instanceof Object[]) {
                Object[] dataPackage = (Object[]) res.getData();
                resultList = (List<Auction>) dataPackage[0];
                sellerNames = (Map<Integer, String>) dataPackage[1];
            } else if (res.getData() instanceof List) {
                // Đề phòng Server cũ vẫn trả về List<Auction>
                resultList = (List<Auction>) res.getData();
                sellerNames = new HashMap<>(); // Khởi tạo map rỗng để tránh NullPointer
            }

            if (resultList != null && !resultList.isEmpty()) {
                int cardIndex = 0;
                for (Auction auc : resultList) {
                    try {
                        // --- TÍNH TOÁN THỜI GIAN VÀ TRẠNG THÁI ---
                        long timeLeftSeconds = 0;
                        LocalDateTime now = LocalDateTime.now();
                        String status = auc.getStatus();

                        if ("RUNNING".equals(status) && auc.getEnd_time() != null) {
                            if (now.isBefore(auc.getEnd_time())) {
                                timeLeftSeconds = Duration.between(now, auc.getEnd_time()).getSeconds();
                            } else {
                                status = "FINISHED"; // Trên server TimeManager chưa kịp chạy, ta ép kết thúc trên UI
                            }
                        } else if ("WAITING".equals(status) && auc.getStart_time() != null) {
                            if (now.isBefore(auc.getStart_time())) {
                                // Nếu chưa tới giờ bắt đầu, đếm ngược tới giờ bắt đầu
                                timeLeftSeconds = Duration.between(now, auc.getStart_time()).getSeconds();
                            } else {
                                // Đã tới giờ nhưng Server chưa kịp đổi trạng thái
                                status = "RUNNING";
                                if (auc.getEnd_time() != null && now.isBefore(auc.getEnd_time())) {
                                    timeLeftSeconds = Duration.between(now, auc.getEnd_time()).getSeconds();
                                } else {
                                    status = "FINISHED";
                                }
                            }
                        }
                        
                        String finalStatus = status;

                        if ("FINISHED".equals(finalStatus)) {
                            boolean isSearchingById = (currentCriteria.getAuctionId() != null && !currentCriteria.getAuctionId().trim().isEmpty());
                            
                            if (!isSearchingById) {
                                List<String> selectedStatuses = currentCriteria.getStatuses();
                                if (selectedStatuses == null || !selectedStatuses.contains("FINISHED")) {
                                    continue; // Bỏ qua không vẽ thẻ sản phẩm này lên màn hình
                                }
                            }
                        }

                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/prd_preview.fxml"));
                        Node productCard = loader.load();
                        prd_previewController cardController = loader.getController();

                        // Lấy tên, ảnh và mô tả từ Item nằm trong Auction
                        String name = (auc.getItem() != null) ? auc.getItem().getName() : "Không tên";
                        String imgPath = (auc.getItem() != null) ? auc.getItem().getImgPath() : null;
                        
                        // Lấy mảng byte hình ảnh từ Item
                        byte[] imageBytes = (auc.getItem() != null) ? auc.getItem().getImageBytes() : null;
                        
                        // LẤY TÊN SELLER TỪ BẢNG MAP (Đã gửi kèm trong Response)
                        String sellerNameStr = "Unknown";
                        if (auc.getItem() != null && sellerNames != null) {
                            int sellerId = auc.getItem().getSeller_id();
                            sellerNameStr = sellerNames.getOrDefault(sellerId, "Unknown");
                        }

                        // Lấy giá hiện tại từ Auction
                        long currentPrice = (long) auc.getCurrent_price();

                        // Cập nhật card với số giây còn lại thực tế, trạng thái, và TRUYỀN MẢNG BYTE ẢNH VÀO
                        cardController.setData(name, currentPrice, timeLeftSeconds, imgPath, finalStatus, sellerNameStr, imageBytes);

                        // Thêm hành động khi click vào card sẽ mở trang chi tiết sản phẩm
                        cardController.setOnBidAction(() -> {
                            if (mainPageController.getInstance() != null) {
                                // Chỉ cần truyền đối tượng Auction, prdPageController sẽ tự chịu trách nhiệm tính toán thời gian thực tế
                                mainPageController.getInstance().fillProductPage(auc);
                            }
                        });

                        // --- THIẾT LẬP ANIMATION THẢ RƠI (STAGGERED DROP) ---
                        // 1. Trạng thái bắt đầu: Mờ và nằm cao hơn vị trí thật 30px
                        productCard.setOpacity(0);
                        productCard.setTranslateY(-30);

                        // 2. Tạo hiệu ứng hiện dần
                        FadeTransition fadeIn = new FadeTransition(javafx.util.Duration.millis(400), productCard);
                        fadeIn.setToValue(1.0);

                        // 3. Tạo hiệu ứng rơi xuống vị trí chuẩn
                        TranslateTransition dropDown = new TranslateTransition(javafx.util.Duration.millis(400), productCard);
                        dropDown.setToY(0);

                        // 4. Kết hợp và tạo độ trễ (mỗi card xuất hiện cách nhau 60ms)
                        ParallelTransition combinedAnim = new ParallelTransition(fadeIn, dropDown);
                        combinedAnim.setDelay(javafx.util.Duration.millis(cardIndex * 60));

                        // Thêm card vào lưới và chạy animation
                        productGrid.getChildren().add(productCard);
                        combinedAnim.play();

                        cardIndex++; // Tăng index để card tiếp theo trễ hơn

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
            // Truyền bộ lọc hiện tại vào popup Filter để khôi phục trạng thái nút bấm
            sceneSwitcher.openFilter(currentCriteria);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
