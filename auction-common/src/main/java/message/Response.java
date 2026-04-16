package message;

import java.io.Serializable; //gui du lieu qua Socket

public class Response implements Serializable {
    private String status;
    private String data;
    private String message;

    public void responeAction(String status, String data, String message) {
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
    public String getData(){
        return data;
    }
    public void setData(String data){
        this.data=data;
    }
    public void setMessage(String message){
        this.message=message;
    }
    public String getMessage(){
     return message;
    }
}