package model;

import java.io.Serializable;
import java.time.LocalDateTime;

//Bid.java: Lưu thông tin một lượt đặt giá: bidderId, itemId, amount, timestamp.
public class Bid implements Serializable {
    private static final long serialVersionUID = 1L; // Đảm bảo đồng bộ Client-Server
    private int id;
    private int auction_id;
    private int bidder_id;
    private int amount;
    private LocalDateTime bid_time;
    public Bid(int id, int auction_id, int bidder_id, int amount, LocalDateTime bid_time){
        this.id = id;
        this.auction_id = auction_id;
        this.bidder_id = bidder_id;
        this.amount =amount;
        this.bid_time = bid_time;
    }
    public Bid(){

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAuction_id() {
        return auction_id;
    }

    public void setAuction_id(int auction_id) {
        this.auction_id = auction_id;
    }

    public int getBidder_id() {
        return bidder_id;
    }

    public void setBidder_id(int bidder_id) {
        this.bidder_id = bidder_id;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public LocalDateTime getBid_time() {
        return bid_time;
    }

    public void setBid_time(LocalDateTime bid_time) {
        this.bid_time = bid_time;
    }
}
