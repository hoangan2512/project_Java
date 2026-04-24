package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.TilePane;

import java.io.IOException;

public class customSearchController {

    @FXML
    private TilePane productGrid;

    private final SceneSwitchController sceneSwitcher = new SceneSwitchController();

    public void initialize() {
        loadProducts();
    }

    private void loadProducts() {
        productGrid.getChildren().clear();

        // Giả lập load 10 sản phẩm từ Database
        for (int i = 1; i <= 15; i++) {
            try {
                // LOAD FILE FXML CHO TỪNG CARD SẢN PHẨM
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/prd_preview.fxml"));
                Node productCard = loader.load();

                // --- NẾU BẠN CÓ CONTROLLER CHO CARD THÌ BƠM DỮ LIỆU Ở ĐÂY ---
                // prdPreviewController cardController = loader.getController();
                // cardController.setProductData("Sản phẩm số " + i, "Giá: " + (i * 100) + ".000đ");

                // THÊM CARD VÀO LƯỚI TILEPANE
                productGrid.getChildren().add(productCard);

            } catch (IOException e) {
                System.err.println("Lỗi: Không thể load file prd_preview.fxml tại vòng lặp thứ " + i);
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleFilter(ActionEvent event) {
        try {
            sceneSwitcher.openFilter();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Lỗi chuyển cảnh");
        }
    }
}