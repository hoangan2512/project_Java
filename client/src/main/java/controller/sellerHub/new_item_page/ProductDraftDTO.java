package controller.sellerHub.new_item_page; // Lời khuyên nhỏ: Thường DTO nên chuyển sang package riêng như 'model' hoặc 'dto' cho chuẩn kiến trúc nhé!

import javafx.scene.image.Image;

public class ProductDraftDTO {
    // Basic Info
    private String name;
    private String id;
    private String description;
    private String categories;

    // Graphic Info
    private Image image;

    // Auction Info
    private String price;
    private String startTime;
    private String auctionChoice;

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
    // GETTERS & SETTERS - GRAPHIC INFO
    // ==========================================
    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
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

    public String getAuctionChoice() {
        return auctionChoice;
    }

    public void setAuctionChoice(String auctionChoice) {
        this.auctionChoice = auctionChoice;
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
        this.image = null;
        this.price = null;
        this.startTime = null;
        this.auctionChoice = null;
    }
}