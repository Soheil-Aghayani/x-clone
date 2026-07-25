package client.profile;

import client.UserSession;
import client.timeline.PostStore;
import shared.models.User;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AccountDirectory {
    private AccountDirectory() {}

    public static AccountProfile find(String username) {
        if (username == null || username.isBlank()) return current();
        String key = normalizeUsername(username);

        // Always prefer the live session user for the signed-in account.
        User active = UserSession.getInstance().getCurrentUser();
        if (active != null && key.equals(normalizeUsername(active.getUsername()))) {
            return fromUser(active);
        }

        // Real accounts that logged in / edited profile on this machine.
        PostStore.UserProfile stored = PostStore.getInstance().findProfile(key);
        if (stored != null) return fromStored(key, stored);

        // At least show a name from posts if they have any.
        String authorName = PostStore.getInstance().findAuthorDisplayName(key);
        if (authorName != null) {
            return realAccount(authorName, key, "No bio available", "", "", null, null);
        }

        return fallback(key);
    }

    public static AccountProfile current() {
        User user = UserSession.getInstance().getCurrentUser();
        return user == null ? fallback("user") : fromUser(user);
    }

    public static List<AccountProfile> all() {
        Map<String, AccountProfile> byUsername = new LinkedHashMap<>();
        for (String username : PostStore.getInstance().knownUsernames()) {
            byUsername.put(username, find(username));
        }
        User active = UserSession.getInstance().getCurrentUser();
        if (active != null) {
            byUsername.put(normalizeUsername(active.getUsername()), fromUser(active));
        }
        return new ArrayList<>(byUsername.values());
    }

    public static List<AccountProfile> search(String query) {
        if (query == null || query.isBlank()) return all();
        String normalized = query.toLowerCase(Locale.ROOT);
        return all().stream().filter(profile -> profile.displayName().toLowerCase(Locale.ROOT).contains(normalized)
                || profile.username().toLowerCase(Locale.ROOT).contains(normalized)
                || profile.bio().toLowerCase(Locale.ROOT).contains(normalized)).toList();
    }

    private static AccountProfile fromUser(User user) {
        String displayName = user.getDisplayName() == null || user.getDisplayName().isBlank()
                ? user.getUsername() : user.getDisplayName();
        return realAccount(displayName, user.getUsername(),
                user.getBio() == null || user.getBio().isBlank() ? "No bio available" : user.getBio(),
                user.getLocation() == null ? "" : user.getLocation(),
                user.getWebsite() == null ? "" : user.getWebsite(),
                blankToNull(user.getAvatarUrl()), blankToNull(user.getBannerUrl()),
                joinedLabel(user.getCreatedAt()));
    }

    private static AccountProfile fromStored(String usernameKey, PostStore.UserProfile stored) {
        String displayName = stored.displayName() == null || stored.displayName().isBlank()
                ? usernameKey : stored.displayName();
        String bio = stored.bio() == null || stored.bio().isBlank() ? "No bio available" : stored.bio();
        return realAccount(displayName, usernameKey, bio,
                stored.location() == null ? "" : stored.location(),
                stored.website() == null ? "" : stored.website(),
                blankToNull(stored.avatarUrl()), blankToNull(stored.bannerUrl()),
                "Joined 2026");
    }

    private static AccountProfile realAccount(String displayName, String username, String bio,
                                              String location, String website,
                                              String avatar, String banner) {
        return realAccount(displayName, username, bio, location, website, avatar, banner, "Joined 2026");
    }

    private static AccountProfile realAccount(String displayName, String username, String bio,
                                              String location, String website,
                                              String avatar, String banner, String joined) {
        PostStore store = PostStore.getInstance();
        return new AccountProfile(displayName, username, bio, location, website, joined,
                Integer.toString(store.getFollowingCount(username)),
                Integer.toString(store.getFollowerCount(username)),
                false, false, avatar, banner);
    }

    private static AccountProfile fallback(String username) {
        String clean = normalizeUsername(username);
        return realAccount(clean, clean, "No bio available", "", "", null, null);
    }

    private static String joinedLabel(String createdAt) {
        try {
            LocalDate date = LocalDate.parse(createdAt);
            return "Joined " + date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
        } catch (DateTimeParseException | NullPointerException ignored) {
            return "Joined 2026";
        }
    }

    private static String normalizeUsername(String username) {
        if (username == null) return "";
        String clean = username.trim();
        if (clean.startsWith("@")) clean = clean.substring(1);
        return clean.toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
