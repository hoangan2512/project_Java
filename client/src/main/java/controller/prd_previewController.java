package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import controller.mainPageController;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class prd_previewController {
    @FXML
    private Label prdName;
    @FXML
    private Label currentPrice;
    @FXML
    private Button Bid;
    @FXML
    private ImageView prdImage;
    @FXML
    private Label auctionStatus;

    private Runnable toPrdPage;

    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();

    public void setData(String name, long price, long time, String imagePath) {
        prdName.setText(name);

        // --- BẮT ĐẦU ĐỊNH DẠNG TIỀN TỆ ---
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.'); // Thiết lập dấu phân cách là dấu chấm

        // Mẫu định dạng: ###,### (ngăn cách mỗi 3 chữ số)
        DecimalFormat formatter = new DecimalFormat("###,###", symbols);
        String formattedPrice = formatter.format(price);

        currentPrice.setText(formattedPrice + " VNĐ");
        // --- KẾT THÚC ĐỊNH DẠNG ---

        if (auctionStatus != null) {
            long hours = time / 3600;
            long minutes = (time % 3600) / 60;
            long seconds = time % 60;
            String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            auctionStatus.setText(timeString);
        }
    }

    // Hàm này để MainPage truyền lệnh vào
    public void setOnBidAction(Runnable action) {
        this.toPrdPage = action;
    }

    public void handleBidBtn(MouseEvent event) {
        if (toPrdPage != null) {
            toPrdPage.run(); // Kích hoạt lệnh mà MainPage đã giao
        }
    }
}
