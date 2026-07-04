package shared.models;

public class Session {

    private int id;
    private int userId;
    private String token;
    private String expiresAt;

    public Session(){}

    public Session(int id, int userId, String token, String expiresAt) {
        this.id = id;
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getToken() { return token; }
    public String getExpiresAt() { return expiresAt; }

}
