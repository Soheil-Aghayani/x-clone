package shared.protocol;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Request {
    private String requestId;
    private RequestType type;
    private JsonElement payload;

    private Request(){}

    public Request(String requestId, RequestType type, JsonElement payload) {
        this.requestId = requestId;
        this.type = type;
        this.payload = payload;
    }

    public String getRequestId() { return requestId; }
    public RequestType getType() { return type; }
    public JsonElement getPayload() { return payload; }
}
