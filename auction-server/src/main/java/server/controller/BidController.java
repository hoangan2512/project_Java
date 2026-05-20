package server.controller;

import message.Request;
import message.Response;
import model.AutoBidConfig;
import model.Bid;
import server.service.AuctionService;
import server.service.AutoBidManager;

public class BidController {
    private final AuctionService auctionService = new AuctionService();

    public Response handleBid(Request request) {
        Bid bid = (Bid) request.getPayload();
        return auctionService.placeBid(bid);
    }

    public Response handleRegisterAutoBid(Request request) {
        AutoBidConfig config = (AutoBidConfig) request.getPayload();
        AutoBidManager.getInstance().registerAutoBid(config);
        return new Response("SUCCESS", null, "Auto-bid active.");
    }

    public Response handleUnregisterAutoBid(Request request) {
        AutoBidConfig config = (AutoBidConfig) request.getPayload();
        AutoBidManager.getInstance().unregisterAutoBid(config.getAuctionId(), config.getBidderId());
        return new Response("SUCCESS", null, "Auto-bid inactive.");
    }

    public Response handleGetBidHistory(Request request) {
        return new Response("ERROR", null, "Get bid history not implemented in BidController.");
    }
}
