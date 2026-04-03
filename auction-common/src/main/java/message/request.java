package message;
import model.ActionType;
public class request {
    private String payload;
    private ActionType action;
    public request(String payload,ActionType action){
        this.payload=payload;
        this.action=action;
    }
public ActionType getAction(){
        return action;
}
public String getPayload(){
        return payload;
}



}
