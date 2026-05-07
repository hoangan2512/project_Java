package message;

import model.ActionType;

import java.io.Serializable;

public class Request implements Serializable {
    // ĐỔI TỪ String SANG Object
    private Object payload;
    private ActionType action;

    public Request(Object payload, ActionType action) {
        this.payload = payload;
        this.action = action;
    }

    public ActionType getAction() {
        return action;
    }

    // ĐỔI KIỂU TRẢ VỀ TỪ String SANG Object
    public Object getPayload() {
        return payload;
    }
}