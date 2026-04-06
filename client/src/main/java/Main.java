import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/mainPage.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        Image icon = new Image(getClass().getResourceAsStream("/image/logo_project_2.jpg"));
        primaryStage.getIcons().add(icon);
        primaryStage.setTitle("BidHub");
        primaryStage.setFullScreen(true);

        primaryStage.setScene(scene);
        primaryStage.show();
    }
}