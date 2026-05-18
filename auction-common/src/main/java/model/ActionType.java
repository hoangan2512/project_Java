package model;

public enum ActionType {
    LOGIN_BIDDER,
    LOGIN_SELLER,
    LOGIN_ADMIN,    
    LOGOUT,
    REGISTER,
    BID,
    REGISTER_AUTO_BID, // Hành động mới cho auto bid
    GET_BID_HISTORY,
    GET_LIST,
    GET_ITEM_DETAIL,
    NOTIFY_NEW_PRICE,
    AUCTION_END,
    AUCTION_EXTENDED,  // Bổ sung: Thông báo gia hạn phiên đấu giá (Anti-Sniping)
    CREATE_ITEM,
    CHECK_DUPLICATE_NAME,
    CUSTOM_SEARCH,
    GET_SELLER_NAME,
    
    // --- Các hành động dành riêng cho Admin ---
    ADMIN_GET_ALL_USERS,
    ADMIN_BAN_USER,
    ADMIN_UNBAN_USER,
    ADMIN_GET_ITEMS,
    ADMIN_APPROVE_ITEM,
    ADMIN_REJECT_ITEM,
    ADMIN_GET_PENDING_ITEMS, ADMIN_STOP_AUCTION // Thêm hành động mới để dừng phiên đấu giá
}