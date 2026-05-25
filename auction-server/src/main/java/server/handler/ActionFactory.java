package server.handler;

import message.Response;
import model.ActionType;
import server.handler.actions.*;

public class ActionFactory {
    public static ActionHandler getHandler(ActionType type) {
        if (type == null) return null;

        switch (type) {
            case LOGIN_BIDDER:
            case LOGIN_SELLER:
            case LOGIN_ADMIN:
            case REGISTER:
            case LOGOUT:
            case GOOGLE_LOGIN:
                return new UserHandler();

            case ADMIN_GET_ALL_USERS:
            case ADMIN_BAN_USER:
            case ADMIN_UNBAN_USER:
            case ADMIN_APPROVE_ITEM:
            case ADMIN_REJECT_ITEM:
            case ADMIN_STOP_AUCTION:
            case ADMIN_DELETE_ITEM:
            case ADMIN_GET_ITEM_REASON:
            case ADMIN_GET_AUCTION_REASON:
            case ADMIN_GET_USER_REASON:
                return new AdminHandler();

            case CREATE_ITEM:
            case CHECK_DUPLICATE_NAME:
            case SELLER_DELETE_ITEM:
            case UPDATE_ITEM_DESCRIPTION:
            case SELLER_GET_REASON:
                return new SellerHandler();

            case BID:
            case REGISTER_AUTOBID:
            case UNREGISTER_AUTOBID:
            case GET_BID_HISTORY:
                return new BidHandler();

            case GET_LIST:
            case GET_ITEM_DETAIL:
            case CUSTOM_SEARCH:
            case GET_IMAGE:
            case AUCTION_END:
                return new AuctionHandler();

            default:
                return (req, client) -> new Response("ERROR", null, "Hành động không xác định: " + type);
        }
    }
}