package server.database;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcContentBankTest {
    @Test
    void providesDistinctSafeLengthContentForEveryPersonality() {
        assertEquals(10, NpcContentBank.PERSONALITIES.size());

        for (NpcContentBank.Personality personality : NpcContentBank.PERSONALITIES) {
            assertNotNull(NpcContentBank.class.getResource(personality.avatarUri()));
            assertNotNull(NpcContentBank.class.getResource(personality.bannerUri()));
            assertEquals(3, personality.mediaUris().stream().distinct().count());
            personality.mediaUris().forEach(uri ->
                    assertNotNull(NpcContentBank.class.getResource(uri), uri));

            Random random = new Random(personality.username().hashCode());
            Set<String> posts = new HashSet<>();
            for (int index = 0; index < 200; index++) {
                String post = NpcContentBank.post(personality, random);
                assertTrue(post.length() <= 280, post);
                posts.add(post);

                String reply = NpcContentBank.reply(personality, "another_demo", random);
                assertTrue(reply.startsWith("@another_demo "));
                assertTrue(reply.length() <= 280, reply);
            }
            assertTrue(posts.size() >= 50,
                    personality.username() + " should have a varied content bank");
        }
    }
}
