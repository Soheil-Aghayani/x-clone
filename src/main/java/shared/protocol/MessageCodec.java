package shared.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class MessageCodec {

    private static final Gson gson = new Gson();

    public MessageCodec() {}

    public static String encodeRequest(Request req) {
        return gson.toJson(req);
    }

    public static Request decodeRequest(String jsonString) {
        return gson.fromJson(jsonString, Request.class);
    }

    public static String encodeResponse(Response resp) {
        return gson.toJson(resp);
    }

    public static Response decodeResponse(String jsonString) {
        return gson.fromJson(jsonString, Response.class);
    }
}
