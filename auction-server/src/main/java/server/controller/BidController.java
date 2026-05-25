package server.controller;

import message.Request;
import message.Response;
import model.AutoBidConfig;
import model.Bid;
import server.repository.BidRepository;
import server.service.AuctionService;
import server.service.AutoBidManager;

import java.util.List;

public class BidController {
    private final AuctionService auctionService = new AuctionService();
    private final BidRepository bidRepo = new BidRepository();

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
        try {
            int auctionId = (Integer) request.getPayload();

            // Gọi thẳng Repository
            List<Bid> history = bidRepo.getBidHistory(auctionId);

            if (history != null) {
                // Đảo ngược mảng để Client vẽ biểu đồ không bị ngược
                java.util.Collections.reverse(history);
                return new Response("SUCCESS", history, "Lấy lịch sử đấu giá thành công.");
            } else {
                return new Response("FAIL", null, "Không tìm thấy lịch sử.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Response("ERROR", null, "Lỗi server.");
        }
    }

    public Response handleCheckAutobidStatus(Request request) {
        try {
            Object[] payload = (Object[]) request.getPayload();
            int auctionId = (Integer) payload[0];
            int bidderId = (Integer) payload[1];

            AutoBidConfig config = AutoBidManager.getInstance().getUserAutoBidConfig(auctionId, bidderId);

            // Trả về nguyên object AutoBidConfig, client sẽ tự check null
            return new Response("SUCCESS", config, "Checked autobid status successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return new Response("ERROR", null, "Error checking autobid status.");
        }
    }
}
