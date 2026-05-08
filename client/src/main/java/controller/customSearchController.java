package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
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
            // Ép kiểu về List<Auction> vì Server đã đổi sang dùng AuctionRepository
            List<Auction> resultList = (List<Auction>) res.getData();

            if (resultList != null && !resultList.isEmpty()) {
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

                        // Bỏ qua các auction bị ép kết thúc bởi UI NẾU người dùng không chủ động chọn xem "FINISHED" trong bộ lọc
                        // NGOẠI TRỪ TRƯỜNG HỢP: Người dùng đang tìm kiếm cụ thể bằng ID. 
                        // Nếu tìm bằng ID thì luôn hiển thị kết quả bất kể trạng thái nào.
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

                        // Lấy giá hiện tại từ Auction
                        long currentPrice = (long) auc.getCurrent_price();

                        // Cập nhật card với số giây còn lại thực tế và trạng thái
                        cardController.setData(name, currentPrice, timeLeftSeconds, imgPath, finalStatus);

                        // Thêm hành động khi click vào card sẽ mở trang chi tiết sản phẩm
                        final long finalTimeLeft = timeLeftSeconds;
                        cardController.setOnBidAction(() -> {
                            if (mainPageController.getInstance() != null) {
                                // Tính lại lần nữa khi click để đảm bảo thời gian cập nhật nhất
                                long currentRemaining = 0;
                                String currentStatus = auc.getStatus();
                                LocalDateTime nowClick = LocalDateTime.now();
                                
                                if ("RUNNING".equals(currentStatus) && auc.getEnd_time() != null) {
                                    if (nowClick.isBefore(auc.getEnd_time())) {
                                        currentRemaining = Duration.between(nowClick, auc.getEnd_time()).getSeconds();
                                    } else {
                                        currentStatus = "FINISHED";
                                    }
                                } else if ("WAITING".equals(currentStatus) && auc.getStart_time() != null) {
                                    if (nowClick.isBefore(auc.getStart_time())) {
                                        currentRemaining = Duration.between(nowClick, auc.getStart_time()).getSeconds();
                                    } else {
                                        currentStatus = "RUNNING";
                                        if (auc.getEnd_time() != null && nowClick.isBefore(auc.getEnd_time())) {
                                            currentRemaining = Duration.between(nowClick, auc.getEnd_time()).getSeconds();
                                        } else {
                                            currentStatus = "FINISHED";
                                        }
                                    }
                                }
                                
                                // Gọi fillProductPage và truyền CẢ ĐỐI TƯỢNG AUCTION
                                mainPageController.getInstance().fillProductPage(auc, currentRemaining, currentStatus);
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
            // Truyền bộ lọc hiện tại vào popup Filter để khôi phục trạng thái nút bấm
            sceneSwitcher.openFilter(currentCriteria);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
