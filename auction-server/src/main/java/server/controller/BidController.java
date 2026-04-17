package server.controller;

import message.Request;
import message.Response;
import server.ServerApplication;
// import server.repository.BidRepository; // Sau này bạn tạo BidRepository thì mở comment ra

public class BidController {
    // private BidRepository bidRepo;

    public BidController() {
        // this.bidRepo = new BidRepository();
    }

    public Response handleBid(Request request) {
        Response response = new Response();

        // Dữ liệu Client gửi lên (Có thể là chuỗi, hoặc đối tượng Bid)
        Object bidData = request.getPayload();

        // TODO: Gọi bidRepo.saveBid(...) để lưu vào Database ở đây
        // Giả sử lưu thành công:

        response.setStatus("SUCCESS");
        response.setMessage("Đặt giá mới thành công!");
        System.out.println("Đã ghi nhận mức giá mới: " + bidData);

        // --- TÍNH NĂNG ĐẮT GIÁ: BROADCAST CẬP NHẬT GIÁ CHO MỌI NGƯỜI ---
        Response notifyPrice = new Response();
        notifyPrice.setStatus("NOTIFY_NEW_PRICE"); // Dùng 1 trạng thái riêng để Client dễ bắt
        notifyPrice.setMessage("Có người vừa đặt giá mới: " + bidData);
        notifyPrice.setData(bidData);

        // Gọi thẳng Lễ tân để bắc loa thông báo cho tất cả các Client đang online
        ServerApplication.broadcast(notifyPrice);
        // -------------------------------------------------------------

        return response; // Trả response SUCCESS về cho cái Client vừa bấm nút
    }
}