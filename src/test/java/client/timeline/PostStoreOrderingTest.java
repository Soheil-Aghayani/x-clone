package client.timeline;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostStoreOrderingTest {

    @Test
    void newestPostComesFirstEvenWhenAnOlderPostHasMoreEngagement() {
        Instant now = Instant.parse("2026-07-25T00:00:00Z");
        Post olderPopularPost = new Post(1, "Popular", "popular", "Older", now.minusSeconds(60));
        olderPopularPost.restoreState(18_400, 2_400, 3_700, 2_400_000,
                false, Set.of(), Set.of(), Set.of(), Set.of());
        Post newPost = new Post(2, "New user", "newuser", "Just posted", now);

        List<Post> timeline = new ArrayList<>(List.of(olderPopularPost, newPost));
        timeline.sort(PostStore.NEWEST_FIRST);

        assertEquals(List.of(newPost, olderPopularPost), timeline);
    }

    @Test
    void largerIdBreaksTiesForPostsCreatedAtTheSameInstant() {
        Instant createdAt = Instant.parse("2026-07-25T00:00:00Z");
        Post first = new Post(10, "User", "user", "First", createdAt);
        Post second = new Post(11, "User", "user", "Second", createdAt);

        List<Post> timeline = new ArrayList<>(List.of(first, second));
        timeline.sort(PostStore.NEWEST_FIRST);

        assertEquals(List.of(second, first), timeline);
    }
}
