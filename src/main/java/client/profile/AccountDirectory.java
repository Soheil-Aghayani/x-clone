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
    private static final Map<String, AccountProfile> KNOWN = Map.ofEntries(
            Map.entry("elonmusk", new AccountProfile(
                    "Elon Musk", "elonmusk", "Starmind", "", "spacex.com/spaceai/starmind",
                    "Joined June 2009", "1,370", "240.9M", true, false,
                    "/images/accounts/elon-avatar.jpg", "/images/accounts/elon-banner.jpg")),
            Map.entry("elonmuskpda", new AccountProfile(
                    "Not Elon Musk", "ElonMuskPDA",
                    "Who controls the memes, controls the Universe · Dogecoin · Aliens · Elon Musk Parody Account",
                    "", "", "Joined February 2011", "54", "2.3M", true, true,
                    "/images/accounts/not-elon-avatar.jpg", "/images/accounts/not-elon-banner.jpg")),
            Map.entry("openjfx", new AccountProfile(
                    "JavaFX", "openjfx", "Open-source client application platform for desktop, mobile and embedded systems.",
                    "", "openjfx.io", "Joined May 2011", "128", "91.4K", true, false,
                    "/images/accounts/arjun-mehta.png", null)),
            Map.entry("designdaily", new AccountProfile(
                    "Design Daily", "designdaily", "Daily ideas about interface design, typography, and product craft.",
                    "", "", "Joined September 2018", "420", "36.8K", false, false,
                    "/images/accounts/marcus-cole.png", null)),
            Map.entry("laylacodes", new AccountProfile(
                    "Layla Rahimi", "laylacodes", "Java engineer building calm, useful software. فارسی / English.",
                    "Berlin", "layla.dev", "Joined April 2018", "612", "18.7K", true, false,
                    "/images/accounts/layla-rahimi.png", null)),
            Map.entry("marcusux", new AccountProfile(
                    "Marcus Cole", "marcusux", "Product designer. Accessibility, systems thinking, and tiny details.",
                    "London", "marcuscole.design", "Joined January 2016", "894", "42.1K", false, false,
                    "/images/accounts/marcus-cole.png", null)),
            Map.entry("minapixels", new AccountProfile(
                    "Mina Park", "minapixels", "Indie game developer making small worlds with big feelings.",
                    "Seoul", "minapixels.games", "Joined August 2019", "337", "27.4K", false, false,
                    "/images/accounts/mina-park.png", null)),
            Map.entry("danielscience", new AccountProfile(
                    "Daniel Hart", "danielscience", "Science journalist. Space, climate, and the stories inside the data.",
                    "Boston", "danielhart.media", "Joined March 2012", "1,104", "118K", true, false,
                    "/images/accounts/daniel-hart.png", null)),
            Map.entry("sofiashots", new AccountProfile(
                    "Sofía Reyes", "sofiashots", "Sports photographer chasing decisive moments and good light.",
                    "Madrid", "sofia.photos", "Joined July 2015", "728", "64.2K", false, false,
                    "/images/accounts/sofia-reyes.png", null)),
            Map.entry("arjunoss", new AccountProfile(
                    "Arjun Mehta", "arjunoss", "Open-source maintainer. APIs, release engineering, and kind reviews.",
                    "Bengaluru", "github.com/arjunoss", "Joined November 2017", "503", "31.8K", true, false,
                    "/images/accounts/arjun-mehta.png", null))
    );

    private AccountDirectory() {}

    /**
     * Only built-in demo identities may simulate replies. A locally stored
     * profile wins over the demo list so a real registered user is never
     * impersonated even if their handle matches a seeded account.
     */
    public static boolean isNpc(String username) {
        String key = normalizeUsername(username);
        if (key.isBlank()) return false;
        User active = UserSession.getInstance().getCurrentUser();
        if (active != null && key.equals(normalizeUsername(active.getUsername()))) return false;
        if (PostStore.getInstance().findProfile(key) != null) return false;
        return KNOWN.containsKey(key);
    }

    public static AccountProfile find(String username) {
        if (username == null || username.isBlank()) return current();
        String key = normalizeUsername(username);

        // Always prefer the live session user for the signed-in account.
        User active = UserSession.getInstance().getCurrentUser();
        if (active != null && key.equals(normalizeUsername(active.getUsername()))) {
            return fromUser(active);
        }

        // Seeded demo accounts keep their canned stats/images.
        AccountProfile known = KNOWN.get(key);
        if (known != null) return known;

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
        for (AccountProfile profile : KNOWN.values()) {
            byUsername.put(normalizeUsername(profile.username()), profile);
        }
        for (String username : PostStore.getInstance().knownUsernames()) {
            if (KNOWN.containsKey(username)) continue;
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
