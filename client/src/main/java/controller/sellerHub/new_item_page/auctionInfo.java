package controller.sellerHub.new_item_page;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.Locale;

public class auctionInfo {
    @FXML
    private ChoiceBox<String> duration_h, duration_m, start_h, start_m;
    @FXML
    private TextField prdPrice;
    @FXML
    private DatePicker start_date;

    public void initialize() {
        duration_h.getItems().addAll("0", "1","2","3","4","5","6","7","8","9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20","21","22", "23", "24");
        duration_m.getItems().addAll("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59");
        start_h.getItems().addAll("0", "1","2","3","4","5","6","7","8","9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20","21","22", "23", "24");
        start_m.getItems().addAll("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59");
        
        // Đặt giá trị mặc định để tránh null
        duration_h.setValue("0");
        duration_m.setValue("0");
        start_h.setValue("0");
        start_m.setValue("0");

        // Gắn hiệu ứng định dạng tiền tệ cho ô nhập giá
        addCurrencyFormat(prdPrice);
    }

    private void addCurrencyFormat(TextField textField) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);

        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;

            // Xóa bỏ tất cả các ký tự không phải là số
            String numericString = newValue.replaceAll("[^\\d]", "");
            if (numericString.isEmpty()) {
                textField.setText("");
                return;
            }

            try {
                long value = Long.parseLong(numericString);
                String formattedString = formatter.format(value);

                if (!newValue.equals(formattedString)) {
                    textField.setText(formattedString);
                    Platform.runLater(() -> textField.positionCaret(formattedString.length()));
                }
            } catch (NumberFormatException e) {
                textField.setText(oldValue);
            }
        });
    }

    // Trả về chuỗi số nguyên chất (đã loại bỏ dấu chấm) để hệ thống xử lý logic
    public String getPrice() { 
        String text = prdPrice.getText();
        if (text == null) return "";
        return text.replaceAll("\\.", "");
    }
    
    // Gộp ngày và giờ bắt đầu thành 1 chuỗi
    public String getTime() { 
        LocalDate date = start_date.getValue();
        if (date == null) return null; // Trả về null nếu chưa chọn ngày
        return date.toString() + " " + start_h.getValue() + ":" + start_m.getValue(); 
    }
    
    // Gộp thời lượng thành 1 chuỗi
    public String getDuration() {
        return duration_h.getValue() + " hours " + duration_m.getValue() + " mins";
    }
    
    // Hàm kiểm tra xem thời lượng có bị để là 0h 0m không
    public boolean isDurationZero() {
        return "0".equals(duration_h.getValue()) && "0".equals(duration_m.getValue());
    }

    public void setDraftData(ProductDraftDTO draft) {
        if (draft == null) return;

        if (draft.getPrice() != null) {
            // Khi load lại draft, ta cũng format lại giá tiền
            try {
                long value = Long.parseLong(draft.getPrice().replaceAll("\\.", ""));
                DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
                symbols.setGroupingSeparator('.');
                DecimalFormat formatter = new DecimalFormat("#,###", symbols);
                prdPrice.setText(formatter.format(value));
            } catch (NumberFormatException e) {
                prdPrice.setText(draft.getPrice());
            }
        }
    }
}
