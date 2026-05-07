package model;

import java.io.Serializable;
import java.time.LocalDateTime;

// Auction: Lớp quản lý trung tâm trạng thái một phiên đấu giá (chứa Item và trạng thái OPEN/RUNNING/FINISHED).
// THÊM implements Serializable ĐỂ GỬI ĐƯỢC QUA SOCKET
public class Auction implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int item_id;
    private LocalDateTime start_time;
    private LocalDateTime end_time;
    private String status;
    private Item item;

    // --- BỔ SUNG 2 TRƯỜNG MỚI ---
    private double current_price;
    private int highest_bidder_id;

    // Constructor đầy đủ
    public Auction(int id, int item_id, LocalDateTime start_time, LocalDateTime end_time, String status, double current_price, int highest_bidder_id){
        this.id = id;
        this.item_id = item_id;
        this.start_time = start_time;
        this.end_time = end_time;
        this.status = status;
        this.current_price = current_price;
        this.highest_bidder_id = highest_bidder_id;
    }

    // Giữ lại constructor cũ để các file khác gọi new Auction(...) không bị lỗi
    public Auction(int id, int item_id, LocalDateTime start_time, LocalDateTime end_time, String status){
        this.id = id;
        this.item_id = item_id;
        this.start_time = start_time;
        this.end_time = end_time;
        this.status = status;
    }

    public Auction() {
    }

    public int getId() {
        return id;
    }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public void setId(int id) {
        this.id = id;
    }

    public int getItem_id() {
        return item_id;
    }

    public void setItem_id(int item_id) {
        this.item_id = item_id;
    }

    public LocalDateTime getStart_time() {
        return start_time;
    }

    public void setStart_time(LocalDateTime start_time) {
        this.start_time = start_time;
    }

    public LocalDateTime getEnd_time() {
        return end_time;
    }

    public void setEnd_time(LocalDateTime end_time) {
        this.end_time = end_time;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getCurrent_price() {
        return current_price;
    }

    public void setCurrent_price(double current_price) {
        this.current_price = current_price;
    }

    public int getHighest_bidder_id() {
        return highest_bidder_id;
    }

    public void setHighest_bidder_id(int highest_bidder_id) {
        this.highest_bidder_id = highest_bidder_id;
    }
}