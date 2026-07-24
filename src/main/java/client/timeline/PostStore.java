package client.timeline;

import client.UserSession;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.util.Duration;
import shared.models.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PostStore {
    private static final PostStore INSTANCE = new PostStore();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d").withZone(ZoneId.systemDefault());
    private static final Pattern MENTION_PATTERN = Pattern.compile("(?<![\\p{L}\\p{N}_])@([\\p{L}\\p{N}_]+)", Pattern.UNICODE_CHARACTER_CLASS);

    private final List<Post> posts = new ArrayList<>();
    private final List<NotificationItem> notifications = new ArrayList<>();
    private final Map<String, Set<String>> following = new HashMap<>();
    private final Map<String, Set<Long>> hiddenPosts = new HashMap<>();
    private final Map<String, Set<String>> mutedAccounts = new HashMap<>();
    private final Map<String, DraftData> drafts = new HashMap<>();
    private final Map<String, ProfileData> profiles = new HashMap<>();
    private final Set<String> recordedViews = new HashSet<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private final AtomicLong nextNotificationId = new AtomicLong(1);
    private final ReadOnlyLongWrapper clock = new ReadOnlyLongWrapper(Instant.now().getEpochSecond());
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path stateFile;
    private String requestedHashtag;
    private boolean composerFocusRequested;
    private String requestedView;
    private String requestedSearch;
    private String requestedProfileUsername;
    private Long requestedPostId;

    private PostStore() {
        String customDirectory = System.getProperty("xclone.data.dir");
        Path dataDirectory = customDirectory == null || customDirectory.isBlank()
                ? Path.of(System.getProperty("user.home"), ".x-clone")
                : Path.of(customDirectory);
        stateFile = dataDirectory.resolve("social-state.json");
        load();
        seed();

        Timeline clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event ->
                clock.set(Instant.now().getEpochSecond())));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    public static PostStore getInstance() { return INSTANCE; }

    private void seed() {
        Instant now = Instant.now();
        boolean changed = false;

        changed |= addSeedPost("JavaFX", "openjfx",
                "Developers ship a faster generation of desktop apps using JavaFX! Check out the new performance benchmark results. #JavaFX #Technology", now.minus(1, ChronoUnit.HOURS), 18400, 190, 310, 124000);
        changed |= addSeedPost("Arjun Mehta", "arjunoss",
                "Open-source communities announce new releases this week, introducing advanced privacy controls and layout engines. #OpenSource #Technology", now.minus(2, ChronoUnit.HOURS), 840, 44, 98, 48000);
        changed |= addSeedPost("Layla Rahimi", "laylacodes",
                "Local teams build privacy-first social tools to give users full ownership of their data. Proud to see these projects ship! #Design #Technology", now.minus(3, ChronoUnit.HOURS), 950, 72, 180, 62000);
        changed |= addSeedPost("Sofía Reyes", "sofiashots",
                "Tonight’s biggest matches and live conversations: what a historic night for international sports! ⚽🏆 #Sports", now.minus(4, ChronoUnit.HOURS), 3100, 240, 520, 180000);
        changed |= addSeedPost("Daniel Hart", "danielscience",
                "The championship race enters its final week with three teams neck and neck. Who are you rooting for? #Sports", now.minus(5, ChronoUnit.HOURS), 1500, 110, 290, 95000);
        changed |= addSeedPost("Marcus Cole", "marcusux",
                "The Brain Teaser Shirt Puzzle Divides Opinions on Hole Count. Is it 2, 4, or 8? Let's settle this design riddle! #Design #Entertainment", now.minus(6, ChronoUnit.HOURS), 2800, 490, 810, 240000);
        changed |= addSeedPost("Mina Park", "minapixels",
                "New trailers and upcoming releases this season: here is my breakdown of the most anticipated game engines and designs. #Gaming #Entertainment", now.minus(8, ChronoUnit.HOURS), 1900, 130, 310, 110000);
        changed |= addSeedPost("Not Elon Musk", "ElonMuskPDA",
                "Developer Unboxes Laptop, Deletes Only Browser, Asks for Help. Truly the peak of modern engineering! 😂 #News", now.minus(10, ChronoUnit.HOURS), 5600, 420, 890, 420000);

        changed |= addSeedPost("Layla Rahimi", "laylacodes",
                "Tiny win: the keyboard-only path through our settings screen now takes 11 fewer tab stops. Accessibility work is product work. #a11y", now.minus(12, ChronoUnit.MINUTES), 184, 19, 31, 12400);
        changed |= addSeedPost("Marcus Cole", "marcusux",
                "A design system should reduce decisions without erasing judgment. The best components leave room for context.", now.minus(28, ChronoUnit.MINUTES), 96, 8, 14, 7200);
        changed |= addSeedPost("Mina Park", "minapixels",
                "Spent the morning making footsteps sound different on stone, moss, and old wood. Nobody asked for it. Everyone will feel it. 🎮", now.minus(47, ChronoUnit.MINUTES), 542, 37, 68, 28100);
        changed |= addSeedPost("Arjun Mehta", "arjunoss",
                "Release note of the day: “No breaking changes” is only useful when the migration guide proves it. #OpenSource", now.minus(1, ChronoUnit.HOURS), 221, 16, 44, 15600);
        changed |= addSeedPost("Sofía Reyes", "sofiashots",
                "The photo before the celebration is usually the one I keep—the half-second when an athlete realizes what just happened.", now.minus(2, ChronoUnit.HOURS), 1300, 72, 188, 88400);
        changed |= addSeedPost("JavaFX", "openjfx",
                "Building responsive desktop interfaces with JavaFX. #JavaFX #Desktop", now.minus(2, ChronoUnit.HOURS), 318, 24, 57, 19600);
        changed |= addSeedPost("Daniel Hart", "danielscience",
                "A useful reminder from today’s climate paper: uncertainty is a range to plan around, not a reason to wait.", now.minus(3, ChronoUnit.HOURS), 764, 61, 203, 52900);
        changed |= addSeedPost("Elon Musk", "elonmusk",
                "Starship launch window opens at 5:45pm Texas time. #SpaceX", now.minus(3, ChronoUnit.HOURS), 18400, 2400, 3700, 2400000);
        changed |= addSeedPost("Layla Rahimi", "laylacodes",
                "Today’s debugging lesson: if the impossible state appears in production, it was possible. Write the invariant down, then enforce it.", now.minus(5, ChronoUnit.HOURS), 407, 28, 65, 23000);
        changed |= addSeedPost("Marcus Cole", "marcusux",
                "Three interface states worth designing before the happy path: empty, loading, and permission denied.", now.minus(7, ChronoUnit.HOURS), 682, 32, 142, 45100);
        changed |= addSeedPost("Mina Park", "minapixels",
                "The first playable build is terrible in a very useful way. It replaces ten imagined problems with three real ones.", now.minus(9, ChronoUnit.HOURS), 918, 44, 119, 63800);
        changed |= addSeedPost("Arjun Mehta", "arjunoss",
                "Code review gets better when comments explain the risk, not just the preferred syntax.", now.minus(11, ChronoUnit.HOURS), 355, 21, 87, 27400);
        changed |= addSeedPost("Design Daily", "designdaily",
                "Dark interfaces work best when spacing and hierarchy stay consistent. #Design", now.minus(14, ChronoUnit.HOURS), 476, 29, 91, 33600);
        changed |= addSeedPost("Sofía Reyes", "sofiashots",
                "Rain delay. Empty track. One groundskeeper under a red umbrella. Sometimes the assignment gives you a different picture.", now.minus(17, ChronoUnit.HOURS), 2100, 83, 274, 142000);
        changed |= addSeedPost("Not Elon Musk", "ElonMuskPDA",
                "The greatest gift you can give your child isn't wealth, it's wisdom, values, and the courage to think.", now.minus(23, ChronoUnit.HOURS), 7200, 381, 1100, 640000);
        changed |= addSeedPost("Daniel Hart", "danielscience",
                "The night sky is a time machine with no pause button. Every point of light is arriving late.", now.minus(1, ChronoUnit.DAYS), 3300, 126, 690, 220000);
        changed |= addSeedPost("Layla Rahimi", "laylacodes",
                "برای ساختن نرم‌افزار خوب، اول باید مسئله را درست بفهمیم. Good software starts with understanding the problem.", now.minus(1, ChronoUnit.DAYS).minus(3, ChronoUnit.HOURS), 603, 48, 102, 41800);
        changed |= addSeedPost("Marcus Cole", "marcusux",
                "If the destructive button looks exactly like the safe button, confirmation text is doing too much work.", now.minus(1, ChronoUnit.DAYS).minus(8, ChronoUnit.HOURS), 811, 53, 177, 57000);
        changed |= addSeedPost("Mina Park", "minapixels",
                "Current status: the boss fight works, the boss music works, and they absolutely do not work at the same time.", now.minus(2, ChronoUnit.DAYS), 1700, 91, 236, 116000);
        changed |= addSeedPost("Arjun Mehta", "arjunoss",
                "Maintainer tip: close the issue kindly, document why, and leave a path for better evidence. Boundaries can still be welcoming.", now.minus(2, ChronoUnit.DAYS).minus(6, ChronoUnit.HOURS), 492, 39, 81, 34900);
        changed |= addSeedPost("JavaFX", "openjfx",
                "Tip: use virtualized controls for very large data sets, and keep expensive work off the JavaFX Application Thread.", now.minus(3, ChronoUnit.DAYS), 287, 17, 64, 20800);
        changed |= addSeedPost("Daniel Hart", "danielscience",
                "A chart can be technically accurate and still tell the wrong story. Always check the baseline, units, and missing context.", now.minus(4, ChronoUnit.DAYS), 1200, 77, 308, 94000);
        changed |= addSeedPost("Sofía Reyes", "sofiashots",
                "Pack light, arrive early, and learn where the light will be before the action starts.", now.minus(5, ChronoUnit.DAYS), 967, 41, 132, 71800);
        changed |= addSeedPost("Design Daily", "designdaily",
                "Good empty states answer three questions: what happened, why it matters, and what the user can do next.", now.minus(6, ChronoUnit.DAYS), 709, 35, 164, 48800);
        posts.sort(Comparator.comparing(Post::getCreatedAt).reversed());
        if (changed) save();
    }

    private boolean addSeedPost(String authorName, String username, String content, Instant createdAt,
                                int likes, int replies, int reposts, int views) {
        Post existing = posts.stream().filter(post -> post.getContent().equals(content)).findFirst().orElse(null);
        if (existing != null) {
            if (existing.getLikes() == 0 && existing.getReplies() == 0 && existing.getRetweets() == 0 && existing.getViews() == 0) {
                existing.restoreState(likes, replies, reposts, views, existing.isPinned(), existing.getLikedBy(),
                        existing.getRepliedBy(), existing.getRetweetedBy(), existing.getBookmarkedBy());
                return true;
            }
            return false;
        }
        Post post = new Post(nextId.getAndIncrement(), authorName, username, content, createdAt);
        post.restoreState(likes, replies, reposts, views, false, Set.of(), Set.of(), Set.of(), Set.of());
        posts.add(post);
        return true;
    }

    public synchronized Post createPost(User author, String content) { return createPost(author, content, null); }

    public synchronized Post createPost(User author, String content, String mediaUri) {
        return createPost(author, content, mediaUri, null, null);
    }

    public synchronized Post createPoll(User author, String question, List<String> choices, java.time.Duration duration) {
        Post post = createPost(author, question, null, null, null);
        post.setPoll(new PollData(choices, Instant.now().plus(duration)));
        save();
        return post;
    }

    public synchronized boolean vote(Post post, int choiceIndex) {
        if (post == null || post.getPoll() == null || !post.getPoll().vote(choiceIndex)) return false;
        save();
        return true;
    }

    private Post createPost(User author, String content, String mediaUri, Long replyToId, Long quotedPostId) {
        String displayName = author.getDisplayName() == null || author.getDisplayName().isBlank()
                ? author.getUsername() : author.getDisplayName();
        // Keep this author visible in the shared profile directory for other accounts.
        if (profiles.get(normalize(author.getUsername())) == null) {
            profiles.put(normalize(author.getUsername()), new ProfileData(displayName, author.getBio(),
                    author.getAvatarUrl(), author.getBannerUrl(), author.getLocation(), author.getWebsite(),
                    author.getBirthDate(), author.isProfessional()));
        }
        Post post = new Post(nextId.getAndIncrement(), displayName, author.getUsername(),
                content == null ? "" : content, Instant.now(), mediaUri, replyToId, quotedPostId);
        posts.addFirst(post);
        createMentionNotifications(author, post);
        clearDraft(author.getUsername());
        touch();
        return post;
    }

    public synchronized Post createReply(User author, String content, String mediaUri, Post original) {
        original.addReply();
        Post reply = createPost(author, content, mediaUri, original.getId(), null);
        notify(original.getAuthorUsername(), author, NotificationItem.Type.REPLY, original.getId(), content);
        save();
        return reply;
    }

    public synchronized Post createQuote(User author, String content, String mediaUri, Post original) {
        original.addQuote();
        Post quote = createPost(author, content, mediaUri, null, original.getId());
        notify(original.getAuthorUsername(), author, NotificationItem.Type.REPOST, original.getId(), content);
        save();
        return quote;
    }

    public synchronized void toggleLike(Post post) {
        boolean increasing = !post.isLiked();
        post.toggleLike();
        if (increasing) notify(post.getAuthorUsername(), activeUser(), NotificationItem.Type.LIKE, post.getId(), post.getContent());
        save();
    }

    public synchronized void toggleRepost(Post post) {
        boolean increasing = !post.isRetweeted();
        post.toggleRetweet();
        if (increasing) notify(post.getAuthorUsername(), activeUser(), NotificationItem.Type.REPOST, post.getId(), post.getContent());
        save();
    }

    public synchronized void toggleBookmark(Post post) { post.toggleBookmark(); save(); }

    public synchronized boolean toggleFollow(User actor, String targetUsername) {
        if (actor == null || targetUsername == null || actor.getUsername().equalsIgnoreCase(targetUsername)) return false;
        String actorKey = normalize(actor.getUsername());
        String targetKey = normalize(targetUsername);
        Set<String> targets = following.computeIfAbsent(actorKey, ignored -> new LinkedHashSet<>());
        boolean nowFollowing;
        if (targets.remove(targetKey)) nowFollowing = false;
        else { targets.add(targetKey); nowFollowing = true; notify(targetKey, actor, NotificationItem.Type.FOLLOW, null, ""); }
        save();
        return nowFollowing;
    }

    public synchronized boolean isFollowing(String actor, String target) {
        return following.getOrDefault(normalize(actor), Set.of()).contains(normalize(target));
    }

    public synchronized Set<String> getFollowing(String username) {
        return Set.copyOf(following.getOrDefault(normalize(username), Set.of()));
    }

    public synchronized Set<String> getFollowers(String username) {
        String target = normalize(username);
        Set<String> result = new LinkedHashSet<>();
        following.forEach((actor, targets) -> { if (targets.contains(target)) result.add(actor); });
        return result;
    }

    public synchronized int getFollowingCount(String username) { return getFollowing(username).size(); }
    public synchronized int getFollowerCount(String username) { return getFollowers(username).size(); }

    public synchronized Post getPost(long id) { return posts.stream().filter(post -> post.getId() == id).findFirst().orElse(null); }

    public synchronized List<Post> getAllPosts() { return visiblePosts(posts); }

    public synchronized List<Post> getForYouPosts() {
        return visiblePosts(posts).stream().sorted(Comparator
                .comparingInt((Post post) -> post.getLikes() + post.getRetweets() * 2 + post.getReplies() * 2).reversed()
                .thenComparing(Post::getCreatedAt, Comparator.reverseOrder())).toList();
    }

    public synchronized List<Post> getFollowingPosts(String username) {
        Set<String> targets = new HashSet<>(getFollowing(username));
        targets.add(normalize(username));
        return visiblePosts(posts).stream()
                .filter(post -> targets.contains(normalize(post.getAuthorUsername())))
                .filter(post -> post.getReplyToId() == null)
                .toList();
    }

    public synchronized List<Post> getPostsByUsername(String username) {
        if (username == null) return List.of();
        return visiblePosts(posts).stream().filter(post -> username.equalsIgnoreCase(post.getAuthorUsername())).toList();
    }

    public synchronized List<Post> getProfilePosts(String username) {
        return getPostsByUsername(username).stream().filter(post -> post.getReplyToId() == null)
                .sorted(Comparator.comparing(Post::isPinned).reversed()
                        .thenComparing(Post::getCreatedAt, Comparator.reverseOrder())).toList();
    }

    public synchronized List<Post> getReplies(long postId) {
        return visiblePosts(posts).stream().filter(post -> post.getReplyToId() != null && post.getReplyToId() == postId).toList();
    }

    public synchronized List<Post> getBookmarkedPosts() { return visiblePosts(posts).stream().filter(Post::isBookmarked).toList(); }
    public synchronized List<Post> getLikedPosts() { return visiblePosts(posts).stream().filter(Post::isLiked).toList(); }

    public synchronized List<Post> getRepliesByUsername(String username) {
        if (username == null) return List.of();
        return visiblePosts(posts).stream().filter(post -> post.getReplyToId() != null)
                .filter(post -> username.equalsIgnoreCase(post.getAuthorUsername())).toList();
    }

    public synchronized List<Post> search(String query) {
        if (query == null || query.isBlank()) return getAllPosts();
        String normalized = query.toLowerCase(Locale.ROOT);
        return visiblePosts(posts).stream().filter(post ->
                post.getContent().toLowerCase(Locale.ROOT).contains(normalized)
                        || post.getAuthorName().toLowerCase(Locale.ROOT).contains(normalized)
                        || post.getAuthorUsername().toLowerCase(Locale.ROOT).contains(normalized)).toList();
    }

    public synchronized boolean deletePost(Post post) {
        if (post == null || !post.getAuthorUsername().equalsIgnoreCase(activeUsername())) return false;
        boolean removed = posts.removeIf(candidate -> candidate.getId() == post.getId() ||
                (candidate.getReplyToId() != null && candidate.getReplyToId() == post.getId()));
        if (removed) save();
        return removed;
    }

    public synchronized void togglePinned(Post post) {
        if (post == null || !post.getAuthorUsername().equalsIgnoreCase(activeUsername())) return;
        posts.stream().filter(candidate -> candidate.getAuthorUsername().equalsIgnoreCase(activeUsername()))
                .forEach(candidate -> candidate.setPinned(false));
        post.setPinned(!post.isPinned());
        save();
    }

    public synchronized void hidePost(Post post) {
        hiddenPosts.computeIfAbsent(normalize(activeUsername()), ignored -> new HashSet<>()).add(post.getId());
        save();
    }

    public synchronized void toggleMute(String username) {
        Set<String> muted = mutedAccounts.computeIfAbsent(normalize(activeUsername()), ignored -> new HashSet<>());
        if (!muted.remove(normalize(username))) muted.add(normalize(username));
        save();
    }

    public synchronized boolean isMuted(String username) {
        return mutedAccounts.getOrDefault(normalize(activeUsername()), Set.of()).contains(normalize(username));
    }

    public synchronized void recordView(Post post) {
        if (post == null) return;
        String key = normalize(activeUsername()) + ":" + post.getId();
        if (recordedViews.add(key)) { post.addView(); save(); }
    }

    public synchronized List<NotificationItem> getNotifications(String recipient) {
        String key = normalize(recipient);
        return notifications.stream().filter(item -> normalize(item.recipientUsername()).equals(key))
                .sorted(Comparator.comparing(NotificationItem::createdAt).reversed()).toList();
    }

    public synchronized int getUnreadNotificationCount(String recipient) {
        return (int) getNotifications(recipient).stream().filter(item -> !item.read()).count();
    }

    public synchronized void markNotificationsRead(String recipient) {
        String key = normalize(recipient);
        for (int index = 0; index < notifications.size(); index++) {
            NotificationItem item = notifications.get(index);
            if (normalize(item.recipientUsername()).equals(key) && !item.read()) notifications.set(index, item.withRead(true));
        }
        save();
    }

    public synchronized void saveDraft(String username, String text, String mediaUri) {
        if (username == null) return;
        if ((text == null || text.isBlank()) && mediaUri == null) drafts.remove(normalize(username));
        else drafts.put(normalize(username), new DraftData(text == null ? "" : text, mediaUri));
        save();
    }

    public synchronized DraftData getDraft(String username) { return drafts.get(normalize(username)); }
    public synchronized void clearDraft(String username) { if (username != null && drafts.remove(normalize(username)) != null) save(); }

    public synchronized void saveProfile(User user) {
        if (user == null) return;
        profiles.put(normalize(user.getUsername()), new ProfileData(user.getDisplayName(), user.getBio(),
                user.getAvatarUrl(), user.getBannerUrl(), user.getLocation(), user.getWebsite(),
                user.getBirthDate(), user.isProfessional()));
        updateAuthorName(user.getUsername(), user.getDisplayName());
        save();
    }

    /**
     * Persist the signed-in user's profile into the shared local store so other
     * accounts on this machine can see bio, avatar, banner, and location.
     */
    public synchronized void ensureProfile(User user) {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) return;
        saveProfile(user);
    }

    public synchronized void applyProfile(User user) {
        if (user == null) return;
        ProfileData profile = profiles.get(normalize(user.getUsername()));
        if (profile == null) return;
        user.setDisplayName(profile.displayName);
        user.setBio(profile.bio);
        user.setAvatarUrl(profile.avatarUrl);
        user.setBannerUrl(profile.bannerUrl);
        user.setLocation(profile.location);
        user.setWebsite(profile.website);
        user.setBirthDate(profile.birthDate);
        user.setProfessional(profile.professional);
    }

    /** Shared profile fields for any account that has logged in or edited on this device. */
    public synchronized UserProfile findProfile(String username) {
        if (username == null || username.isBlank()) return null;
        ProfileData profile = profiles.get(normalize(username));
        if (profile == null) return null;
        return new UserProfile(profile.displayName, profile.bio, profile.avatarUrl, profile.bannerUrl,
                profile.location, profile.website, profile.birthDate, profile.professional);
    }

    /** Best-effort display name from posts when no stored profile exists. */
    public synchronized String findAuthorDisplayName(String username) {
        if (username == null || username.isBlank()) return null;
        String key = normalize(username);
        return posts.stream()
                .filter(post -> key.equals(normalize(post.getAuthorUsername())))
                .map(Post::getAuthorName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(null);
    }

    /** Usernames that appear in profiles or as post authors (for search / directory). */
    public synchronized Set<String> knownUsernames() {
        Set<String> names = new LinkedHashSet<>(profiles.keySet());
        for (Post post : posts) {
            if (post.getAuthorUsername() != null && !post.getAuthorUsername().isBlank()) {
                names.add(normalize(post.getAuthorUsername()));
            }
        }
        for (Set<String> targets : following.values()) names.addAll(targets);
        following.keySet().forEach(names::add);
        return names;
    }

    public record UserProfile(String displayName, String bio, String avatarUrl, String bannerUrl,
                              String location, String website, String birthDate, boolean professional) {}

    public synchronized void updateAuthorName(String username, String displayName) {
        posts.stream().filter(post -> username.equalsIgnoreCase(post.getAuthorUsername()))
                .forEach(post -> post.setAuthorName(displayName));
        save();
    }

    public ReadOnlyLongProperty clockProperty() { return clock.getReadOnlyProperty(); }
    public void requestHashtag(String hashtag) { requestedHashtag = hashtag; }
    public String consumeRequestedHashtag() { String value = requestedHashtag; requestedHashtag = null; return value; }
    public void requestComposerFocus() { composerFocusRequested = true; }
    public boolean consumeComposerFocusRequest() { boolean value = composerFocusRequested; composerFocusRequested = false; return value; }
    public void requestView(String view) { requestedView = view; }
    public String consumeRequestedView() { String value = requestedView; requestedView = null; return value; }
    public void requestSearch(String query) { requestedSearch = query; }
    public String consumeRequestedSearch() { String value = requestedSearch; requestedSearch = null; return value; }
    public void requestProfile(String username) { requestedProfileUsername = username; }
    public String consumeRequestedProfile() { String value = requestedProfileUsername; requestedProfileUsername = null; return value; }
    public void requestPost(long id) { requestedPostId = id; requestedView = "post-detail"; }
    public Long consumeRequestedPost() { Long value = requestedPostId; requestedPostId = null; return value; }

    public String relativeTime(Post post) {
        long seconds = Math.max(0, Instant.now().getEpochSecond() - post.getCreatedAt().getEpochSecond());
        if (seconds < 60) return seconds + "s";
        if (seconds < 3_600) return (seconds / 60) + "m";
        if (seconds < 86_400) return (seconds / 3_600) + "h";
        if (seconds < 604_800) return (seconds / 86_400) + "d";
        return DATE_FORMAT.format(post.getCreatedAt());
    }

    private List<Post> visiblePosts(List<Post> source) {
        Set<Long> hidden = hiddenPosts.getOrDefault(normalize(activeUsername()), Set.of());
        Set<String> muted = mutedAccounts.getOrDefault(normalize(activeUsername()), Set.of());
        return source.stream().filter(post -> !hidden.contains(post.getId()))
                .filter(post -> !muted.contains(normalize(post.getAuthorUsername()))).toList();
    }

    private void createMentionNotifications(User author, Post post) {
        Matcher matcher = MENTION_PATTERN.matcher(post.getContent());
        Set<String> recipients = new HashSet<>();
        while (matcher.find()) recipients.add(normalize(matcher.group(1)));
        recipients.forEach(recipient -> notify(recipient, author, NotificationItem.Type.MENTION,
                post.getId(), post.getContent()));
    }

    private void notify(String recipient, User actor, NotificationItem.Type type, Long postId, String excerpt) {
        if (recipient == null || actor == null || recipient.equalsIgnoreCase(actor.getUsername())) return;
        String displayName = actor.getDisplayName() == null || actor.getDisplayName().isBlank()
                ? actor.getUsername() : actor.getDisplayName();
        String clipped = excerpt == null ? "" : excerpt.length() > 140 ? excerpt.substring(0, 140) + "…" : excerpt;
        notifications.add(new NotificationItem(nextNotificationId.getAndIncrement(), normalize(recipient), displayName,
                actor.getUsername(), type, postId, clipped, Instant.now(), false));
    }

    private User activeUser() { return UserSession.getInstance().getCurrentUser(); }
    private String activeUsername() { return UserSession.getInstance().getUsername() == null ? "anonymous" : UserSession.getInstance().getUsername(); }
    private static String normalize(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT); }
    private void touch() { clock.set(Instant.now().getEpochSecond()); save(); }

    private boolean load() {
        if (!Files.isRegularFile(stateFile)) return false;
        try {
            StoredState state = gson.fromJson(Files.readString(stateFile), StoredState.class);
            if (state == null || state.posts == null) return false;
            nextId.set(Math.max(1, state.nextId));
            nextNotificationId.set(Math.max(1, state.nextNotificationId));
            for (StoredPost stored : state.posts) {
                Post post = new Post(stored.id, stored.authorName, stored.authorUsername, stored.content,
                        Instant.parse(stored.createdAt), stored.mediaUri, stored.replyToId, stored.quotedPostId);
                post.restoreState(stored.likes, stored.replies, stored.retweets, stored.views, stored.pinned,
                        set(stored.likedBy), set(stored.repliedBy), set(stored.retweetedBy), set(stored.bookmarkedBy));
                if (stored.poll != null) post.setPoll(new PollData(stored.poll.choices, stored.poll.votes,
                        stored.poll.votesByUser, Instant.parse(stored.poll.endsAt)));
                posts.add(post);
            }
            if (state.following != null) state.following.forEach((key, values) -> following.put(key, new LinkedHashSet<>(values)));
            if (state.hiddenPosts != null) state.hiddenPosts.forEach((key, values) -> hiddenPosts.put(key, new HashSet<>(values)));
            if (state.mutedAccounts != null) state.mutedAccounts.forEach((key, values) -> mutedAccounts.put(key, new HashSet<>(values)));
            if (state.drafts != null) drafts.putAll(state.drafts);
            if (state.profiles != null) profiles.putAll(state.profiles);
            if (state.notifications != null) for (StoredNotification item : state.notifications) {
                notifications.add(new NotificationItem(item.id, item.recipientUsername, item.actorName, item.actorUsername,
                        NotificationItem.Type.valueOf(item.type), item.postId, item.excerpt, Instant.parse(item.createdAt), item.read));
            }
            return true;
        } catch (Exception exception) {
            System.err.println("Could not load social state: " + exception.getMessage());
            return false;
        }
    }

    private synchronized void save() {
        try {
            Files.createDirectories(stateFile.getParent());
            StoredState state = new StoredState();
            state.nextId = nextId.get();
            state.nextNotificationId = nextNotificationId.get();
            posts.forEach(post -> state.posts.add(new StoredPost(post)));
            following.forEach((key, values) -> state.following.put(key, new ArrayList<>(values)));
            hiddenPosts.forEach((key, values) -> state.hiddenPosts.put(key, new ArrayList<>(values)));
            mutedAccounts.forEach((key, values) -> state.mutedAccounts.put(key, new ArrayList<>(values)));
            state.drafts.putAll(drafts);
            state.profiles.putAll(profiles);
            notifications.forEach(item -> state.notifications.add(new StoredNotification(item)));
            Path temporary = stateFile.resolveSibling(stateFile.getFileName() + ".tmp");
            Files.writeString(temporary, gson.toJson(state));
            try { Files.move(temporary, stateFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (IOException ignored) { Files.move(temporary, stateFile, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException exception) {
            System.err.println("Could not save social state: " + exception.getMessage());
        }
    }

    private static Set<String> set(List<String> values) { return values == null ? Set.of() : new HashSet<>(values); }

    public record DraftData(String text, String mediaUri) {}

    private static final class StoredState {
        long nextId = 1;
        long nextNotificationId = 1;
        List<StoredPost> posts = new ArrayList<>();
        List<StoredNotification> notifications = new ArrayList<>();
        Map<String, List<String>> following = new HashMap<>();
        Map<String, List<Long>> hiddenPosts = new HashMap<>();
        Map<String, List<String>> mutedAccounts = new HashMap<>();
        Map<String, DraftData> drafts = new HashMap<>();
        Map<String, ProfileData> profiles = new HashMap<>();
    }

    private static final class ProfileData {
        String displayName; String bio; String avatarUrl; String bannerUrl; String location; String website; String birthDate;
        boolean professional;
        ProfileData(String displayName, String bio, String avatarUrl, String bannerUrl, String location,
                    String website, String birthDate, boolean professional) {
            this.displayName = displayName; this.bio = bio; this.avatarUrl = avatarUrl; this.bannerUrl = bannerUrl;
            this.location = location; this.website = website; this.birthDate = birthDate; this.professional = professional;
        }
    }

    private static final class StoredPost {
        long id; String authorName; String authorUsername; String content; String createdAt; String mediaUri;
        Long replyToId; Long quotedPostId; int likes; int replies; int retweets; int views; boolean pinned;
        List<String> likedBy; List<String> repliedBy; List<String> retweetedBy; List<String> bookmarkedBy;
        StoredPoll poll;
        StoredPost(Post post) {
            id = post.getId(); authorName = post.getAuthorName(); authorUsername = post.getAuthorUsername();
            content = post.getContent(); createdAt = post.getCreatedAt().toString(); mediaUri = post.getMediaUri();
            replyToId = post.getReplyToId(); quotedPostId = post.getQuotedPostId(); likes = post.getLikes();
            replies = post.getReplies(); retweets = post.getRetweets(); views = post.getViews(); pinned = post.isPinned();
            likedBy = new ArrayList<>(post.getLikedBy()); repliedBy = new ArrayList<>(post.getRepliedBy());
            retweetedBy = new ArrayList<>(post.getRetweetedBy()); bookmarkedBy = new ArrayList<>(post.getBookmarkedBy());
            if (post.getPoll() != null) poll = new StoredPoll(post.getPoll());
        }
    }

    private static final class StoredPoll {
        List<String> choices; List<Integer> votes; Map<String, Integer> votesByUser; String endsAt;
        StoredPoll(PollData poll) {
            choices = poll.getChoices(); votes = poll.getVotes(); votesByUser = poll.getVotesByUser(); endsAt = poll.getEndsAt().toString();
        }
    }

    private static final class StoredNotification {
        long id; String recipientUsername; String actorName; String actorUsername; String type;
        Long postId; String excerpt; String createdAt; boolean read;
        StoredNotification(NotificationItem item) {
            id = item.id(); recipientUsername = item.recipientUsername(); actorName = item.actorName(); actorUsername = item.actorUsername();
            type = item.type().name(); postId = item.postId(); excerpt = item.excerpt(); createdAt = item.createdAt().toString(); read = item.read();
        }
    }
}
