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
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.util.function.UnaryOperator;

public class prdPageController {

    @FXML
    private Label currrentPrice;
    @FXML
    private ImageView prdImage;
    @FXML
    private Circle userAvatar;
    @FXML
    private Circle searchBtn;
    @FXML
    private TextField searchBar;
    @FXML
    private Button CustomSearch;
    @FXML
    private Button SellerHub;
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
        // tải ảnh avt
        Image usr_img = new Image(getClass().getResourceAsStream("/image/avatar1.png"));
        userAvatar.setFill(new ImagePattern(usr_img));

        Image search_img = new Image(getClass().getResourceAsStream("/image/search_icon1.png"));
        searchBtn.setFill(new ImagePattern(search_img));
        // Tải ảnh sp
        String ImgPath = "/image/prd/qualophihanhgia.jpg";
        Image prdImg = new Image(getClass().getResourceAsStream(ImgPath));
        prdImage.setPreserveRatio(true);
        prdImage.setSmooth(true);

        prdImage.setImage(prdImg);

        //set prd_name - prd_price
        String prd_name = "Quả lọ phi hành gia";
        long current_price = 10000000;
        prdName.setText(prd_name);
        // định dạng giá trị current_price
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("###,###,###", symbols);
        String formattedPrice = formatter.format(current_price);

        //set time remaining
        long secondsLeft = 3665;
        long hours = secondsLeft / 3600;
        long minutes = (secondsLeft % 3600) / 60;
        long seconds = secondsLeft % 60;
        // định dạng thời gian
        String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        timeLeft.setText(timeString);

        currrentPrice.setText(formattedPrice + " VNĐ");
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getControlNewText();
            if (text.matches("\\d*")) {
                return change;
            }
            return null;
        };
        TextFormatter<String> textFormatter = new TextFormatter<>(filter);
        bidAmount.setTextFormatter(textFormatter);

        // price_chart
    }

    public void handleAvatarClick(MouseEvent event) {
        try {
            sceneSwitcher.openSignInPopup();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Lỗi chuyển cảnh");
        }
    }

    public void handleSearchBtnClick(MouseEvent event) {
        String searchText = searchBar.getText();
        if (searchText == null || searchText.trim().isEmpty()) {
            searchBar.requestFocus();
        } else {
            System.out.println("searching");
            try {
                sceneSwitcher.switchToPrdPage(event);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Lỗi chuyển cảnh");
            }
        }
    }

    public void handleSellerHub(MouseEvent event) {
        try {
            sceneSwitcher.switchToSellerSignIn(event);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Lỗi chuyển cảnh");
        }
    }

    public void handleHomePage(MouseEvent event) {
        try {
            sceneSwitcher.switchToMainPage(event);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Lỗi chuyển cảnh");
        }
    }

    public void handleBidBtn(MouseEvent event) {
        String bid_amount = bidAmount.getText();
        System.out.println("Bidding: " + bid_amount);
    }
}
