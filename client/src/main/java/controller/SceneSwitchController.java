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

    // 1. Chuyển sang Sign In (Thay thế toàn bộ nội dung cửa sổ hiện tại)
    public void switchToSignIn(Event e) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/view/signIn.fxml"));
        // Lấy Stage hiện tại từ sự kiện (Event)
        Stage stage = (Stage)((Node)e.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    // 2. Chuyển sang MainPage (Thay thế toàn bộ nội dung cửa sổ hiện tại)
    // Dùng Event e thay cho ActionEvent e để hỗ trợ cả MouseEvent (nếu cần)
    public void switchToMainPage(Event e) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/view/mainPage.fxml"));
        Stage stage = (Stage)((Node)e.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    // 3. Mở Sign In dưới dạng Popup (Trang Main vẫn nằm ở dưới)
    public void openSignInPopup() throws IOException {
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
    }
}
