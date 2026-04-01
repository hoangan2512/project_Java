import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/sample.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        Image icon = new Image(getClass().getResourceAsStream("/logo_project.jpg"));
        primaryStage.getIcons().add(icon);
        primaryStage.setTitle("BidHub");

        primaryStage.setScene(scene);
        primaryStage.show();
    }
}