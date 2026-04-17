package controller;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import model.User;

import java.io.IOException;
import java.util.function.UnaryOperator;

public class prdPageController {

    @FXML
    private Label currentPrice;
    @FXML
    private ImageView prdImage;
    @FXML
    private Label prdName;
    @FXML
    private TextField bidAmount;
    @FXML
    private StackPane imgPane;
    @FXML
    private ImageView BidHub;
    @FXML
    private Button Bid;
    @FXML
    private Label timeLeft;
    @FXML
    private LineChart<Number, Number> priceChart;
    @FXML
    private NumberAxis xAxis;
    @FXML
    private NumberAxis yAxis;

    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();

    public void initialize() {
        // Tải ảnh sp (default)
        String ImgPath = "/image/prd/qualophihanhgia.jpg";
        Image prdImg = new Image(getClass().getResourceAsStream(ImgPath));
        prdImage.setPreserveRatio(true);
        prdImage.setSmooth(true);
        prdImage.setImage(prdImg);

        // Chặn nhập chữ vào ô bidAmount ngay khi màn hình vừa hiện lên
        UnaryOperator<TextFormatter.Change> filter = change -> {
            if (change.getControlNewText().matches("\\d*")) {
                return change;
            }
            return null;
        };
        bidAmount.setTextFormatter(new TextFormatter<>(filter));
        // price_chart
    }


    public void handleBidBtn(MouseEvent event) throws IOException {
        if (SessionManager.getInstance().isBidder()) {
            String bid_amount = bidAmount.getText();
            System.out.println("Bidding: " + bid_amount);
        } else {
            sceneSwitcher.openSignInPopup(null);
        }
    }

    public void setData(String name, long price, long time, String imagePath) {
        // 1. Set tên sản phẩm
        if (prdName != null) {
            prdName.setText(name);
        }

        // 2. Định dạng và set giá tiền
        if (currentPrice != null) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
            symbols.setGroupingSeparator('.');
            DecimalFormat formatter = new DecimalFormat("###,###", symbols);
            currentPrice.setText(formatter.format(price) + " VNĐ");
        }

        // 3. Định dạng và set thời gian còn lại
        if (timeLeft != null) {
            long hours = time / 3600;
            long minutes = (time % 3600) / 60;
            long seconds = time % 60;
            String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            timeLeft.setText(timeString);
        }

        System.out.println("Đã hiển thị chi tiết sản phẩm: " + name);
    }
}
