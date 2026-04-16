package controller.sellerHub.new_item_page;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Arrays;
import java.util.List;

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
    @FXML
    private Button picInsertBtn;
    @FXML
    private Label Status;

    private List<ImageView> imageViews;

    public void initialize() {
        // Tải ảnh sp
        prd_previewImage.setPreserveRatio(true);
        prd_previewImage.setSmooth(true);
        imageViews = Arrays.asList(prd_previewImage, prdImage1, prdImage2, prdImage3, prdImage4, prdImage5, prdImage6);

    }

    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();

        // Thiết lập bộ lọc để chỉ chọn file ảnh
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        // Hiển thị cửa sổ chọn file
        // Get window từ bất kỳ node nào đang hiển thị (ví dụ productImageView)
        Stage stage = (Stage) prd_previewImage.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            // 1. Lấy đường dẫn file (để sau này lưu vào Database)
            String imagePath = selectedFile.toURI().toString();
            boolean assigned = false;

            // 2. Hiển thị ảnh preview lên ImageView
            Image image = new Image(imagePath);
            for (ImageView iv : imageViews) {
                if (iv.getImage() == null) {
                    iv.setImage(image);
                    assigned = true;
                    Status.setVisible(true);
                    Status.setText("Đã thêm ảnh: " + imagePath);
                    break;
                }
            }

            if (!assigned) {
                Status.setVisible(true);
                Status.setText("Uploaded 7/7 pictures");
            }
            // 3. (Tùy chọn) Lưu đường dẫn vào Model để dùng cho Phase 3
            // currentProduct.setImagePath(selectedFile.getAbsolutePath());
        } else {
            System.out.println("Image uploading canceled.");
        }
    }

    public Image getImg() { return prd_previewImage.getImage(); }

    @FXML
    private void handleDeleteImage(ActionEvent event) {
        // Lấy nút X
        Button clickedButton = (Button) event.getSource();

        // Lấy cha của nút X (chính là cái StackPane chứa ảnh và nút)
        StackPane parent = (StackPane) clickedButton.getParent();

        // Tìm thằng con nào là ImageView trong cái StackPane đó và xóa ảnh
        for (Node node : parent.getChildren()) {
            if (node instanceof ImageView) {
                ((ImageView) node).setImage(null);
                Status.setVisible(true);
                Status.setText("Image deleted");
                break;
            }
        }
    }
}
