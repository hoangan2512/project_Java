package message;

import java.io.Serializable; //gui du lieu qua Socket

public class Response implements Serializable {
    private String status;
    private Object data;
    private String message;
    public Response() {} //để UserController không bị lỗi
    public Response(String status, Object data, String message) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status){
    this.status=status;
    }
    public Object getData(){
        return data;
    }
    public void setData(Object data){
        this.data=data;
    }
    public void setMessage(String message){
        this.message=message;
    }
    public String getMessage(){
     return message;
    }
    }