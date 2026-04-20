package network;
import java.io.*;
import message.Request;
import message.Response;
import java.net.Socket;
public class ClientSocket {
    private static final String SERVER_IP="localhost"; //tạo IP
    private static final Integer SERVER_PORT=2810;  //tạo port (giống với port thuộc auctionserver)
    public static Response sendRequest(Request request){ // tạo sendrequest trả về response
        try ( Socket socket= new Socket(SERVER_IP,SERVER_PORT)){ //tạo socket kết nối tới server
            ObjectOutputStream out= new ObjectOutputStream(socket.getOutputStream());   //out first tránh dreadlock
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            out.writeObject(request);   //gửi request
            out.flush();    //đẩy request tới server ngay lập tức
            Response res = (Response) in.readObject();
            return res;
        }
        catch (IOException ioException){
            System.err.println("Không thể kết nối tới hệ thống!");
            ioException.printStackTrace();
        }
        catch (ClassNotFoundException classNotFoundException){
            System.err.println("Không thể nhận diện được lớp trả về!");
            classNotFoundException.printStackTrace();
        }
        return null; //trả về null nếu không nằm trong các trường hợp trên
    }
}
