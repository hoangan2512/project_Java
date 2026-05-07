package controller.sellerHub.new_item_page;

import java.io.Serializable;

public class ProductDraftDTO implements Serializable {

    private static final long serialVersionUID = 1L; // Đảm bảo tính ổn định khi truyền qua Socket

    // Basic Info
    private String name;
    private String id;
    private String description;
    private String categories;

    // Graphic Info (7 đường dẫn ảnh)
    private String imgPath;
    private String imgPath1;
    private String imgPath2;
    private String imgPath3;
    private String imgPath4;
    private String imgPath5;
    private String imgPath6;

    // Auction Info
    private String price;
    private String startTime;
    private String duration; // Thêm duration

    // ==========================================
    // GETTERS & SETTERS - BASIC INFO
    // ==========================================
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getCategories() {
        return categories;
    }
    public void setCategories(String categories) {
        this.categories = categories;
    }

    // ==========================================
    // GETTERS & SETTERS - GRAPHIC INFO (ẢNH)
    // ==========================================
    public String getImgPath() {
        return imgPath;
    }
    public void setImgPath(String imgPath) {
        this.imgPath = imgPath;
    }

    public String getImgPath1() {
        return imgPath1;
    }
    public void setImgPath1(String imgPath1) {
        this.imgPath1 = imgPath1;
    }

    public String getImgPath2() {
        return imgPath2;
    }
    public void setImgPath2(String imgPath2) {
        this.imgPath2 = imgPath2;
    }

    public String getImgPath3() {
        return imgPath3;
    }
    public void setImgPath3(String imgPath3) {
        this.imgPath3 = imgPath3;
    }

    public String getImgPath4() {
        return imgPath4;
    }
    public void setImgPath4(String imgPath4) {
        this.imgPath4 = imgPath4;
    }

    public String getImgPath5() {
        return imgPath5;
    }
    public void setImgPath5(String imgPath5) {
        this.imgPath5 = imgPath5;
    }

    public String getImgPath6() {
        return imgPath6;
    }
    public void setImgPath6(String imgPath6) {
        this.imgPath6 = imgPath6;
    }

    // ==========================================
    // GETTERS & SETTERS - AUCTION INFO
    // ==========================================
    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
    
    public String getDuration() {
        return duration;
    }
    
    public void setDuration(String duration) {
        this.duration = duration;
    }

    // ==========================================
    // UTILITY METHODS
    // ==========================================
    // Hàm reset để xóa dữ liệu khi tạo sản phẩm mới
    public void clear() {
        this.name = null;
        this.id = null;
        this.description = null;
        this.categories = null;

        // Reset toàn bộ 7 đường dẫn ảnh
        this.imgPath = null;
        this.imgPath1 = null;
        this.imgPath2 = null;
        this.imgPath3 = null;
        this.imgPath4 = null;
        this.imgPath5 = null;
        this.imgPath6 = null;

        this.price = null;
        this.startTime = null;
        this.duration = null;
    }
}
