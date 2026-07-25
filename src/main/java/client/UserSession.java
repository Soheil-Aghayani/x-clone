package client;
import shared.models.User;
import shared.models.Session;
import client.timeline.PostStore;
import client.network.serverConnection;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import shared.protocol.Request;
import shared.protocol.RequestType;
import shared.protocol.Response;
import shared.protocol.StatusCode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class UserSession {
    private static UserSession instance;

    private User currentUser;
    private Session currentSession;
    private final Set<String> followedUsernames = new HashSet<>();
    private String chatPasscode;
    private boolean chatUnlocked = false;
    private final Gson gson = new Gson();

    private UserSession() {}

    /**
     * Globally retrieves the active session context instance channel.
     */
    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    /**
     * Initializes user variables upon successful login or registration.
     */
    public void startSession(User user, Session session) {
        followedUsernames.clear();
        chatPasscode = null;
        chatUnlocked = false;
        this.currentUser = user;
        this.currentSession = session;
        if (user != null) followedUsernames.addAll(PostStore.getInstance().getFollowing(user.getUsername()));
        persist();
        if (user != null && session != null) {
            System.out.println("Session Activated: Welcome @" + user.getUsername() + " (" + user.getDisplayName() + ")");
        }
    }

    /**
     * Purges all reference pointers upon logout to secure application state.
     */
    public void clearSession() {
        boolean hadPersistedSession = this.currentSession != null;
        if (this.currentUser != null) {
            System.out.println("Session Terminated: @" + this.currentUser.getUsername() + " logged out safely.");
        }
        PostStore.getInstance().clearSharedState();
        this.currentUser = null;
        this.currentSession = null;
        this.followedUsernames.clear();
        this.chatPasscode = null;
        this.chatUnlocked = false;
        if (hadPersistedSession) {
            try {
                Files.deleteIfExists(sessionFile());
            } catch (Exception exception) {
                System.err.println("Could not remove saved session: " + exception.getMessage());
            }
        }
    }

    public synchronized boolean hasSavedSession() {
        return Files.isRegularFile(sessionFile());
    }

    public synchronized boolean restoreSavedSession() {
        if (!hasSavedSession()) return false;
        try {
            SavedSession saved = gson.fromJson(Files.readString(sessionFile()), SavedSession.class);
            if (saved == null || saved.session == null || saved.session.getToken() == null) {
                clearSession();
                return false;
            }
            JsonObject payload = new JsonObject();
            payload.addProperty("token", saved.session.getToken());
            Response response = new serverConnection().sendMessage(new Request(
                    UUID.randomUUID().toString(), RequestType.VALIDATE_SESSION, payload));
            if (response.getStatus() != StatusCode.OK || response.getPayload() == null) {
                clearSession();
                return false;
            }
            JsonObject result = response.getPayload().getAsJsonObject();
            startSession(
                    gson.fromJson(result.get("user"), User.class),
                    gson.fromJson(result.get("session"), Session.class));
            return true;
        } catch (Exception exception) {
            System.err.println("Could not restore saved session: " + exception.getMessage());
            clearSession();
            return false;
        }
    }

    public synchronized void logout() {
        String token = getToken();
        if (token != null && !token.isBlank()) {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("token", token);
                new serverConnection().sendMessage(new Request(
                        UUID.randomUUID().toString(), RequestType.LOGOUT, payload));
            } catch (Exception exception) {
                System.err.println("Could not revoke remote session: " + exception.getMessage());
            }
        }
        clearSession();
    }

    private void persist() {
        if (currentUser == null || currentSession == null) return;
        try {
            Path file = sessionFile();
            Files.createDirectories(file.getParent());
            Files.writeString(file, gson.toJson(new SavedSession(currentUser, currentSession)));
        } catch (Exception exception) {
            System.err.println("Could not save session: " + exception.getMessage());
        }
    }

    private Path sessionFile() {
        String custom = System.getProperty("xclone.data.dir");
        Path directory = custom == null || custom.isBlank()
                ? Path.of(System.getProperty("user.home"), ".x-clone")
                : Path.of(custom);
        return directory.resolve("session.json");
    }

    private record SavedSession(User user, Session session) {}

    // Helper utilities to cleanly request fields across FX controllers
    public String getUsername() {
        return currentUser != null ? currentUser.getUsername() : null;
    }

    public String getDisplayName() {
        return currentUser != null ? currentUser.getDisplayName() : null;
    }

    public String getToken() {
        return currentSession != null ? currentSession.getToken() : null;
    }

    public User getCurrentUser() { return currentUser; }
    public Session getCurrentSession() { return currentSession; }

    public boolean toggleFollow(String username) {
        if (currentUser == null || username == null || username.isBlank()) return false;
        String normalized = username.toLowerCase();
        boolean following = PostStore.getInstance().toggleFollow(currentUser, normalized);
        if (following) followedUsernames.add(normalized);
        else followedUsernames.remove(normalized);
        return following;
    }

    public boolean isFollowing(String username) {
        return currentUser != null
                && PostStore.getInstance().isFollowing(currentUser.getUsername(), username);
    }

    public int getFollowingCount() {
        return currentUser == null ? 0 : PostStore.getInstance().getFollowingCount(currentUser.getUsername());
    }

    public boolean hasChatPasscode() {
        return chatPasscode != null || (getUsername() != null && client.chat.ChatStore.getInstance().hasPasscode(getUsername()));
    }

    public void setChatPasscode(String chatPasscode) {
        this.chatPasscode = chatPasscode;
    }

    public boolean isChatUnlocked() {
        return chatUnlocked;
    }

    public void setChatUnlocked(boolean chatUnlocked) {
        this.chatUnlocked = chatUnlocked;
    }
}
