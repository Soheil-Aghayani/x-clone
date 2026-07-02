package shared.models;

import java.util.List;

public class Tweet {

    private int id;
    private int authorId;
    private String content;
    private Integer replyToId;
    private Integer retweetToId;
    private String createdAt;

    private List<Media> media;
    private List<Hashtag> hashtags;
    private int likesCount;
    private boolean likedByCurrentUser;

    public Tweet() {}

    public Tweet(int id, int authorId, String content, Integer replyToId, Integer retweetToId, String createdAt) {
        this.id = id;
        this.authorId = authorId;
        this.content = content;
        this.replyToId = replyToId;
        this.retweetToId = retweetToId;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public int getAuthorId() { return authorId; }
    public String getContent() { return content; }
    public Integer getReplyToId() { return replyToId; }
    public Integer getRetweetToId() { return retweetToId; }
    public String getCreatedAt() { return createdAt; }

    public List<Media> getMedia() { return media; }
    public List<Hashtag> getHashtags()  { return hashtags; }
    public int getLikesCount() { return likesCount; }
    public boolean isLikedByCurrentUser() { return likedByCurrentUser; }

    public void setMedia(List<Media> media) { this.media = media; }
    public void setHashtags(List<Hashtag> hashtags) { this.hashtags = hashtags; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }
    public void setLikedByCurrentUser(boolean likedByCurrentUser) { this.likedByCurrentUser = likedByCurrentUser; }

}
