//Tạo lớp quản lí client thuộc mỗi luồng
package server.network;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import  java.util.*;
import  message.Request;
import message.Response;
import model.ActionType;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectInputStream in;   //Luồng vào
    private ObjectOutputStream out;  //Luồng ra
   //Constructor: nhận socket từ auctionserver
    public ClientHandler(Socket socket){
        this.socket=socket;
    }
    private void closeEverything(){ //Hàm ngắt kết nối với Client không còn kết nối(tắt mạng/app)
        AuctionServer.clients.remove(this); //loại bỏ ClientHandler ra khỏi danh sách, tránh Server cố gắng gửi thông báo tới kết nối này giảm khả năng bị lỗi hệ thống.
    try{
        //Đóng các luồng mạng
        if (in!=null){
            in.close();
        }
        if (out!=null){
            out.close();
        }
        if (socket != null){
            socket.close();
        }
    }catch (IOException ioException){
        ioException.printStackTrace();
    }
    }
    @Override
    public void run(){
        try{
            out=new ObjectOutputStream(socket.getOutputStream());
            in=new ObjectInputStream(socket.getInputStream());
            //Vòng lặp vô hạn để duy trì kết nối
            while (true){
                //1.Nhận yêu cầu từ người dùng.
                // Đọc gói tin request từ client. readObject() sẽ tạm dừng luồng cho đến khi có tin nhắn tới.
            Request request = (Request) in.readObject();
                System.out.println("Nhập yêu cầu đầu vào: "+ request.getAction());
            //2.Xử lý logic- Dựa vào ActionType(LOGIN,LOGOUT,GET_BID,...) để quyết định làm gì.
                Response response=handleBusinessLogic(request);
                out.writeObject(response);
                out.flush(); //liên tục đẩy dữ liệu ra khỏi bộ đệm
            }
        }
        catch (IOException | ClassNotFoundException exception){
            System.err.println("Người dùng "+socket.getInetAddress()+"đã ngắt kết nối tới máy chủ");
        }
        finally {
            closeEverything(); //Đóng tất cả các luồng
        }
    }
    //Hàm xử lí request
    private Response handleBusinessLogic(Request request){
        Response response=new Response();
        ActionType type =request.getAction();   //
    switch(type){
        case LOGIN:
            response.setStatus("SUCCESS");
            response.setMessage("Chào mừng quý khách " + request.getPayload() + " đã trở lại");
            break;

        case LOGOUT:
            response.setStatus("SUCCESS");
            response.setMessage("Đăng xuất tài khoản thành công: " + request.getPayload());
            break;

        case REGISTER:
            response.setStatus("SUCCESS");
            response.setMessage("Đăng ký thành công!: " + request.getPayload());
            break;

        case BID:
            response.setStatus("SUCCESS");
            response.setMessage("Đặt giá mới thành công: " + request.getPayload());
           //Thong bao cap nhap gia cho nhung nguoi dung khac
            Response notifyPrice = new Response();
            notifyPrice.setStatus("NOTIFY");
            notifyPrice.setMessage("Giá mới của vật phẩm: " + request.getPayload());
            //Gửi cho everyone
            AuctionServer.broadcast(notifyPrice);
            break;

        case GET_LIST:
            response.setStatus("SUCCESS");
            response.setMessage("Danh sách các vật phẩm đang đấu giá đã được tải.");
            break;

        case GET_ITEM_DETAIL:
            response.setStatus("SUCCESS");
            response.setMessage("Thông tin chi tiết vật phẩm: " + request.getPayload());
            break;

        case NOTIFY_NEW_PRICE:
            response.setStatus("NOTIFY");
            response.setMessage("Có người vừa đặt giá mới: " + request.getPayload());
            break;

        case AUCTION_END:
            // Thông báo kết thúc phiên đấu giá
            response.setStatus("SUCCESS");
            response.setMessage("Phiên đấu giá đã kết thúc! Người thắng cuộc là: " + request.getPayload());
            break;

        default:
            response.setStatus("ERROR");
            response.setMessage("Hành động không xác định.");
    }
    return response;
    }
    //hàm gửi tin cập nhập giá tự động đến client
    public void sendMessage(Object msg){
        try{
            out.writeObject(msg);
            out.flush(); //đẩy tất cả dữ liệu đến client liên tục
        }catch (IOException ioException){
            System.err.println("Không thể gửi tin nhắn tới người dùng này.");
        }
    }
}
