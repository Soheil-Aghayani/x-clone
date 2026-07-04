package shared.models;

public class User {

    private int id;
    private String username;
    private String email;
    private String displayName;
    private String bio;
    private String avatarUrl;
    private String bannerUrl;
    private String createdAt;

    public User(){}

    public User(int id, String username, String email, String displayName, String bio, String avatarUrl, String bannerUrl, String createdAt){
        this.id = id;
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
        this.bannerUrl = bannerUrl;
        this.createdAt = createdAt;

    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getBio() { return bio; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getBannerUrl() { return bannerUrl; }
    public String getCreatedAt() { return createdAt; }

    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setBio(String bio) { this.bio = bio; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }

}
