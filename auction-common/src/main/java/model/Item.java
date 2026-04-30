package model;

import java.io.Serializable;
import java.time.LocalDateTime;

// Bỏ abstract nếu bạn muốn dùng 'new Item()' trong Repository
public class Item extends entity implements Serializable {
    private static final long serialVersionUID = 1L; // Khuyên dùng khi implements Serializable

    private final String open = "OPEN";
    private String description;
    private double starting_price;
    private int seller_id;
    private double currentPrice;
    private String categories;

    // --- BỔ SUNG BIẾN MÃ SẢN PHẨM CỦA NGƯỜI DÙNG ---
    private String user_prdID;

    // --- BỔ SUNG KHAI BÁO CÁC BIẾN LƯU ĐƯỜNG DẪN ẢNH ---
    private String imgPath;
    private String imgPath1;
    private String imgPath2;
    private String imgPath3;
    private String imgPath4;
    private String imgPath5;
    private String imgPath6;

    // Constructor có tham số (Đã bổ sung user_prdID)
    public Item(int id, String user_prdID, String name, String description, double starting_price, int seller_id, String categories,
                String imgPath, String imgPath1, String imgPath2, String imgPath3,
                String imgPath4, String imgPath5, String imgPath6) {
        super(id, name);
        this.user_prdID = user_prdID;
        this.description = description;
        this.starting_price = starting_price;
        this.currentPrice = starting_price;
        this.seller_id = seller_id;
        this.categories = categories;

        // --- BỔ SUNG GÁN GIÁ TRỊ VÀO BIẾN ---
        this.imgPath = imgPath;
        this.imgPath1 = imgPath1;
        this.imgPath2 = imgPath2;
        this.imgPath3 = imgPath3;
        this.imgPath4 = imgPath4;
        this.imgPath5 = imgPath5;
        this.imgPath6 = imgPath6;
    }

    // Constructor mặc định
    public Item() {
        super(0, "");
    }

    // ==========================================
    // GETTER & SETTER CHO USER_PRDID (MỚI)
    // ==========================================
    public String getUser_prdID() {
        return user_prdID;
    }

    public void setUser_prdID(String user_prdID) {
        this.user_prdID = user_prdID;
    }

    // ==========================================
    // CÁC GETTER & SETTER KHÁC (GIỮ NGUYÊN)
    // ==========================================
    public void setDescription(String description) {
        this.description = description;
    }

    public void setStarting_price(double starting_price) {
        this.starting_price = starting_price;
        this.currentPrice = starting_price; // Mặc định giá hiện tại = giá khởi điểm
    }

    public void setSeller_id(int seller_id) {
        this.seller_id = seller_id;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }

    public void setImgPath(String imgPath) {
        this.imgPath = imgPath;
    }

    public void setImgPath1(String imgPath1) {
        this.imgPath1 = imgPath1;
    }

    public void setImgPath2(String imgPath2) {
        this.imgPath2 = imgPath2;
    }

    public void setImgPath3(String imgPath3) {
        this.imgPath3 = imgPath3;
    }

    public void setImgPath4(String imgPath4) {
        this.imgPath4 = imgPath4;
    }

    public void setImgPath5(String imgPath5) {
        this.imgPath5 = imgPath5;
    }

    public void setImgPath6(String imgPath6) {
        this.imgPath6 = imgPath6;
    }

    public String getDescription() {
        return description;
    }

    public double getStarting_price() {
        return starting_price;
    }

    public int getSeller_id() {
        return seller_id;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public String getCategories() {
        return categories;
    }

    public String getImgPath() {
        return imgPath;
    }

    public String getImgPath1() {
        return imgPath1;
    }

    public String getImgPath2() {
        return imgPath2;
    }

    public String getImgPath3() {
        return imgPath3;
    }

    public String getImgPath4() {
        return imgPath4;
    }

    public String getImgPath5() {
        return imgPath5;
    }

    public String getImgPath6() {
        return imgPath6;
    }

    // --- BỔ SUNG CÁC MẢNG BYTE ĐỂ CHỨA DỮ LIỆU ẢNH THỰC TẾ ---
    private byte[] imageBytes;
    private byte[] imageBytes1;
    private byte[] imageBytes2;
    private byte[] imageBytes3;
    private byte[] imageBytes4;
    private byte[] imageBytes5;
    private byte[] imageBytes6;

    // --- BỔ SUNG GETTER VÀ SETTER CHO CÁC MẢNG BYTE NÀY ---
    public byte[] getImageBytes() { return imageBytes; }
    public void setImageBytes(byte[] imageBytes) { this.imageBytes = imageBytes; }

    public byte[] getImageBytes1() { return imageBytes1; }
    public void setImageBytes1(byte[] imageBytes1) { this.imageBytes1 = imageBytes1; }

    public byte[] getImageBytes2() { return imageBytes2; }
    public void setImageBytes2(byte[] imageBytes2) { this.imageBytes2 = imageBytes2; }

    public byte[] getImageBytes3() { return imageBytes3; }
    public void setImageBytes3(byte[] imageBytes3) { this.imageBytes3 = imageBytes3; }

    public byte[] getImageBytes4() { return imageBytes4; }
    public void setImageBytes4(byte[] imageBytes4) { this.imageBytes4 = imageBytes4; }

    public byte[] getImageBytes5() { return imageBytes5; }
    public void setImageBytes5(byte[] imageBytes5) { this.imageBytes5 = imageBytes5; }

    public byte[] getImageBytes6() { return imageBytes6; }
    public void setImageBytes6(byte[] imageBytes6) { this.imageBytes6 = imageBytes6; }
}