package model;

import java.time.LocalDateTime;

//Item.java (Abstract Class): Chứa id, name, description, startingPrice, currentPrice, endTime, status.
//các lớp con Electronics.java, Art.java, Vehicle.java: Kế thừa từ Item để minh họa tính kế thừa rõ ràng.
public abstract class Item extends entity {
    private String description;
    private double starting_price;
    private int seller_id;
    private LocalDateTime create_id;

    public Item(int id, String name, String description, double starting_price, int seller_id, LocalDateTime create_id) {
        super(id, name);
        this.description = description;
        this.starting_price = starting_price;
        this.seller_id = seller_id;
        this.create_id = create_id;
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

    public LocalDateTime getCreate_id() {
        return create_id;
    }

    public void setCreate_id(LocalDateTime create_id) {
        this.create_id = create_id;
    }

    class Electronics extends Item {
        public Electronics(int id, String name, String description, double starting_price, int seller_id, LocalDateTime create_id){
            super(id, name, description, starting_price, seller_id, create_id);
        }
    }


    class Art extends Item {
        public Art(int id, String name, String description, double starting_price, int seller_id, LocalDateTime create_id){
            super(id, name, description, starting_price, seller_id, create_id);
        }
    }

    class Vehicle extends Item {
        public Vehicle(int id, String name, String description, double starting_price, int seller_id, LocalDateTime create_id){
            super(id, name, description, starting_price, seller_id, create_id);
        }
    }
}