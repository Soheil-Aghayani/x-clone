package shared.models;

public class Media {
    private int id;
    private int tweetId;
    private String url;

    public Media() {}

    public Media(int id, int tweetId, String url) {
        this.id = id;
        this.tweetId = tweetId;
        this.url = url;
    }

    public int getId() { return id; }
    public int getTweetId() { return tweetId; }
    public String getUrl() { return url; }
}