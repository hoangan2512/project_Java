package controller;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

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

        if (callback != null) {
            callback.run();
        }
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
}
