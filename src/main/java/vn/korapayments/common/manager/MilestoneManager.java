package vn.korapayments.common.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import vn.korapayments.KoraPayments;
import vn.korapayments.common.scheduler.PlatformScheduler.ScheduledTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class MilestoneManager {
    public static final String PERSONAL_MILESTONES_PATH = "milestones";
    public static final String SERVER_MILESTONES_ROOT = "server-milestones";
    public static final String SERVER_MILESTONES_PATH = SERVER_MILESTONES_ROOT + ".milestones";
    public static final String SERVER_MILESTONES_ANTI_CLONE_PATH = SERVER_MILESTONES_ROOT + ".anti-clone";
    private static final long DEFAULT_SERVER_MILESTONE_MIN_PERSONAL_DONATED = 20_000L;

    private final KoraPayments plugin;
    private final AtomicLong cachedServerTotal = new AtomicLong(0L);
    private final Set<String> hiddenBossBarPlayers = ConcurrentHashMap.newKeySet();
    private BossBar bossBar;
    private ScheduledTask bossBarTask;

    public record ServerMilestoneClaimState(long serverTotal, long personalDonated,
                                            long minimumPersonalDonated, boolean reached,
                                            boolean claimed, boolean antiCloneProtected) {
        public boolean blockedByAntiClone() {
            return antiCloneProtected && personalDonated < minimumPersonalDonated;
        }

        public boolean claimable() {
            return reached && !claimed && !blockedByAntiClone();
        }

        public long remainingPersonalDonated() {
            return Math.max(0L, minimumPersonalDonated - personalDonated);
        }
    }

    public MilestoneManager(KoraPayments plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        stopBossBarTask();
        ensureDefaultServerMilestonesConfig();
        reloadBossBarPreferences();
        refreshServerTotalCache();
        synchronizeReachedServerMilestones(false);
        startBossBarTask();
    }

    public void shutdown() {
        stopBossBarTask();
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }

    public void handleSuccessfulPayment(Player player, long amount) {
        if (amount > 0L) {
            cachedServerTotal.addAndGet(amount);
        } else {
            refreshServerTotalCache();
        }

        if (player != null) {
            check(player);
        }

        synchronizeReachedServerMilestones(true);
        requestBossBarUpdate();
    }

    public void check(Player player) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(PERSONAL_MILESTONES_PATH);
        if (section == null) return;

        long total = plugin.getDatabaseManager().getTotalDonated(player.getName());

        for (Long target : getPersonalMilestones()) {
            if (total < target) {
                continue;
            }

            grantPersonalMilestone(player, target, true, total);
        }
    }

    public boolean claimPersonalMilestone(Player player, long milestone) {
        long total = plugin.getDatabaseManager().getTotalDonated(player.getName());
        return grantPersonalMilestone(player, milestone, false, total);
    }

    public boolean claimServerMilestone(Player player, long milestone) {
        if (!isServerMilestonesEnabled()) {
            player.sendMessage(plugin.tr("server-milestone.disabled"));
            return false;
        }

        ConfigurationSection section = plugin.getConfig().getConfigurationSection(SERVER_MILESTONES_PATH);
        if (section == null) {
            player.sendMessage(plugin.tr("server-milestone.no-config"));
            return false;
        }

        String milestoneKey = String.valueOf(milestone);
        ServerMilestoneClaimState claimState = getServerMilestoneClaimState(player, milestone);

        if (!claimState.reached()) {
            player.sendMessage(plugin.tr("server-milestone.not-reached"));
            return false;
        }

        if (claimState.claimed()) {
            player.sendMessage(plugin.tr("server-milestone.already-claimed"));
            return false;
        }

        if (claimState.blockedByAntiClone()) {
            player.sendMessage(plugin.tr("server-milestone.need-personal-topup",
                    "required", GUIUtils.formatMoney(claimState.minimumPersonalDonated()),
                    "current", GUIUtils.formatMoney(claimState.personalDonated()),
                    "remaining", GUIUtils.formatMoney(claimState.remainingPersonalDonated())));
            return false;
        }

        List<String> rewards = section.getStringList(milestoneKey + ".rewards");
        if (rewards.isEmpty()) {
            player.sendMessage(plugin.tr("server-milestone.reward-missing"));
            return false;
        }

        for (String command : rewards) {
            plugin.dispatchConsoleCommand(applyRewardPlaceholders(command, player, milestone));
        }

        plugin.getDatabaseManager().saveClaimServerMilestone(player.getName(), milestoneKey);
        player.sendMessage(plugin.tr("server-milestone.claimed", "amount", GUIUtils.formatMoney(milestone)));
        return true;
    }

    public ServerMilestoneClaimState getServerMilestoneClaimState(Player player, long milestone) {
        long personalDonated = player == null ? 0L : plugin.getDatabaseManager().getTotalDonatedIgnoreCase(player.getName());
        return getServerMilestoneClaimState(player, milestone, personalDonated);
    }

    public ServerMilestoneClaimState getServerMilestoneClaimState(Player player, long milestone, long personalDonated) {
        String milestoneKey = String.valueOf(milestone);
        long serverTotal = getCachedServerTotal();
        boolean reached = serverTotal >= milestone;
        boolean claimed = player != null && plugin.getDatabaseManager().hasClaimedServerMilestone(player.getName(), milestoneKey);
        long minimumPersonalDonated = getServerMilestoneMinimumPersonalDonated();
        boolean antiCloneProtected = reached && !claimed
                && isServerMilestoneAntiCloneProtected(player, milestoneKey, minimumPersonalDonated);

        return new ServerMilestoneClaimState(
                serverTotal,
                Math.max(0L, personalDonated),
                minimumPersonalDonated,
                reached,
                claimed,
                antiCloneProtected
        );
    }

    public boolean isServerMilestoneAntiCloneEnabled() {
        return plugin.getConfig().getBoolean(SERVER_MILESTONES_ANTI_CLONE_PATH + ".enabled", true)
                && getServerMilestoneMinimumPersonalDonated() > 0L;
    }

    public boolean isServerMilestoneAntiCloneNewPlayerOnly() {
        return plugin.getConfig().getBoolean(SERVER_MILESTONES_ANTI_CLONE_PATH + ".new-player-only", true);
    }

    public long getServerMilestoneMinimumPersonalDonated() {
        return Math.max(0L, plugin.getConfig().getLong(
                SERVER_MILESTONES_ANTI_CLONE_PATH + ".minimum-personal-donated",
                DEFAULT_SERVER_MILESTONE_MIN_PERSONAL_DONATED
        ));
    }

    private boolean isServerMilestoneAntiCloneProtected(Player player, String milestoneKey, long minimumPersonalDonated) {
        if (!isServerMilestoneAntiCloneEnabled() || minimumPersonalDonated <= 0L) {
            return false;
        }

        if (!isServerMilestoneAntiCloneNewPlayerOnly()) {
            return true;
        }

        if (player == null) {
            return true;
        }

        long firstPlayedAt = Math.max(0L, player.getFirstPlayed());
        long reachedAt = plugin.getDatabaseManager().getReachedServerMilestoneAt(milestoneKey);
        if (firstPlayedAt <= 0L || reachedAt <= 0L) {
            return true;
        }

        return firstPlayedAt > reachedAt;
    }

    private boolean grantPersonalMilestone(Player player, long milestone, boolean automatic, long total) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(PERSONAL_MILESTONES_PATH);
        if (section == null) return false;

        String milestoneKey = String.valueOf(milestone);

        if (total < milestone) {
            if (!automatic) player.sendMessage(plugin.tr("milestone.not-reached"));
            return false;
        }

        if (plugin.getDatabaseManager().hasClaimedMilestone(player.getName(), milestoneKey)) {
            if (!automatic) player.sendMessage(plugin.tr("milestone.already-claimed"));
            return false;
        }

        List<String> rewards = section.getStringList(milestoneKey + ".rewards");
        if (rewards.isEmpty() && !automatic) {
            player.sendMessage(plugin.tr("milestone.reward-missing"));
            return false;
        }

        for (String command : rewards) {
            plugin.dispatchConsoleCommand(applyRewardPlaceholders(command, player, milestone));
        }

        plugin.getDatabaseManager().saveClaimMilestone(player.getName(), milestoneKey);
        if (automatic) {
            player.sendMessage(plugin.tr("milestone.reached", "amount", GUIUtils.formatMoney(milestone)));
        } else {
            player.sendMessage(plugin.tr("milestone.claimed", "amount", GUIUtils.formatMoney(milestone)));
        }
        return true;
    }

    public List<Long> getPersonalMilestones() {
        return getMilestones(PERSONAL_MILESTONES_PATH);
    }

    public List<Long> getServerMilestones() {
        return getMilestones(SERVER_MILESTONES_PATH);
    }

    public boolean isServerMilestoneConfigured(long milestone) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(SERVER_MILESTONES_PATH);
        return section != null && section.isConfigurationSection(String.valueOf(milestone));
    }

    public long getCachedServerTotal() {
        return Math.max(0L, cachedServerTotal.get());
    }

    public long refreshServerTotalCache() {
        long total = Math.max(0L, plugin.getDatabaseManager().getServerTotalDonated());
        cachedServerTotal.set(total);
        return total;
    }

    public void resetServerMilestoneRuntimeData() {
        cachedServerTotal.set(0L);
        plugin.getDatabaseManager().resetServerMilestoneClaims();
        plugin.getDatabaseManager().resetReachedServerMilestones();
        requestBossBarUpdate();
    }

    public long getActiveServerMilestoneTarget() {
        return Math.max(0L, plugin.getConfig().getLong(SERVER_MILESTONES_ROOT + ".active-target", 0L));
    }

    public void setActiveServerMilestoneTarget(long target) {
        plugin.getConfig().set(SERVER_MILESTONES_ROOT + ".active-target", Math.max(0L, target));
        plugin.saveConfig();
        reload();
    }

    public long resolveBossBarTarget(long total) {
        long activeTarget = getActiveServerMilestoneTarget();
        if (activeTarget > 0L) {
            return activeTarget;
        }

        List<Long> milestones = getServerMilestones();
        if (milestones.isEmpty()) {
            return 0L;
        }

        for (Long milestone : milestones) {
            if (total < milestone) {
                return milestone;
            }
        }
        return milestones.get(milestones.size() - 1);
    }

    public boolean isServerMilestonesEnabled() {
        return plugin.getConfig().getBoolean(SERVER_MILESTONES_ROOT + ".enabled", true);
    }

    public boolean isBossBarGloballyEnabled() {
        return plugin.getConfig().getBoolean(SERVER_MILESTONES_ROOT + ".bossbar.enabled", true);
    }

    public void setBossBarGloballyEnabled(boolean enabled) {
        plugin.getConfig().set(SERVER_MILESTONES_ROOT + ".bossbar.enabled", enabled);
        plugin.saveConfig();
        reload();
    }

    public boolean isBossBarVisibleFor(Player player) {
        return player != null && isBossBarVisibleFor(player.getName());
    }

    public boolean isBossBarVisibleFor(String playerName) {
        String key = normalizePlayerKey(playerName);
        return !key.isBlank() && !hiddenBossBarPlayers.contains(key);
    }

    public void setBossBarVisibleFor(Player player, boolean visible) {
        if (player == null) return;
        setBossBarVisibleFor(player.getName(), visible);
    }

    public void setBossBarVisibleFor(String playerName, boolean visible) {
        String key = normalizePlayerKey(playerName);
        if (key.isBlank()) return;

        if (visible) {
            hiddenBossBarPlayers.remove(key);
        } else {
            hiddenBossBarPlayers.add(key);
        }

        plugin.getPlatformScheduler().runAsync(() -> plugin.getDatabaseManager().setBossBarEnabled(key, visible));
        requestBossBarUpdate();
    }

    public boolean toggleBossBarVisibility(Player player) {
        boolean visible = !isBossBarVisibleFor(player);
        setBossBarVisibleFor(player, visible);
        return visible;
    }

    private void reloadBossBarPreferences() {
        hiddenBossBarPlayers.clear();
        hiddenBossBarPlayers.addAll(plugin.getDatabaseManager().getDisabledBossBarPlayers());
    }

    private String normalizePlayerKey(String playerName) {
        return playerName == null ? "" : playerName.trim().toLowerCase(Locale.ROOT);
    }

    private void ensureDefaultServerMilestonesConfig() {
        boolean changed = false;

        if (!plugin.getConfig().contains(SERVER_MILESTONES_ROOT + ".enabled")) {
            plugin.getConfig().set(SERVER_MILESTONES_ROOT + ".enabled", true);
            changed = true;
        }
        if (!plugin.getConfig().contains(SERVER_MILESTONES_ROOT + ".active-target")) {
            plugin.getConfig().set(SERVER_MILESTONES_ROOT + ".active-target", 1000000L);
            changed = true;
        }
        if (!plugin.getConfig().contains(SERVER_MILESTONES_ANTI_CLONE_PATH + ".enabled")) {
            plugin.getConfig().set(SERVER_MILESTONES_ANTI_CLONE_PATH + ".enabled", true);
            changed = true;
        }
        if (!plugin.getConfig().contains(SERVER_MILESTONES_ANTI_CLONE_PATH + ".minimum-personal-donated")) {
            plugin.getConfig().set(SERVER_MILESTONES_ANTI_CLONE_PATH + ".minimum-personal-donated", DEFAULT_SERVER_MILESTONE_MIN_PERSONAL_DONATED);
            changed = true;
        }
        if (!plugin.getConfig().contains(SERVER_MILESTONES_ANTI_CLONE_PATH + ".new-player-only")) {
            plugin.getConfig().set(SERVER_MILESTONES_ANTI_CLONE_PATH + ".new-player-only", true);
            changed = true;
        }
        if (!plugin.getConfig().contains(SERVER_MILESTONES_ROOT + ".bossbar.enabled")) {
            plugin.getConfig().set(SERVER_MILESTONES_ROOT + ".bossbar.enabled", true);
            changed = true;
        }
        if (!plugin.getConfig().contains(SERVER_MILESTONES_ROOT + ".bossbar.color")) {
            plugin.getConfig().set(SERVER_MILESTONES_ROOT + ".bossbar.color", "BLUE");
            changed = true;
        }
        if (!plugin.getConfig().contains(SERVER_MILESTONES_ROOT + ".bossbar.style")) {
            plugin.getConfig().set(SERVER_MILESTONES_ROOT + ".bossbar.style", "SEGMENTED_10");
            changed = true;
        }
        if (!plugin.getConfig().contains(SERVER_MILESTONES_ROOT + ".bossbar.update-interval-ticks")) {
            plugin.getConfig().set(SERVER_MILESTONES_ROOT + ".bossbar.update-interval-ticks", 100L);
            changed = true;
        }
        if (!plugin.getConfig().contains(SERVER_MILESTONES_ROOT + ".bossbar.progress-title")) {
            plugin.getConfig().set(SERVER_MILESTONES_ROOT + ".bossbar.progress-title",
                    "&bMốc nạp server &8» &f{current}&7/&a{target} VNĐ &8(&e{percent}%&8)");
            changed = true;
        }
        if (!plugin.getConfig().contains(SERVER_MILESTONES_ROOT + ".bossbar.reached-title")) {
            plugin.getConfig().set(SERVER_MILESTONES_ROOT + ".bossbar.reached-title",
                    "&aMáy chủ đã đạt mốc nạp &e{target} VNĐ&a. &eDùng /mocnap server để nhận.");
            changed = true;
        }

        if (!plugin.getConfig().isConfigurationSection(SERVER_MILESTONES_PATH)) {
            setDefaultServerMilestone(1000000L, List.of(
                    "give %player% diamond 5"
            ));
            setDefaultServerMilestone(2000000L, List.of(
                    "give %player% diamond 10",
                    "eco give %player% 100000"
            ));
            setDefaultServerMilestone(3000000L, List.of(
                    "give %player% diamond 15",
                    "eco give %player% 200000"
            ));
            setDefaultServerMilestone(4000000L, List.of(
                    "give %player% diamond 20",
                    "eco give %player% 300000"
            ));
            setDefaultServerMilestone(5000000L, List.of(
                    "give %player% diamond 32",
                    "eco give %player% 500000"
            ));
            changed = true;
        }

        if (changed) {
            plugin.saveConfig();
        }
    }

    private void setDefaultServerMilestone(long milestone, List<String> rewards) {
        plugin.getConfig().set(SERVER_MILESTONES_PATH + "." + milestone + ".rewards", rewards);
    }

    private List<Long> getMilestones(String path) {
        List<Long> milestones = new ArrayList<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section == null) return milestones;

        for (String key : section.getKeys(false)) {
            try {
                milestones.add(Long.parseLong(key));
            } catch (NumberFormatException e) {
                plugin.logWarning("[MilestoneManager] Invalid milestone in config.yml at " + path + ": " + key);
            }
        }

        milestones.sort(Comparator.naturalOrder());
        return milestones;
    }

    private void synchronizeReachedServerMilestones(boolean announce) {
        if (!isServerMilestonesEnabled()) return;

        long total = getCachedServerTotal();
        for (Long milestone : getServerMilestones()) {
            if (total < milestone) {
                break;
            }

            String milestoneKey = String.valueOf(milestone);
            if (plugin.getDatabaseManager().hasReachedServerMilestone(milestoneKey)) {
                continue;
            }

            plugin.getDatabaseManager().saveReachedServerMilestone(milestoneKey);
            if (announce) {
                plugin.broadcast(plugin.tr("server-milestone.reached-broadcast", "amount", GUIUtils.formatMoney(milestone)));
            }
        }
    }

    private void startBossBarTask() {
        if (!isServerMilestonesEnabled() || !isBossBarGloballyEnabled()) {
            removeBossBar();
            return;
        }

        long interval = Math.max(20L, plugin.getConfig().getLong(SERVER_MILESTONES_ROOT + ".bossbar.update-interval-ticks", 100L));
        bossBarTask = plugin.getPlatformScheduler().runTimerAsync(this::requestBossBarUpdate, 20L, interval);
        requestBossBarUpdate();
    }

    private void stopBossBarTask() {
        if (bossBarTask != null) {
            bossBarTask.cancel();
            bossBarTask = null;
        }
    }

    private void requestBossBarUpdate() {
        plugin.getPlatformScheduler().runGlobal(this::updateBossBar);
    }

    private void updateBossBar() {
        if (!isServerMilestonesEnabled() || !isBossBarGloballyEnabled()) {
            removeBossBar();
            return;
        }

        long total = getCachedServerTotal();
        long target = resolveBossBarTarget(total);
        if (target <= 0L) {
            removeBossBar();
            return;
        }

        double progress = Math.max(0.0D, Math.min(1.0D, total / (double) target));
        boolean reached = total >= target;

        if (bossBar == null) {
            bossBar = Bukkit.createBossBar("", parseBossBarColor(), parseBossBarStyle());
        } else {
            bossBar.setColor(parseBossBarColor());
            bossBar.setStyle(parseBossBarStyle());
        }

        bossBar.setTitle(formatBossBarTitle(total, target, progress, reached));
        bossBar.setProgress(progress);

        for (Player player : new ArrayList<>(bossBar.getPlayers())) {
            if (!player.isOnline() || !isBossBarVisibleFor(player)) {
                bossBar.removePlayer(player);
            }
        }

        Set<Player> currentBossBarPlayers = new HashSet<>(bossBar.getPlayers());
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isBossBarVisibleFor(player) && !currentBossBarPlayers.contains(player)) {
                bossBar.addPlayer(player);
            }
        }
    }

    private void removeBossBar() {
        plugin.getPlatformScheduler().runGlobal(() -> {
            if (bossBar != null) {
                bossBar.removeAll();
                bossBar = null;
            }
        });
    }

    private String formatBossBarTitle(long total, long target, double progress, boolean reached) {
        String path = reached
                ? SERVER_MILESTONES_ROOT + ".bossbar.reached-title"
                : SERVER_MILESTONES_ROOT + ".bossbar.progress-title";
        String fallback = reached
                ? "&aMáy chủ đã đạt mốc nạp &e{target} VNĐ&a. &eDùng /mocnap server để nhận."
                : "&bMốc nạp server &8» &f{current}&7/&a{target} VNĐ";
        String title = normalizeMilestoneCommandHints(plugin.getConfig().getString(path, fallback));
        int percent = (int) Math.floor(progress * 100.0D);
        return ChatColor.translateAlternateColorCodes('&', title
                .replace("{current}", GUIUtils.formatMoney(total))
                .replace("{total}", GUIUtils.formatMoney(total))
                .replace("{target}", GUIUtils.formatMoney(target))
                .replace("{amount}", GUIUtils.formatMoney(target))
                .replace("{percent}", String.valueOf(percent)));
    }

    private String normalizeMilestoneCommandHints(String message) {
        if (message == null || message.isBlank()) return message;
        return message.replace("/mocnap rewards", "/mocnap server");
    }

    private BarColor parseBossBarColor() {
        String raw = plugin.getConfig().getString(SERVER_MILESTONES_ROOT + ".bossbar.color", "BLUE");
        try {
            return BarColor.valueOf(raw == null ? "BLUE" : raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BarColor.BLUE;
        }
    }

    private BarStyle parseBossBarStyle() {
        String raw = plugin.getConfig().getString(SERVER_MILESTONES_ROOT + ".bossbar.style", "SEGMENTED_10");
        try {
            return BarStyle.valueOf(raw == null ? "SEGMENTED_10" : raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BarStyle.SEGMENTED_10;
        }
    }

    private String applyRewardPlaceholders(String command, Player player, long milestone) {
        String playerName = player == null ? "" : player.getName();
        String total = String.valueOf(getCachedServerTotal());
        String formattedTotal = GUIUtils.formatMoney(getCachedServerTotal());
        String formattedMilestone = GUIUtils.formatMoney(milestone);
        return command
                .replace("%player%", playerName)
                .replace("{player}", playerName)
                .replace("%amount%", String.valueOf(milestone))
                .replace("%target%", String.valueOf(milestone))
                .replace("%milestone%", String.valueOf(milestone))
                .replace("%server_total%", total)
                .replace("%total%", total)
                .replace("%amount_formatted%", formattedMilestone)
                .replace("%target_formatted%", formattedMilestone)
                .replace("%server_total_formatted%", formattedTotal)
                .replace("%total_formatted%", formattedTotal);
    }
}
