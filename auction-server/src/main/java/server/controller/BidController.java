package server.controller;

import message.Request;
import message.Response;
import model.Bid;
import server.service.AuctionService;
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
        // Do request được handle đóng gói, lúc này dùng getPayload để bóc tách dữ liệu
        Object bidData = request.getPayload();

        // Kiểm tra xem payload có đúng là đối tượng Bid không
        if (!(bidData instanceof Bid)) {
            return new Response("FAIL", null, "Dữ liệu đấu giá không hợp lệ.");
        }

        Bid bid = (Bid) bidData;
        
        // Chuyển toàn bộ logic nghiệp vụ (kiểm tra điều kiện, khóa, lưu DB) cho Service xử lý
        return auctionService.placeBid(bid);
    }

    public Response handleGetBidHistory(Request request) { //Xem lịch sử
        if (request.getPayload() instanceof Integer) { //Bóc request IdItem và kiểm tra có phải là số nguyên(ID)
            int itemId = (Integer) request.getPayload();
            // Lấy lịch sử chỉ là thao tác đọc đơn giản, Controller có thể gọi thẳng Repository
            List<Bid> history = bidRepo.getBidHistory(itemId);

            return new Response("SUCCESS", history, "Tải lịch sử thành công");
        }
        return new Response("FAIL", null, "ID không hợp lệ");
    }
}
