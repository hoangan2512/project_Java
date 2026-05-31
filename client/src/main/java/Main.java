import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import network.ClientSocket;

import java.io.IOException;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        ClientSocket.setPrimaryStage(primaryStage);

        // --- KIỂM TRA KẾT NỐI SERVER ---
        boolean isConnected = ClientSocket.tryConnect();
        
        if (!isConnected) {
            System.out.println("WARNING: Unable to connect to the server!");
        } else {
            System.out.println("SUCCESS: Successfully connected to the server.");
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/welcome.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        Image icon = new Image(getClass().getResourceAsStream("/image/logo_project_2-removebg-preview.png"));
        primaryStage.getIcons().add(icon);
        primaryStage.setTitle("BidHub");
        primaryStage.setResizable(false);

        primaryStage.setScene(scene);
        primaryStage.show();
        
        // --- ĐÓNG KẾT NỐI KHI TẮT APP ---
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Đang đóng ứng dụng. Ngắt kết nối Server...");
            ClientSocket.disconnect();
            System.exit(0);
        });
    }
}
