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

public class ItemController {
    private ItemRepository itemRepo;
    private AuctionRepository auctionRepo;

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
     *
     * @param imageBytes   Dữ liệu ảnh dưới dạng byte array.
     * @param originalName Tên file gốc (dùng để lấy phần mở rộng, ví dụ: "anh_san_pham.jpg").
     * @return Đường dẫn tương đối của ảnh đã lưu (ví dụ: "/images/products/163..._anh_san_pham.jpg"),
     *         hoặc trả về null nếu có lỗi hoặc không có dữ liệu ảnh.
     */
    private String saveImageFromBytes(byte[] imageBytes, String originalName) {
        // Nếu không có dữ liệu ảnh hoặc không có tên file gốc thì không xử lý
        if (imageBytes == null || imageBytes.length == 0 || originalName == null || originalName.trim().isEmpty()) {
            return null;
        }

        try {
            // 1. Xác định thư mục đích trong project
            String uploadDir = "auction-server/src/main/resources/images/products/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs(); // Tạo thư mục nếu chưa có
            }

            // 2. Tạo tên file mới, duy nhất để tránh trùng lặp
            // Lấy phần mở rộng từ tên file gốc
            String extension = "";
            int lastIndexOfDot = originalName.lastIndexOf(".");
            if (lastIndexOfDot > 0) {
                extension = originalName.substring(lastIndexOfDot); // .jpg, .png, ...
            }
            String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            Path targetPath = Paths.get(uploadDir + fileName);

            // 3. Ghi mảng byte vào file
            Files.write(targetPath, imageBytes);

            // 4. Trả về đường dẫn tương đối để lưu vào DB
            return "/images/products/" + fileName;

        } catch (IOException e) {
            System.err.println("Lỗi khi lưu file ảnh từ byte array: " + e.getMessage());
            e.printStackTrace();
            return null; // Trả về null nếu có lỗi xảy ra
        }
    }
}