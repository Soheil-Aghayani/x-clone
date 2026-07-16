package client;

public class UserSession {
    private static UserSession instance;

    private String username;
    private String displayName;
    private String token;

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
    public void startSession(String username, String displayName, String token) {
        this.username = username;
        this.displayName = displayName;
        this.token = token;
        System.out.println("Session Activated: Welcome @" + username + " (" + displayName + ")");
    }

    /**
     * Clears all reference pointers during logout cycles to secure context.
     */
    public void clearSession() {
        System.out.println("Session Terminated: @" + this.username + " logged out safely.");
        this.username = null;
        this.displayName = null;
        this.token = null;
    }

    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getToken() { return token; }
}

