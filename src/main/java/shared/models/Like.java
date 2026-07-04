package shared.models;

public class Like {
    private int id;
    private int userId;
    private int tweetId;

    public Like() {}

    public Like(int id, int userId, int tweetId) {
        this.id = id;
        this.userId = userId;
        this.tweetId = tweetId;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public int getTweetId() { return tweetId; }
}