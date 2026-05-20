package controller;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import model.SearchCriteria;

import java.io.IOException;

public class SceneSwitchController {

    // Chuyển sang SignIn (Thay thế toàn bộ nội dung cửa sổ hiện tại)
    public void switchToSignIn(Event e) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/view/signIn.fxml"));
        Stage stage = (Stage)((Node)e.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void switchToMainPage(Event e) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/view/mainPage.fxml"));
        Stage stage = (Stage)((Node)e.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    // Mở Sign In dưới dạng Popup
    public void openSignInPopup(Runnable callback) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/signIn.fxml"));
        Parent root = loader.load();

        Stage popupStage = new Stage();
        popupStage.setTitle("SignIn - LogIn");

        // KHÓA cửa sổ chính bên dưới, bắt buộc tương tác với Popup trước
        popupStage.initModality(Modality.APPLICATION_MODAL);

        Scene scene = new Scene(root);
        popupStage.setScene(scene);

        // Hiển thị và đợi người dùng đóng cửa sổ này mới thực hiện code tiếp theo (nếu có)
        popupStage.showAndWait();

        if (mainPageController.getInstance() != null) {
            mainPageController.getInstance().updateAvatarUI();
        }

        if (callback != null) {
            callback.run();
        }
    }

    // Hỗ trợ truyền tham số cho bộ lọc
    public void openFilter(SearchCriteria currentCriteria) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/filter.fxml"));
        Parent root = loader.load();

        filterController controller = loader.getController();
        if (controller != null && currentCriteria != null) {
            controller.setInitialCriteria(currentCriteria);
        }

        Stage popupStage = new Stage();
        popupStage.setTitle("BidHub: Search Engine");

        // KHÓA cửa sổ chính bên dưới, bắt buộc tương tác với Popup trước
        popupStage.initModality(Modality.APPLICATION_MODAL);

        Scene scene = new Scene(root);
        popupStage.setScene(scene);

        // Hiển thị và đợi người dùng đóng cửa sổ này mới thực hiện code tiếp theo (nếu có)
        popupStage.showAndWait();
    }

    // Overload cho hàm không tham số để code cũ không lỗi
    public void openFilter() throws IOException {
        openFilter(null);
    }

    public void openBidded() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/bidded.fxml"));
        Parent root = loader.load();

        Stage popupStage = new Stage();
        popupStage.setTitle("BidHub: Bid successful");

        // KHÓA cửa sổ chính bên dưới, bắt buộc tương tác với Popup trước
        popupStage.initModality(Modality.APPLICATION_MODAL);

        Scene scene = new Scene(root);
        popupStage.setScene(scene);

        // Hiển thị và đợi người dùng đóng cửa sổ này mới thực hiện code tiếp theo (nếu có)
        popupStage.showAndWait();
    }

    public void switchToSellerHub(Event e) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/view/sellerHub/sellerHub_homepage.fxml"));
        Stage stage = (Stage)((Node)e.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void switchToSellerSignIn(Event e) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/view/sellerHub/sellerHub_signin.fxml"));
        Stage stage = (Stage)((Node)e.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void switchToPrdPage(Event e) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/view/productPage.fxml"));
        Stage stage = (Stage)((Node)e.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void switchToCustomSearch(Event e) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/view/customSearch.fxml"));
        Stage stage = (Stage)((Node)e.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void switchToAdminConsole(Event e) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/view/admin/homepage.fxml"));
        Stage stage = (Stage)((Node)e.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void openWinner(String productName) {
        try {
            // 1. Load file FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/winner.fxml")); // Sửa lại đường dẫn fxml cho đúng
            Parent root = loader.load();

            // 2. LẤY CONTROLLER VÀ TRUYỀN DỮ LIỆU VÀO
            winnerController controller = loader.getController();
            controller.setProductName(productName);

            // 3. Hiển thị Popup
            Stage stage = new Stage();
            stage.setTitle("Chúc mừng!");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            System.out.println("Lỗi khi mở popup winner: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
