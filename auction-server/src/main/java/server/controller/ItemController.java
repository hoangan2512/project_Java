package server.controller;

import message.Request;
import message.Response;
import model.Auction;
import model.Item;
import server.repository.AuctionRepository;
import server.repository.ItemRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.List;

public class ItemController {
    private final ItemRepository itemRepo;
    private final AuctionRepository auctionRepo;

    public ItemController() {
        this.itemRepo = new ItemRepository();
        this.auctionRepo = new AuctionRepository();
    }

    public Response handleCreateItem(Request request) {
        Response response = new Response();

        try {
            Object[] payload = (Object[]) request.getPayload();
            Item newItem = (Item) payload[0];
            Auction newAuction = (Auction) payload[1];

            // ========================================================
            // LƯU ẢNH TỪ DẠNG BYTE[] VÀ LẤY ĐƯỜNG DẪN TƯƠNG ĐỐI
            // ========================================================
            newItem.setImgPath(saveImageFromBytes(newItem.getImageBytes(), newItem.getImgPath()));
            newItem.setImgPath1(saveImageFromBytes(newItem.getImageBytes1(), newItem.getImgPath1()));
            newItem.setImgPath2(saveImageFromBytes(newItem.getImageBytes2(), newItem.getImgPath2()));
            newItem.setImgPath3(saveImageFromBytes(newItem.getImageBytes3(), newItem.getImgPath3()));
            newItem.setImgPath4(saveImageFromBytes(newItem.getImageBytes4(), newItem.getImgPath4()));
            newItem.setImgPath5(saveImageFromBytes(newItem.getImageBytes5(), newItem.getImgPath5()));
            newItem.setImgPath6(saveImageFromBytes(newItem.getImageBytes6(), newItem.getImgPath6()));

            String currentPrdID = newItem.getUser_prdID();
            if (currentPrdID == null || currentPrdID.trim().isEmpty()) {
                String generatedID = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                newItem.setUser_prdID(generatedID);
            }

            int generatedItemId = itemRepo.addItem(newItem);

            if (generatedItemId > 0) {
                newAuction.setItem_id(generatedItemId);
                boolean isAuctionSaved = auctionRepo.createAuction(newAuction);

                if (isAuctionSaved) {
                    response.setStatus("SUCCESS");
                    response.setMessage("Thêm sản phẩm thành công, đang chờ Admin phê duyệt!");
                    System.out.println("Đã lên sàn SP chờ duyệt: " + newItem.getName() + " | Mã SP (User): " + newItem.getUser_prdID() + " | DB Item ID: " + generatedItemId + " | Categories: " + newItem.getCategories());
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
        String conflictResult = itemRepo.checkProductConflicts(item.getName(), item.getCategories(), item.getUser_prdID(), item.getSeller_id());

        if ("DUPLICATE_ID".equals(conflictResult)) {
            return new Response("DUPLICATE_ID", null, "Mã ID đã tồn tại trong hệ thống!");
        } else if (conflictResult.startsWith("DUPLICATE_NAME_CAT:")) {
            String existingId = conflictResult.split(":")[1];
            return new Response("DUPLICATE_NAME_CAT", existingId, "Trùng tên và danh mục!");
        }

        return new Response("OK", null, "Hợp lệ.");
    }

    /**
     * Lưu một mảng byte (dữ liệu ảnh) vào thư mục của project và trả về đường dẫn tương đối.
     */
    private String saveImageFromBytes(byte[] imageBytes, String originalName) {
        if (imageBytes == null || imageBytes.length == 0 || originalName == null || originalName.trim().isEmpty()) {
            return null;
        }

        try {
            String uploadDir = "auction-server/src/main/resources/images/products/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                // Sửa lỗi: Kiểm tra kết quả của mkdirs()
                if (!dir.mkdirs()) {
                    // Nếu không tạo được thư mục, ghi log và ném ra ngoại lệ để dừng xử lý
                    System.err.println("Không thể tạo thư mục lưu ảnh: " + dir.getAbsolutePath());
                    throw new IOException("Could not create directory for image uploads.");
                }
            }

            String extension = "";
            int lastIndexOfDot = originalName.lastIndexOf(".");
            if (lastIndexOfDot > 0) {
                extension = originalName.substring(lastIndexOfDot);
            }
            String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            Path targetPath = Paths.get(uploadDir + fileName);

            Files.write(targetPath, imageBytes);

            return "/images/products/" + fileName;

        } catch (IOException e) {
            System.err.println("Lỗi khi lưu file ảnh từ byte array: " + e.getMessage());
            e.printStackTrace();
            return null; 
        }
    }
    
    // ==========================================
    // CÁC HÀNH ĐỘNG DÀNH CHO ADMIN QUẢN LÝ ITEM
    // ==========================================
    
    public Response handleGetPendingItems(Request request) {
         Response response = new Response();
         List<Item> pendingItems = itemRepo.getPendingItems();
         
         response.setStatus("SUCCESS");
         response.setMessage("Lấy danh sách sản phẩm chờ duyệt thành công.");
         response.setData(pendingItems);
         return response;
    }
    
    public Response handleApproveItem(Request request) {
         Response response = new Response();
         Integer itemId = (Integer) request.getPayload();
         
         boolean success = itemRepo.updateItemModerationStatus(itemId, "APPROVED");
         if (success) {
             response.setStatus("SUCCESS");
             response.setMessage("Đã phê duyệt sản phẩm thành công.");
         } else {
             response.setStatus("FAIL");
             response.setMessage("Không thể phê duyệt sản phẩm.");
         }
         return response;
    }
    
    public Response handleRejectItem(Request request) {
         Response response = new Response();
         Integer itemId = (Integer) request.getPayload();
         
         boolean success = itemRepo.updateItemModerationStatus(itemId, "REJECTED");
         if (success) {
             // Có thể bạn muốn hủy luôn phiên đấu giá tương ứng
             // auctionRepo.updateStatus(auctionId, "CANCELLED");
             response.setStatus("SUCCESS");
             response.setMessage("Đã từ chối sản phẩm.");
         } else {
             response.setStatus("FAIL");
             response.setMessage("Không thể từ chối sản phẩm.");
         }
         return response;
    }
}
