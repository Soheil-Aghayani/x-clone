package shared.models;

public record SharedPost(
        long id,
        String authorName,
        String authorUsername,
        String content,
        String createdAt,
        String mediaUri,
        Long replyToId,
        Long quotedPostId,
        int likes,
        int replies,
        int reposts,
        int views,
        boolean likedByViewer,
        boolean repostedByViewer,
        boolean bookmarkedByViewer) {
}
