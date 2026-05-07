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
import java.nio.file.StandardCopyOption;
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
            // 1. Tách mảng payload ra thành Item và Auction (Dữ liệu gửi từ Client)
            Object[] payload = (Object[]) request.getPayload();
            Item newItem = (Item) payload[0];
            Auction newAuction = (Auction) payload[1];

            // ========================================================
            // BƯỚC QUAN TRỌNG: XỬ LÝ LƯU 7 BỨC ẢNH VÀO THƯ MỤC PROJECT
            // Chuyển đổi đường dẫn tuyệt đối (từ máy khách) thành đường dẫn tương đối (lưu DB)
            // ========================================================
            newItem.setImgPath(processAndSaveImage(newItem.getImgPath()));
            newItem.setImgPath1(processAndSaveImage(newItem.getImgPath1()));
            newItem.setImgPath2(processAndSaveImage(newItem.getImgPath2()));
            newItem.setImgPath3(processAndSaveImage(newItem.getImgPath3()));
            newItem.setImgPath4(processAndSaveImage(newItem.getImgPath4()));
            newItem.setImgPath5(processAndSaveImage(newItem.getImgPath5()));
            newItem.setImgPath6(processAndSaveImage(newItem.getImgPath6()));

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

    // ==========================================
    // CÁC HÀM HỖ TRỢ XỬ LÝ ẢNH
    // ==========================================

    /**
     * Hàm hỗ trợ kiểm tra xem đường dẫn có thực sự trỏ tới một file ảnh tồn tại không.
     * Nếu có, gọi hàm copy file. Nếu không, trả về nguyên trạng.
     */
    private String processAndSaveImage(String originalPath) {
        if (originalPath != null && !originalPath.trim().isEmpty()) {
            File file = new File(originalPath);
            // Kiểm tra xem file có tồn tại trên máy tính Client không
            if (file.exists()) {
                return saveImageToProject(file);
            }
        }
        return originalPath; // Trả về null hoặc đường dẫn cũ nếu không có file thực
    }

    /**
     * Copy ảnh từ máy tính vào thư mục của project và trả về đường dẫn tương đối.
     */
    public String saveImageToProject(File selectedFile) {
        try {
            // 1. Xác định thư mục đích trong project
            String uploadDir = "auction-server/src/main/resources/images/products/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs(); // Tạo thư mục nếu chưa có

            // 2. Đổi tên file để tránh trùng lặp (dùng System.currentTimeMillis)
            String fileName = System.currentTimeMillis() + "_" + selectedFile.getName();
            Path targetPath = Paths.get(uploadDir + fileName);

            // 3. Copy file từ máy tính vào thư mục project
            Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 4. Trả về đường dẫn tương đối để lưu vào DB
            return "/images/products/" + fileName;
        } catch (IOException e) {
            System.err.println("Lỗi khi lưu file ảnh: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}