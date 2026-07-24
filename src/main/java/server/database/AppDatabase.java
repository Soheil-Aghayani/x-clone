package server.database;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.google.gson.Gson;
import shared.models.Session;
import shared.models.User;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * SQLite-compatible account persistence.
 *
 * <p>Render uses the hosted Turso database when TURSO_DATABASE_URL and
 * TURSO_AUTH_TOKEN are configured. Local development uses an SQLite file and
 * automatically imports the old accounts.json database once.</p>
 */
public final class AppDatabase {
    private static final AppDatabase INSTANCE = new AppDatabase();
    private final AccountStore store;

    private AppDatabase() {
        TursoConfig config = TursoConfig.fromEnvironment();
        store = config == null ? new SQLiteStore() : new TursoStore(config);
        System.out.println("Account database: " + store.description());
    }

    public static AppDatabase getInstance() { return INSTANCE; }
    public void verifyReady() { store.verifyReady(); }

    public User register(String displayName, String username, String email, String password) {
        return store.register(displayName, username, email, password);
    }

    public User authenticate(String username, String password) {
        return store.authenticate(username, password);
    }

    public Session createSession(User user) {
        return store.createSession(user);
    }

    private interface AccountStore {
        User register(String displayName, String username, String email, String password);
        User authenticate(String username, String password);
        Session createSession(User user);
        void verifyReady();
        String description();
    }

    private static final class SQLiteStore implements AccountStore {
        private final Path directory;
        private final Path databaseFile;
        private final String jdbcUrl;

        private SQLiteStore() {
            String customDirectory = System.getProperty("xclone.server.data.dir");
            directory = customDirectory == null || customDirectory.isBlank()
                    ? Path.of(System.getProperty("user.home"), ".x-clone-server")
                    : Path.of(customDirectory);
            databaseFile = directory.resolve("xclone.db");
            try {
                Files.createDirectories(directory);
                Class.forName("org.sqlite.JDBC");
            } catch (Exception exception) {
                throw new IllegalStateException("Could not initialize local SQLite", exception);
            }
            jdbcUrl = "jdbc:sqlite:" + databaseFile.toAbsolutePath();
            initializeSchema();
            migrateLegacyJson();
        }

