package controller.admin;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import model.User;

import java.time.LocalDateTime;
import java.util.Map;

public class listController {

    @FXML
    private Label col1, col2, col3, col4, col5, col6, col7;

    private Timeline countdownTimeline;

    public void setRowData(String[] rowData) {
        if (rowData != null) {
            col1.setText(rowData.length > 0 ? rowData[0] : "");
            col2.setText(rowData.length > 1 ? rowData[1] : "");
            col3.setText(rowData.length > 2 ? rowData[2] : "");
            col4.setText(rowData.length > 3 ? rowData[3] : "");
            col5.setText(rowData.length > 4 ? rowData[4] : "");
            col6.setText(rowData.length > 5 ? rowData[5] : "");
            col7.setText(rowData.length > 6 ? rowData[6] : "");

            // Apply conditional styling for generic status if present in col7
            if (rowData.length > 6 && rowData[6] != null) {
                String status = rowData[6].toUpperCase();
                if (status.equals("ACTIVE") || status.equals("APPROVED") || status.equals("RUNNING")) {
                    col7.setStyle("-fx-text-fill: #4CAF50;");
                } else if (status.equals("BANNED") || status.equals("REJECTED") || status.equals("SUSPENDED")) {
                    col7.setStyle("-fx-text-fill: #F44336;");
                } else if (status.equals("PENDING_APPROVAL") || status.equals("WAITING")) {
                    col7.setStyle("-fx-text-fill: #FFC107;");
                } else if (status.equals("FINISHED")) {
                    col7.setStyle("-fx-text-fill: #919191;");
                }else {
                    col7.setStyle("-fx-text-fill: WHITE;");
                }
            }
        }
    }

    public void startCountdown(LocalDateTime startTime, LocalDateTime endTime) {
        // Khử bộ đếm cũ nếu có (phòng trường hợp bấm chuyển tab liên tục gây chồng luồng)
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        if (startTime == null || endTime == null) {
            if (col5 != null) col5.setText("N/A");
            return;
        }

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            LocalDateTime now = LocalDateTime.now();

            if (now.isBefore(startTime)) {
                // 1. Trường hợp: CHƯA ĐẾN GIỜ MỞ CỬA -> Đếm ngược đến lúc Start (Thêm dấu - ở đầu)
                java.time.Duration durationToStart = java.time.Duration.between(now, startTime);
                long totalHours = durationToStart.toHours();
                long minutes = durationToStart.toMinutes() % 60;
                long seconds = durationToStart.toSeconds() % 60;

                if (col5 != null) {
                    col5.setText(String.format("- %02d:%02d:%02d", totalHours, minutes, seconds));
                }

            } else if (now.isBefore(endTime)) {
                // 2. Trường hợp: PHIÊN ĐANG CHẠY -> Đếm ngược thời gian còn lại đến lúc Kết thúc
                java.time.Duration durationLeft = java.time.Duration.between(now, endTime);
                long totalHours = durationLeft.toHours();
                long minutes = durationLeft.toMinutes() % 60;
                long seconds = durationLeft.toSeconds() % 60;

                if (col5 != null) {
                    col5.setText(String.format("%02d:%02d:%02d", totalHours, minutes, seconds));
                }

            } else {
                // 3. Trường hợp: PHIÊN ĐÃ KẾT THÚC
                if (col5 != null) {
                    col5.setText("00:00:00");
                }
                countdownTimeline.stop(); // Dừng luồng chạy ngầm để tiết kiệm CPU
            }
        }));

        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

    /**
     * Hàm giải phóng tài nguyên khi hàng bị xóa hoặc làm mới danh sách
     */
    public void stopTimeline() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
    }

    public interface OnRowClickListener {
        void onClick();
    }

    private OnRowClickListener rowClickListener;

    // Hàm để homepageController truyền hành động vào
    public void setOnRowClick(OnRowClickListener listener) {
        this.rowClickListener = listener;
    }

    // Bắt sự kiện click vào dòng này (Gán hàm này vào thuộc tính OnMouseClicked của thẻ cha trong file FXML)
    @FXML
    private void handleRowClick(MouseEvent event) {
        if (rowClickListener != null) {
            rowClickListener.onClick();
        }
    }
}