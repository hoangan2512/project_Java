package controller.admin;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import model.User;

import java.util.Map;

public class listController {

    @FXML
    private Label col1, col2, col3, col4, col5, col6, col7;

    /**
     * Populates a row in the list with summary data for a single user.
     * This method expects a Map containing the User object and their stats.
     * @param userDataMap A map containing user details and their item/auction/warning counts.
     */
    public void setUserData(Map<String, Object> userDataMap) {
        User user = (User) userDataMap.get("user");
        // Use getOrDefault to avoid NullPointerException if counts are missing.
        int itemsCount = ((Number) userDataMap.getOrDefault("itemsCount", 0)).intValue();
        int auctionsCount = ((Number) userDataMap.getOrDefault("auctionsCount", 0)).intValue();
        int warningsCount = ((Number) userDataMap.getOrDefault("warningsCount", 0)).intValue();

        if (user != null) {
            col1.setText(String.valueOf(user.getID()));
            col2.setText(user.getUsername());
            col3.setText(user.getRole());
            col4.setText(String.valueOf(itemsCount));
            col5.setText(String.valueOf(auctionsCount));
            col6.setText(String.valueOf(warningsCount));
            col7.setText(user.getStatus() != null ? user.getStatus() : "ACTIVE");

            // --- Apply conditional styling ---

            // Style for Status: Green for ACTIVE, Red for others (e.g., BANNED)
            if ("ACTIVE".equalsIgnoreCase(user.getStatus())) {
                col7.setStyle("-fx-text-fill: #4CAF50;"); // Green color
            } else {
                col7.setStyle("-fx-text-fill: #F44336;"); // Red color
            }

            // Style for Warnings: Amber/Yellow if count > 0
            if (warningsCount > 0) {
                col6.setStyle("-fx-text-fill: #FFC107;"); // Amber color
            } else {
                col6.setStyle("-fx-text-fill: WHITE;");
            }
        }
    }
}