        @Override
        public synchronized User register(
                String displayName, String username, String email, String password) {
            String sql = """
                    INSERT INTO app_users
                      (username, username_key, email, email_key, display_name, bio, created_at, password_hash)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
            try (Connection connection = connection();
                 PreparedStatement statement =
                         connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bindRegistration(statement, displayName, username, email, hash);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("SQLite returned no user id");
                    return newUser(keys.getInt(1), username, email, displayName);
                }
            } catch (SQLException exception) {
                if (isUniqueViolation(exception)) return null;
                throw databaseFailure("register account", exception);
            }
        }

        @Override
        public synchronized User authenticate(String username, String password) {
            String sql = """
                    SELECT id, username, email, display_name, bio, avatar_url, banner_url, created_at,
                           location, website, birth_date, professional, password_hash
                    FROM app_users
                    WHERE username_key = ? OR email_key = ?
                    LIMIT 1
                    """;
            try (Connection connection = connection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                String normalized = normalize(username);
                statement.setString(1, normalized);
                statement.setString(2, normalized);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) return null;
                    if (!BCrypt.verifyer()
                            .verify(password.toCharArray(), result.getString("password_hash")).verified) {
                        return null;
                    }
                    return user(result);
                }
            } catch (SQLException exception) {
                throw databaseFailure("authenticate account", exception);
            }
        }

        @Override
        public synchronized Session createSession(User user) {
            String sql = "INSERT INTO app_sessions (user_id, token, expires_at) VALUES (?, ?, ?)";
            String token = UUID.randomUUID().toString();
            LocalDate expiry = LocalDate.now().plusDays(30);
            try (Connection connection = connection();
                 PreparedStatement statement =
                         connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, user.getId());
                statement.setString(2, token);
                statement.setString(3, expiry.toString());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("SQLite returned no session id");
                    return new Session(keys.getInt(1), user.getId(), token, expiry.toString());
                }
            } catch (SQLException exception) {
                throw databaseFailure("create session", exception);
            }
        }

        @Override
        public void verifyReady() {
            try (Connection connection = connection();
                 Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1");
            } catch (SQLException exception) {
                throw databaseFailure("verify SQLite database", exception);
            }
        }

        @Override
        public String description() { return "SQLite (" + databaseFile + ")"; }

        private Connection connection() throws SQLException {
            Connection connection = DriverManager.getConnection(jdbcUrl);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA busy_timeout = 5000");
            }
            return connection;
        }

        private void initializeSchema() {
            try (Connection connection = connection();
                 Statement statement = connection.createStatement()) {
                statement.execute(USERS_SCHEMA);
                statement.execute(SESSIONS_SCHEMA);
                statement.execute(SESSIONS_INDEX);
            } catch (SQLException exception) {
                throw databaseFailure("initialize SQLite schema", exception);
            }
        }

        private void migrateLegacyJson() {
            Path legacyFile = directory.resolve("accounts.json");
            if (!Files.isRegularFile(legacyFile) || countUsers() > 0) return;
            try {
                LegacyState legacy = new Gson().fromJson(Files.readString(legacyFile), LegacyState.class);
                if (legacy == null || legacy.accounts == null || legacy.accounts.isEmpty()) return;
                String sql = """
                        INSERT OR IGNORE INTO app_users
                          (id, username, username_key, email, email_key, display_name, bio, avatar_url,
                           banner_url, created_at, location, website, birth_date, professional, password_hash)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """;
                try (Connection connection = connection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    for (LegacyAccount account : legacy.accounts) {
                        if (account == null || account.user == null || account.passwordHash == null) continue;
                        User user = account.user;
                        statement.setInt(1, user.getId());
                        statement.setString(2, user.getUsername());
                        statement.setString(3, normalize(user.getUsername()));
                        statement.setString(4, user.getEmail());
                        statement.setString(5, normalize(user.getEmail()));
                        statement.setString(6, user.getDisplayName());
                        statement.setString(7, user.getBio());
                        statement.setString(8, user.getAvatarUrl());
                        statement.setString(9, user.getBannerUrl());
                        statement.setString(10, user.getCreatedAt());
                        statement.setString(11, user.getLocation());
                        statement.setString(12, user.getWebsite());
                        statement.setString(13, user.getBirthDate());
                        statement.setInt(14, user.isProfessional() ? 1 : 0);
                        statement.setString(15, account.passwordHash);
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
                System.out.println("Imported legacy accounts.json into SQLite.");
            } catch (Exception exception) {
                throw new IllegalStateException("Could not migrate accounts.json to SQLite", exception);
            }
        }

        private int countUsers() {
            try (Connection connection = connection();
                 Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM app_users")) {
                return result.next() ? result.getInt(1) : 0;
            } catch (SQLException exception) {
                throw databaseFailure("count SQLite users", exception);
            }
        }

        private void bindRegistration(
                PreparedStatement statement,
                String displayName,
                String username,
                String email,
                String hash) throws SQLException {
            statement.setString(1, username.trim());
            statement.setString(2, normalize(username));
            statement.setString(3, email.trim());
            statement.setString(4, normalize(email));
            statement.setString(5, displayName.trim());
            statement.setString(6, "Hello X!");
            statement.setString(7, LocalDate.now().toString());
            statement.setString(8, hash);
        }

        private User user(ResultSet result) throws SQLException {
            User user = new User(
                    result.getInt("id"),
                    result.getString("username"),
                    result.getString("email"),
                    result.getString("display_name"),
                    result.getString("bio"),
                    result.getString("avatar_url"),
                    result.getString("banner_url"),
                    result.getString("created_at")
            );
            user.setLocation(result.getString("location"));
            user.setWebsite(result.getString("website"));
            user.setBirthDate(result.getString("birth_date"));
            user.setProfessional(result.getInt("professional") != 0);
            return user;
        }
    }

    private static final class TursoStore implements AccountStore {
        private final TursoDatabase database;

        private TursoStore(TursoConfig config) {
            database = new TursoDatabase(config.url(), config.token());
            initializeSchema();
        }

        @Override
        public User register(String displayName, String username, String email, String password) {
            String sql = """
                    INSERT INTO app_users
                      (username, username_key, email, email_key, display_name, bio, created_at, password_hash)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
            try {
                TursoDatabase.QueryResult result = database.execute(
                        sql,
                        username.trim(),
                        normalize(username),
                        email.trim(),
                        normalize(email),
                        displayName.trim(),
                        "Hello X!",
                        LocalDate.now().toString(),
                        hash
                );
                if (result.lastInsertRowId() == null) {
                    throw new IllegalStateException("Turso returned no user id");
                }
                return newUser(result.lastInsertRowId().intValue(), username, email, displayName);
            } catch (TursoDatabase.DatabaseException exception) {
                if (exception.isUniqueViolation()) return null;
                throw exception;
            }
        }

