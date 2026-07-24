package client.chat;

import client.profile.AccountDirectory;
import client.timeline.PostStore;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import shared.models.User;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcReplyPolicyTest {
    @TempDir static Path temporaryDirectory;

    @BeforeAll
    static void startJavaFxAndIsolateState() throws Exception {
        System.setProperty("xclone.data.dir", temporaryDirectory.toString());
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertTrue(started.await(5, TimeUnit.SECONDS));
    }

    @Test
    void automaticRepliesAreAllowedForSeededNpcsButRejectedForRealUsers() {
        User potato = new User(1, "potato", "potato@example.test", "Soheil", "",
                null, null, "2026-07-17");
        User agseyl = new User(2, "agseyl", "agseyl@example.test", "Fatemeh", "",
                null, null, "2026-07-17");
        PostStore.getInstance().saveProfile(potato);
        PostStore.getInstance().saveProfile(agseyl);

        assertTrue(AccountDirectory.isNpc("arjunoss"));
        assertFalse(AccountDirectory.isNpc("agseyl"));

        ChatStore store = ChatStore.getInstance();
        ChatStore.Conversation realConversation = store.startDirect("potato", "agseyl");
        assertFalse(store.sendAutomaticReply(realConversation, "agseyl", "simulated"));
        assertEquals(0, realConversation.messages().size());

        ChatStore.Conversation npcConversation = store.startDirect("potato", "arjunoss");
        assertTrue(store.sendAutomaticReply(npcConversation, "arjunoss", "simulated"));
        assertEquals(1, npcConversation.messages().size());
        assertTrue(npcConversation.unreadFor("potato"));
        store.markAllRead("potato");
        assertFalse(npcConversation.unreadFor("potato"));

        ChatStore.ChatPreferences preferences = new ChatStore.ChatPreferences(
                "Everyone", true, "60 days", true);
        store.savePreferences("potato", preferences);
        assertEquals(preferences, store.preferences("potato"));
    }
}
