package model;

import java.io.Serializable;
import java.util.List;

public class SearchCriteria implements Serializable {
    private List<String> categories;
    private long minPrice;
    private long maxPrice;
    private List<String> statuses;
    private String auctionId;

    // Constructor mặc định
    public SearchCriteria() {
    }

    // ==========================================
    // GETTER VÀ SETTER
    // ==========================================

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public long getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(long minPrice) {
        this.minPrice = minPrice;
    }

    public long getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(long maxPrice) {
        this.maxPrice = maxPrice;
    }

    public List<String> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<String> statuses) {
        this.statuses = statuses;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }
}