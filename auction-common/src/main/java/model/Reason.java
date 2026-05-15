package model;

import java.io.Serializable;

public class Reason implements Serializable {
    private int id;
    private int targetId; // Tên chung cho auctionId, itemId hoặc userId
    private String reasonType; // "AUCTION", "ITEM", "USER"
    private String reason;

    // Constructors
    public Reason() {}

    public Reason(int targetId, String reasonType, String reason) {
        this.targetId = targetId;
        this.reasonType = reasonType;
        this.reason = reason;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTargetId() {
        return targetId;
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }

    public String getReasonType() {
        return reasonType;
    }

    public void setReasonType(String reasonType) {
        this.reasonType = reasonType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
