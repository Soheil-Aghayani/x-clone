package client.chat;

import client.profile.AccountDirectory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public final class ChatStore {
    private static final Set<String> LEGACY_AUTOMATIC_REPLIES = Set.of(
            "Hey there! Thanks for reaching out. How's it going?",
            "I'm doing great, working on some new designs! How about you?",
            "This X desktop clone is looking really premium! JavaFX is surprisingly fast.",
            "Awesome! Glad to hear that.",
            "Hey! I'm a bit busy at the moment, but let's connect later. Talk soon!"
    );
    private static final ChatStore INSTANCE = new ChatStore();
    private final List<Conversation> conversations = new ArrayList<>();
    private final AtomicLong nextConversationId = new AtomicLong(1);
    private final AtomicLong nextMessageId = new AtomicLong(1);
    private final java.util.Map<String, String> userPasscodeHashes = new java.util.HashMap<>();
    private final Set<Long> pinnedConversations = new HashSet<>();
    private final Set<Long> mutedConversations = new HashSet<>();
    private final Map<String, ChatPreferences> preferencesByUser = new java.util.HashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;

    private ChatStore() {
        String custom = System.getProperty("xclone.data.dir");
        Path directory = custom == null || custom.isBlank()
                ? Path.of(System.getProperty("user.home"), ".x-clone") : Path.of(custom);
        file = directory.resolve("chat-state.json");
        load();
    }

    public static ChatStore getInstance() { return INSTANCE; }

    public synchronized Conversation startDirect(String first, String second) {
        Conversation existing = conversations.stream().filter(conversation -> conversation.participants.size() == 2
                && conversation.participants.stream().anyMatch(value -> value.equalsIgnoreCase(first))
                && conversation.participants.stream().anyMatch(value -> value.equalsIgnoreCase(second))).findFirst().orElse(null);
        if (existing != null) return existing;
        Conversation created = new Conversation(nextConversationId.getAndIncrement(), List.of(normalize(first), normalize(second)));
        conversations.add(created);
        save();
        return created;
    }

    public synchronized List<Conversation> forUser(String username) {
        String key = normalize(username);
        return conversations.stream().filter(conversation -> conversation.participants.contains(key))
                .sorted(Comparator.comparing(Conversation::lastActivity).reversed()).toList();
    }

    public synchronized Conversation get(long id) {
        return conversations.stream().filter(conversation -> conversation.id == id).findFirst().orElse(null);
    }

    public synchronized void send(Conversation conversation, String sender, String text) {
        if (conversation == null || text == null || text.isBlank()) return;
        conversation.messages.add(new ChatMessage(nextMessageId.getAndIncrement(), normalize(sender), text.trim(), Instant.now()));
        conversation.participants.stream().filter(participant -> !participant.equals(normalize(sender)))
                .forEach(conversation.unreadBy::add);
        save();
    }

    /**
     * Adds a simulated response only for one of the app's explicit demo NPCs.
     * Keeping this rule in the store prevents another UI from accidentally
     * sending messages on behalf of a real account.
     */
    public synchronized boolean sendAutomaticReply(Conversation conversation, String sender, String text) {
        if (!AccountDirectory.isNpc(sender)) return false;
        send(conversation, sender, text);
        return true;
    }

    public synchronized void markRead(Conversation conversation, String username) {
        if (conversation != null && conversation.unreadBy.remove(normalize(username))) save();
    }

    public synchronized void markAllRead(String username) {
        String key = normalize(username);
        boolean changed = false;
        for (Conversation conversation : conversations) {
            if (conversation.participants.contains(key)) {
                changed |= conversation.unreadBy.remove(key);
            }
        }
        if (changed) save();
    }

    public synchronized void deleteConversation(long id) {
        conversations.removeIf(conversation -> conversation.id == id);
        pinnedConversations.remove(id);
        mutedConversations.remove(id);
        save();
    }

    public synchronized void pinConversation(long id, boolean pin) {
        if (pin) pinnedConversations.add(id);
        else pinnedConversations.remove(id);
        save();
    }

    public synchronized boolean isPinned(long id) {
        return pinnedConversations.contains(id);
    }

    public synchronized void muteConversation(long id, boolean mute) {
        if (mute) mutedConversations.add(id);
        else mutedConversations.remove(id);
        save();
    }

    public synchronized boolean isMuted(long id) {
        return mutedConversations.contains(id);
    }

    public synchronized ChatPreferences preferences(String username) {
        return preferencesByUser.getOrDefault(normalize(username), ChatPreferences.defaults());
    }

    public synchronized void savePreferences(String username, ChatPreferences preferences) {
        if (username == null || preferences == null) return;
        preferencesByUser.put(normalize(username), preferences);
        save();
    }

    private void load() {
        if (!Files.isRegularFile(file)) return;
        try {
            StoredState state = gson.fromJson(Files.readString(file), StoredState.class);
            if (state == null) return;
            boolean removedImpersonatedReplies = false;
            nextConversationId.set(Math.max(1, state.nextConversationId));
            nextMessageId.set(Math.max(1, state.nextMessageId));
            if (state.userPasscodeHashes != null) {
                userPasscodeHashes.putAll(state.userPasscodeHashes);
            }
            if (state.pinnedConversations != null) {
                pinnedConversations.addAll(state.pinnedConversations);
            }
            if (state.mutedConversations != null) {
                mutedConversations.addAll(state.mutedConversations);
            }
            if (state.preferencesByUser != null) {
                preferencesByUser.putAll(state.preferencesByUser);
            }
            for (StoredConversation stored : state.conversations) {
                Conversation conversation = new Conversation(stored.id, stored.participants);
                conversation.unreadBy.addAll(stored.unreadBy);
                for (StoredMessage message : stored.messages) conversation.messages.add(new ChatMessage(
                        message.id, message.sender, message.text, Instant.parse(message.createdAt)));
                removedImpersonatedReplies |= conversation.messages.removeIf(message ->
                        LEGACY_AUTOMATIC_REPLIES.contains(message.text())
                                && !AccountDirectory.isNpc(message.sender()));
                conversations.add(conversation);
            }
            if (removedImpersonatedReplies) save();
        } catch (Exception exception) {
            System.err.println("Could not load chats: " + exception.getMessage());
        }
    }

    private synchronized void save() {
        try {
            Files.createDirectories(file.getParent());
            StoredState state = new StoredState();
            state.nextConversationId = nextConversationId.get();
            state.nextMessageId = nextMessageId.get();
            state.userPasscodeHashes = new java.util.HashMap<>(userPasscodeHashes);
            state.pinnedConversations = new java.util.ArrayList<>(pinnedConversations);
            state.mutedConversations = new java.util.ArrayList<>(mutedConversations);
            state.preferencesByUser = new java.util.HashMap<>(preferencesByUser);
            conversations.forEach(conversation -> state.conversations.add(new StoredConversation(conversation)));
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, gson.toJson(state));
            try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (IOException ignored) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException exception) {
            System.err.println("Could not save chats: " + exception.getMessage());
        }
    }

    private static String normalize(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT); }

    public synchronized boolean hasPasscode(String username) {
        return userPasscodeHashes.containsKey(normalize(username));
    }

    public synchronized void setPasscode(String username, String passcode) {
        if (passcode == null || passcode.length() != 4) return;
        String hash = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults().hashToString(12, passcode.toCharArray());
        userPasscodeHashes.put(normalize(username), hash);
        save();
    }

    public synchronized boolean verifyPasscode(String username, String passcode) {
        if (passcode == null) return false;
        String hash = userPasscodeHashes.get(normalize(username));
        if (hash == null) return false;
        return at.favre.lib.crypto.bcrypt.BCrypt.verifyer().verify(passcode.toCharArray(), hash).verified;
    }

    public static final class Conversation {
        private final long id;
        private final List<String> participants;
        private final List<ChatMessage> messages = new ArrayList<>();
        private final Set<String> unreadBy = new HashSet<>();
        private Conversation(long id, List<String> participants) { this.id = id; this.participants = new ArrayList<>(participants); }
        public long id() { return id; }
        public List<String> participants() { return List.copyOf(participants); }
        public List<ChatMessage> messages() { return List.copyOf(messages); }
        public boolean unreadFor(String username) { return unreadBy.contains(normalize(username)); }
        public Instant lastActivity() { return messages.isEmpty() ? Instant.EPOCH : messages.getLast().createdAt(); }
        public String otherParticipant(String username) { return participants.stream().filter(value -> !value.equals(normalize(username))).findFirst().orElse(username); }
    }

    public record ChatMessage(long id, String sender, String text, Instant createdAt) {}

    public record ChatPreferences(
            String messageRequests,
            boolean subscriberMessages,
            String autoDeleteMedia,
            boolean debugLogs) {
        public static ChatPreferences defaults() {
            return new ChatPreferences("Checkmark users", false, "30 days", false);
        }
    }

    private static final class StoredState {
        long nextConversationId = 1; long nextMessageId = 1;
        List<StoredConversation> conversations = new ArrayList<>();
        java.util.Map<String, String> userPasscodeHashes = new java.util.HashMap<>();
        List<Long> pinnedConversations = new ArrayList<>();
        List<Long> mutedConversations = new ArrayList<>();
        Map<String, ChatPreferences> preferencesByUser = new java.util.HashMap<>();
    }
    private static final class StoredConversation {
        long id; List<String> participants; List<StoredMessage> messages = new ArrayList<>(); List<String> unreadBy;
        StoredConversation(Conversation conversation) {
            id = conversation.id; participants = new ArrayList<>(conversation.participants); unreadBy = new ArrayList<>(conversation.unreadBy);
            conversation.messages.forEach(message -> messages.add(new StoredMessage(message)));
        }
    }
    private static final class StoredMessage {
        long id; String sender; String text; String createdAt;
        StoredMessage(ChatMessage message) { id = message.id(); sender = message.sender(); text = message.text(); createdAt = message.createdAt().toString(); }
    }
}
