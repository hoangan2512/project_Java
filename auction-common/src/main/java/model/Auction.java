package model;

import java.time.LocalDateTime;

//Auction: Lớp quản lý trung tâm trạng thái một phiên đấu giá (chứa Item và trạng thái OPEN/RUNNING/FINISHED).
public class Auction {
    private int id;
    private int item_id;
    private LocalDateTime start_time;
    private LocalDateTime end_time;
    private String status;
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
}
