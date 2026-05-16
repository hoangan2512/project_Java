package server.controller;

import message.Request;
import message.Response;
import model.AutoBidConfig;
import model.Bid;
import server.service.AuctionService;
import server.service.AutoBidManager;
import server.repository.BidRepository;
import java.util.*;

public class BidController {
    private final AuctionService auctionService;
    private final BidRepository bidRepo;

    public BidController() {
        this.auctionService = new AuctionService();
        this.bidRepo = new BidRepository();
    }

    public Response handleBid(Request request) {
        Object bidData = request.getPayload();

        if (!(bidData instanceof Bid)) {
            return new Response("FAIL", null, "Dữ liệu đấu giá không hợp lệ.");
        }

        Bid bid = (Bid) bidData;
        
        // Chuyển toàn bộ logic nghiệp vụ cho Service xử lý
        return auctionService.placeBid(bid);
    }
    
    public Response handleRegisterAutoBid(Request request) {
        Object payload = request.getPayload();
        
        if (!(payload instanceof AutoBidConfig)) {
            return new Response("FAIL", null, "Dữ liệu cấu hình tự động đấu giá không hợp lệ.");
        }
        
        AutoBidConfig config = (AutoBidConfig) payload;
        
        // Đăng ký với Manager
        AutoBidManager.getInstance().registerAutoBid(config);
        
        return new Response("SUCCESS", null, "Đăng ký đấu giá tự động thành công!");
    }

    public Response handleGetBidHistory(Request request) {
        if (request.getPayload() instanceof Integer) { 
            int itemId = (Integer) request.getPayload();
            List<Bid> history = bidRepo.getBidHistory(itemId);
            return new Response("SUCCESS", history, "Tải lịch sử thành công");
        }
        return new Response("FAIL", null, "ID không hợp lệ");
    }
}
