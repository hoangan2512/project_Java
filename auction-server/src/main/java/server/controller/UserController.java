package server.controller;

import message.Request;
import message.Response;
import model.ActionType;
import model.Reason;
import model.User;
import server.repository.AuctionRepository;
import server.repository.ItemRepository;
import server.repository.ReasonRepository;
import server.repository.UserRepository;
import server.network.AuctionServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserController {
    private static final Logger LOGGER = Logger.getLogger(UserController.class.getName());
    private final UserRepository userRepo;
    private final ItemRepository itemRepo;
    private final AuctionRepository auctionRepo;
    private final ReasonRepository reasonRepo;

    public UserController() {
        this.reasonRepo = new ReasonRepository();
        this.userRepo = new UserRepository();
        this.itemRepo = new ItemRepository();
        this.auctionRepo = new AuctionRepository();
    }

    public Response handleGoogleLoginAuth(String email, String name) {
        Response response = new Response();

        // Kiểm tra xem user có tồn tại chưa (dùng email làm username)
        User user = userRepo.findUserByUsername(email);

        if (user == null) {
            // User chưa tồn tại, tạo mới
            // Yêu cầu: "chỉnh lại, phương thức đăng nhập/ đăng kí qua google chỉ dành cho BIDDER"
            String defaultRole = "BIDDER";
            boolean isCreated = userRepo.createGoogleUser(email, defaultRole);

            if (isCreated) {
                user = userRepo.findUserByUsername(email);
                LOGGER.log(Level.INFO, "New Google user registered successfully: ''{0}'' with role: {1}", new Object[]{email, defaultRole});
                response.setStatus("SUCCESS");
                response.setMessage("Đăng ký & đăng nhập qua Google thành công!");
                response.setData(user);
            } else {
                LOGGER.log(Level.SEVERE, "Failed to create Google user: ''{0}''", email);
                response.setStatus("FAIL");
                response.setMessage("Có lỗi xảy ra trong quá trình đăng ký tài khoản Google.");
            }
        } else {
            // User đã tồn tại, kiểm tra trạng thái
            if ("BANNED".equalsIgnoreCase(user.getStatus())) {
                LOGGER.log(Level.WARNING, "Google Login failed: Account is banned. User: ''{0}''", email);
                response.setStatus("FAIL");
                response.setMessage("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Admin.");
            } else if (!"BIDDER".equalsIgnoreCase(user.getRole()) && !"BOTH".equalsIgnoreCase(user.getRole())) {
                // Đảm bảo user có role hợp lệ (chỉ BIDDER hoặc BOTH)
                LOGGER.log(Level.WARNING, "Google Login failed: Invalid role. User: ''{0}''", email);
                response.setStatus("FAIL");
                response.setMessage("Tính năng đăng nhập bằng Google chỉ dành cho Bidder.");
            } else {
                LOGGER.log(Level.INFO, "Google Login successful for user: ''{0}''", email);
                response.setStatus("SUCCESS");
                response.setMessage("Đăng nhập qua Google thành công!");
                response.setData(user);
            }
        }

        return response;
    }

    public Response handleLogin(Request request) {
        Response response = new Response();
        User userFromClient = (User) request.getPayload();
        String username = userFromClient.getName();

        // Bước 1: Xác thực người dùng (Authentication)
        User authenticatedUser = userRepo.login(username, userFromClient.getPassword());

        // Bước 2: Kiểm tra quyền hạn (Authorization)
        if (authenticatedUser != null) {
            
            // Xử lý trường hợp tài khoản bị khóa
            if ("BANNED".equalsIgnoreCase(authenticatedUser.getStatus())) {
                 response.setStatus("FAIL");
                 response.setMessage("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Admin.");
                 return response;
            }

            String role = authenticatedUser.getRole();
            ActionType action = request.getAction();

            // Kiểm tra role dựa trên loại login request
            boolean isLoginRequestValid = isIsLoginRequestValid(action, role);

            if (isLoginRequestValid) {
                // Đăng nhập thành công và đúng vai trò
                LOGGER.log(Level.INFO, "Authorization successful for user ''{0}''. Granting access.", username);
                response.setStatus("SUCCESS");
                response.setMessage("Đăng nhập thành công!");
                response.setData(authenticatedUser);
            } else {
                // Đăng nhập thành công nhưng sai vai trò
                LOGGER.log(Level.WARNING,
                        "Authorization failed for user ''{0}''. Role mismatch. Expected login type: {1}, but user role is: {2}",
                        new Object[]{username, action, role});
                response.setStatus("FAIL");
                response.setMessage("Bạn không có quyền truy cập vào khu vực này."); 
            }
        } else {
            // Xác thực thất bại (sai username/password)
            response.setStatus("FAIL");
            response.setMessage("Sai tài khoản hoặc mật khẩu!");
        }
        return response;
    }

    private static boolean isIsLoginRequestValid(ActionType action, String role) {
        boolean isLoginRequestValid = false;
        if (action == ActionType.LOGIN_BIDDER) {
             isLoginRequestValid = "BIDDER".equalsIgnoreCase(role) || "BOTH".equalsIgnoreCase(role);
        } else if (action == ActionType.LOGIN_SELLER) {
             isLoginRequestValid = "SELLER".equalsIgnoreCase(role) || "BOTH".equalsIgnoreCase(role);
        } else if (action == ActionType.LOGIN_ADMIN) {
             isLoginRequestValid = "ADMIN".equalsIgnoreCase(role);
        }
        return isLoginRequestValid;
    }

    public Response handleRegister(Request request) {
        Response response = new Response();
        User newUser = (User) request.getPayload();

        // Thêm bước kiểm tra tài khoản đã tồn tại hay chưa
        if (userRepo.isUserExists(newUser.getName())) {
            LOGGER.log(Level.WARNING, "Registration failed: Username ''{0}'' already exists.", newUser.getName());
            response.setStatus("FAIL");
            response.setMessage("Tài khoản đã tồn tại.");
            return response;
        }

        boolean isRegistered = userRepo.addUser(newUser);

        if (isRegistered) {
            // Lấy lại thông tin user đầy đủ từ DB (bao gồm cả ID vừa được tạo)
            User registeredUser = userRepo.findUserByUsername(newUser.getName());
            if (registeredUser != null) {
                LOGGER.log(Level.INFO, "New user registered successfully: ''{0}'' with role: {1}", new Object[]{registeredUser.getName(), registeredUser.getRole()});
                response.setStatus("SUCCESS");
                response.setMessage("Đăng ký thành công!");
                response.setData(registeredUser); // Trả về user đầy đủ thông tin
            } else {
                LOGGER.log(Level.SEVERE, "Registration failed for username: ''{0}''. Could not retrieve user after creation.", newUser.getName());
                response.setStatus("FAIL");
                response.setMessage("Có lỗi xảy ra trong quá trình đăng ký (không thể lấy thông tin user).");
            }
        } else {
            LOGGER.log(Level.SEVERE, "Registration failed for username: ''{0}'' due to a database error.", newUser.getName());
            response.setStatus("FAIL");
            response.setMessage("Có lỗi xảy ra trong quá trình đăng ký.");
        }
        return response;
    }
    
    // ==========================================
    // CÁC HÀNH ĐỘNG DÀNH CHO ADMIN QUẢN LÝ USER
    // ==========================================
    
    public Response handleGetAllUsers(Request request) {
        Response response = new Response();
        List<User> users = userRepo.getAllUsers();
        
        List<Map<String, Object>> usersData = new ArrayList<>();
        
        for (User user : users) {
            Map<String, Object> userDataMap = new HashMap<>();
            userDataMap.put("user", user);
            
            // Đếm số lượng items và auctions cho seller (nếu role là SELLER hoặc BOTH)
            int activeItemsCount = 0;
            int rejectedItemsCount = 0;
            int activeAuctionsCount = 0;
            int suspendedAuctionsCount = 0;
            int warningsCount = 0;
            
            if ("SELLER".equalsIgnoreCase(user.getRole()) || "BOTH".equalsIgnoreCase(user.getRole())) {
                Map<String, Integer> itemCounts = itemRepo.countItemsBySellerId(user.getId());
                activeItemsCount = itemCounts.getOrDefault("active", 0);
                rejectedItemsCount = itemCounts.getOrDefault("rejected", 0);
                
                Map<String, Integer> auctionCounts = auctionRepo.countAuctionsBySellerId(user.getId());
                activeAuctionsCount = auctionCounts.getOrDefault("active", 0);
                suspendedAuctionsCount = auctionCounts.getOrDefault("suspended", 0);
            }
            warningsCount = rejectedItemsCount + suspendedAuctionsCount;
            
            userDataMap.put("itemsCount", activeItemsCount);
            userDataMap.put("rejectedItemsCount", rejectedItemsCount);
            userDataMap.put("auctionsCount", activeAuctionsCount);
            userDataMap.put("suspendedAuctionsCount", suspendedAuctionsCount);
            userDataMap.put("warningsCount", warningsCount);
            
            usersData.add(userDataMap);
        }
        
        response.setStatus("SUCCESS");
        response.setMessage("Lấy danh sách user thành công.");
        response.setData(usersData);
        return response;
    }

    public Response handleBanUser(Request request) {
        Response response = new Response();
        Object payloadObj = request.getPayload();

        // CHỈ XỬ LÝ: Client bắt buộc phải gửi lên mảng Object [userId, reason]
        if (payloadObj instanceof Object[]) {
            Object[] payload = (Object[]) payloadObj;

            // Kiểm tra số lượng phần tử và kiểm tra kiểu dữ liệu an toàn (Number, String)
            if (payload.length == 2 && payload[0] instanceof Number && payload[1] instanceof String) {
                int userIdToBan = ((Number) payload[0]).intValue(); // Ép kiểu an toàn tránh lỗi Long/Integer qua Socket
                String reasonText = (String) payload[1];

                // 1. Thực hiện cập nhật trạng thái sang BANNED trong bảng users
                boolean success = userRepo.updateUserStatus(userIdToBan, "BANNED");
                if (success) {
                    // 2. Thử lưu lý do khóa tài khoản vào bảng reasons (Bọc try-catch để an toàn luồng chính)
                    try {
                        Reason reason = new Reason();
                        reason.setTargetId(userIdToBan);
                        reason.setReasonType("USER_BANNED");
                        reason.setReason(reasonText);
                        reasonRepo.addReason(reason);
                    } catch (Exception e) {
                        System.err.println("[SERVER WARNING] Không thể lưu lý do ban user vào DB: " + e.getMessage());
                    }

                    response.setStatus("SUCCESS");
                    response.setMessage("Đã khóa tài khoản thành công kèm lý do.");

                    // 3. Đuổi trực tiếp Client đang online ra khỏi hệ thống ngay lập tức
                    AuctionServer.forceLogoutUser(userIdToBan);
                } else {
                    response.setStatus("FAIL");
                    response.setMessage("Không thể khóa tài khoản, vui lòng thử lại.");
                }
            } else {
                response.setStatus("FAIL");
                // Thông báo lỗi tường minh cấu trúc mảng để Client điều chỉnh code cho đúng
                response.setMessage("Định dạng dữ liệu mảng không hợp lệ. Yêu cầu mảng cấu trúc [Number, String].");
            }
        } else {
            response.setStatus("FAIL");
            response.setMessage("Dữ liệu payload không hợp lệ. Yêu cầu bắt buộc gửi dạng mảng Object[].");
        }

        return response;
    }
    
    public Response handleUnbanUser(Request request) {
         Response response = new Response();
         Object payload = request.getPayload();
         if (payload instanceof Integer userIdToUnban) {

             boolean success = userRepo.updateUserStatus(userIdToUnban, "ACTIVE");
             if (success) {
                 response.setStatus("SUCCESS");
                 response.setMessage("Đã mở khóa tài khoản thành công.");
             } else {
                 response.setStatus("FAIL");
                 response.setMessage("Không thể mở khóa tài khoản, vui lòng thử lại.");
             }
         } else {
             response.setStatus("FAIL");
             response.setMessage("Dữ liệu không hợp lệ.");
         }
         return response;
    }
}
