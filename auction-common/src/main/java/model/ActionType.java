package model;

public enum ActionType {
    LOGIN_BIDDER,
    LOGIN_SELLER,
    LOGIN_ADMIN,    // Thêm login cho admin
    LOGOUT,
    REGISTER,
    BID,
    GET_BID_HISTORY,
    GET_LIST,
    GET_ITEM_DETAIL,
    CHECK_BALANCE,
    DEPOSIT,
    NOTIFY_NEW_PRICE,
    AUCTION_END,
    CREATE_ITEM,
    CHECK_DUPLICATE_NAME,
    CUSTOM_SEARCH,
    GET_SELLER_NAME,
    
    // --- Các hành động dành riêng cho Admin ---
    ADMIN_GET_ALL_USERS,
    ADMIN_BAN_USER,
    ADMIN_UNBAN_USER,
    ADMIN_GET_PENDING_ITEMS,
    ADMIN_APPROVE_ITEM,
    ADMIN_REJECT_ITEM
}
