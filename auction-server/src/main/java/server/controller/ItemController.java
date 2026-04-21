package server.controller;

import message.Request;
import message.Response;
import model.Auction;
import model.Item;
import server.repository.AuctionRepository;
import server.repository.ItemRepository;

import java.util.UUID;

public class ItemController {
    private ItemRepository itemRepo;
    private AuctionRepository auctionRepo; // Khai báo thêm Repo của Auction

    public ItemController() {
        this.itemRepo = new ItemRepository();
        this.auctionRepo = new AuctionRepository();
    }

    public Response handleCreateItem(Request request) {
        Response response = new Response();

        try {
            // 1. Tách mảng payload ra thành Item và Auction (Dữ liệu gửi từ Client)
            Object[] payload = (Object[]) request.getPayload();
            Item newItem = (Item) payload[0];
            Auction newAuction = (Auction) payload[1];

            // 2. Kiểm tra và tự động sinh mã user_prdID nếu người dùng để trống
            String currentPrdID = newItem.getUser_prdID();
            if (currentPrdID == null || currentPrdID.trim().isEmpty()) {
                // Tự động sinh ra 1 chuỗi ngẫu nhiên dài 8 ký tự, ví dụ: PRD-A1B2C3D4
                String generatedID = "PRD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                newItem.setUser_prdID(generatedID);
            }

            // 3. Lưu Item vào Database và lấy ID tự động tăng (auto-increment) vừa tạo
            int generatedItemId = itemRepo.addItem(newItem);

            if (generatedItemId > 0) {
                // 4. Gắn Item ID vừa lấy được vào Auction và lưu tiếp xuống Database
                newAuction.setItem_id(generatedItemId);
                boolean isAuctionSaved = auctionRepo.createAuction(newAuction);

                if (isAuctionSaved) {
                    response.setStatus("SUCCESS");
                    response.setMessage("Thêm sản phẩm và tạo phiên đấu giá id:" + newAuction.getId() + " thành công!");
                    System.out.println("Đã lên sàn SP: " + newItem.getName() + " | Mã SP (User): " + newItem.getUser_prdID() + " | DB Item ID: " + generatedItemId + " | Categories: " + newItem.getCategories());
                } else {
                    response.setStatus("FAIL");
                    response.setMessage("Thêm sản phẩm thành công nhưng có lỗi khi tạo phiên đấu giá!");
                }
            } else {
                response.setStatus("FAIL");
                response.setMessage("Lỗi khi lưu sản phẩm vào cơ sở dữ liệu!");
            }

        } catch (ClassCastException | ArrayIndexOutOfBoundsException e) {
            System.err.println("Lỗi định dạng Payload gửi từ Client!");
            response.setStatus("FAIL");
            response.setMessage("Dữ liệu gửi lên không đúng định dạng yêu cầu.");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Lỗi hệ thống khi xử lý Create Item!");
            response.setStatus("ERROR");
            response.setMessage("Đã xảy ra lỗi trên Server.");
            e.printStackTrace();
        }

        return response;
    }

    public Response handleCheckDuplicateName(Request request) {
        Item item = (Item) request.getPayload();

        // Gọi hàm kiểm tra đa điều kiện mới tạo
        String conflictResult = itemRepo.checkProductConflicts(item.getName(), item.getCategories(), item.getUser_prdID(), item.getSeller_id());

        if ("DUPLICATE_ID".equals(conflictResult)) {
            // Trạng thái 1: Trùng ID -> Chặn cứng
            return new Response("DUPLICATE_ID", null, "Mã ID đã tồn tại trong hệ thống!");
        } else if (conflictResult.startsWith("DUPLICATE_NAME_CAT:")) {
            // Trạng thái 2: Trùng Tên + Danh mục -> Cảnh báo
            String existingId = conflictResult.split(":")[1];
            return new Response("DUPLICATE_NAME_CAT", existingId, "Trùng tên và danh mục!");
        }

        return new Response("OK", null, "Hợp lệ.");
    }
}