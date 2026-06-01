package controller.sellerHub;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import message.Request;
import message.Response;
import model.ActionType;
import model.Auction;
import model.Item;
import network.ClientSocket;

import java.io.ByteArrayInputStream;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class auction_managerController {
    @FXML
    private TextArea prd_description;
    @FXML
    private ImageView prdImage;
    @FXML
    private Button propose_changes, propose_delete;
    @FXML
    private Label currentTime, startingPrice, startDate, startTime, duration, prdName, Status, prdIDbySeller, reasonArea;

    private Runnable onBackAction;
    private Auction currentAuction;
    private Timeline liveClockTimeline;

    // Biến lưu trữ đoạn mô tả gốc để so sánh sự khác biệt
    private String originalDescription = "";

    // Cờ đánh dấu sản phẩm hiện tại có đang có đề xuất thay đổi không
    private boolean hasProposedChanges = false;

    public void initialize() {
        if (reasonArea != null) {
            reasonArea.setVisible(false);
            reasonArea.setManaged(false);
        }

        // Lắng nghe sự thay đổi của TextArea mô tả sản phẩm để bật/tắt nút propose_changes
        if (prd_description != null) {
            prd_description.textProperty().addListener((observable, oldValue, newValue) -> {
                String currentStatus = currentAuction != null && currentAuction.getStatus() != null
                        ? currentAuction.getStatus().toUpperCase() : "";
                updateButtonStates(currentStatus);
            });
        }
    }

    public void setAuctionData(Auction auction) {
        if (auction == null) return;

        this.currentAuction = auction;
        Item item = auction.getItem();
        String currentStatus = auction.getStatus() != null ? auction.getStatus().toUpperCase() : "WAITING";

        // ========================================================
        // 1. DỌN DẸP UI (XÓA ẢNH CŨ VÀ ĐẶT TRẠNG THÁI LOADING CHO MÔ TẢ)
        // ========================================================
        if (prdImage != null) prdImage.setImage(null);
        if (prd_description != null) {
            originalDescription = ""; // Xóa bộ nhớ gốc tạm thời
            hasProposedChanges = false;
            prd_description.setEditable(true);
            prd_description.setText("Đang tải mô tả chi tiết từ hệ thống...");
        }

        // ========================================================
        // 2. ĐẨY DỮ LIỆU TRẠNG THÁI VÀ BỘ ĐẾM THỜI GIAN
        // ========================================================
        updateStatusLabel(currentStatus);

        if (reasonArea != null) {
            if ("SUSPENDED".equals(currentStatus)) {
                reasonArea.setVisible(true);
                reasonArea.setManaged(true);
                reasonArea.setText("Đang tải lý do đình chỉ...");
                fetchSuspendReason(auction.getId());
            } else {
                reasonArea.setVisible(false);
                reasonArea.setManaged(false);
            }
        }

        if (auction.getStart_time() != null) {
            LocalDateTime startDateTime = auction.getStart_time();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

            if (startDate != null) startDate.setText(startDateTime.format(dateFormatter));
            if (startTime != null) startTime.setText(startDateTime.format(timeFormatter));
        }

        if (auction.getStart_time() != null && auction.getEnd_time() != null) {
            java.time.Duration diff = java.time.Duration.between(auction.getStart_time(), auction.getEnd_time());
            long totalHours = diff.toHours();
            long minutes = diff.toMinutes() % 60;
            if (duration != null) duration.setText(String.format("%02d:%02d", totalHours, minutes));
        }

        startLiveClock(auction.getStart_time(), auction.getEnd_time());

        // ========================================================
        // 3. ĐẨY DỮ LIỆU SẢN PHẨM THÔ CÓ SẴN NGAY LẬP TỨC
        // ========================================================
        if (item != null) {
            if (prdName != null) prdName.setText(item.getName() != null ? item.getName() : "Chưa xác định");
            if (prdIDbySeller != null) prdIDbySeller.setText(item.getUser_prdID() != null ? item.getUser_prdID() : "Không có ID");

            if (startingPrice != null) {
                Locale localeVN = new Locale("vi", "VN");
                NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(localeVN);
                startingPrice.setText(currencyFormatter.format(item.getStarting_price()));
            }
        }

        updateButtonStates(currentStatus);

        // ========================================================
        // 4. KÍCH HOẠT LAZY LOADING
        // ========================================================
        fetchDetailedTextDataAndChanges(auction.getId(), auction.getItem_id(), currentStatus);
        fetchProductImageLazy(auction.getId());
    }

    private void updateStatusLabel(String statusText) {
        if (Status != null) {
            Status.setStyle("-fx-text-fill: #FFC107;");
            Status.setText(statusText);
        }
    }

    /**
     * LUỒNG NGẦM 1: Lấy chi tiết Description VÀ lấy Changes (nếu có)
     */
    private void fetchDetailedTextDataAndChanges(int auctionId, int itemId, String currentStatus) {
        new Thread(() -> {
            // Lấy chi tiết mô tả sản phẩm
            Request reqDetail = new Request(auctionId, ActionType.GET_ITEM_DETAIL);
            Response resDetail = ClientSocket.sendRequest(reqDetail);

            // Lấy dữ liệu changes (đề xuất thay đổi)
            Request reqChanges = new Request(itemId, ActionType.SELLER_GET_CHANGES);
            Response resChanges = ClientSocket.sendRequest(reqChanges);

            Platform.runLater(() -> {
                if (resDetail != null && "SUCCESS".equals(resDetail.getStatus()) && resDetail.getData() instanceof Auction detailedAuction) {
                    if (this.currentAuction != null && detailedAuction.getItem() != null) {
                        Item fullItem = detailedAuction.getItem();
                        Item currentItem = this.currentAuction.getItem();

                        if (currentItem != null) {
                            currentItem.setDescription(fullItem.getDescription());
                            currentItem.setCategories(fullItem.getCategories());
                            currentItem.setUser_prdID(fullItem.getUser_prdID());
                            if (prdIDbySeller != null) {
                                prdIDbySeller.setText(fullItem.getUser_prdID() != null && !fullItem.getUser_prdID().trim().isEmpty()
                                        ? fullItem.getUser_prdID()
                                        : "Không có ID");
                            }
                        }

                        // Cập nhật biến gốc
                        originalDescription = fullItem.getDescription() != null ? fullItem.getDescription() : "";

                        // Kiểm tra xem có changes không
                        if (resChanges != null && "SUCCESS".equals(resChanges.getStatus()) && resChanges.getData() != null) {
                            String changes = (String) resChanges.getData();
                            if (!changes.trim().isEmpty()) {
                                hasProposedChanges = true;
                                if (prd_description != null) {
                                    prd_description.setText(changes); // Fill changes vào khung text
                                    prd_description.setEditable(false); // Khóa sửa nếu đã có đề xuất
                                }
                            } else {
                                restoreOriginalDescription();
                            }
                        } else {
                            restoreOriginalDescription();
                        }
                    }

                    // Dùng trạng thái hiện tại của auction (phòng khi DB đã cập nhật là PROPOSAL)
                    String latestStatus = this.currentAuction.getStatus() != null
                            ? this.currentAuction.getStatus().toUpperCase()
                            : currentStatus;
                    updateButtonStates(latestStatus);
                } else {
                    if (prd_description != null) prd_description.setText("Không thể kết nối để tải mô tả chi tiết.");
                }
            });
        }).start();
    }

    private void restoreOriginalDescription() {
        hasProposedChanges = false;
        if (prd_description != null) {
            prd_description.setText(originalDescription.isEmpty() ? "" : originalDescription);
            prd_description.setEditable(true);
        }
    }

    /**
     * LUỒNG NGẦM 2: Lấy mảng byte hình ảnh (Fullsize)
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
                            if (this.currentAuction != null && this.currentAuction.getItem() != null) {
                                this.currentAuction.getItem().setImageBytes(imageBytes);
                            }
                            try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
                                prdImage.setImage(new Image(bis));
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[SELLER HUB] Lỗi dựng hình ảnh lên ImageView tại client!");
                        e.printStackTrace();
                    }
                }
            });
        }).start();
    }

    private void fetchSuspendReason(int auctionId) {
        new Thread(() -> {
            Request req = new Request(auctionId, ActionType.SELLER_GET_REASON);
            Response res = ClientSocket.sendRequest(req);

            Platform.runLater(() -> {
                if (reasonArea != null) {
                    if (res != null && "SUCCESS".equals(res.getStatus()) && res.getData() != null) {
                        reasonArea.setText((String) res.getData());
                    } else {
                        reasonArea.setText("SUSPEND do hết thời gian duyệt");
                    }
                }
            });
        }).start();
    }

    private void updateButtonStates(String currentStatus) {
        if (currentAuction == null) {
            if (propose_changes != null) propose_changes.setDisable(true);
            if (propose_delete != null) propose_delete.setDisable(true);
            return;
        }

        String currentDescText = prd_description != null ? prd_description.getText() : "";
        boolean isLoading = currentDescText.equals("Đang tải mô tả chi tiết từ hệ thống...");
        boolean isDescriptionChanged = !currentDescText.equals(originalDescription) && !isLoading;

        if ("PENDING_APPROVAL".equals(currentStatus)) {
            if (propose_changes != null) {
                propose_changes.setText("Save");
                propose_changes.setDisable(!isDescriptionChanged);
            }
            if (propose_delete != null) {
                propose_delete.setText("Delete Auction");
                propose_delete.setDisable(false);
            }

        } else if ("WAITING".equals(currentStatus) || "PROPOSAL".equals(currentStatus)) {
             if (propose_changes != null) {
                if ("PROPOSAL".equals(currentStatus)) {
                    propose_changes.setText("Delete Proposal");
                    propose_changes.setDisable(false);
                } else {
                    propose_changes.setText("Propose Changes");
                    propose_changes.setDisable(!isDescriptionChanged);
                }
             }
             if (propose_delete != null) {
                propose_delete.setText("Propose Delete");
                propose_delete.setDisable(false);
             }

        } else if ("DELETE_PROPOSAL".equals(currentStatus)) {
             if (propose_changes != null) {
                 propose_changes.setText("Propose Changes");
                 propose_changes.setDisable(true);
             }
             if (propose_delete != null) {
                 propose_delete.setText("Delete Proposal");
                 propose_delete.setDisable(false);
             }
        } else {
            // FINISHED, RUNNING, SUSPENDED
            if (propose_changes != null) propose_changes.setDisable(true);
            if (propose_delete != null) propose_delete.setDisable(true);
        }
    }

    private void startLiveClock(LocalDateTime startTime, LocalDateTime endTime) {
        if (liveClockTimeline != null) {
            liveClockTimeline.stop();
        }

        if (startTime == null || endTime == null) {
            if (currentTime != null) currentTime.setText("N/A");
            return;
        }

        liveClockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            LocalDateTime now = LocalDateTime.now();

            if (now.isBefore(startTime)) {
                java.time.Duration durationToStart = java.time.Duration.between(now, startTime);
                long totalHours = durationToStart.toHours();
                long minutes = durationToStart.toMinutes() % 60;
                long seconds = durationToStart.toSeconds() % 60;

                if (currentTime != null) currentTime.setText(String.format("- %02d:%02d:%02d", totalHours, minutes, seconds));

            } else if (now.isBefore(endTime)) {
                java.time.Duration durationLeft = java.time.Duration.between(now, endTime);
                long totalHours = durationLeft.toHours();
                long minutes = durationLeft.toMinutes() % 60;
                long seconds = durationLeft.toSeconds() % 60;

                if (currentTime != null) currentTime.setText(String.format("%02d:%02d:%02d", totalHours, minutes, seconds));

            } else {
                if (currentTime != null) currentTime.setText("00:00:00");
                liveClockTimeline.stop();
            }
        }));

        liveClockTimeline.setCycleCount(Animation.INDEFINITE);
        liveClockTimeline.play();
    }

    public void setOnBack(Runnable onBackAction) {
        this.onBackAction = onBackAction;
    }

    @FXML
    private void handleBackBtn(ActionEvent event) {
        if (liveClockTimeline != null) liveClockTimeline.stop();
        if (onBackAction != null) onBackAction.run();
    }

    @FXML
    private void handleSuspendAuction(MouseEvent event) {
        if (currentAuction == null) return;

        String currentStatus = currentAuction.getStatus() != null ? currentAuction.getStatus().toUpperCase() : "";

        if ("FINISHED".equals(currentStatus) || "RUNNING".equals(currentStatus) || "SUSPENDED".equals(currentStatus)) {
            return;
        }

        if (propose_delete != null) propose_delete.setDisable(true);
        if (propose_changes != null) propose_changes.setDisable(true);

        new Thread(() -> {
            Request request;
            int itemId = currentAuction.getItem_id();

            if ("PENDING_APPROVAL".equals(currentStatus)) {
                request = new Request(itemId, ActionType.SELLER_DELETE_ITEM);
            } else if ("WAITING".equals(currentStatus) || "PROPOSAL".equals(currentStatus)) {
                request = new Request(itemId, ActionType.SELLER_DELETE_PROPOSAL);
            } else if ("DELETE_PROPOSAL".equals(currentStatus)) {
                // If it's already a delete proposal, pressing the button again (Delete Proposal) should probably cancel it.
                // Assuming we can use ADMIN_REFUSE_DELETE_PROPOSAL or similar logic. Let's just use SELLER_GET_CHANGES to reset or a new action.
                // Since there is no "SELLER_CANCEL_DELETE_PROPOSAL", and this might not be fully fleshed out, let's just make it do nothing or use a hypothetical cancel action.
                // Reverting to WAITING makes sense if they cancel their delete proposal. Let's send a request to cancel it. We can repurpose ADMIN_REFUSE_DELETE_PROPOSAL if the server allows seller, but it's an admin action.
                // We'll leave it simple: they can't cancel it easily from here without a specific action. I'll just return for now.
                Platform.runLater(() -> updateButtonStates(currentStatus));
                return;
            } else {
                Object[] payload = new Object[]{currentAuction.getId(), "Người bán tự hủy phiên"};
                request = new Request(payload, ActionType.ADMIN_STOP_AUCTION); // This seems weird for a seller to call ADMIN_STOP_AUCTION, but keeping existing logic.
            }

            Response response = ClientSocket.sendRequest(request);

            Platform.runLater(() -> {
                if (response != null && "SUCCESS".equals(response.getStatus())) {
                     if ("WAITING".equals(currentStatus) || "PROPOSAL".equals(currentStatus)) {
                        currentAuction.setStatus("DELETE_PROPOSAL");
                        updateStatusLabel("DELETE_PROPOSAL");
                        updateButtonStates("DELETE_PROPOSAL");
                    } else if (onBackAction != null) {
                        onBackAction.run();
                    }
                } else {
                    updateButtonStates(currentStatus);
                }
            });
        }).start();
    }

    @FXML
    private void handleProposeChanges(ActionEvent event) {
        if (currentAuction == null) return;

        String currentStatus = currentAuction.getStatus() != null ? currentAuction.getStatus().toUpperCase() : "";
        int itemId = currentAuction.getItem_id();

        if ("PENDING_APPROVAL".equals(currentStatus)) {
            String newDescription = prd_description != null ? prd_description.getText() : "";

            if (propose_changes != null) propose_changes.setDisable(true);
            if (propose_delete != null) propose_delete.setDisable(true);

            new Thread(() -> {
                Object[] payload = new Object[]{itemId, newDescription};
                Request request = new Request(payload, ActionType.UPDATE_ITEM_DESCRIPTION);

                Response response = ClientSocket.sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        originalDescription = newDescription;
                        System.out.println("[AUCTION MANAGER] Đã cập nhật mô tả thành công.");
                    } else {
                        System.err.println("[AUCTION MANAGER] Cập nhật mô tả thất bại.");
                    }

                    updateButtonStates(currentStatus);
                });
            }).start();

        } else if ("WAITING".equals(currentStatus)) {
            if (propose_changes != null) propose_changes.setDisable(true);

            // =============== CREATE PROPOSAL ===============
            String newDescription = prd_description != null ? prd_description.getText() : "";

            new Thread(() -> {
                Object[] payload = new Object[]{itemId, newDescription};
                Request request = new Request(payload, ActionType.PROPOSE_CHANGES);
                Response response = ClientSocket.sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        System.out.println("[AUCTION MANAGER] Đã tạo đề xuất thay đổi thành công.");
                        hasProposedChanges = true;
                        if (prd_description != null) {
                            prd_description.setEditable(false); // Khóa textarea lại
                        }

                        // Chuyển trạng thái nội bộ sang PROPOSAL để UI tự cập nhật lại
                        currentAuction.setStatus("PROPOSAL");
                        updateStatusLabel("PROPOSAL");
                        updateButtonStates("PROPOSAL");

                    } else {
                        System.err.println("[AUCTION MANAGER] Tạo đề xuất thất bại.");
                        updateButtonStates(currentStatus);
                    }
                });
            }).start();

        } else if ("PROPOSAL".equals(currentStatus)) {
            if (propose_changes != null) propose_changes.setDisable(true);

            // =============== DELETE PROPOSAL ===============
            new Thread(() -> {
                Request request = new Request(itemId, ActionType.SELLER_DELETE_CHANGES);
                Response response = ClientSocket.sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        System.out.println("[AUCTION MANAGER] Đã xóa đề xuất thay đổi.");
                        restoreOriginalDescription(); // Phục hồi desc gốc và mở khóa edit

                        // Trả trạng thái nội bộ về lại WAITING
                        currentAuction.setStatus("WAITING");
                        updateStatusLabel("WAITING");
                        updateButtonStates("WAITING");

                    } else {
                        System.err.println("[AUCTION MANAGER] Xóa đề xuất thất bại.");
                        updateButtonStates(currentStatus);
                    }
                });
            }).start();
        }
    }
}
