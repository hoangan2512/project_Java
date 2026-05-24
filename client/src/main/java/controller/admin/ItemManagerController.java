package controller.admin;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import message.Request;
import message.Response;
import model.ActionType;
import model.Auction;
import model.Item;
import network.ClientSocket;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ItemManagerController {
    @FXML
    private Label startingPrice, startDate, startTime, duration, prd_description, prdName;
    @FXML
    private ToggleButton approve, not_approve;
    @FXML
    private Button confirm_btn, backBtn;
    @FXML
    private TextArea reasonArea;
    @FXML
    private ImageView prdImage;

    private Auction currentAuction;
    private Runnable onBackAction;

    public void initialize() {
        ToggleGroup actionGroup = new ToggleGroup();
        if (approve != null) approve.setToggleGroup(actionGroup);
        if (not_approve != null) not_approve.setToggleGroup(actionGroup);

        if (confirm_btn != null) {
            confirm_btn.setOnAction(this::handleConfirmAction);
        }

        // ========================================================
        // GẮN LISTENER ĐỂ CẬP NHẬT TRẠNG THÁI NÚT CONFIRM
        // ========================================================
        if (reasonArea != null) {
            reasonArea.textProperty().addListener((observable, oldValue, newValue) -> updateConfirmButtonState());
        }
        if (approve != null) {
            approve.selectedProperty().addListener((observable, oldValue, newValue) -> updateConfirmButtonState());
        }
        if (not_approve != null) {
            not_approve.selectedProperty().addListener((observable, oldValue, newValue) -> updateConfirmButtonState());
        }
    }

    /**
     * Thuật toán bật/tắt nút Confirm dựa trên hành động
     */
    private void updateConfirmButtonState() {
        if (currentAuction == null || confirm_btn == null) return;

        Item item = currentAuction.getItem();

        // Nếu sản phẩm ĐÃ BỊ TỪ CHỐI TỪ TRƯỚC -> Khóa nút Confirm vĩnh viễn (không cho sửa)
        if (item != null && "REJECTED".equalsIgnoreCase(item.getModeration_status())) {
            confirm_btn.setDisable(true);
            return;
        }

        boolean isApprove = approve != null && approve.isSelected();
        boolean isReject = not_approve != null && not_approve.isSelected();
        String currentReason = reasonArea != null ? reasonArea.getText().trim() : "";

        if (isApprove) {
            confirm_btn.setDisable(false);
        } else if (isReject) {
            confirm_btn.setDisable(currentReason.isEmpty());
        } else {
            confirm_btn.setDisable(true);
        }
    }

    /**
     * Bật/Tắt toàn bộ UI trong lúc chờ Server phản hồi (tránh spam)
     */
    private void setUIDisabled(boolean disabled) {
        if (approve != null) approve.setDisable(disabled);
        if (not_approve != null) not_approve.setDisable(disabled);
        if (reasonArea != null) reasonArea.setDisable(disabled);
        if (confirm_btn != null) confirm_btn.setDisable(disabled);
    }

    @FXML
    private void handleConfirmAction(ActionEvent event) {
        if (currentAuction == null) return;

        boolean isApproveSelected = (approve != null && approve.isSelected());
        boolean isRejectSelected = (not_approve != null && not_approve.isSelected());

        if (isApproveSelected) {
            executeApproveItem();
        } else if (isRejectSelected) {
            executeRejectItem();
        }
    }

    private void executeApproveItem() {
        setUIDisabled(true);
        if (confirm_btn != null) confirm_btn.setText("Processing...");

        new Thread(() -> {
            Request req = new Request(Integer.valueOf(currentAuction.getId()), ActionType.ADMIN_APPROVE_ITEM);
            Response res = ClientSocket.sendRequest(req);

            Platform.runLater(() -> {
                if (res != null && "SUCCESS".equals(res.getStatus())) {
                    if (onBackAction != null) onBackAction.run();
                } else {
                    if (confirm_btn != null) confirm_btn.setText("Confirm");
                    setUIDisabled(false);
                }
            });
        }).start();
    }

    private void executeRejectItem() {
        String reasonText = (reasonArea != null) ? reasonArea.getText().trim() : "";
        if (reasonText.isEmpty()) return;

        setUIDisabled(true);
        if (confirm_btn != null) confirm_btn.setText("Processing...");

        new Thread(() -> {
            Object[] payloadToSend = new Object[] { Integer.valueOf(currentAuction.getId()), reasonText };
            Request req = new Request(payloadToSend, ActionType.ADMIN_REJECT_ITEM);
            Response res = ClientSocket.sendRequest(req);

            Platform.runLater(() -> {
                if (res != null && "SUCCESS".equals(res.getStatus())) {
                    if (onBackAction != null) onBackAction.run();
                } else {
                    if (confirm_btn != null) confirm_btn.setText("Confirm");
                    setUIDisabled(false);
                }
            });
        }).start();
    }

    /**
     * Lấy lý do từ chối từ Server và fill vào reasonArea
     */
    private void fetchRejectReason(int auctionId) {
        new Thread(() -> {
            Request req = new Request(auctionId, ActionType.ADMIN_GET_ITEM_REASON);
            Response res = ClientSocket.sendRequest(req);

            Platform.runLater(() -> {
                if (reasonArea != null) {
                    if (res != null && "SUCCESS".equals(res.getStatus()) && res.getData() != null) {
                        reasonArea.setText((String) res.getData());
                    } else {
                        reasonArea.setText("Reject do hết thời gian duyệt");
                    }
                }
            });
        }).start();
    }

    /**
     * Hàm dọn dẹp trạng thái các linh kiện giao diện sau khi hoàn tất hoặc mở mới
     */
    private void resetActionComponents() {
        if (approve != null) {
            approve.setSelected(false);
            approve.setDisable(false);
        }
        if (not_approve != null) {
            not_approve.setSelected(false);
            not_approve.setDisable(false);
        }
        if (reasonArea != null) {
            reasonArea.clear();
            reasonArea.setEditable(true);
            reasonArea.setDisable(false);
        }
        if (confirm_btn != null) {
            confirm_btn.setText("Confirm");
            confirm_btn.setDisable(true);
        }
    }

    /**
     * Hàm chính nhận dữ liệu thô từ homepageController truyền sang khi click vào dòng Item
     */
    public void setItemData(Auction auction) {
        if (auction == null) return;

        this.currentAuction = auction;
        Item item = auction.getItem();

        resetActionComponents();
        if (prdImage != null) prdImage.setImage(null); // Xóa ảnh cũ để tránh nháy ảnh cũ của phiên trước

        // 1. Đổ thời gian thô (Hiển thị ngay lập tức)
        if (auction.getStart_time() != null) {
            LocalDateTime startDateTime = auction.getStart_time();

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

            if (startDate != null) startDate.setText(startDateTime.format(dateFormatter));
            if (startTime != null) startTime.setText(startDateTime.format(timeFormatter));
        }

        if (auction.getStart_time() != null && auction.getEnd_time() != null) {
            Duration diff = Duration.between(auction.getStart_time(), auction.getEnd_time());
            long totalHours = diff.toHours();
            long minutes = diff.toMinutes() % 60;
            String durationStr = String.format("%02d:%02d", totalHours, minutes);

            if (duration != null) duration.setText(durationStr);
        }

        // 2. Đổ dữ liệu thô có sẵn từ LIST_SELECT_SQL lên UI ngay lập tức
        if (item != null) {
            if (prdName != null) prdName.setText(item.getName());

            // LƯU Ý: Giữ nguyên hiển thị đúng cột Giá khởi điểm gốc ban đầu (starting_price)
            if (startingPrice != null) {
                startingPrice.setText(String.format("%,.0f đ", item.getStarting_price()));
            }

            // Set chữ trạng thái chờ tải cho phần mô tả dài bị khuyết
            if (prd_description != null) {
                prd_description.setText("Đang tải mô tả chi tiết từ hệ thống...");
            }

            // ========================================================
            // NẾU SẢN PHẨM ĐÃ BỊ TỪ CHỐI TỪ TRƯỚC -> KHÓA UI VÀ ĐỌC LÝ DO
            // ========================================================
            if ("REJECTED".equalsIgnoreCase(item.getModeration_status())) {
                if (approve != null) approve.setDisable(true);

                if (not_approve != null) {
                    not_approve.setSelected(true);
                    not_approve.setDisable(true);
                }

                if (reasonArea != null) {
                    reasonArea.setText("Đang tải lý do từ chối...");
                    reasonArea.setEditable(false);
                }

                if (confirm_btn != null) {
                    confirm_btn.setDisable(true);
                }

                // Gọi Server lấy lý do cũ
                fetchRejectReason(auction.getId());
            }
        }

        updateConfirmButtonState();

        // =======================================================================
        // KÍCH HOẠT LAZY LOADING: Tải bất đồng bộ phần mô tả chữ dài và hình ảnh
        // =======================================================================
        fetchDetailedItemData(auction.getId());
        fetchProductImageLazy(auction.getId());
    }

    /**
     * LUỒNG NGẦM 1: Tải thông tin văn bản còn khuyết (Description, Categories)
     */
    private void fetchDetailedItemData(int auctionId) {
        new Thread(() -> {
            Request req = new Request(auctionId, ActionType.GET_ITEM_DETAIL);
            Response res = ClientSocket.sendRequest(req);

            Platform.runLater(() -> {
                if (res != null && "SUCCESS".equals(res.getStatus()) && res.getData() instanceof Auction detailedAuction) {
                    // Đắp nối thuộc tính thiếu vào RAM của thực thể cũ
                    if (this.currentAuction != null && detailedAuction.getItem() != null) {
                        Item fullItem = detailedAuction.getItem();
                        Item currentItem = this.currentAuction.getItem();

                        if (currentItem != null) {
                            currentItem.setDescription(fullItem.getDescription());
                            currentItem.setCategories(fullItem.getCategories());
                        }
                    }

                    // Đổ dữ liệu text mô tả chi tiết lên UI
                    Item item = this.currentAuction.getItem();
                    if (item != null && prd_description != null) {
                        prd_description.setText(item.getDescription() != null && !item.getDescription().trim().isEmpty()
                                ? item.getDescription()
                                : "Không có mô tả sản phẩm.");
                    }
                } else {
                    if (prd_description != null) {
                        prd_description.setText("Không thể kết nối để tải mô tả chi tiết.");
                    }
                }
            });
        }).start();
    }

    /**
     * LUỒNG NGẦM 2: Tải dữ liệu byte mảng hình ảnh bổ sung
     */
    private void fetchProductImageLazy(int auctionId) {
        new Thread(() -> {
            Request req = new Request(auctionId, ActionType.GET_IMAGE);
            Response res = ClientSocket.sendRequest(req);

            Platform.runLater(() -> {
                if (res != null && "SUCCESS".equals(res.getStatus()) && res.getData() != null) {
                    try {
                        byte[] imageBytes = (byte[]) res.getData();
                        if (imageBytes != null && imageBytes.length > 0 && prdImage != null) {
                            // Lưu trữ byte ảnh vào Ram đối tượng đề phòng cần tái cấu trúc dữ liệu
                            if (this.currentAuction != null && this.currentAuction.getItem() != null) {
                                this.currentAuction.getItem().setImageBytes(imageBytes);
                            }
                            // Dựng mảng byte stream thành đối tượng Image hiển thị
                            try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
                                prdImage.setImage(new Image(bis));
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[ITEM IMAGE LAZY] Lỗi hiển thị hình ảnh sản phẩm tại client!");
                        e.printStackTrace();
                    }
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