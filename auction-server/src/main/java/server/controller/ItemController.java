package server.controller;

import message.Request;
import message.Response;
import model.Item;
import server.repository.ItemRepository;

public class ItemController {
    private ItemRepository itemRepo;

    public ItemController() {
        this.itemRepo = new ItemRepository();
    }

    public Response handleCreateItem(Request request) {
        Response response = new Response();
        Item newItem = (Item) request.getPayload();

        boolean isSaved = itemRepo.addItem(newItem);

        if (isSaved) {
            response.setStatus("SUCCESS");
            response.setMessage("Thêm sản phẩm thành công!");
            System.out.println("Đã lưu vào DB sản phẩm: " + newItem.getName());
        } else {
            response.setStatus("FAIL");
            response.setMessage("Lỗi khi lưu sản phẩm vào DB!");
        }
        return response;
    }

    // Sau này bạn viết thêm các hàm handleBid(Request req), handleGetList(Request req)... ở đây
}