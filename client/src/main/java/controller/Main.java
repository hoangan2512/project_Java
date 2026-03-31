package controller;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Group root = new Group();
        Scene scene = new Scene(root, Color.BLACK);
        Image icon = new Image(getClass().getResourceAsStream("/logo_project.jpg"));
        primaryStage.getIcons().add(icon);

        primaryStage.setTitle("BidHub");
        primaryStage.setFullScreen(true);
        primaryStage.setFullScreenExitHint("Bấm ESC mà thoát thg ngu ạ");

        primaryStage.setScene(scene);
        primaryStage.show();
    }
}