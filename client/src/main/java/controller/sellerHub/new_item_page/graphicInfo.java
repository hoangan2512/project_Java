package controller.sellerHub.new_item_page;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class graphicInfo {

    @FXML
    private ImageView prd_previewImage;
    @FXML
    private ImageView prdImage1;
    @FXML
    private ImageView prdImage2;
    @FXML
    private ImageView prdImage3;
    @FXML
    private ImageView prdImage4;
    @FXML
    private ImageView prdImage5;
    @FXML
    private ImageView prdImage6;

    public void initialize() {
        // Tải ảnh sp
        String ImgPath = "/image/prd/qualophihanhgia.jpg";
        Image prdImg = new Image(getClass().getResourceAsStream(ImgPath));
        prd_previewImage.setPreserveRatio(true);
        prd_previewImage.setSmooth(true);
        prd_previewImage.setImage(prdImg);

    }
}
