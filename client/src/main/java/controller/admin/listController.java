package controller.admin;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import model.User;

import java.util.Map;

public class listController {

    @FXML
    private Label col1, col2, col3, col4, col5, col6, col7;

    /**
     * Generic method to populate row data based on the columns
     * order matching the headers in homepage.fxml.
     * @param rowData Array of strings representing the data for each column
     */
    public void setRowData(String[] rowData) {
        if (rowData != null) {
            col1.setText(rowData.length > 0 ? rowData[0] : "");
            col2.setText(rowData.length > 1 ? rowData[1] : "");
            col3.setText(rowData.length > 2 ? rowData[2] : "");
            col4.setText(rowData.length > 3 ? rowData[3] : "");
            col5.setText(rowData.length > 4 ? rowData[4] : "");
            col6.setText(rowData.length > 5 ? rowData[5] : "");
            col7.setText(rowData.length > 6 ? rowData[6] : "");
            
            // Apply conditional styling for generic status if present in col7
            if (rowData.length > 6 && rowData[6] != null) {
                String status = rowData[6].toUpperCase();
                if (status.equals("ACTIVE") || status.equals("APPROVED") || status.equals("RUNNING")) {
                    col7.setStyle("-fx-text-fill: #4CAF50;");
                } else if (status.equals("BANNED") || status.equals("REJECTED") || status.equals("SUSPENDED")) {
                    col7.setStyle("-fx-text-fill: #F44336;");
                } else if (status.equals("PENDING_APPROVAL") || status.equals("WAITING")) {
                    col7.setStyle("-fx-text-fill: #FFC107;");
                } else if (status.equals("FINISHED")) {
                    col7.setStyle("-fx-text-fill: #919191;");
                }else {
                    col7.setStyle("-fx-text-fill: WHITE;");
                }
            }
        }
    }
}
