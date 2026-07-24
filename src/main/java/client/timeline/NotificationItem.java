package client.timeline;

import java.time.Instant;

public record NotificationItem(
        long id,
        String recipientUsername,
        String actorName,
        String actorUsername,
        Type type,
        Long postId,
        String excerpt,
        Instant createdAt,
        boolean read) {

    public enum Type { LIKE, REPOST, REPLY, FOLLOW, MENTION, SYSTEM }

    public NotificationItem withRead(boolean value) {
        return new NotificationItem(id, recipientUsername, actorName, actorUsername, type,
                postId, excerpt, createdAt, value);
    }
}
