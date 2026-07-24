package shared.models;

public record SharedProfile(
        String username,
        String displayName,
        String bio,
        String avatarUrl,
        String bannerUrl,
        String createdAt,
        String location,
        String website,
        String birthDate,
        boolean professional) {
}
