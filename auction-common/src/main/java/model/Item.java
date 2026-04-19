package model;

import java.time.LocalDateTime;

//Item.java (Abstract Class): Chứa id, name, description, startingPrice, currentPrice, endTime, status.
//các lớp con Electronics.java, Art.java, Vehicle.java: Kế thừa từ Item để minh họa tính kế thừa rõ ràng.

// Bỏ abstract nếu bạn muốn dùng 'new Item()' trong Repository
public class Item extends entity {
    private final String open = "OPEN";
    private String description;
    private double starting_price;
    private int seller_id;
    private LocalDateTime created_at;
    private String status = open;
    private double currentPrice;
    private LocalDateTime endTime;
    private int highestBidderId = -1;

    public Item(int id, String name, String description, double starting_price, int seller_id, LocalDateTime created_at) {
        super(id, name);
        this.description = description;
        this.starting_price = starting_price;
        this.currentPrice = starting_price;
        this.seller_id = seller_id;
        this.created_at = created_at;
    }

    public Item() {
        super(0, "");
    }

    // --- CÁC HÀM SETTER (Để fix lỗi đỏ ở Repository và Service) ---
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

    public void setCreated_at(LocalDateTime created_at) {
        this.created_at = created_at;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setHighestBidderId(int highestBidderId) {
        this.highestBidderId = highestBidderId;
    }

    // --- CÁC HÀM GETTER ---
    public String getDescription() {
        return description;
    }

    public double getStarting_price() {
        return starting_price;
    }

    public int getSeller_id() {
        return seller_id;
    }

    public LocalDateTime getCreated_at() {
        return created_at;
    }

    public String getStatus() {
        return status;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public int getHighestBidderId() {
        return highestBidderId;
    }
}