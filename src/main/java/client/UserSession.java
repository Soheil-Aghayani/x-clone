package client;
import shared.models.User;
import shared.models.Session;

public class UserSession {
    private static UserSession instance;

    private User currentUser;
    private Session currentSession;

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
        this.currentUser = user;
        this.currentSession = session;
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
}
