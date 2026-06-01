package server.handler.actions;

import message.Request;
import message.Response;
import server.controller.BidController;
import server.handler.ActionHandler;
import server.network.ClientHandler;

import java.util.logging.Logger;

public class BidHandler implements ActionHandler {
    private static final Logger LOGGER = Logger.getLogger(BidHandler.class.getName());
    private BidController bidController = new BidController();
    @Override
    public Response execute(Request request, ClientHandler client){
        //Bảo đảm chỉ BIDDER ACCOUNT mới có quyền thực hiện hành động này
        if (!client.checkAuthorization("BIDDER")){
            LOGGER.warning("Unauthorized bid action attempted by user");
            return new Response("FAIL", null, "You are not allowed to bid! Please Sign in or Sign up to continue!");
        }
        switch(request.getAction()){
            case BID:
               return bidController.handleBid(request);
            case REGISTER_AUTOBID:
                return  bidController.handleRegisterAutoBid(request);
            case UNREGISTER_AUTOBID:
                return bidController.handleUnregisterAutoBid(request);
            case GET_BID_HISTORY:
                return bidController.handleGetBidHistory(request);
            case GET_HIGHEST_BIDDER_ID:
                return bidController.handleGetHighestBidderId(request);
            case CHECK_AUTOBID_STATUS:
                return bidController.handleCheckAutobidStatus(request);
            default:
                LOGGER.warning("Unsupported bidder action attempted: "+request.getAction());
                return new Response("ERROR", null, "Bidder action not supported.");
        }
    }

}
