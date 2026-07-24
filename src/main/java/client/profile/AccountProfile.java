package client.profile;

public record AccountProfile(
        String displayName,
        String username,
        String bio,
        String location,
        String website,
        String joined,
        String following,
        String followers,
        boolean verified,
        boolean parody,
        String avatarResource,
        String bannerResource) {
}
