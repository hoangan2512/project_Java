package model;

import java.time.LocalDateTime;

//Item.java (Abstract Class): Chứa id, name, description, startingPrice, currentPrice, endTime, status.
//các lớp con Electronics.java, Art.java, Vehicle.java: Kế thừa từ Item để minh họa tính kế thừa rõ ràng.
public class Item extends entity {
    private String description;
    private double starting_price;
    private int seller_id;
    private LocalDateTime created_at;

    public Item(int id, String name, String description, double starting_price, int seller_id, LocalDateTime created_at) {
        super(id, name);
        this.description = description;
        this.starting_price = starting_price;
        this.seller_id = seller_id;
        this.created_at = created_at;
    }
    public Item(){
        super(0, "");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getStarting_price() {
        return starting_price;
    }

    public void setStarting_price(double starting_price) {
        this.starting_price = starting_price;
    }

    public int getSeller_id() {
        return seller_id;
    }

    public void setSeller_id(int seller_id) {
        this.seller_id = seller_id;
    }

    public LocalDateTime getCreated_at() {
        return created_at;
    }

    public void setCreated_at(LocalDateTime created_at) {
        this.created_at = created_at;
    }
}