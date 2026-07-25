package server.database;

import at.favre.lib.crypto.bcrypt.BCrypt;
import shared.models.SharedFollow;
import shared.models.SharedNotification;
import shared.models.SharedPost;
import shared.models.SharedProfile;
import shared.models.SharedSocialState;
import shared.models.SharedTrend;
import shared.models.SocialSettings;
import shared.models.User;

import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared social persistence used by both the embedded SQLite backend and the
 * hosted Turso backend.
 */
public final class SocialDatabase {
    private static final long DEFAULT_NPC_INTERVAL_SECONDS = 15 * 60;
    private static final int NPC_BOOTSTRAP_POSTS = 24;
    private static final Pattern MENTION = Pattern.compile(
            "(?<![\\p{L}\\p{N}_])@([\\p{L}\\p{N}_]+)",
            Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern HASHTAG = Pattern.compile(
            "(?<![\\p{L}\\p{N}_])#([\\p{L}\\p{N}_]+)",
            Pattern.UNICODE_CHARACTER_CLASS);
    private static final int MAX_MEDIA_BYTES = 8 * 1024 * 1024;

    private final SqlBackend backend;

    private SocialDatabase() {
        String url = firstNonBlank(
                System.getProperty("xclone.turso.url"),
                System.getenv("TURSO_DATABASE_URL"));
        String token = firstNonBlank(
                System.getProperty("xclone.turso.token"),
                System.getenv("TURSO_AUTH_TOKEN"));
        if (url == null && token == null) {
            backend = new SQLiteBackend();
        } else {
            if (url == null || token == null) {
                throw new IllegalStateException(
                        "TURSO_DATABASE_URL and TURSO_AUTH_TOKEN must both be configured");
            }
            backend = new TursoBackend(url, token);
        }
        backend.initialize(SCHEMA);
        ensureSocialPostColumns();
        if (fakeContentEnabled()) initializeNpcWorld();
    }

    public static SocialDatabase getInstance() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final SocialDatabase INSTANCE = new SocialDatabase();
    }

    public SharedSocialState state(String sessionToken) {
        Viewer viewer = requireViewer(sessionToken);
        if (fakeContentEnabled()) maintainNpcWorld();
        return new SharedSocialState(
                profiles(),
                posts(viewer.id()),
                follows(),
                notifications(viewer));
    }

    public SharedSocialState feed(
            String sessionToken, Long beforeId, int requestedLimit, boolean followingOnly) {
        Viewer viewer = requireViewer(sessionToken);
        if (fakeContentEnabled()) maintainNpcWorld();
        int limit = Math.max(1, Math.min(100, requestedLimit));
        return new SharedSocialState(
                profiles(),
                posts(viewer.id(), null, false, followingOnly, beforeId, limit),
                follows(),
                notifications(viewer));
    }

    public SharedSocialState search(
            String sessionToken, String query, String tab, Long beforeId, int requestedLimit) {
        Viewer viewer = requireViewer(sessionToken);
        String normalized = query == null ? "" : query.trim();
        int limit = Math.max(1, Math.min(100, requestedLimit));
        boolean people = "people".equalsIgnoreCase(tab);
        boolean media = "media".equalsIgnoreCase(tab);
        return new SharedSocialState(
                people || !normalized.isBlank() ? profiles(normalized, limit) : profiles(),
                people ? List.of() : posts(viewer.id(), normalized, media, false, beforeId, limit),
                follows(),
                List.of());
    }

    public List<SharedTrend> trends(String sessionToken) {
        requireViewer(sessionToken);
        Result result = backend.execute("""
                SELECT p.content
                FROM social_posts p
                WHERE (? = 1 OR NOT EXISTS (
                    SELECT 1 FROM social_npc_accounts n WHERE n.user_id = p.author_id
                ))
                ORDER BY p.created_at DESC, p.id DESC
                LIMIT 1000
                """, fakeContentEnabled() ? 1 : 0);
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, String> display = new LinkedHashMap<>();
        for (Map<String, String> row : result.rows()) {
            Matcher matcher = HASHTAG.matcher(string(row, "content"));
            Set<String> seen = new HashSet<>();
            while (matcher.find()) {
                String tag = matcher.group(1);
                String key = normalize(tag);
                if (seen.add(key)) {
                    counts.merge(key, 1, Integer::sum);
                    display.putIfAbsent(key, "#" + tag);
                }
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(20)
                .map(entry -> new SharedTrend(display.get(entry.getKey()), entry.getValue()))
                .toList();
    }

    public SocialSettings settings(String sessionToken) {
        Viewer viewer = requireViewer(sessionToken);
        return new SocialSettings(fakeContentEnabled(), viewer.admin());
    }

    public SocialSettings updateFakeContent(String sessionToken, boolean enabled) {
        Viewer viewer = requireViewer(sessionToken);
        if (!viewer.admin()) throw new SecurityException("Administrator access is required.");
        backend.execute("""
                INSERT INTO app_settings (setting_key, setting_value, updated_at, updated_by)
                VALUES ('fake_content_enabled', ?, ?, ?)
                ON CONFLICT(setting_key) DO UPDATE SET
                  setting_value = excluded.setting_value,
                  updated_at = excluded.updated_at,
                  updated_by = excluded.updated_by
                """, enabled ? "true" : "false", Instant.now().toString(), viewer.id());
        if (enabled) initializeNpcWorld();
        return new SocialSettings(enabled, true);
    }

    public String uploadMedia(
            String sessionToken, String mimeType, String originalName, String base64Data) {
        Viewer viewer = requireViewer(sessionToken);
        String mime = mimeType == null ? "" : mimeType.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("image/png", "image/jpeg", "image/gif").contains(mime)) {
            throw new IllegalArgumentException("Only PNG, JPEG, and GIF media are supported.");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Data == null ? "" : base64Data);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("The uploaded media is not valid Base64.");
        }
        if (bytes.length == 0 || bytes.length > MAX_MEDIA_BYTES) {
            throw new IllegalArgumentException("Media must be between 1 byte and 8 MB.");
        }
        if (!matchesImageSignature(bytes, mime)) {
            throw new IllegalArgumentException("The uploaded file does not match its image type.");
        }
        Result inserted = backend.execute("""
                INSERT INTO social_media
                  (owner_id, mime_type, original_name, data_base64, byte_size, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """, viewer.id(), mime, limited(originalName, 180),
                Base64.getEncoder().encodeToString(bytes), bytes.length, Instant.now().toString());
        return "xclone-media:" + longValue(inserted.first(), "id");
    }

