package shared.models;

public class Follow {
    private int id;
    private int followerId;
    private int followingId;
    private String createdAt;

    public Follow() {}

    public Follow(int id, int followerId, int followingId, String createdAt) {
        this.id = id;
        this.followerId = followerId;
        this.followingId = followingId;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public int getFollowerId() { return followerId; }
    public int getFollowingId() { return followingId; }
    public String getCreatedAt() { return createdAt; }
}