        @Override
        public User authenticate(String username, String password) {
            String sql = """
                    SELECT id, username, email, display_name, bio, avatar_url, banner_url, created_at,
                           location, website, birth_date, professional, password_hash
                    FROM app_users
                    WHERE username_key = ? OR email_key = ?
                    LIMIT 1
                    """;
            String normalized = normalize(username);
            TursoDatabase.QueryResult result = database.execute(sql, normalized, normalized);
            if (result.rows().isEmpty()) return null;
            Map<String, String> row = result.rows().getFirst();
            String hash = row.get("password_hash");
            if (hash == null
                    || !BCrypt.verifyer().verify(password.toCharArray(), hash).verified) {
                return null;
            }
            return user(row);
        }

        @Override
        public Session createSession(User user) {
            String token = UUID.randomUUID().toString();
            LocalDate expiry = LocalDate.now().plusDays(30);
            TursoDatabase.QueryResult result = database.execute(
                    "INSERT INTO app_sessions (user_id, token, expires_at) VALUES (?, ?, ?)",
                    user.getId(), token, expiry.toString());
            if (result.lastInsertRowId() == null) {
                throw new IllegalStateException("Turso returned no session id");
            }
            return new Session(result.lastInsertRowId().intValue(), user.getId(), token, expiry.toString());
        }

        @Override
        public void verifyReady() {
            database.healthCheck();
            database.execute("SELECT 1");
        }

        @Override
        public String description() { return "Turso/libSQL (" + database.safeHost() + ")"; }

        private void initializeSchema() {
            database.executeBatch(List.of(
                    new TursoDatabase.SqlStatement(USERS_SCHEMA),
                    new TursoDatabase.SqlStatement(SESSIONS_SCHEMA),
                    new TursoDatabase.SqlStatement(SESSIONS_INDEX)
            ));
        }

        private User user(Map<String, String> row) {
            User user = new User(
                    Integer.parseInt(row.get("id")),
                    row.get("username"),
                    row.get("email"),
                    row.get("display_name"),
                    row.get("bio"),
                    row.get("avatar_url"),
                    row.get("banner_url"),
                    row.get("created_at")
            );
            user.setLocation(row.get("location"));
            user.setWebsite(row.get("website"));
            user.setBirthDate(row.get("birth_date"));
            user.setProfessional("1".equals(row.get("professional"))
                    || "true".equalsIgnoreCase(row.get("professional")));
            return user;
        }
    }

    private record TursoConfig(String url, String token) {
        private static TursoConfig fromEnvironment() {
            String url = firstNonBlank(
                    System.getProperty("xclone.turso.url"),
                    System.getenv("TURSO_DATABASE_URL")
            );
            String token = firstNonBlank(
                    System.getProperty("xclone.turso.token"),
                    System.getenv("TURSO_AUTH_TOKEN")
            );
            if (url == null && token == null) return null;
            if (url == null) throw new IllegalStateException("TURSO_DATABASE_URL is required");
            if (token == null) throw new IllegalStateException("TURSO_AUTH_TOKEN is required");
            return new TursoConfig(url, token);
        }
    }

    private static User newUser(int id, String username, String email, String displayName) {
        return new User(id, username.trim(), email.trim(), displayName.trim(),
                "Hello X!", null, null, LocalDate.now().toString());
    }

    private static boolean isUniqueViolation(SQLException exception) {
        return exception.getErrorCode() == 19
                || (exception.getMessage() != null
                && exception.getMessage().toLowerCase(Locale.ROOT).contains("unique"));
    }

    private static IllegalStateException databaseFailure(String action, SQLException exception) {
        return new IllegalStateException("Could not " + action + ": " + exception.getMessage(), exception);
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

    private static final String USERS_SCHEMA = """
            CREATE TABLE IF NOT EXISTS app_users (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              username TEXT NOT NULL,
              username_key TEXT NOT NULL UNIQUE,
              email TEXT NOT NULL,
              email_key TEXT NOT NULL UNIQUE,
              display_name TEXT NOT NULL,
              bio TEXT,
              avatar_url TEXT,
              banner_url TEXT,
              created_at TEXT NOT NULL,
              location TEXT,
              website TEXT,
              birth_date TEXT,
              professional INTEGER NOT NULL DEFAULT 0,
              password_hash TEXT NOT NULL
            )
            """;

    private static final String SESSIONS_SCHEMA = """
            CREATE TABLE IF NOT EXISTS app_sessions (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              user_id INTEGER NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
              token TEXT NOT NULL UNIQUE,
              expires_at TEXT NOT NULL,
              created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """;

    private static final String SESSIONS_INDEX =
            "CREATE INDEX IF NOT EXISTS app_sessions_user_id_idx ON app_sessions(user_id)";

    private static final class LegacyState {
        List<LegacyAccount> accounts;
    }

    private static final class LegacyAccount {
        User user;
        String passwordHash;
    }
}