    public MediaAsset media(long id) {
        Result result = backend.execute("""
                SELECT mime_type, data_base64
                FROM social_media
                WHERE id = ?
                LIMIT 1
                """, id);
        if (result.rows().isEmpty()) return null;
        Map<String, String> row = result.first();
        try {
            return new MediaAsset(
                    string(row, "mime_type"),
                    Base64.getDecoder().decode(string(row, "data_base64")));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public record MediaAsset(String mimeType, byte[] bytes) {}

    public SharedSocialState createPost(
            String sessionToken,
            String content,
            String mediaUri,
            Long replyToId,
            Long quotedPostId) {
        Viewer viewer = requireViewer(sessionToken);
        String normalizedContent = content == null ? "" : content.trim();
        String normalizedMedia = publicMediaUri(mediaUri);
        if (normalizedContent.isBlank() && normalizedMedia == null) {
            throw new IllegalArgumentException("A post needs text or public media.");
        }
        if (normalizedContent.length() > 280) {
            throw new IllegalArgumentException("Posts cannot exceed 280 characters.");
        }
        validateReferencedPost(replyToId);
        validateReferencedPost(quotedPostId);

        Result inserted = backend.execute("""
                INSERT INTO social_posts
                  (author_id, content, media_uri, reply_to_id, quoted_post_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """, viewer.id(), normalizedContent, normalizedMedia, replyToId, quotedPostId,
                Instant.now().toString());
        long postId = longValue(inserted.first(), "id");

        if (replyToId != null) {
            notifyPostAuthor(replyToId, viewer, "REPLY", postId, normalizedContent);
        }
        if (quotedPostId != null) {
            notifyPostAuthor(quotedPostId, viewer, "REPOST", postId, normalizedContent);
        }
        createMentionNotifications(viewer, postId, normalizedContent);
        return state(sessionToken);
    }

    public SharedSocialState togglePostInteraction(
            String sessionToken,
            long postId,
            Interaction interaction) {
        Viewer viewer = requireViewer(sessionToken);
        Map<String, String> post = requirePost(postId);
        String table = switch (interaction) {
            case LIKE -> "social_likes";
            case REPOST -> "social_reposts";
            case BOOKMARK -> "social_bookmarks";
        };
        Result existing = backend.execute(
                "SELECT 1 AS present FROM " + table + " WHERE post_id = ? AND user_id = ? LIMIT 1",
                postId, viewer.id());
        boolean adding = existing.rows().isEmpty();
        if (adding) {
            backend.execute("INSERT INTO " + table + " (post_id, user_id) VALUES (?, ?)",
                    postId, viewer.id());
        } else {
            backend.execute("DELETE FROM " + table + " WHERE post_id = ? AND user_id = ?",
                    postId, viewer.id());
        }
        if (adding && interaction != Interaction.BOOKMARK) {
            int authorId = intValue(post, "author_id");
            if (authorId != viewer.id()) {
                notifyUser(authorId, viewer, interaction.name(), postId, string(post, "content"));
            }
        }
        return state(sessionToken);
    }

    public SharedSocialState toggleFollow(String sessionToken, String targetUsername) {
        Viewer viewer = requireViewer(sessionToken);
        String targetKey = normalize(targetUsername);
        if (targetKey.isBlank() || targetKey.equals(normalize(viewer.username()))) {
            throw new IllegalArgumentException("Choose another account to follow.");
        }
        Result targetResult = backend.execute(
                "SELECT id FROM app_users WHERE username_key = ? LIMIT 1", targetKey);
        if (targetResult.rows().isEmpty()) {
            throw new IllegalArgumentException("That account does not exist on the shared server.");
        }
        int targetId = intValue(targetResult.first(), "id");
        Result existing = backend.execute("""
                SELECT 1 AS present FROM social_follows
                WHERE follower_id = ? AND followed_id = ? LIMIT 1
                """, viewer.id(), targetId);
        if (existing.rows().isEmpty()) {
            backend.execute(
                    "INSERT INTO social_follows (follower_id, followed_id) VALUES (?, ?)",
                    viewer.id(), targetId);
            notifyUser(targetId, viewer, "FOLLOW", null, "");
        } else {
            backend.execute(
                    "DELETE FROM social_follows WHERE follower_id = ? AND followed_id = ?",
                    viewer.id(), targetId);
        }
        return state(sessionToken);
    }

    public SharedSocialState deletePost(String sessionToken, long postId) {
        Viewer viewer = requireViewer(sessionToken);
        Result result = backend.execute(
                "DELETE FROM social_posts WHERE id = ? AND author_id = ?", postId, viewer.id());
        if (result.affectedRows() == 0) {
            throw new IllegalArgumentException("Only the author can delete this post.");
        }
        return state(sessionToken);
    }

    public SharedSocialState markNotificationsRead(String sessionToken) {
        Viewer viewer = requireViewer(sessionToken);
        backend.execute("""
                UPDATE social_notifications SET read_at = ?
                WHERE recipient_id = ? AND read_at IS NULL
                """, Instant.now().toString(), viewer.id());
        return state(sessionToken);
    }

    public SharedSocialState updateProfile(String sessionToken, User profile) {
        Viewer viewer = requireViewer(sessionToken);
        if (profile == null) throw new IllegalArgumentException("Profile details are required.");
        String displayName = profile.getDisplayName() == null ? "" : profile.getDisplayName().trim();
        if (displayName.isBlank() || displayName.length() > 50) {
            throw new IllegalArgumentException("Display name must contain 1 to 50 characters.");
        }
        backend.execute("""
                UPDATE app_users
                SET display_name = ?, bio = ?, avatar_url = ?, banner_url = ?, location = ?,
                    website = ?, birth_date = ?, professional = ?
                WHERE id = ?
                """,
                displayName,
                limited(profile.getBio(), 160),
                publicOrLocalProfileUri(profile.getAvatarUrl()),
                publicOrLocalProfileUri(profile.getBannerUrl()),
                limited(profile.getLocation(), 50),
                limited(profile.getWebsite(), 200),
                limited(profile.getBirthDate(), 30),
                profile.isProfessional() ? 1 : 0,
                viewer.id());
        return state(sessionToken);
    }

    private void ensureSocialPostColumns() {
        try {
            backend.execute(
                    "ALTER TABLE social_posts ADD COLUMN view_count INTEGER NOT NULL DEFAULT 0");
        } catch (IllegalStateException exception) {
            String message = exception.getMessage() == null
                    ? "" : exception.getMessage().toLowerCase(Locale.ROOT);
            if (!message.contains("duplicate column") && !message.contains("already exists")) {
                throw exception;
            }
        }
    }

    /**
     * Creates reserved, non-login NPC identities and an initial shared feed.
     * These rows live in the same Turso database as real activity, so every
     * desktop sees the same demo network.
     */
    private synchronized void initializeNpcWorld() {
        String disabledPassword = BCrypt.withDefaults().hashToString(
                6, UUID.randomUUID().toString().toCharArray());
        List<Command> userCommands = new ArrayList<>();
        for (NpcContentBank.Personality personality : NpcContentBank.PERSONALITIES) {
            String email = "npc+" + personality.username() + "@xclone.invalid";
            userCommands.add(new Command("""
                    INSERT OR IGNORE INTO app_users
                      (username, username_key, email, email_key, display_name, bio,
                       avatar_url, banner_url, created_at, location, professional, is_fake,
                       status, password_hash)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, 'active', ?)
                    """,
                    personality.username(),
                    normalize(personality.username()),
                    email,
                    normalize(email),
                    personality.displayName(),
                    personality.bio(),
                    personality.avatarUri(),
                    personality.bannerUri(),
                    LocalDate.now().toString(),
                    personality.location(),
                    disabledPassword));
        }
        backend.executeBatch(userCommands);

        Result reservedUsers = backend.execute("""
                SELECT id, username, email FROM app_users
                WHERE username_key LIKE 'xclone_%'
                """);
        List<Command> accountCommands = new ArrayList<>();
        for (Map<String, String> row : reservedUsers.rows()) {
            String username = string(row, "username");
            NpcContentBank.Personality personality = NpcContentBank.PERSONALITIES.stream()
                    .filter(item -> item.username().equalsIgnoreCase(username))
                    .findFirst()
                    .orElse(null);
            String expectedEmail = personality == null
                    ? "" : "npc+" + personality.username() + "@xclone.invalid";
            if (personality != null && expectedEmail.equalsIgnoreCase(string(row, "email"))) {
                accountCommands.add(new Command("""
                        UPDATE app_users
                        SET display_name = ?, bio = ?, avatar_url = ?, banner_url = ?,
                            location = ?, professional = 1, is_fake = 1, status = 'active'
                        WHERE id = ?
                        """,
                        personality.displayName(),
                        personality.bio(),
                        personality.avatarUri(),
                        personality.bannerUri(),
                        personality.location(),
                        intValue(row, "id")));
                accountCommands.add(new Command("""
                        INSERT OR IGNORE INTO social_npc_accounts (user_id, persona_key)
                        VALUES (?, ?)
                        """, intValue(row, "id"), personality.username()));
            }
        }
        backend.executeBatch(accountCommands);

        List<NpcActor> actors = npcActors();
        if (actors.isEmpty()) return;
        backfillNpcMedia(actors);
        Result count = backend.execute("""
                SELECT COUNT(*) AS total
                FROM social_posts p
                JOIN social_npc_accounts n ON n.user_id = p.author_id
                WHERE p.reply_to_id IS NULL
                """);
        int existingTopLevelPosts = intValue(count.first(), "total");
        if (existingTopLevelPosts < NPC_BOOTSTRAP_POSTS) {
            bootstrapNpcFeed(actors, NPC_BOOTSTRAP_POSTS - existingTopLevelPosts);
        }
        Result lastBucket = backend.execute("""
                SELECT state_value FROM social_npc_state
                WHERE state_key = 'last_bucket' LIMIT 1
                """);
        if (lastBucket.rows().isEmpty()) {
            setNpcState("last_bucket", Long.toString(currentNpcBucket()));
        }
    }

    /**
     * Repairs NPC posts created by older builds, including stale local-file
     * references. The choice is deterministic per post so restarts do not make
     * media jump between images.
     */
    private void backfillNpcMedia(List<NpcActor> actors) {
        Map<Integer, NpcContentBank.Personality> personalities = new LinkedHashMap<>();
        actors.forEach(actor -> personalities.put(actor.id(), actor.personality()));
        Result legacy = backend.execute("""
                SELECT p.id, p.author_id, p.reply_to_id
                FROM social_posts p
                JOIN social_npc_accounts n ON n.user_id = p.author_id
                WHERE p.media_uri IS NULL
                   OR p.media_uri = ''
                   OR p.media_uri NOT LIKE '/images/npc/media/%'
                """);
        List<Command> updates = new ArrayList<>();
        for (Map<String, String> row : legacy.rows()) {
            long postId = longValue(row, "id");
            NpcContentBank.Personality personality =
                    personalities.get(intValue(row, "author_id"));
            if (personality == null) continue;
            String media = row.get("reply_to_id") == null
                    ? NpcContentBank.media(personality, new Random(postId * 104_729L + 17))
                    : null;
            updates.add(new Command(
                    "UPDATE social_posts SET media_uri = ? WHERE id = ?",
                    media, postId));
        }
        for (int start = 0; start < updates.size(); start += 50) {
            backend.executeBatch(updates.subList(
                    start, Math.min(start + 50, updates.size())));
        }
    }

    private void bootstrapNpcFeed(List<NpcActor> actors, int postCount) {
        Instant now = Instant.now();
        Random random = new Random(now.truncatedTo(ChronoUnit.DAYS).getEpochSecond());
        List<Command> postCommands = new ArrayList<>();
        Set<String> generatedContent = new HashSet<>();
        for (int index = 0; index < postCount; index++) {
            NpcActor actor = actors.get(index % actors.size());
            String content;
            int attempts = 0;
            do {
                content = NpcContentBank.post(actor.personality(), random);
                attempts++;
            } while (!generatedContent.add(actor.username() + "\n" + content) && attempts < 12);
            Instant createdAt = now.minus((postCount - index) * 37L, ChronoUnit.MINUTES);
            postCommands.add(npcPostCommand(
                    actor.id(), content, NpcContentBank.media(actor.personality(), random),
                    null, createdAt, 120 + random.nextInt(18_000)));
        }
        backend.executeBatch(postCommands);

        List<NpcTarget> targets = npcTargets();
        List<Command> replyCommands = new ArrayList<>();
        int replyCount = Math.min(12, Math.max(3, postCount / 2));
        for (int index = 0; index < replyCount && !targets.isEmpty(); index++) {
            NpcActor actor = actors.get(random.nextInt(actors.size()));
            NpcTarget target = differentTarget(targets, actor.id(), random);
            if (target == null) continue;
            replyCommands.add(npcPostCommand(
                    actor.id(),
                    NpcContentBank.reply(actor.personality(), target.username(), random),
                    null,
                    target.postId(),
                    now.minus(11L * (replyCount - index), ChronoUnit.MINUTES),
                    25 + random.nextInt(1_500)));
        }
        backend.executeBatch(replyCommands);

        targets = npcTargets();
        List<Command> engagementCommands = new ArrayList<>();
        for (NpcActor actor : actors) {
            for (int index = 0; index < 4 && !targets.isEmpty(); index++) {
                NpcTarget target = differentTarget(targets, actor.id(), random);
                if (target == null) continue;
                engagementCommands.add(new Command(
                        "INSERT OR IGNORE INTO social_likes (post_id, user_id) VALUES (?, ?)",
                        target.postId(), actor.id()));
                if (random.nextBoolean()) {
                    engagementCommands.add(new Command(
                            "INSERT OR IGNORE INTO social_reposts (post_id, user_id) VALUES (?, ?)",
                            target.postId(), actor.id()));
                }
                if (random.nextInt(4) == 0) {
                    engagementCommands.add(new Command(
                            "INSERT OR IGNORE INTO social_bookmarks (post_id, user_id) VALUES (?, ?)",
                            target.postId(), actor.id()));
                }
            }
            NpcActor followed = differentActor(actors, actor.id(), random);
            if (followed != null) {
                engagementCommands.add(new Command("""
                        INSERT OR IGNORE INTO social_follows (follower_id, followed_id)
                        VALUES (?, ?)
                        """, actor.id(), followed.id()));
            }
        }
        backend.executeBatch(engagementCommands);
    }

    /**
     * Advances at most a few missed activity slots when any authenticated
     * client syncs. This avoids requiring a separate cron worker and remains
     * useful when a free host has been asleep.
     */
    private synchronized void maintainNpcWorld() {
        long currentBucket = currentNpcBucket();
        long lastBucket = longNpcState("last_bucket", currentBucket);
        if (currentBucket <= lastBucket) return;

        int steps = (int) Math.min(6, currentBucket - lastBucket);
        for (int step = steps - 1; step >= 0; step--) {
            long bucket = currentBucket - step;
            performNpcActivity(new Random(bucket * 7_919L + 31));
        }
        setNpcState("last_bucket", Long.toString(currentBucket));

        String cutoff = Instant.now().minus(7, ChronoUnit.DAYS).toString();
        backend.execute("""
                DELETE FROM social_posts
                WHERE id IN (
                  SELECT p.id
                  FROM social_posts p
                  WHERE p.author_id IN (SELECT user_id FROM social_npc_accounts)
                    AND p.created_at < ?
                    AND NOT EXISTS (
                      SELECT 1 FROM social_posts reply WHERE reply.reply_to_id = p.id
                    )
                    AND NOT EXISTS (
                      SELECT 1 FROM social_posts quote WHERE quote.quoted_post_id = p.id
                    )
                )
                """, cutoff);
    }

    private void performNpcActivity(Random random) {
        List<NpcActor> actors = npcActors();
        if (actors.size() < 2) return;
        NpcActor actor = actors.get(random.nextInt(actors.size()));
        int action = random.nextInt(100);

        if (action < 40) {
            insertNpcPost(actor.id(), uniqueNpcPost(actor, random),
                    NpcContentBank.media(actor.personality(), random), null,
                    Instant.now(), 20 + random.nextInt(900));
            return;
        }

        List<NpcTarget> targets = npcTargets();
        NpcTarget target = differentTarget(targets, actor.id(), random);
        if (target == null) return;
        backend.execute(
                "UPDATE social_posts SET view_count = view_count + ? WHERE id = ?",
                8 + random.nextInt(240), target.postId());

        if (action < 66) {
            insertNpcPost(
                    actor.id(),
                    NpcContentBank.reply(actor.personality(), target.username(), random),
                    null,
                    target.postId(),
                    Instant.now(),
                    5 + random.nextInt(180));
        } else if (action < 79) {
            backend.execute(
                    "INSERT OR IGNORE INTO social_likes (post_id, user_id) VALUES (?, ?)",
                    target.postId(), actor.id());
        } else if (action < 89) {
            backend.execute(
                    "INSERT OR IGNORE INTO social_reposts (post_id, user_id) VALUES (?, ?)",
                    target.postId(), actor.id());
        } else if (action < 95) {
            backend.execute(
                    "INSERT OR IGNORE INTO social_bookmarks (post_id, user_id) VALUES (?, ?)",
                    target.postId(), actor.id());
        } else {
            NpcActor followed = differentActor(actors, actor.id(), random);
            if (followed != null) {
                backend.execute("""
                        INSERT OR IGNORE INTO social_follows (follower_id, followed_id)
                        VALUES (?, ?)
                        """, actor.id(), followed.id());
            }
        }
    }

    private String uniqueNpcPost(NpcActor actor, Random random) {
        String content = NpcContentBank.post(actor.personality(), random);
        for (int attempt = 0; attempt < 6; attempt++) {
            Result duplicate = backend.execute("""
                    SELECT 1 AS present FROM social_posts
                    WHERE author_id = ? AND content = ? LIMIT 1
                    """, actor.id(), content);
            if (duplicate.rows().isEmpty()) return content;
            content = NpcContentBank.post(actor.personality(), random);
        }
        return content;
    }

    private void insertNpcPost(
            int authorId,
            String content,
            String mediaUri,
            Long replyToId,
            Instant createdAt,
            int initialViews) {
        Command command = npcPostCommand(
                authorId, content, mediaUri, replyToId, createdAt, initialViews);
        backend.execute(command.sql(), command.arguments());
    }

    private Command npcPostCommand(
            int authorId,
            String content,
            String mediaUri,
            Long replyToId,
            Instant createdAt,
            int initialViews) {
        return new Command("""
                INSERT INTO social_posts
                  (author_id, content, media_uri, reply_to_id, created_at, view_count)
                VALUES (?, ?, ?, ?, ?, ?)
                """, authorId, content, mediaUri, replyToId, createdAt.toString(), initialViews);
    }

    private List<NpcActor> npcActors() {
        Result result = backend.execute("""
                SELECT u.id, u.username, n.persona_key
                FROM social_npc_accounts n
                JOIN app_users u ON u.id = n.user_id
                ORDER BY u.id
                """);
        List<NpcActor> actors = new ArrayList<>();
        for (Map<String, String> row : result.rows()) {
            String personaKey = string(row, "persona_key");
            NpcContentBank.Personality personality = NpcContentBank.PERSONALITIES.stream()
                    .filter(item -> item.username().equalsIgnoreCase(personaKey))
                    .findFirst()
                    .orElse(null);
            if (personality != null) {
                actors.add(new NpcActor(
                        intValue(row, "id"),
                        string(row, "username"),
                        personality));
            }
        }
        return List.copyOf(actors);
    }

    private List<NpcTarget> npcTargets() {
        Result result = backend.execute("""
                SELECT p.id, p.author_id, u.username
                FROM social_posts p
                JOIN social_npc_accounts n ON n.user_id = p.author_id
                JOIN app_users u ON u.id = p.author_id
                ORDER BY p.created_at DESC, p.id DESC
                LIMIT 300
                """);
        return result.rows().stream().map(row -> new NpcTarget(
                longValue(row, "id"),
                intValue(row, "author_id"),
                string(row, "username"))).toList();
    }

    private NpcTarget differentTarget(List<NpcTarget> targets, int actorId, Random random) {
        if (targets == null || targets.isEmpty()) return null;
        for (int attempt = 0; attempt < 12; attempt++) {
            NpcTarget target = targets.get(random.nextInt(targets.size()));
            if (target.authorId() != actorId) return target;
        }
        return targets.stream().filter(target -> target.authorId() != actorId).findFirst().orElse(null);
    }

    private NpcActor differentActor(List<NpcActor> actors, int actorId, Random random) {
        for (int attempt = 0; attempt < 12; attempt++) {
            NpcActor actor = actors.get(random.nextInt(actors.size()));
            if (actor.id() != actorId) return actor;
        }
        return actors.stream().filter(actor -> actor.id() != actorId).findFirst().orElse(null);
    }

    private long currentNpcBucket() {
        return Instant.now().getEpochSecond() / npcIntervalSeconds();
    }

    private long npcIntervalSeconds() {
        String property = System.getProperty("xclone.npc.interval.seconds");
        String configured = property == null || property.isBlank()
                ? System.getenv("XCLONE_NPC_INTERVAL_SECONDS")
                : property;
        if (configured == null || configured.isBlank()) return DEFAULT_NPC_INTERVAL_SECONDS;
        try {
            long minimum = property == null || property.isBlank() ? 60 : 1;
            return Math.max(minimum, Long.parseLong(configured));
        } catch (NumberFormatException ignored) {
            return DEFAULT_NPC_INTERVAL_SECONDS;
        }
    }

    private long longNpcState(String key, long fallback) {
        Result result = backend.execute(
                "SELECT state_value FROM social_npc_state WHERE state_key = ? LIMIT 1", key);
        if (result.rows().isEmpty()) return fallback;
        try {
            return Long.parseLong(string(result.first(), "state_value"));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void setNpcState(String key, String value) {
        backend.execute("""
                INSERT INTO social_npc_state (state_key, state_value)
                VALUES (?, ?)
                ON CONFLICT(state_key) DO UPDATE SET state_value = excluded.state_value
                """, key, value);
    }

    private List<SharedProfile> profiles() {
        return profiles("", 1000);
    }

    private List<SharedProfile> profiles(String query, int requestedLimit) {
        String pattern = "%" + (query == null ? "" : query.trim().toLowerCase(Locale.ROOT)) + "%";
        Result result = backend.execute("""
                SELECT username, display_name, bio, avatar_url, banner_url, created_at,
                       location, website, birth_date, professional
                FROM app_users u
                WHERE u.status = 'active'
                  AND (? = 1 OR u.is_fake = 0)
                  AND (? = '%%' OR lower(u.username) LIKE ? OR lower(u.display_name) LIKE ?
                       OR lower(COALESCE(u.bio, '')) LIKE ?)
                ORDER BY id DESC
                LIMIT ?
                """, fakeContentEnabled() ? 1 : 0, pattern, pattern, pattern, pattern,
                Math.max(1, Math.min(1000, requestedLimit)));
        return result.rows().stream().map(row -> new SharedProfile(
                string(row, "username"),
                string(row, "display_name"),
                string(row, "bio"),
                string(row, "avatar_url"),
                string(row, "banner_url"),
                string(row, "created_at"),
                string(row, "location"),
                string(row, "website"),
                string(row, "birth_date"),
                booleanValue(row, "professional"))).toList();
    }

    private List<SharedPost> posts(int viewerId) {
        return posts(viewerId, null, false, false, null, 500);
    }

    private List<SharedPost> posts(
            int viewerId,
            String query,
            boolean mediaOnly,
            boolean followingOnly,
            Long beforeId,
            int requestedLimit) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String pattern = "%" + normalizedQuery + "%";
        Result result = backend.execute("""
                SELECT p.id, p.author_id, p.content, p.media_uri, p.reply_to_id,
                       p.quoted_post_id, p.created_at, p.view_count, u.username, u.display_name,
                       (SELECT COUNT(*) FROM social_likes l WHERE l.post_id = p.id) AS likes,
                       (SELECT COUNT(*) FROM social_posts r WHERE r.reply_to_id = p.id) AS replies,
                       ((SELECT COUNT(*) FROM social_reposts rr WHERE rr.post_id = p.id) +
                        (SELECT COUNT(*) FROM social_posts q WHERE q.quoted_post_id = p.id)) AS reposts,
                       CASE WHEN EXISTS (
                         SELECT 1 FROM social_likes vl WHERE vl.post_id = p.id AND vl.user_id = ?
                       ) THEN 1 ELSE 0 END AS liked_by_viewer,
                       CASE WHEN EXISTS (
                         SELECT 1 FROM social_reposts vr WHERE vr.post_id = p.id AND vr.user_id = ?
                       ) THEN 1 ELSE 0 END AS reposted_by_viewer,
                       CASE WHEN EXISTS (
                         SELECT 1 FROM social_bookmarks vb WHERE vb.post_id = p.id AND vb.user_id = ?
                       ) THEN 1 ELSE 0 END AS bookmarked_by_viewer
                FROM social_posts p
                JOIN app_users u ON u.id = p.author_id
                WHERE u.status = 'active'
                  AND (? = 1 OR u.is_fake = 0)
                  AND (? IS NULL OR p.id < ?)
                  AND (? = 0 OR p.media_uri IS NOT NULL)
                  AND (? = 0 OR p.author_id = ? OR EXISTS (
                    SELECT 1 FROM social_follows sf
                    WHERE sf.follower_id = ? AND sf.followed_id = p.author_id
                  ))
                  AND (? = '%%' OR lower(p.content) LIKE ?
                       OR lower(u.username) LIKE ? OR lower(u.display_name) LIKE ?)
                ORDER BY p.created_at DESC, p.id DESC
                LIMIT ?
                """, viewerId, viewerId, viewerId,
                fakeContentEnabled() ? 1 : 0,
                beforeId, beforeId,
                mediaOnly ? 1 : 0,
                followingOnly ? 1 : 0, viewerId, viewerId,
                pattern, pattern, pattern, pattern,
                Math.max(1, Math.min(500, requestedLimit)));
        return result.rows().stream().map(row -> new SharedPost(
                longValue(row, "id"),
                string(row, "display_name"),
                string(row, "username"),
                string(row, "content"),
                string(row, "created_at"),
                string(row, "media_uri"),
                nullableLong(row, "reply_to_id"),
                nullableLong(row, "quoted_post_id"),
                intValue(row, "likes"),
                intValue(row, "replies"),
                intValue(row, "reposts"),
                intValue(row, "view_count"),
                booleanValue(row, "liked_by_viewer"),
                booleanValue(row, "reposted_by_viewer"),
                booleanValue(row, "bookmarked_by_viewer"))).toList();
    }

    private List<SharedFollow> follows() {
        Result result = backend.execute("""
                SELECT follower.username AS follower_username,
                       followed.username AS followed_username
                FROM social_follows f
                JOIN app_users follower ON follower.id = f.follower_id
                JOIN app_users followed ON followed.id = f.followed_id
                WHERE follower.status = 'active' AND followed.status = 'active'
                  AND (? = 1 OR (follower.is_fake = 0 AND followed.is_fake = 0))
                """, fakeContentEnabled() ? 1 : 0);
        return result.rows().stream().map(row -> new SharedFollow(
                string(row, "follower_username"),
                string(row, "followed_username"))).toList();
    }

    private List<SharedNotification> notifications(Viewer viewer) {
        Result result = backend.execute("""
                SELECT n.id, recipient.username AS recipient_username,
                       actor.display_name AS actor_name, actor.username AS actor_username,
                       n.type, n.post_id, n.excerpt, n.created_at,
                       CASE WHEN n.read_at IS NULL THEN 0 ELSE 1 END AS is_read
                FROM social_notifications n
                JOIN app_users recipient ON recipient.id = n.recipient_id
                JOIN app_users actor ON actor.id = n.actor_id
                WHERE n.recipient_id = ?
                ORDER BY n.created_at DESC, n.id DESC
                LIMIT 200
                """, viewer.id());
        return result.rows().stream().map(row -> new SharedNotification(
                longValue(row, "id"),
                string(row, "recipient_username"),
                string(row, "actor_name"),
                string(row, "actor_username"),
                string(row, "type"),
                nullableLong(row, "post_id"),
                string(row, "excerpt"),
                string(row, "created_at"),
                booleanValue(row, "is_read"))).toList();
    }

    private Viewer requireViewer(String token) {
        if (token == null || token.isBlank()) throw new SecurityException("Sign in is required.");
        Result result = backend.execute("""
                SELECT u.id, u.username, u.display_name, u.role, u.status, s.expires_at
                FROM app_sessions s
                JOIN app_users u ON u.id = s.user_id
                WHERE s.token = ? AND u.status = 'active'
                LIMIT 1
                """, token.trim());
        if (result.rows().isEmpty()) throw new SecurityException("Your session is invalid.");
        Map<String, String> row = result.first();
        String expiresAt = string(row, "expires_at");
        if (!expiresAt.isBlank() && LocalDate.parse(expiresAt).isBefore(LocalDate.now())) {
            throw new SecurityException("Your session has expired.");
        }
        return new Viewer(
                intValue(row, "id"),
                string(row, "username"),
                string(row, "display_name"),
                "admin".equalsIgnoreCase(string(row, "role")));
    }

    private void validateReferencedPost(Long postId) {
        if (postId != null) requirePost(postId);
    }

    private Map<String, String> requirePost(long postId) {
        Result result = backend.execute(
                "SELECT id, author_id, content FROM social_posts WHERE id = ? LIMIT 1", postId);
        if (result.rows().isEmpty()) throw new IllegalArgumentException("The original post no longer exists.");
        return result.first();
    }

    private void notifyPostAuthor(
            long originalPostId,
            Viewer actor,
            String type,
            Long notificationPostId,
            String excerpt) {
        Map<String, String> original = requirePost(originalPostId);
        notifyUser(intValue(original, "author_id"), actor, type, notificationPostId, excerpt);
    }

    private void createMentionNotifications(Viewer actor, long postId, String content) {
        Matcher matcher = MENTION.matcher(content == null ? "" : content);
        java.util.Set<String> recipients = new java.util.LinkedHashSet<>();
        while (matcher.find()) {
            recipients.add(normalize(matcher.group(1)));
        }
        for (String username : recipients) {
            Result recipient = backend.execute(
                    "SELECT id FROM app_users WHERE username_key = ? LIMIT 1",
                    username);
            if (!recipient.rows().isEmpty()) {
                notifyUser(intValue(recipient.first(), "id"), actor, "MENTION", postId, content);
            }
        }
    }

    private void notifyUser(
            int recipientId,
            Viewer actor,
            String type,
            Long postId,
            String excerpt) {
        if (recipientId == actor.id()) return;
        backend.execute("""
                INSERT INTO social_notifications
                  (recipient_id, actor_id, type, post_id, excerpt, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, recipientId, actor.id(), type, postId, limited(excerpt, 140),
                Instant.now().toString());
    }

    private static String publicMediaUri(String uri) {
        if (uri == null || uri.isBlank()) return null;
        String value = uri.trim();
        return value.startsWith("https://")
                || value.startsWith("xclone-media:")
                || value.startsWith("/images/npc/media/")
                ? value : null;
    }

    private static String publicOrLocalProfileUri(String uri) {
        if (uri == null || uri.isBlank()) return null;
        String value = uri.trim();
        return value.startsWith("https://")
                || value.startsWith("xclone-media:")
                || value.startsWith("/images/npc/")
                ? value : null;
    }

    private boolean fakeContentEnabled() {
        Result result = backend.execute("""
                SELECT setting_value
                FROM app_settings
                WHERE setting_key = 'fake_content_enabled'
                LIMIT 1
                """);
        return !result.rows().isEmpty()
                && "true".equalsIgnoreCase(string(result.first(), "setting_value"));
    }

    private static boolean matchesImageSignature(byte[] bytes, String mime) {
        if ("image/png".equals(mime)) {
            return bytes.length >= 8
                    && (bytes[0] & 0xff) == 0x89
                    && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47;
        }
        if ("image/jpeg".equals(mime)) {
            return bytes.length >= 3
                    && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8
                    && (bytes[2] & 0xff) == 0xff;
        }
        return bytes.length >= 6
                && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F'
                && bytes[3] == '8' && (bytes[4] == '7' || bytes[4] == '9')
                && bytes[5] == 'a';
    }

    private static String limited(String value, int limit) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.length() <= limit ? trimmed : trimmed.substring(0, limit);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String string(Map<String, String> row, String name) {
        String value = row.get(name);
        return value == null ? "" : value;
    }

    private static long longValue(Map<String, String> row, String name) {
        return Long.parseLong(string(row, name));
    }

    private static int intValue(Map<String, String> row, String name) {
        String value = string(row, name);
        return value.isBlank() ? 0 : Integer.parseInt(value);
    }

    private static Long nullableLong(Map<String, String> row, String name) {
        String value = row.get(name);
        return value == null || value.isBlank() ? null : Long.parseLong(value);
    }

    private static boolean booleanValue(Map<String, String> row, String name) {
        String value = row.get(name);
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    public enum Interaction { LIKE, REPOST, BOOKMARK }

    private record Viewer(int id, String username, String displayName, boolean admin) {}
    private record NpcActor(
            int id,
            String username,
            NpcContentBank.Personality personality) {}
    private record NpcTarget(long postId, int authorId, String username) {}

    private record Result(List<Map<String, String>> rows, long affectedRows) {
        private Map<String, String> first() {
            if (rows.isEmpty()) throw new IllegalStateException("Database returned no row.");
            return rows.getFirst();
        }
    }

    private record Command(String sql, Object... arguments) {}

    private interface SqlBackend {
        Result execute(String sql, Object... arguments);
        List<Result> executeBatch(List<Command> commands);
        void initialize(List<String> schema);
    }

    private static final class TursoBackend implements SqlBackend {
        private final TursoDatabase database;

        private TursoBackend(String url, String token) {
            database = new TursoDatabase(url, token);
        }

        @Override
        public Result execute(String sql, Object... arguments) {
            TursoDatabase.QueryResult result = database.execute(sql, arguments);
            return new Result(result.rows(), result.affectedRows());
        }

        @Override
        public List<Result> executeBatch(List<Command> commands) {
            return database.executeBatch(commands.stream()
                    .map(command -> new TursoDatabase.SqlStatement(
                            command.sql(), command.arguments()))
                    .toList()).stream()
                    .map(result -> new Result(result.rows(), result.affectedRows()))
                    .toList();
        }

        @Override
        public void initialize(List<String> schema) {
            database.executeBatch(schema.stream().map(TursoDatabase.SqlStatement::new).toList());
        }
    }

    private static final class SQLiteBackend implements SqlBackend {
        private final String jdbcUrl;

        private SQLiteBackend() {
            String customDirectory = System.getProperty("xclone.server.data.dir");
            Path directory = customDirectory == null || customDirectory.isBlank()
                    ? Path.of(System.getProperty("user.home"), ".x-clone-server")
                    : Path.of(customDirectory);
            try {
                Files.createDirectories(directory);
                Class.forName("org.sqlite.JDBC");
            } catch (Exception exception) {
                throw new IllegalStateException("Could not initialize shared SQLite storage", exception);
            }
            jdbcUrl = "jdbc:sqlite:" + directory.resolve("xclone.db").toAbsolutePath();
        }

        @Override
        public synchronized Result execute(String sql, Object... arguments) {
            try (Connection connection = connection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int index = 0; index < arguments.length; index++) {
                    statement.setObject(index + 1, arguments[index]);
                }
                boolean hasRows = statement.execute();
                List<Map<String, String>> rows = hasRows
                        ? rows(statement.getResultSet())
                        : List.of();
                return new Result(rows, Math.max(0, statement.getUpdateCount()));
            } catch (Exception exception) {
                throw new IllegalStateException(
                        "Could not execute shared SQLite operation: " + exception.getMessage(),
                        exception);
            }
        }

        @Override
        public synchronized List<Result> executeBatch(List<Command> commands) {
            try (Connection connection = connection()) {
                connection.setAutoCommit(false);
                List<Result> results = new ArrayList<>();
                try {
                    for (Command command : commands) {
                        try (PreparedStatement statement =
                                     connection.prepareStatement(command.sql())) {
                            for (int index = 0; index < command.arguments().length; index++) {
                                statement.setObject(index + 1, command.arguments()[index]);
                            }
                            boolean hasRows = statement.execute();
                            results.add(new Result(
                                    hasRows ? rows(statement.getResultSet()) : List.of(),
                                    Math.max(0, statement.getUpdateCount())));
                        }
                    }
                    connection.commit();
                    return List.copyOf(results);
                } catch (Exception exception) {
                    connection.rollback();
                    throw exception;
                }
            } catch (Exception exception) {
                throw new IllegalStateException(
                        "Could not execute shared SQLite batch: " + exception.getMessage(),
                        exception);
            }
        }

        @Override
        public synchronized void initialize(List<String> schema) {
            try (Connection connection = connection();
                 Statement statement = connection.createStatement()) {
                for (String sql : schema) statement.execute(sql);
            } catch (Exception exception) {
                throw new IllegalStateException(
                        "Could not initialize shared SQLite schema: " + exception.getMessage(),
                        exception);
            }
        }

        private Connection connection() throws Exception {
            Connection connection = DriverManager.getConnection(jdbcUrl);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA busy_timeout = 5000");
            }
            return connection;
        }

        private List<Map<String, String>> rows(ResultSet resultSet) throws Exception {
            List<Map<String, String>> rows = new ArrayList<>();
            ResultSetMetaData metadata = resultSet.getMetaData();
            while (resultSet.next()) {
                Map<String, String> row = new LinkedHashMap<>();
                for (int index = 1; index <= metadata.getColumnCount(); index++) {
                    Object value = resultSet.getObject(index);
                    row.put(metadata.getColumnLabel(index), value == null ? null : value.toString());
                }
                rows.add(row);
            }
            return List.copyOf(rows);
        }
    }

    private static final List<String> SCHEMA = List.of(
            """
            CREATE TABLE IF NOT EXISTS app_settings (
              setting_key TEXT PRIMARY KEY,
              setting_value TEXT NOT NULL,
              updated_at TEXT NOT NULL,
              updated_by INTEGER REFERENCES app_users(id) ON DELETE SET NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS social_media (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              owner_id INTEGER NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
              mime_type TEXT NOT NULL,
              original_name TEXT,
              data_base64 TEXT NOT NULL,
              byte_size INTEGER NOT NULL,
              created_at TEXT NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS social_posts (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              author_id INTEGER NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
              content TEXT NOT NULL DEFAULT '',
              media_uri TEXT,
              reply_to_id INTEGER REFERENCES social_posts(id) ON DELETE CASCADE,
              quoted_post_id INTEGER REFERENCES social_posts(id) ON DELETE SET NULL,
              created_at TEXT NOT NULL,
              view_count INTEGER NOT NULL DEFAULT 0
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS social_likes (
              post_id INTEGER NOT NULL REFERENCES social_posts(id) ON DELETE CASCADE,
              user_id INTEGER NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
              created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (post_id, user_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS social_reposts (
              post_id INTEGER NOT NULL REFERENCES social_posts(id) ON DELETE CASCADE,
              user_id INTEGER NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
              created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (post_id, user_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS social_bookmarks (
              post_id INTEGER NOT NULL REFERENCES social_posts(id) ON DELETE CASCADE,
              user_id INTEGER NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
              created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (post_id, user_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS social_follows (
              follower_id INTEGER NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
              followed_id INTEGER NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
              created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (follower_id, followed_id),
              CHECK (follower_id <> followed_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS social_npc_accounts (
              user_id INTEGER PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
              persona_key TEXT NOT NULL UNIQUE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS social_npc_state (
              state_key TEXT PRIMARY KEY,
              state_value TEXT NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS social_notifications (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              recipient_id INTEGER NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
              actor_id INTEGER NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
              type TEXT NOT NULL,
              post_id INTEGER REFERENCES social_posts(id) ON DELETE CASCADE,
              excerpt TEXT,
              created_at TEXT NOT NULL,
              read_at TEXT
            )
            """,
            "CREATE INDEX IF NOT EXISTS social_posts_created_idx ON social_posts(created_at DESC)",
            "CREATE INDEX IF NOT EXISTS social_posts_author_idx ON social_posts(author_id, created_at DESC)",
            "CREATE INDEX IF NOT EXISTS social_media_owner_idx ON social_media(owner_id, created_at DESC)",
            "CREATE INDEX IF NOT EXISTS social_notifications_recipient_idx ON social_notifications(recipient_id, created_at DESC)"
    );
}
