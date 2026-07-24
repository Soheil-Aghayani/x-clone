package client;
import shared.models.User;
import shared.models.Session;
import client.timeline.PostStore;
import java.util.HashSet;
import java.util.Set;

public class UserSession {
    private static UserSession instance;

    private User currentUser;
    private Session currentSession;
    private final Set<String> followedUsernames = new HashSet<>();
    private String chatPasscode;
    private boolean chatUnlocked = false;

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
        // Overlay any local profile edits, then publish into the shared directory
        // so other accounts on this machine can see bio / avatar / follow graph.
        PostStore.getInstance().applyProfile(user);
        if (user != null) PostStore.getInstance().ensureProfile(user);
        this.currentUser = user;
        this.currentSession = session;
        if (user != null) followedUsernames.addAll(PostStore.getInstance().getFollowing(user.getUsername()));
        if (user != null && session != null) {
            System.out.println("Session Activated: Welcome @" + user.getUsername() + " (" + user.getDisplayName() + ")");
            System.out.println("Token Secured in UI Context: [" + session.getToken() + "]");
        }
    }

    /**
     * Purges all reference pointers upon logout to secure application state.
     */
    public void clearSession() {
        if (this.currentUser != null) {
            System.out.println("Session Terminated: @" + this.currentUser.getUsername() + " logged out safely.");
        }
        this.currentUser = null;
        this.currentSession = null;
        this.followedUsernames.clear();
        this.chatPasscode = null;
        this.chatUnlocked = false;
    }

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
        return followedUsernames.contains(username.toLowerCase());
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
