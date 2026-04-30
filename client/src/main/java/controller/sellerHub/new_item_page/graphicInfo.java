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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Nhớ import class ProductDraftDTO của bạn nếu nó nằm ở package khác
// import model.ProductDraftDTO;

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

    // Map lưu trữ đường dẫn chuẩn (Tuyệt đối) của file ảnh để gửi lên Server
    private final Map<ImageView, String> imagePathMap = new HashMap<>();

    public void initialize() {
        // Tải cấu hình ảnh
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
            // Lấy đường dẫn chuẩn của hệ điều hành (VD: C:\Users\anh.jpg)
            String absolutePath = selectedFile.getAbsolutePath();
            boolean assigned = false;

            // Cần toURI().toString() để JavaFX có thể vẽ ảnh lên màn hình
            Image image = new Image(selectedFile.toURI().toString());

            for (ImageView iv : imageViews) {
                if (iv.getImage() == null) {
                    iv.setImage(image);

                    // Lưu đường dẫn chuẩn vào Map
                    imagePathMap.put(iv, absolutePath);

                    assigned = true;
                    Status.setVisible(true);
                    Status.setText("Đã thêm ảnh: " + selectedFile.getName());
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
                ImageView iv = (ImageView) node;
                iv.setImage(null);

                // Đồng thời xóa đường dẫn ảnh tương ứng trong Map
                imagePathMap.remove(iv);

                Status.setVisible(true);
                Status.setText("Image deleted");
                break;
            }
        }
    }

    // ==========================================
    // CÁC HÀM GETTER LẤY ĐƯỜNG DẪN ẢNH ĐỂ LƯU DB
    // ==========================================

    public String getImgPath() { return imagePathMap.get(prd_previewImage); }
    public String getImgPath1() { return imagePathMap.get(prdImage1); }
    public String getImgPath2() { return imagePathMap.get(prdImage2); }
    public String getImgPath3() { return imagePathMap.get(prdImage3); }
    public String getImgPath4() { return imagePathMap.get(prdImage4); }
    public String getImgPath5() { return imagePathMap.get(prdImage5); }
    public String getImgPath6() { return imagePathMap.get(prdImage6); }

    // ==========================================
    // LOAD DỮ LIỆU BẢN NHÁP (DRAFT)
    // ==========================================
    public void setDraftData(ProductDraftDTO draft) {
        if (draft == null) return;

        loadDraftImage(prd_previewImage, draft.getImgPath());
        loadDraftImage(prdImage1, draft.getImgPath1());
        loadDraftImage(prdImage2, draft.getImgPath2());
        loadDraftImage(prdImage3, draft.getImgPath3());
        loadDraftImage(prdImage4, draft.getImgPath4());
        loadDraftImage(prdImage5, draft.getImgPath5());
        loadDraftImage(prdImage6, draft.getImgPath6());
    }

    // Hàm hỗ trợ load ảnh từ Draft và lưu ngược lại vào Map
    private void loadDraftImage(ImageView iv, String path) {
        if (path != null && !path.trim().isEmpty()) {
            try {
                File file = new File(path);
                if (file.exists()) {
                    iv.setImage(new Image(file.toURI().toString()));
                    imagePathMap.put(iv, file.getAbsolutePath());
                } else {
                    // Nếu là đường dẫn project tương đối
                    iv.setImage(new Image(path));
                    imagePathMap.put(iv, path);
                }
            } catch (Exception e) {
                System.err.println("Không thể load ảnh từ Draft: " + path);
            }
        }
    }
}