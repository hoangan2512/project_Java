import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import network.ClientSocket;

import java.io.IOException;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        // --- KIỂM TRA KẾT NỐI SERVER ---
        System.out.println("Đang khởi động Client, kiểm tra kết nối Server...");
        boolean isConnected = ClientSocket.tryConnect();
        
        if (!isConnected) {
            System.out.println("WARNING: Kết nối tới Server không thành công!");
            System.out.println("Ứng dụng vẫn sẽ mở, nhưng hầu hết các chức năng sẽ không hoạt động.");
            System.out.println("Vui lòng bật Server (AuctionServer) sau đó thao tác lại.");
        } else {
            System.out.println("SUCCESS: Đã kết nối thành công tới Server.");
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/mainPage.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        Image icon = new Image(getClass().getResourceAsStream("/image/logo_project_2.jpg"));
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
