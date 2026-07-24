package shared.models;

import java.util.List;

public record SharedSocialState(
        List<SharedProfile> profiles,
        List<SharedPost> posts,
        List<SharedFollow> follows,
        List<SharedNotification> notifications) {
}
