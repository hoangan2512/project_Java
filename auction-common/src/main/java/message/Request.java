package message;
import model.ActionType;

import java.io.Serializable;

public class Request implements Serializable {
    private String payload;
    private ActionType action;
    public Request(String payload, ActionType action){
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
