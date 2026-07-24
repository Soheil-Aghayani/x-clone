package shared.models;

public record SharedNotification(
        long id,
        String recipientUsername,
        String actorName,
        String actorUsername,
        String type,
        Long postId,
        String excerpt,
        String createdAt,
        boolean read) {
}
