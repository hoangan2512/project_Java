package server.controller;

import message.Request;
import message.Response;
import model.Auction;
import model.Item;
import model.Reason;
import server.repository.AuctionRepository;
import server.repository.ItemRepository;
import server.repository.ReasonRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class ItemController {
    private final ItemRepository itemRepo;
    private final AuctionRepository auctionRepo;
    private final ReasonRepository reasonRepo;

    public ItemController() {
        this.reasonRepo = new ReasonRepository();
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
            
            newItem.setModeration_status("PENDING_APPROVAL");

            int generatedItemId = itemRepo.addItem(newItem);

            if (generatedItemId > 0) {
                newAuction.setItem_id(generatedItemId);
                newAuction.setStatus("PENDING_APPROVAL"); // Set trạng thái chờ duyệt
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

    public Response handleApproveItem(Request request) {
        Response response = new Response();

        // Phía Client đang gửi lên auctionId (Integer)
        if (request.getPayload() instanceof Integer) {
            int auctionId = (Integer) request.getPayload();

            // 1. Fetch auction to get item_id
            Auction auction = auctionRepo.getAuctionById(auctionId);
            if (auction != null) {
                // Check if the auction is suspended
                if ("SUSPENDED".equals(auction.getStatus())) {
                    response.setStatus("FAIL");
                    response.setMessage("Phiên đấu giá đã bị đình chỉ và không thể phê duyệt lại.");
                    return response;
                }

                int itemId = auction.getItem_id();

                // 2. Cập nhật trạng thái kiểm duyệt của sản phẩm thành APPROVED
                boolean isItemUpdated = itemRepo.updateStatus(itemId, "APPROVED");

                // 3. Kích hoạt trạng thái hoạt động cho phiên đấu giá (Chuyển sang WAITING để đợi đến giờ mở sàn)
                boolean isAuctionUpdated = auctionRepo.updateStatus(auctionId, "WAITING");

                if (isItemUpdated && isAuctionUpdated) {
                    response.setStatus("SUCCESS");
                    response.setMessage("Đã phê duyệt sản phẩm thành công lên hệ thống.");
                } else {
                    response.setStatus("FAIL");
                    response.setMessage("Không thể phê duyệt sản phẩm hoặc cập nhật phiên đấu giá.");
                }
            } else {
                response.setStatus("FAIL");
                response.setMessage("Không tìm thấy phiên đấu giá với ID: " + auctionId);
            }
        } else {
            response.setStatus("FAIL");
            response.setMessage("Dữ liệu Payload không hợp lệ. Yêu cầu kiểu số nguyên Integer.");
        }

        return response;
    }

    public Response handleRejectItem(Request request) {
        Response response = new Response();
        Object payloadObj = request.getPayload();

        // CHỈ XỬ LÝ: Client bắt buộc phải gửi lên mảng Object [auctionId, reason]
        if (payloadObj instanceof Object[]) {
            Object[] payload = (Object[]) payloadObj;

            // Kiểm tra số lượng phần tử và kiểm tra kiểu dữ liệu an toàn (Number, String)
            if (payload.length == 2 && payload[0] instanceof Number && payload[1] instanceof String) {
                int auctionId = ((Number) payload[0]).intValue(); // Ép kiểu an toàn tránh lỗi Long/Integer qua Socket
                String reasonText = (String) payload[1];

                // 1. Fetch auction to get item_id
                Auction auction = auctionRepo.getAuctionById(auctionId);
                if (auction != null) {
                    int itemId = auction.getItem_id();

                    // 2. Thực hiện cập nhật trạng thái kiểm duyệt của sản phẩm thành REJECTED
                    boolean isItemUpdated = itemRepo.updateStatus(itemId, "REJECTED");

                    // 3. Đồng thời sửa lại trạng thái của phiên đấu giá (Auction) thành SUSPENDED
                    boolean isAuctionUpdated = auctionRepo.updateStatus(auctionId, "SUSPENDED");

                    if (isItemUpdated && isAuctionUpdated) {
                        // 4. Thử lưu lý do từ chối vào bảng reasons
                        try {
                            Reason reason = new Reason();
                            reason.setTargetId(auctionId);
                            reason.setReasonType("ITEM_REJECTED");
                            reason.setReason(reasonText);
                            reasonRepo.addReason(reason);
                        } catch (Exception e) {
                            System.err.println("[SERVER WARNING] Không thể lưu lý do từ chối sản phẩm vào DB: " + e.getMessage());
                        }

                        response.setStatus("SUCCESS");
                        response.setMessage("Đã từ chối sản phẩm thành công và đình chỉ phiên đấu giá liên quan.");
                    } else {
                        response.setStatus("FAIL");
                        response.setMessage("Không thể cập nhật trạng thái từ chối, vui lòng thử lại.");
                    }
                } else {
                    response.setStatus("FAIL");
                    response.setMessage("Không tìm thấy phiên đấu giá với ID: " + auctionId);
                }
            } else {
                response.setStatus("FAIL");
                response.setMessage("Định dạng dữ liệu mảng không hợp lệ. Yêu cầu mảng cấu trúc [Number, String].");
            }
        } else {
            response.setStatus("FAIL");
            response.setMessage("Dữ liệu payload không hợp lệ. Yêu cầu bắt buộc gửi dạng mảng Object[].");
        }

        return response;
    }

    public Response handleDeleteItem(Request request) {
        Response response = new Response();

        try {
            // Kiểm tra và ép kiểu an toàn ID gửi lên từ Client
            if (request.getPayload() instanceof Number) {
                int itemId = ((Number) request.getPayload()).intValue();

                // BƯỚC 1: Xóa phiên đấu giá (Auction) trước để giải phóng khóa ngoại
                // Lưu ý: Ta không cần kiểm tra kết quả trả về của hàm này vì có thể Item chưa kịp tạo Auction
                auctionRepo.deleteAuctionByItemId(itemId);

                // BƯỚC 2: Xóa sản phẩm (Item) sau khi đã xóa phiên liên quan
                boolean isItemDeleted = itemRepo.deleteItemById(itemId);

                if (isItemDeleted) {
                    response.setStatus("SUCCESS");
                    response.setMessage("Đã xóa sản phẩm và phiên đấu giá thành công khỏi hệ thống.");
                } else {
                    response.setStatus("FAIL");
                    response.setMessage("Không thể xóa sản phẩm. Có thể ID không tồn tại hoặc đã bị xóa trước đó.");
                }
            } else {
                response.setStatus("FAIL");
                response.setMessage("Dữ liệu Payload không hợp lệ. Yêu cầu truyền lên ID kiểu số (Integer).");
            }
        } catch (Exception e) {
            System.err.println("Lỗi hệ thống khi xử lý yêu cầu xóa Item!");
            response.setStatus("ERROR");
            response.setMessage("Đã xảy ra lỗi trên Server khi xóa sản phẩm.");
            e.printStackTrace();
        }

        return response;
    }

    public Response handleUpdateItemDescription(Request request) {
        Response response = new Response();
        Object payloadObj = request.getPayload();

        try {
            // Bắt buộc client phải gửi lên mảng Object [itemId, newDescription]
            if (payloadObj instanceof Object[]) {
                Object[] payload = (Object[]) payloadObj;

                // Kiểm tra số lượng phần tử và kiểu dữ liệu
                if (payload.length == 2 && payload[0] instanceof Number && payload[1] instanceof String) {
                    int itemId = ((Number) payload[0]).intValue(); // Ép kiểu an toàn
                    String newDescription = (String) payload[1];

                    // Gọi hàm cập nhật mô tả dưới ItemRepository
                    boolean isUpdated = itemRepo.updateItemDescription(itemId, newDescription);

                    if (isUpdated) {
                        response.setStatus("SUCCESS");
                        response.setMessage("Cập nhật mô tả sản phẩm thành công.");
                    } else {
                        response.setStatus("FAIL");
                        response.setMessage("Cập nhật thất bại. Không tìm thấy sản phẩm hoặc có lỗi xảy ra.");
                    }
                } else {
                    response.setStatus("FAIL");
                    response.setMessage("Định dạng dữ liệu không hợp lệ. Yêu cầu mảng [Number, String].");
                }
            } else {
                response.setStatus("FAIL");
                response.setMessage("Dữ liệu payload không hợp lệ. Yêu cầu bắt buộc gửi dạng mảng Object[].");
            }
        } catch (Exception e) {
            System.err.println("Lỗi hệ thống khi xử lý Update Item Description!");
            response.setStatus("ERROR");
            response.setMessage("Đã xảy ra lỗi trên Server khi cập nhật mô tả.");
            e.printStackTrace();
        }

        return response;
    }

    public Response handleGetRejectReason(Request request) {
        Response response = new Response();

        try {
            // Kiểm tra và ép kiểu an toàn ID gửi lên từ Client
            if (request.getPayload() instanceof Number) {
                int auctionId = ((Number) request.getPayload()).intValue();

                // Lấy lý do mới nhất với reasonType là "ITEM_REJECTED"
                // Do ở hàm handleRejectItem, targetId được lưu chính là auctionId
                Reason latestReason = reasonRepo.getLatestReason(auctionId, "ITEM_REJECTED");

                if (latestReason != null) {
                    response.setStatus("SUCCESS");
                    response.setMessage("Lấy lý do từ chối thành công.");
                    response.setData(latestReason.getReason()); // Chỉ trả về nội dung String cho Client dễ dùng
                } else {
                    response.setStatus("FAIL");
                    response.setMessage("Không tìm thấy lý do từ chối nào cho phiên đấu giá này.");
                }
            } else {
                response.setStatus("FAIL");
                response.setMessage("Dữ liệu Payload không hợp lệ. Yêu cầu truyền lên Auction ID kiểu số (Integer).");
            }
        } catch (Exception e) {
            System.err.println("Lỗi hệ thống khi xử lý yêu cầu lấy lý do từ chối!");
            response.setStatus("ERROR");
            response.setMessage("Đã xảy ra lỗi trên Server khi lấy lý do.");
            e.printStackTrace();
        }

        return response;
    }

    public Response handleCreateChanges(Request request) {
        Response response = new Response();
        Object payloadObj = request.getPayload();

        try {
            // Yêu cầu Client gửi lên mảng Object [itemId, changesString]
            if (payloadObj instanceof Object[]) {
                Object[] payload = (Object[]) payloadObj;

                if (payload.length == 2 && payload[0] instanceof Number && payload[1] instanceof String) {
                    int itemId = ((Number) payload[0]).intValue();
                    String changes = (String) payload[1];

                    boolean isUpdated = itemRepo.createChanges(itemId, changes);

                    if (isUpdated) {
                        // NEW LOGIC: Set item and auction status to "PROPOSAL"
                        itemRepo.updateStatus(itemId, "PROPOSAL");
                        Auction auction = auctionRepo.getAuctionByItemId(itemId);
                        if (auction != null) {
                            auctionRepo.updateStatus(auction.getId(), "PROPOSAL");
                        }

                        response.setStatus("SUCCESS");
                        response.setMessage("Cập nhật lịch sử thay đổi (changes) thành công và chuyển trạng thái sang PROPOSAL.");
                    } else {
                        response.setStatus("FAIL");
                        response.setMessage("Không thể cập nhật. Có thể ID sản phẩm không tồn tại.");
                    }
                } else {
                    response.setStatus("FAIL");
                    response.setMessage("Định dạng dữ liệu không hợp lệ. Yêu cầu mảng [Number, String].");
                }
            } else {
                response.setStatus("FAIL");
                response.setMessage("Dữ liệu payload không hợp lệ. Yêu cầu bắt buộc gửi dạng mảng Object[].");
            }
        } catch (Exception e) {
            System.err.println("Lỗi hệ thống khi xử lý Create Changes!");
            response.setStatus("ERROR");
            response.setMessage("Đã xảy ra lỗi trên Server khi tạo changes.");
            e.printStackTrace();
        }

        return response;
    }

    public Response handleGetChanges(Request request) {
        Response response = new Response();

        try {
            // Yêu cầu Client gửi lên itemId
            if (request.getPayload() instanceof Number) {
                int itemId = ((Number) request.getPayload()).intValue();

                String changes = itemRepo.getChanges(itemId);

                if (changes != null) {
                    response.setStatus("SUCCESS");
                    response.setMessage("Lấy dữ liệu changes thành công.");
                    response.setData(changes); // Trả về chuỗi changes cho Client
                } else {
                    response.setStatus("FAIL");
                    response.setMessage("Không tìm thấy dữ liệu changes hoặc sản phẩm không tồn tại.");
                }
            } else {
                response.setStatus("FAIL");
                response.setMessage("Dữ liệu Payload không hợp lệ. Yêu cầu truyền lên ID kiểu số (Integer).");
            }
        } catch (Exception e) {
            System.err.println("Lỗi hệ thống khi xử lý Get Changes!");
            response.setStatus("ERROR");
            response.setMessage("Đã xảy ra lỗi trên Server khi lấy changes.");
            e.printStackTrace();
        }

        return response;
    }

    public Response handleDeleteChanges(Request request) {
        Response response = new Response();

        try {
            // Yêu cầu Client gửi lên itemId
            if (request.getPayload() instanceof Number) {
                int itemId = ((Number) request.getPayload()).intValue();

                boolean isDeleted = itemRepo.deleteChanges(itemId);

                if (isDeleted) {
                    // NEW LOGIC: Set item and auction status to "PROPOSAL"
                    itemRepo.updateStatus(itemId, "WAITING");
                    Auction auction = auctionRepo.getAuctionByItemId(itemId);
                    if (auction != null) {
                        auctionRepo.updateStatus(auction.getId(), "WAITING");
                    }

                    response.setStatus("SUCCESS");
                    response.setMessage("Đã xóa dữ liệu changes thành công và chuyển trạng thái sang PROPOSAL.");
                } else {
                    response.setStatus("FAIL");
                    response.setMessage("Không thể xóa. Có thể ID sản phẩm không tồn tại.");
                }
            } else {
                response.setStatus("FAIL");
                response.setMessage("Dữ liệu Payload không hợp lệ. Yêu cầu truyền lên ID kiểu số (Integer).");
            }
        } catch (Exception e) {
            System.err.println("Lỗi hệ thống khi xử lý Delete Changes!");
            response.setStatus("ERROR");
            response.setMessage("Đã xảy ra lỗi trên Server khi xóa changes.");
            e.printStackTrace();
        }

        return response;
    }

    public Response handleAcceptChanges(Request request) {
        Response response = new Response();

        try {
            // Yêu cầu Client gửi lên itemId
            if (request.getPayload() instanceof Number) {
                int itemId = ((Number) request.getPayload()).intValue();

                // Gọi hàm đắp dữ liệu từ changes sang description và xóa changes
                boolean isAccepted = itemRepo.acceptChanges(itemId);

                if (isAccepted) {
                    // LOGIC TƯƠNG TỰ DELETE: Chuyển item và auction về lại trạng thái bình thường (WAITING)
                    itemRepo.updateStatus(itemId, "WAITING");
                    Auction auction = auctionRepo.getAuctionByItemId(itemId);
                    if (auction != null) {
                        auctionRepo.updateStatus(auction.getId(), "WAITING");
                    }

                    response.setStatus("SUCCESS");
                    response.setMessage("Đã chấp nhận các thay đổi, cập nhật mô tả thành công và đưa sản phẩm về WAITING.");
                } else {
                    response.setStatus("FAIL");
                    response.setMessage("Không thể chấp nhận thay đổi. Có thể ID sản phẩm không tồn tại hoặc không có thay đổi (changes) nào.");
                }
            } else {
                response.setStatus("FAIL");
                response.setMessage("Dữ liệu Payload không hợp lệ. Yêu cầu truyền lên ID kiểu số (Integer).");
            }
        } catch (Exception e) {
            System.err.println("Lỗi hệ thống khi xử lý Accept Changes!");
            response.setStatus("ERROR");
            response.setMessage("Đã xảy ra lỗi trên Server khi chấp nhận thay đổi.");
            e.printStackTrace();
        }

        return response;
    }

    public Response handleSellerDeleteProposal(Request request) {
        Response response = new Response();
        try {
            if (request.getPayload() instanceof Number) {
                int itemId = ((Number) request.getPayload()).intValue();
                itemRepo.updateStatus(itemId, "DELETE_PROPOSAL");
                Auction auction = auctionRepo.getAuctionByItemId(itemId);
                if (auction != null) {
                    auctionRepo.updateStatus(auction.getId(), "DELETE_PROPOSAL");
                }
                response.setStatus("SUCCESS");
                response.setMessage("Đã gửi yêu cầu xóa sản phẩm đến quản trị viên.");
            } else {
                response.setStatus("FAIL");
                response.setMessage("Dữ liệu không hợp lệ.");
            }
        } catch (Exception e) {
            response.setStatus("ERROR");
            response.setMessage("Lỗi hệ thống.");
            e.printStackTrace();
        }
        return response;
    }

    public Response handleRefuseDeleteProposal(Request request) {
        Response response = new Response();
        try {
            if (request.getPayload() instanceof Number) {
                int itemId = ((Number) request.getPayload()).intValue();
                itemRepo.updateStatus(itemId, "WAITING");
                Auction auction = auctionRepo.getAuctionByItemId(itemId);
                if (auction != null) {
                    auctionRepo.updateStatus(auction.getId(), "WAITING");
                }
                response.setStatus("SUCCESS");
                response.setMessage("Đã từ chối yêu cầu xóa sản phẩm.");
            } else {
                response.setStatus("FAIL");
                response.setMessage("Dữ liệu không hợp lệ.");
            }
        } catch (Exception e) {
            response.setStatus("ERROR");
            response.setMessage("Lỗi hệ thống.");
            e.printStackTrace();
        }
        return response;
    }
}