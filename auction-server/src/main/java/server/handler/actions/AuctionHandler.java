package server.handler.actions;

import server.handler.ActionHandler;

import java.util.logging.Logger;

import message.Request;
import message.Response;
import server.network.ClientHandler;
import server.controller.AuctionController;

public class AuctionHandler implements ActionHandler {
    private static final Logger LOGGER = Logger.getLogger(AuctionHandler.class.getName());
    private final AuctionController auctionController = new AuctionController();

    @Override
    public Response execute(Request request, ClientHandler client) {
        return switch (request.getAction()) {
            case GET_LIST -> auctionController.handleGetList(request);
            case GET_ITEM_DETAIL -> auctionController.handleGetItemDetail(request);
            case CUSTOM_SEARCH -> auctionController.handleCustomSearch(request);
            case GET_IMAGE -> auctionController.handleGetImage(request);
            case AUCTION_END -> {
                LOGGER.warning("Client attempted to manually trigger AUCTION_END from IP: " +
                        client.getLoggedInUser() != null ? client.getLoggedInUser().getName() : "Anonymous");
                yield new Response("FAIL", null, "Client không có quyền kết thúc phiên đấu giá.");
            }
            default -> {
                LOGGER.warning("Unsupported auction action attempted: " + request.getAction());
                yield new Response("ERROR", null, "Auction action not supported.");
            }
        };
    }
}
