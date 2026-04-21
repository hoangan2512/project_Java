package controller.sellerHub.new_item_page;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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

        Stage stage = (Stage) prd_previewImage.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            // Lấy đường dẫn file dạng URI để JavaFX có thể đọc được
            String imagePath = selectedFile.toURI().toString();
            boolean assigned = false;

            // Hiển thị ảnh preview lên ImageView
            Image image = new Image(imagePath);
            for (ImageView iv : imageViews) {
                if (iv.getImage() == null) {
                    iv.setImage(image);
                    assigned = true;
                    Status.setVisible(true);
                    Status.setText("Đã thêm ảnh: " + selectedFile.getName()); // Hiển thị tên file cho gọn
                    break;
                }
            }

            if (!assigned) {
                Status.setVisible(true);
                Status.setText("Uploaded 7/7 pictures");
            }
        } else {
            System.out.println("Image uploading canceled.");
        }
    }

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

    // ==========================================
    // CÁC HÀM GETTER LẤY ĐƯỜNG DẪN ẢNH ĐỂ LƯU DB
    // ==========================================

    public String getImgPath() {
        if (prd_previewImage.getImage() != null) {
            return prd_previewImage.getImage().getUrl();
        }
        return null;
    }

    public String getImgPath1() {
        if (prdImage1.getImage() != null) {
            return prdImage1.getImage().getUrl();
        }
        return null;
    }

    public String getImgPath2() {
        if (prdImage2.getImage() != null) {
            return prdImage2.getImage().getUrl();
        }
        return null;
    }

    public String getImgPath3() {
        if (prdImage3.getImage() != null) {
            return prdImage3.getImage().getUrl();
        }
        return null;
    }

    public String getImgPath4() {
        if (prdImage4.getImage() != null) {
            return prdImage4.getImage().getUrl();
        }
        return null;
    }

    public String getImgPath5() {
        if (prdImage5.getImage() != null) {
            return prdImage5.getImage().getUrl();
        }
        return null;
    }

    public String getImgPath6() {
        if (prdImage6.getImage() != null) {
            return prdImage6.getImage().getUrl();
        }
        return null;
    }

    public void setDraftData(ProductDraftDTO draft) {
        if (draft == null) return;

        if (draft.getImgPath() != null) prd_previewImage.setImage(new Image(draft.getImgPath()));
        if (draft.getImgPath1() != null) prdImage1.setImage(new Image(draft.getImgPath1()));
        if (draft.getImgPath2() != null) prdImage2.setImage(new Image(draft.getImgPath2()));
        if (draft.getImgPath3() != null) prdImage3.setImage(new Image(draft.getImgPath3()));
        if (draft.getImgPath4() != null) prdImage4.setImage(new Image(draft.getImgPath4()));
        if (draft.getImgPath5() != null) prdImage5.setImage(new Image(draft.getImgPath5()));
        if (draft.getImgPath6() != null) prdImage6.setImage(new Image(draft.getImgPath6()));
    }
}