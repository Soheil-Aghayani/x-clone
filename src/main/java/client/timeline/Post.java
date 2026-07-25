package client.timeline;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Post {
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("(?<![\\p{L}\\p{N}_])#[\\p{L}\\p{N}_]+", Pattern.UNICODE_CHARACTER_CLASS);

    private final long id;
    private String authorName;
    private final String authorUsername;
    private String content;
    private Instant createdAt;
    private final String mediaUri;
    private final Long replyToId;
    private final Long quotedPostId;
    private int likes;
    private int replies;
    private int retweets;
    private int views;
    private boolean pinned;
    private boolean demo;
    private PollData poll;
    private final Set<String> likedBy = new HashSet<>();
    private final Set<String> repliedBy = new HashSet<>();
    private final Set<String> retweetedBy = new HashSet<>();
    private final Set<String> bookmarkedBy = new HashSet<>();

    public Post(long id, String authorName, String authorUsername, String content, Instant createdAt) {
        this(id, authorName, authorUsername, content, createdAt, null);
    }

    public Post(long id, String authorName, String authorUsername, String content, Instant createdAt, String mediaUri) {
        this(id, authorName, authorUsername, content, createdAt, mediaUri, null, null);
    }

    public Post(long id, String authorName, String authorUsername, String content, Instant createdAt,
                String mediaUri, Long replyToId, Long quotedPostId) {
        this.id = id;
        this.authorName = authorName;
        this.authorUsername = authorUsername;
        this.content = content;
        this.createdAt = createdAt;
        this.mediaUri = mediaUri == null || mediaUri.isBlank() ? null : mediaUri.trim();
        this.replyToId = replyToId;
        this.quotedPostId = quotedPostId;
    }

    public long getId() { return id; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorUsername() { return authorUsername; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content == null ? "" : content; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) {
        if (createdAt != null) this.createdAt = createdAt;
    }
    public String getMediaUri() { return mediaUri; }
    public Long getReplyToId() { return replyToId; }
    public Long getQuotedPostId() { return quotedPostId; }
    public int getLikes() { return likes; }
    public int getReplies() { return replies; }
    public int getRetweets() { return retweets; }
    public int getViews() { return views; }
    public boolean isPinned() { return pinned; }
    public PollData getPoll() { return poll; }
    public void setPoll(PollData poll) { this.poll = poll; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
    public boolean isDemo() { return demo; }
    public void setDemo(boolean demo) { this.demo = demo; }
    public boolean isLiked() { return likedBy.contains(activeUsername()); }
    public boolean isReplied() { return repliedBy.contains(activeUsername()); }
    public boolean isRetweeted() { return retweetedBy.contains(activeUsername()); }
    public boolean isBookmarked() { return bookmarkedBy.contains(activeUsername()); }

    public void toggleLike() {
        String actor = activeUsername();
        if (likedBy.remove(actor)) likes = Math.max(0, likes - 1);
        else { likedBy.add(actor); likes++; }
    }

    public void toggleReply() {
        String actor = activeUsername();
        if (repliedBy.remove(actor)) replies = Math.max(0, replies - 1);
        else { repliedBy.add(actor); replies++; }
    }

    public void addReply() {
        repliedBy.add(activeUsername());
        replies++;
    }

    public void toggleRetweet() {
        String actor = activeUsername();
        if (retweetedBy.remove(actor)) retweets = Math.max(0, retweets - 1);
        else { retweetedBy.add(actor); retweets++; }
    }

    public void addQuote() {
        retweets++;
    }

    public void toggleBookmark() {
        String actor = activeUsername();
        if (!bookmarkedBy.remove(actor)) bookmarkedBy.add(actor);
    }

    public void addView() { views++; }

    public Set<String> getLikedBy() { return Set.copyOf(likedBy); }
    public Set<String> getRepliedBy() { return Set.copyOf(repliedBy); }
    public Set<String> getRetweetedBy() { return Set.copyOf(retweetedBy); }
    public Set<String> getBookmarkedBy() { return Set.copyOf(bookmarkedBy); }

    public void restoreState(int likes, int replies, int retweets, int views, boolean pinned,
                             Set<String> likedBy, Set<String> repliedBy,
                             Set<String> retweetedBy, Set<String> bookmarkedBy) {
        this.likes = Math.max(0, likes);
        this.replies = Math.max(0, replies);
        this.retweets = Math.max(0, retweets);
        this.views = Math.max(0, views);
        this.pinned = pinned;
        this.likedBy.clear();
        this.repliedBy.clear();
        this.retweetedBy.clear();
        this.bookmarkedBy.clear();
        if (likedBy != null) likedBy.forEach(value -> this.likedBy.add(normalize(value)));
        if (repliedBy != null) repliedBy.forEach(value -> this.repliedBy.add(normalize(value)));
        if (retweetedBy != null) retweetedBy.forEach(value -> this.retweetedBy.add(normalize(value)));
        if (bookmarkedBy != null) bookmarkedBy.forEach(value -> this.bookmarkedBy.add(normalize(value)));
    }

    private static String activeUsername() {
        String username = client.UserSession.getInstance().getUsername();
        return normalize(username == null ? "anonymous" : username);
    }

    private static String normalize(String username) {
        return username == null ? "" : username.toLowerCase(Locale.ROOT);
    }

    public List<String> getHashtags() {
        List<String> hashtags = new ArrayList<>();
        Matcher matcher = HASHTAG_PATTERN.matcher(content);
        while (matcher.find()) {
            hashtags.add(matcher.group());
        }
        return hashtags;
    }

    public boolean hasHashtag(String hashtag) {
        return getHashtags().stream().anyMatch(tag -> tag.equalsIgnoreCase(hashtag));
    }
}
