package network;

import message.Request;
import message.Response;

import java.io.*;
import java.net.Socket;

public class ClientSocket {
    // 1. Cấu hình thông tin Server
    // Nên để private static final vì IP và Port của Server thường cố định
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 12345;

    /**
     * Hàm cốt lõi: Gửi Request lên Server và đợi nhận Response
     */
    public static Response sendRequest(Request request) {

        // 2. Sử dụng try-with-resources để tự động đóng kết nối khi xong việc
        // Điều này giúp tránh kẹt cổng (port leak) hoặc treo ứng dụng
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             // LƯU Ý CỰC KỲ QUAN TRỌNG: Luôn khởi tạo ObjectOutputStream TRƯỚC ObjectInputStream
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // 3. Gửi gói tin đi
            out.writeObject(request);
            out.flush(); // Lệnh này bắt buộc phải có để "đẩy" dữ liệu đi ngay lập tức

            // 4. Lắng nghe và nhận phản hồi từ Server
            Response response = (Response) in.readObject();
            return response;

        } catch (IOException e) {
            System.err.println("Lỗi kết nối: Không thể kết nối tới Server. Hãy kiểm tra xem Server đã chạy chưa!");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("Lỗi dữ liệu: Không nhận diện được Class trả về từ Server.");
            e.printStackTrace();
        }

        // 5. Trả về null nếu quá trình trên thất bại
        return null;
    }
}