package shared.protocol;

import com.google.gson.JsonElement;

public class Response {

    private String requestId;
    private StatusCode status;
    private String message;
    private JsonElement payload;

    public Response(){}

    public static Response ok(String requestId, JsonElement payload){
        Response response = new Response();
        response.requestId = requestId;
        response.status = StatusCode.OK;
        response.payload = payload;
        return response;
    }

    public static Response error(String requestId, StatusCode status, String message){
        Response response = new Response();
        response.requestId = requestId;
        response.status = status;
        response.message = message;
        return response;
    }

    public String getRequestId() { return requestId; }
    public StatusCode getStatus() { return status; }
    public String getMessage() { return message; }
    public JsonElement getPayload() { return payload; }
}
