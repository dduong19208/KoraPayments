package vn.korapayments.common.manager;

import vn.korapayments.KoraPayments;
import vn.korapayments.common.model.PaymentChannel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DatabaseManager {
    private static final long SCHEMA_RECHECK_INTERVAL_MILLIS = 60_000L;
    private static final int SQLITE_BUSY_TIMEOUT_MILLIS = 5_000;

    private final KoraPayments plugin;
    private final String url;
    private final Object schemaLock = new Object();

    private volatile boolean schemaReady;
    private volatile long lastSchemaCheckAt;

    public record TransactionRecord(long id, String player, long amount, long time,
                                    PaymentChannel channel, String provider, String detail) {}

    public static long getPeriodStartTime(String period) {
        String safePeriod = period == null ? "all" : period.trim().toLowerCase(Locale.ROOT);
        if (safePeriod.equals("all") || safePeriod.equals("lifetime") || safePeriod.equals("total")) {
            return 0L;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        switch (safePeriod) {
            case "today", "day", "daily" -> {
                return calendar.getTimeInMillis();
            }
            case "week", "weekly" -> {
                calendar.setFirstDayOfWeek(Calendar.MONDAY);
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                return calendar.getTimeInMillis();
            }
            case "month", "monthly" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                return calendar.getTimeInMillis();
            }
            case "year", "yearly" -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1);
                return calendar.getTimeInMillis();
            }
            default -> {
                return 0L;
            }
        }
    }

    public DatabaseManager(KoraPayments plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        this.url = "jdbc:sqlite:" + plugin.getDataFolder() + "/database.sqlite";

        try {
            ensureSchema(true);
        } catch (SQLException e) {
            logSqlException("database initialization", e);
        }
    }

    private Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout = " + SQLITE_BUSY_TIMEOUT_MILLIS);
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    private void ensureSchema(boolean force) throws SQLException {
        long now = System.currentTimeMillis();
        if (!force && schemaReady && now - lastSchemaCheckAt < SCHEMA_RECHECK_INTERVAL_MILLIS) {
            return;
        }

        synchronized (schemaLock) {
            now = System.currentTimeMillis();
            if (!force && schemaReady && now - lastSchemaCheckAt < SCHEMA_RECHECK_INTERVAL_MILLIS) {
                return;
            }

            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                throw new SQLException("Cannot create plugin data folder: " + plugin.getDataFolder().getAbsolutePath());
            }

            try (Connection conn = openConnection(); Statement stmt = conn.createStatement()) {
                applyOptionalPragma(stmt, "PRAGMA journal_mode = WAL");
                applyOptionalPragma(stmt, "PRAGMA synchronous = NORMAL");

                createTransactionsTable(stmt);
                migrateTransactionsTable(conn);

                stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_player_time ON transactions(player, time DESC)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_player_nocase_time ON transactions(player COLLATE NOCASE, time DESC)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_time ON transactions(time DESC)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_type_time ON transactions(type, time DESC)");

                stmt.execute("CREATE TABLE IF NOT EXISTS claimed (player TEXT, reward_id TEXT, period TEXT)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_claimed_lookup ON claimed(player, reward_id, period)");

                stmt.execute("CREATE TABLE IF NOT EXISTS claimed_milestones (player TEXT, milestone TEXT)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_claimed_milestones_lookup ON claimed_milestones(player, milestone)");

                stmt.execute("CREATE TABLE IF NOT EXISTS claimed_server_milestones (" +
                        "player TEXT NOT NULL, " +
                        "milestone TEXT NOT NULL, " +
                        "PRIMARY KEY(player, milestone)" +
                        ")");
                stmt.execute("CREATE TABLE IF NOT EXISTS reached_server_milestones (" +
                        "milestone TEXT PRIMARY KEY, " +
                        "reached_at INTEGER NOT NULL" +
                        ")");
                stmt.execute("CREATE TABLE IF NOT EXISTS bossbar_preferences (" +
                        "player TEXT PRIMARY KEY, " +
                        "enabled INTEGER NOT NULL DEFAULT 1" +
                        ")");

                schemaReady = true;
                lastSchemaCheckAt = now;
            } catch (SQLException e) {
                schemaReady = false;
                lastSchemaCheckAt = now;
                throw e;
            }
        }
    }

    private void applyOptionalPragma(Statement stmt, String sql) {
        try {
            stmt.execute(sql);
        } catch (SQLException ignored) {
            // Optional SQLite tuning must never prevent the plugin from starting.
        }
    }

    private void createTransactionsTable(Statement stmt) throws SQLException {
        stmt.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "player TEXT, " +
                "amount INTEGER NOT NULL DEFAULT 0, " +
                "time INTEGER NOT NULL DEFAULT 0, " +
                "type TEXT NOT NULL DEFAULT 'LEGACY', " +
                "provider TEXT NOT NULL DEFAULT '', " +
                "detail TEXT NOT NULL DEFAULT ''" +
                ")");
    }

    private void migrateTransactionsTable(Connection conn) throws SQLException {
        if (requiresTransactionTableRebuild(conn)) {
            rebuildTransactionsTable(conn);
            return;
        }

        ensureColumn(conn, "transactions", "type", "TEXT NOT NULL DEFAULT 'LEGACY'");
        ensureColumn(conn, "transactions", "provider", "TEXT NOT NULL DEFAULT ''");
        ensureColumn(conn, "transactions", "detail", "TEXT NOT NULL DEFAULT ''");
    }

    private boolean requiresTransactionTableRebuild(Connection conn) throws SQLException {
        return !hasColumn(conn, "transactions", "id")
                || !hasColumn(conn, "transactions", "player")
                || !hasColumn(conn, "transactions", "amount")
                || !hasColumn(conn, "transactions", "time");
    }

    private void rebuildTransactionsTable(Connection conn) throws SQLException {
        String legacyTable = "transactions_legacy_" + System.currentTimeMillis();
        try (Statement stmt = conn.createStatement()) {
            boolean hasPlayer = hasColumn(conn, "transactions", "player");
            boolean hasAmount = hasColumn(conn, "transactions", "amount");
            boolean hasTime = hasColumn(conn, "transactions", "time");
            boolean hasType = hasColumn(conn, "transactions", "type");
            boolean hasProvider = hasColumn(conn, "transactions", "provider");
            boolean hasDetail = hasColumn(conn, "transactions", "detail");

            stmt.execute("ALTER TABLE transactions RENAME TO " + legacyTable);
            createTransactionsTable(stmt);

            String playerExpr = hasPlayer ? "player" : "''";
            String amountExpr = hasAmount ? "COALESCE(amount, 0)" : "0";
            String timeExpr = hasTime ? "COALESCE(time, 0)" : "0";
            String typeExpr = hasType ? "COALESCE(type, 'LEGACY')" : "'LEGACY'";
            String providerExpr = hasProvider ? "COALESCE(provider, '')" : "''";
            String detailExpr = hasDetail ? "COALESCE(detail, '')" : "''";

            stmt.execute("INSERT INTO transactions(player, amount, time, type, provider, detail) " +
                    "SELECT " + playerExpr + ", " + amountExpr + ", " + timeExpr + ", " +
                    typeExpr + ", " + providerExpr + ", " + detailExpr + " FROM " + legacyTable);
            stmt.execute("DROP TABLE " + legacyTable);
        }
    }

    private void ensureColumn(Connection conn, String table, String column, String definition) throws SQLException {
        if (hasColumn(conn, table, column)) return;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private boolean hasColumn(Connection conn, String table, String column) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private <T> T executeSql(String action, T fallback, SqlSupplier<T> supplier) {
        try {
            ensureSchema(false);
            return supplier.get();
        } catch (SQLException first) {
            if (isSchemaProblem(first)) {
                try {
                    ensureSchema(true);
                    return supplier.get();
                } catch (SQLException retry) {
                    logSqlException(action, retry);
                    return fallback;
                }
            }

            logSqlException(action, first);
            return fallback;
        }
    }

    private void executeSql(String action, SqlRunnable runnable) {
        executeSql(action, null, () -> {
            runnable.run();
            return null;
        });
    }

    private boolean isSchemaProblem(SQLException exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("no such table")
                        || normalized.contains("no such column")
                        || normalized.contains("missing database")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    public long getRevenue(String period) {
        return getRevenueSince(getPeriodStartTime(period));
    }

    public long getRevenueSince(long startTime) {
        return executeSql("database operation", 0L, () -> {
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE time >= ?")) {
                ps.setLong(1, Math.max(0L, startTime));
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0L;
                }
            }
        });
    }

    public void addTransaction(String p, long amt) {
        addTransaction(p, amt, PaymentChannel.LEGACY, "", "");
    }

    public void addTransaction(String p, long amt, PaymentChannel channel, String provider, String detail) {
        PaymentChannel safeChannel = channel == null ? PaymentChannel.LEGACY : channel;
        executeSql("database operation", () -> {
            try (Connection c = openConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "INSERT INTO transactions(player, amount, time, type, provider, detail) VALUES(?,?,?,?,?,?)")) {
                ps.setString(1, p);
                ps.setLong(2, amt);
                ps.setLong(3, System.currentTimeMillis());
                ps.setString(4, safeChannel.storageKey());
                ps.setString(5, sanitize(provider));
                ps.setString(6, sanitize(detail));
                ps.executeUpdate();
            }
        });
    }

    private String sanitize(String value) {
        if (value == null) return "";
        String cleaned = value.replace('\n', ' ').replace('\r', ' ').trim();
        return cleaned.length() > 240 ? cleaned.substring(0, 240) : cleaned;
    }

    public Map<String, Long> getTop(long startTime, int limit) {
        Map<String, Long> top = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : getTopListSince(startTime, Math.max(1, limit), 0)) {
            top.put(entry.getKey(), entry.getValue());
        }
        return top;
    }

    public List<Map<String, Object>> getRawHistory(String p) {
        return executeSql("database operation", new ArrayList<>(), () -> {
            List<Map<String, Object>> hist = new ArrayList<>();
            try (Connection c = openConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT amount, time, type, provider, detail FROM transactions WHERE player = ? COLLATE NOCASE ORDER BY id DESC LIMIT 45")) {
                ps.setString(1, p);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("amount", rs.getLong("amount"));
                        data.put("time", rs.getLong("time"));
                        data.put("type", rs.getString("type"));
                        data.put("provider", rs.getString("provider"));
                        data.put("detail", rs.getString("detail"));
                        hist.add(data);
                    }
                }
            }
            return hist;
        });
    }

    public boolean hasClaimedMilestone(String player, String milestone) {
        return executeSql("database lookup", false, () -> {
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM claimed_milestones WHERE player = ? AND milestone = ?")) {
                ps.setString(1, player);
                ps.setString(2, milestone);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    public void saveClaimMilestone(String player, String milestone) {
        executeSql("database operation", () -> {
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement("INSERT INTO claimed_milestones (player, milestone) VALUES (?, ?)")) {
                ps.setString(1, player);
                ps.setString(2, milestone);
                ps.executeUpdate();
            }
        });
    }

    public boolean hasClaimedServerMilestone(String player, String milestone) {
        return executeSql("database lookup", false, () -> {
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM claimed_server_milestones WHERE player = ? AND milestone = ?")) {
                ps.setString(1, player);
                ps.setString(2, milestone);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    public void saveClaimServerMilestone(String player, String milestone) {
        executeSql("database operation", () -> {
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO claimed_server_milestones (player, milestone) VALUES (?, ?)")) {
                ps.setString(1, player);
                ps.setString(2, milestone);
                ps.executeUpdate();
            }
        });
    }

    public boolean hasReachedServerMilestone(String milestone) {
        return executeSql("database lookup", false, () -> {
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM reached_server_milestones WHERE milestone = ?")) {
                ps.setString(1, milestone);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    public long getReachedServerMilestoneAt(String milestone) {
        return executeSql("database lookup", 0L, () -> {
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT reached_at FROM reached_server_milestones WHERE milestone = ?")) {
                ps.setString(1, milestone);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Math.max(0L, rs.getLong("reached_at")) : 0L;
                }
            }
        });
    }

    public void saveReachedServerMilestone(String milestone) {
        executeSql("database operation", () -> {
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO reached_server_milestones (milestone, reached_at) VALUES (?, ?)")) {
                ps.setString(1, milestone);
                ps.setLong(2, System.currentTimeMillis());
                ps.executeUpdate();
            }
        });
    }

    public Set<String> getDisabledBossBarPlayers() {
        return executeSql("database operation", new HashSet<>(), () -> {
            Set<String> players = new HashSet<>();
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT player FROM bossbar_preferences WHERE enabled = 0")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String player = normalizePlayerKey(rs.getString("player"));
                        if (!player.isBlank()) {
                            players.add(player);
                        }
                    }
                }
            }
            return players;
        });
    }

    public void setBossBarEnabled(String player, boolean enabled) {
        String key = normalizePlayerKey(player);
        if (key.isBlank()) return;

        if (enabled) {
            executeSql("database operation", () -> {
                try (Connection conn = openConnection();
                     PreparedStatement ps = conn.prepareStatement("DELETE FROM bossbar_preferences WHERE player = ?")) {
                    ps.setString(1, key);
                    ps.executeUpdate();
                }
            });
            return;
        }

        executeSql("database operation", () -> {
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO bossbar_preferences (player, enabled) VALUES (?, 0)")) {
                ps.setString(1, key);
                ps.executeUpdate();
            }
        });
    }

    private String normalizePlayerKey(String player) {
        return player == null ? "" : player.trim().toLowerCase(Locale.ROOT);
    }

    public boolean hasClaimed(String p, String id, String period) {
        return executeSql("database lookup", false, () -> {
            try (Connection c = openConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT 1 FROM claimed WHERE player=? AND reward_id=? AND period=?")) {
                ps.setString(1, p);
                ps.setString(2, id);
                ps.setString(3, period);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    public void setClaimed(String p, String id, String period) {
        executeSql("database operation", () -> {
            try (Connection c = openConnection();
                 PreparedStatement ps = c.prepareStatement("INSERT INTO claimed VALUES(?,?,?)")) {
                ps.setString(1, p);
                ps.setString(2, id);
                ps.setString(3, period);
                ps.executeUpdate();
            }
        });
    }

    public long getTotalDonated(String player) {
        return executeSql("database operation", 0L, () -> {
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE player = ? COLLATE NOCASE")) {
                ps.setString(1, player);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0L;
                }
            }
        });
    }

    public long getServerTotalDonated() {
        return executeSql("database operation", 0L, () -> {
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(amount), 0) FROM transactions")) {
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0L;
                }
            }
        });
    }

    public List<Map.Entry<String, Long>> getTopDonators(int limit) {
        return getTopList("all", limit, 0);
    }

    public long getTransactionCount() {
        return executeSql("database operation", 0L, () -> {
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM transactions")) {
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0L;
                }
            }
        });
    }

    public long getPersonalRevenue(String playerName, long startTime) {
        return executeSql("database operation", 0L, () -> {
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE player = ? COLLATE NOCASE AND time >= ?")) {
                ps.setString(1, playerName);
                ps.setLong(2, Math.max(0L, startTime));
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0L;
                }
            }
        });
    }

    public void resetTopNap() {
        executeSql("database operation", () -> {
            try (Connection conn = openConnection(); Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM transactions");
            }
        });
    }

    public void resetMilestoneClaims() {
        executeSql("database operation", () -> {
            try (Connection conn = openConnection(); Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM claimed_milestones");
            }
        });
    }

    public void resetServerMilestoneClaims() {
        executeSql("database operation", () -> {
            try (Connection conn = openConnection(); Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM claimed_server_milestones");
            }
        });
    }

    public void resetReachedServerMilestones() {
        executeSql("database operation", () -> {
            try (Connection conn = openConnection(); Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM reached_server_milestones");
            }
        });
    }

    public List<Map.Entry<Long, Long>> getPlayerHistory(String playerName) {
        return executeSql("database operation", new ArrayList<>(), () -> {
            List<Map.Entry<Long, Long>> history = new ArrayList<>();
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT time, amount FROM transactions WHERE player = ? COLLATE NOCASE ORDER BY time DESC LIMIT 21")) {
                ps.setString(1, playerName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        history.add(new AbstractMap.SimpleEntry<>(rs.getLong("time"), rs.getLong("amount")));
                    }
                }
            }
            return history;
        });
    }

    public List<TransactionRecord> getPlayerHistoryDetailed(String playerName) {
        return getPlayerHistoryDetailed(playerName, 21);
    }

    public List<TransactionRecord> getPlayerHistoryDetailed(String playerName, int limit) {
        return executeSql("database operation", new ArrayList<>(), () -> {
            List<TransactionRecord> history = new ArrayList<>();
            int safeLimit = Math.max(1, Math.min(limit, 100));

            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT id, player, time, amount, type, provider, detail " +
                                 "FROM transactions WHERE player = ? COLLATE NOCASE " +
                                 "ORDER BY time DESC, id DESC LIMIT ?")) {
                ps.setString(1, playerName);
                ps.setInt(2, safeLimit);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        history.add(new TransactionRecord(
                                rs.getLong("id"),
                                rs.getString("player"),
                                rs.getLong("amount"),
                                rs.getLong("time"),
                                PaymentChannel.fromStored(rs.getString("type")),
                                rs.getString("provider"),
                                rs.getString("detail")
                        ));
                    }
                }
            }
            return history;
        });
    }

    public long getPlayerTransactionCount(String playerName) {
        return executeSql("database operation", 0L, () -> {
            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT COUNT(*) FROM transactions WHERE player = ? COLLATE NOCASE")) {
                ps.setString(1, playerName);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0L;
                }
            }
        });
    }

    public long getTotalDonatedIgnoreCase(String playerName) {
        return getTotalDonated(playerName);
    }

    public List<Map.Entry<String, Long>> getTopList(String mode) {
        return getTopList(mode, 21, 0);
    }

    public List<Map.Entry<String, Long>> getTopList(String mode, int limit, int offset) {
        return getTopListSince(getPeriodStartTime(mode), limit, offset);
    }

    public List<Map.Entry<String, Long>> getTopListSince(long startTime, int limit, int offset) {
        return executeSql("database operation", new ArrayList<>(), () -> {
            List<Map.Entry<String, Long>> topList = new ArrayList<>();

            int safeLimit = Math.max(1, Math.min(limit, 100));
            int safeOffset = Math.max(0, offset);
            long safeStartTime = Math.max(0L, startTime);

            boolean filteredByTime = safeStartTime > 0L;
            String sql = "SELECT " +
                    "COALESCE((" +
                    "SELECT t2.player FROM transactions t2 " +
                    "WHERE t2.player COLLATE NOCASE = t.player COLLATE NOCASE " +
                    "ORDER BY t2.time DESC, t2.id DESC LIMIT 1" +
                    "), t.player) AS display_player, " +
                    "COALESCE(SUM(t.amount), 0) AS total " +
                    "FROM transactions t " +
                    "WHERE t.player IS NOT NULL AND TRIM(t.player) <> '' " +
                    (filteredByTime ? "AND t.time >= ? " : "") +
                    "GROUP BY t.player COLLATE NOCASE " +
                    "ORDER BY total DESC, display_player COLLATE NOCASE ASC " +
                    "LIMIT ? OFFSET ?";

            try (Connection conn = openConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                int index = 1;
                if (filteredByTime) {
                    ps.setLong(index++, safeStartTime);
                }
                ps.setInt(index++, safeLimit);
                ps.setInt(index, safeOffset);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        topList.add(new AbstractMap.SimpleEntry<>(
                                rs.getString("display_player"),
                                rs.getLong("total")
                        ));
                    }
                }
            }

            return topList;
        });
    }

    private void logSqlException(String action, SQLException exception) {
        if (plugin != null) {
            plugin.logWarning("Database error during " + action + ": " + exception.getMessage(), exception);
        }
    }

    @FunctionalInterface
    private interface SqlSupplier<T> {
        T get() throws SQLException;
    }

    @FunctionalInterface
    private interface SqlRunnable {
        void run() throws SQLException;
    }
}
