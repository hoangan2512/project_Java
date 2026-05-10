package network;

import java.io.*;
import java.net.Socket;
import java.security.PublicKey;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javafx.application.Platform;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;

import message.Request;
import message.Response;
import controller.mainPageController;

public class ClientSocket {
    private static final String SERVER_IP = "localhost";
    private static final Integer SERVER_PORT = 2810;

    private static Socket socket;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;

    private static SecretKey sharedAesKey;

    private static final BlockingQueue<Response> responseQueue = new LinkedBlockingQueue<>();

    // Trao đổi khoá AES được mã hoá bằng RSA với server
    private static void performHandshake() throws Exception {
        PublicKey serverPublicKey = (PublicKey) in.readObject();
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        sharedAesKey = keyGen.generateKey();
        Cipher rsaCipher = Cipher.getInstance("RSA");
        rsaCipher.init(Cipher.WRAP_MODE, serverPublicKey); // mã hoá public key
        byte[] encrytedAESkey = rsaCipher.wrap(sharedAesKey);
        out.writeObject(encrytedAESkey);
        out.flush();
    }
    //Hàm ngắt kết nối socket
    public static void disconnect() {
        try {
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();

            sharedAesKey = null; // Xóa khóa khi ngắt kết nối
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // HÀM KIỂM TRA KẾT NỐI BAN ĐẦU
    public static boolean tryConnect() {
        if (socket != null && !socket.isClosed()) {
            return true;
        }
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Thực hiện hàm handshake để trao đổi khóa an toàn
            performHandshake();

            startListenerThread();
            return true;

        } catch (Exception e) {
            System.out.println("ERROR: Không thể kết nối hoặc thực hiện handshake với server.");
            e.printStackTrace();
            disconnect(); // Dọn dẹp tài nguyên nếu có lỗi
            return false;
        }
    }
    public static synchronized Response sendRequest(Request request) {
        if (socket == null || socket.isClosed()) {  //ktra kết nối socket
            if (!tryConnect()) {
                return null;
            }
        }

        try {
            // Mã hóa đối tượng Request trước khi gửi
            Cipher aesCipher = Cipher.getInstance("AES");
            aesCipher.init(Cipher.ENCRYPT_MODE, sharedAesKey);
            SealedObject sealedRequest = new SealedObject(request, aesCipher);

            out.writeObject(sealedRequest);
            out.flush();

            return responseQueue.take();
        } catch (Exception e) {
            System.out.println("ERROR: Failed to send/receive encrypted request. " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    private static void startListenerThread() {
        Thread listenerThread = new Thread(() -> {
            try {
                //tạo một đối tượng Cipher để thực hiện mã hóa/giải mã bằng thuật toán AES.
                Cipher aesCipher =Cipher.getInstance("AES");

                while (true) {
                    // Nhận đối tượng đã được mã hóa
                    SealedObject sealedResponse = (SealedObject) in.readObject();

                    // Giải mã để lấy lại đối tượng Response gốc
                     aesCipher.init(Cipher.DECRYPT_MODE, sharedAesKey); // cau hinh để sẵn sàng giải mã đối tượng được mã hoá
                    Response res = (Response) sealedResponse.getObject(aesCipher); //giải mã đối tượng được mã hoá từ server,

                    String status = res.getStatus();
                    if ("NOTIFY_NEW_PRICE".equals(status) || "AUCTION_END".equals(status) || "AUCTION_START".equals(status)) {
                        Platform.runLater(() -> handleBroadcast(res));
                    } else {
                        responseQueue.put(res);
                    }
                }
            } catch (EOFException e) {
                System.out.println("INFO: Server đã đóng kết nối.");
            } catch (Exception e) {
                // Không in stack trace nếu socket đã bị đóng chủ động
                if (socket != null && !socket.isClosed()) {
                    System.out.println("ERROR: Mất kết nối tới Server hoặc lỗi đọc dữ liệu mã hóa!");
                    e.printStackTrace();
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }


    private static void handleBroadcast(Response res) {
        System.out.println("[Server message]: " + res.getMessage() + " | Dữ liệu: " + res.getData());

        // Tự động làm mới UI, truyền luôn Broadcast Response sang để Controller tự quyết định làm gì
        if (mainPageController.getInstance() != null) {
            mainPageController.getInstance().refreshData(res);
        }
    }
}
