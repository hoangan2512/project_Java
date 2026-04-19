package server.controller;

import message.Request;
import message.Response;
import model.Bid;
import server.ServerApplication;
import server.repository.BidRepository;
import java.util.*;

public class BidController {
    private final BidRepository bidRepo;

    public BidController() {
        this.bidRepo = new BidRepository();
    }
//Khi người dunng bid -> client gửi request qua socket->bidcontroller->bidcontroller đóng gói bằng = handleBid
    public Response handleBid(Request request) {
        // Do request được handle đóng gói, lúc này dùng getPayload để bóc tách dữ liệu
        Object bidData = request.getPayload();

        // Gọi Repo để lưu vào DB
        if (bidData instanceof Bid) { //instanceof Bid: kiểm tra xem chắc chắn là đối tươnng đấu giá
            bidRepo.placeBid((Bid) bidData); //đẩy dữ liệu xuống database thông qua BidRepository
        }

        Response response = new Response("SUCCESS", bidData, "Đặt giá mới thành công");
        System.out.println("Đã ghi nhận mức giá mới: " + bidData);

        // BROADCAST: Gửi thông báo cho toàn bộ Client đang online
        Response notifyPrice = new Response("NOTIFY_NEW_PRICE", bidData, "Có người vừa đặt giá mới!");
        ServerApplication.broadcast(notifyPrice);

        return response;
    }

    public Response handleGetBidHistory(Request request) { //Xem lịch sử
        if (request.getPayload() instanceof Integer) { //Bóc request IdItem và kiểm tra có phải là số nguyên(ID)
            int itemId = (Integer) request.getPayload();
            List<Bid> history = bidRepo.getBidHistory(itemId);

            return new Response("SUCCESS", history, "Tải lịch sử thành công");
        }
        return new Response("FAIL", null, "ID không hợp lệ");
    }
}
//Tính đóng gói (Encapsulation): Controller chỉ lo điều hướng, còn Repository lo nói chuyện với Database.