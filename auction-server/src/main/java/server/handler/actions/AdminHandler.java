package server.handler.actions;

import java.util.logging.Logger;
import message.Request;
import message.Response;
import server.controller.AuctionController;
import server.controller.ItemController;
import server.controller.UserController;
import server.handler.ActionHandler;
import server.network.ClientHandler;

public class AdminHandler implements ActionHandler {
    private static final Logger LOGGER = Logger.getLogger(AdminHandler.class.getName());

    private final UserController userController = new UserController();
    private final ItemController itemController = new ItemController();
    private final AuctionController auctionController = new AuctionController();

    @Override
    public Response execute(Request request, ClientHandler client) {
        // Lớp bảo vệ an ninh nghiêm ngặt: Chỉ tài khoản ADMIN mới được thực hiện các hành động này
        if (!client.checkAuthorization("ADMIN")) {
            LOGGER.warning("Unauthorized admin action attempted by user: " +
                    (client.getLoggedInUser() != null ? client.getLoggedInUser().getName() : "Anonymous"));
            return client.unauthResponse();
        }

        switch (request.getAction()) {
            case ADMIN_GET_ALL_USERS:
                return userController.handleGetAllUsers(request);

            case ADMIN_BAN_USER:
                return userController.handleBanUser(request);

            case ADMIN_UNBAN_USER:
                return userController.handleUnbanUser(request);

            case ADMIN_APPROVE_ITEM:
                return itemController.handleApproveItem(request);

            case ADMIN_REJECT_ITEM:
                return itemController.handleRejectItem(request);

            case ADMIN_STOP_AUCTION:
                return auctionController.handleAdminStopAuction(request);

            case ADMIN_DELETE_ITEM:
                return itemController.handleDeleteItem(request);

            case ADMIN_GET_ITEM_REASON:
                return itemController.handleGetRejectReason(request);

            case ADMIN_GET_AUCTION_REASON:
                return auctionController.handleGetStopReason(request);

            case ADMIN_GET_USER_REASON:
                return userController.handleGetBanReason(request);

            case ADMIN_GET_CHANGES:
                return itemController.handleGetChanges(request);

            case ADMIN_DELETE_CHANGES:
                return itemController.handleDeleteChanges(request);

            case ADMIN_ACCEPT_CHANGES:
                return itemController.handleAcceptChanges(request);

            case ADMIN_REFUSE_DELETE_PROPOSAL:
                return itemController.handleRefuseDeleteProposal(request);

            default:
                LOGGER.warning("Unsupported admin action attempted: " + request.getAction());
                return new Response("ERROR", null, "Admin action not supported.");
        }
    }
}