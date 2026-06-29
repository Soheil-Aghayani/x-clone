package shared.protocol;

public class message {
    private String type;
    private String payload;

    public message(String type, String payload) {
        this.type = type;
        this.payload = payload;
    }

    public String getType() { return type; }
    public String getPayload() { return payload; }
}