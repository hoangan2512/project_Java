package server.handler.actions;

import message.Request;
import message.Response;
import server.controller.ItemController;
import server.handler.ActionHandler;
import server.network.ClientHandler;

import java.util.logging.Logger;

public class SellerHandler implements ActionHandler {
    private static final Logger LOGGER = Logger.getLogger(SellerHandler.class.getName());
    private final ItemController itemController = new ItemController();

    @Override
    public Response execute(Request request, ClientHandler client) {
        // Tầng bảo mật nghiêm ngặt: Chỉ cho phép tài khoản có quyền SELLER đi qua
        if (!client.checkAuthorization("SELLER")) {
            String userName = (client.getLoggedInUser() != null) ? client.getLoggedInUser().getName() : "Anonymous";
            LOGGER.warning("Unauthorized seller action attempted by user: " + userName);
            return new Response("FAIL", null, "Bạn chưa đăng nhập hoặc không phải là Người bán!");
        }

        // Điều hướng các hành động của Người bán sang ItemController
        switch (request.getAction()) {
            case CREATE_ITEM:
                return itemController.handleCreateItem(request);

            case CHECK_DUPLICATE_NAME:
                return itemController.handleCheckDuplicateName(request);

            case SELLER_DELETE_ITEM:
                return itemController.handleDeleteItem(request);

            case UPDATE_ITEM_DESCRIPTION:
                return itemController.handleUpdateItemDescription(request);

            case SELLER_GET_REASON:
                return itemController.handleGetRejectReason(request);

            case PROPOSE_CHANGES:
                return itemController.handleCreateChanges(request);

            case SELLER_GET_CHANGES:
                return itemController.handleGetChanges(request);

            case SELLER_DELETE_CHANGES:
                return itemController.handleDeleteChanges(request);

            case SELLER_DELETE_PROPOSAL:
                return itemController.handleSellerDeleteProposal(request);

            default:
                LOGGER.warning("Unsupported seller action attempted: " + request.getAction());
                return new Response("ERROR", null, "Seller action not supported.");
        }
    }
}