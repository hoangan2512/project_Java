package server.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import message.Request;
import message.Response;
import model.ActionType;
import server.ServerApplication;
import server.controller.AuctionController;
import server.controller.BidController;
import server.controller.ItemController;
import server.controller.UserController;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    // Gọi các Controller ra để làm việc
    private UserController userController = new UserController();
    private ItemController itemController = new ItemController();
    private BidController bidController = new BidController();
    private AuctionController auctionController = new AuctionController();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    private void closeEverything() {
        ServerApplication.clients.remove(this);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                Request request = (Request) in.readObject();
                System.out.println("Nhận yêu cầu: " + request.getAction());

                // Giao việc cho hàm chia chọn
                Response response = handleBusinessLogic(request);

                out.writeObject(response);
                out.flush();
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Một Client đã ngắt kết nối.");
        } finally {
            closeEverything();
        }
    }

    // BỘ PHẬN ĐIỀU HƯỚNG (ROUTER)
    private Response handleBusinessLogic(Request request) {
        ActionType type = request.getAction();

        switch (type) {
            case LOGIN:
                return userController.handleLogin(request);
            case REGISTER:
                return userController.handleRegister(request);
            case LOGOUT:
                return new Response("SUCCESS", null, "Đăng xuất thành công.");
            case CHECK_BALANCE:
                return userController.handleBalance(request);
            case DEPOSIT:
                return userController.handleDeposit(request);
            case CREATE_ITEM:
                return itemController.handleCreateItem(request);
            case BID:
                return bidController.handleBid(request);
            case GET_BID_HISTORY:
                return bidController.handleGetBidHistory(request);
            case GET_LIST:
                return auctionController.handleGetList(request);
            case GET_ITEM_DETAIL:
                return auctionController.handleGetItemDetail(request);
            case CHECK_DUPLICATE_NAME:
                return itemController.handleCheckDuplicateName(request);
            case AUCTION_END:
                return auctionController.handleAuctionEnd(request);
            default:
                return new Response("ERROR",null,"hành động không xác định: " +type);
        }
    }

    public void sendMessage(Object msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.err.println("Không thể gửi tin nhắn.");
        }
    }
}