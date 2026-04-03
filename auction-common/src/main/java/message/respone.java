package message;

public class respone {
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
    public void setData(){
        this.data=data;
    }
    public void setMessage(){
        this.message=message;
    }
    public String getMessage(){
     return message;
    }
}