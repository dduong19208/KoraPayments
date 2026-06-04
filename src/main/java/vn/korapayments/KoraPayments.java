package vn.korapayments;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.ChatColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;
import vn.korapayments.common.lang.LanguageManager;
import vn.korapayments.common.manager.AdminGUIManager;
import vn.korapayments.common.manager.DatabaseManager;
import vn.korapayments.common.manager.LogManager;
import vn.korapayments.common.manager.MilestoneGUIManager;
import vn.korapayments.common.manager.MilestoneManager;
import vn.korapayments.common.manager.PaymentGUIManager;
import vn.korapayments.common.manager.TransactionWebhookManager;
import vn.korapayments.common.model.PaymentChannel;
import vn.korapayments.common.scheduler.PlatformScheduler;
import vn.korapayments.napbank.commands.AdminCommand;
import vn.korapayments.napbank.commands.BankCommand;
import vn.korapayments.napbank.commands.HistoryCommand;
import vn.korapayments.napbank.commands.MilestoneCommand;
import vn.korapayments.napbank.commands.PublicCommand;
import vn.korapayments.napbank.commands.TopCommand;
import vn.korapayments.napbank.listeners.MenuListener;
import vn.korapayments.napbank.manager.BankPaymentManager;
import vn.korapayments.napcard.commands.CancelCardCommand;
import vn.korapayments.napcard.commands.ConfirmCardCommand;
import vn.korapayments.napcard.commands.NapTheCommand;
import vn.korapayments.napcard.listeners.CardListener;
import vn.korapayments.napcard.manager.CardRateManager;
import vn.korapayments.napcard.manager.CardSessionManager;
import vn.korapayments.napcard.models.CardRequest;
import vn.korapayments.napcard.tasks.CheckPendingTask;
import vn.korapayments.store.StoreCommand;
import vn.korapayments.store.StoreManager;
import vn.korapayments.placeholder.KPExpansion;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class KoraPayments extends JavaPlugin {
    private static final List<String> PLACEHOLDER_IDENTIFIERS = List.of("kp", "korapayments");
    private static final String SUPPORT_DISCORD = "lz.dy.dg";
    private static final int BSTATS_PLUGIN_ID = 31772;

    private static KoraPayments instance;

    private PlatformScheduler platformScheduler;
    private LanguageManager languageManager;
    private DatabaseManager databaseManager;
    private MilestoneManager milestoneManager;
    private BankPaymentManager bankPaymentManager;
    private LogManager logManager;
    private AdminGUIManager adminGUIManager;
    private CardSessionManager cardSessionManager;
    private CardListener cardListener;
    private CardRateManager cardRateManager;
    private MilestoneGUIManager milestoneGuiManager;
    private PaymentGUIManager paymentGuiManager;
    private TransactionWebhookManager transactionWebhookManager;
    private StoreManager storeManager;
    private final List<KPExpansion> placeholderExpansions = new ArrayList<>();
    private static final int WEBHOOK_CONNECT_TIMEOUT_MS = 5000;
    private static final int WEBHOOK_READ_TIMEOUT_MS = 10000;

    private final Map<UUID, CardRequest> pendingCards = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.platformScheduler = new PlatformScheduler(this);
        this.logManager = new LogManager(this);
        this.languageManager = new LanguageManager(this);

        this.databaseManager = new DatabaseManager(this);
        this.milestoneManager = new MilestoneManager(this);
        this.bankPaymentManager = new BankPaymentManager(this);
        this.adminGUIManager = new AdminGUIManager(this);
        this.cardSessionManager = new CardSessionManager();
        this.cardListener = new CardListener(this);
        this.cardRateManager = new CardRateManager(this);
        this.milestoneGuiManager = new MilestoneGUIManager(this);
        this.paymentGuiManager = new PaymentGUIManager(this);
        this.transactionWebhookManager = new TransactionWebhookManager(this);
        this.storeManager = new StoreManager(this);

        reloadPlugin();
        startMetrics();

        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(this.cardListener, this);
        registerCompatibilityListeners();

        registerCommands();
        registerPlaceholderExpansion();
        platformScheduler.runGlobalLater(this::registerPlaceholderExpansion, 40L);

        platformScheduler.runTimerAsync(new CheckPendingTask(this), 1200L, 1200L);

        printStartupBanner();
    }

    @Override
    public void onDisable() {
        unregisterPlaceholderExpansions();
        if (milestoneManager != null) {
            milestoneManager.shutdown();
        }
        if (storeManager != null) {
            storeManager.shutdown();
        }
        if (logManager != null) {
            logManager.shutdown();
        }
    }

    private void printStartupBanner() {
        if (!getConfig().getBoolean("startup-banner.enabled", true)) {
            return;
        }

        String version = getDescription().getVersion();
        String author = resolvePluginAuthor();
        String serverName = getServer().getName();
        String serverVersion = getServer().getBukkitVersion();

        boolean placeholderApiEnabled = getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
        boolean metricsEnabled = getConfig().getBoolean("metrics.enabled", true);

        final String reset = "\u001B[0m";
        final String cyan = "\u001B[96m";
        final String blue = "\u001B[36m";
        final String yellow = "\u001B[93m";
        final String white = "\u001B[97m";
        final String gray = "\u001B[90m";
        final String green = "\u001B[92m";
        final String red = "\u001B[91m";

        getLogger().info("");
        getLogger().info(cyan + "  _  __                 ____                                  _       " + reset);
        getLogger().info(cyan + " | |/ /___  _ __ __ _  |  _ \\ __ _ _   _ _ __ ___   ___ _ __ | |_ ___ " + reset);
        getLogger().info(blue + " | ' // _ \\| '__/ _` | | |_) / _` | | | | '_ ` _ \\ / _ \\ '_ \\| __/ __|" + reset);
        getLogger().info(blue + " | . \\ (_) | | | (_| | |  __/ (_| | |_| | | | | | |  __/ | | | |_\\__ \\" + reset);
        getLogger().info(cyan + " |_|\\_\\___/|_|  \\__,_| |_|   \\__,_|\\__, |_| |_| |_|\\___|_| |_|\\__|___/" + reset);
        getLogger().info(cyan + "                                    |___/                              " + reset);
        getLogger().info("");
        getLogger().info("                     " + yellow + "KoraPayments" + reset);
        getLogger().info("                  " + gray + "Modern Payment Gateway" + reset);
        getLogger().info("");
        getLogger().info(yellow + " Status   " + green + "Loaded successfully" + reset);
        getLogger().info(yellow + " Version  " + white + version + reset);
        getLogger().info(yellow + " Author   " + white + author + reset);
        getLogger().info(yellow + " Discord  " + white + SUPPORT_DISCORD + reset);
        getLogger().info(yellow + " Platform " + white + serverName + " / " + serverVersion + reset);
        getLogger().info(yellow + " Modules  " + white + "Bank, Card, Milestones, Promotion, Discord Store" + reset);
        getLogger().info(yellow + " Hooks    " + white + "PlaceholderAPI: "
                + statusText(placeholderApiEnabled, green, red, reset)
                + white + " | bStats: "
                + statusText(metricsEnabled, green, red, reset) + reset);
        getLogger().info(yellow + " License  " + white + "Free edition - no license required" + reset);
        getLogger().info("");
    }

    private String statusText(boolean enabled, String success, String warning, String reset) {
        return enabled
                ? success + "Enabled" + reset
                : warning + "Standby" + reset;
    }

    private String resolvePluginAuthor() {
        List<String> authors = getDescription().getAuthors();
        if (authors != null && !authors.isEmpty()) {
            return String.join(", ", authors);
        }
        return "DuyDuong";
    }

    private String resolveServerBrand() {
        try {
            String name = Bukkit.getName();
            String version = Bukkit.getVersion();
            if (name == null || name.isBlank()) {
                return version == null || version.isBlank() ? "Bukkit" : version;
            }
            return name;
        } catch (Throwable ignored) {
            return "Bukkit";
        }
    }

    private void startMetrics() {
        if (!getConfig().getBoolean("metrics.enabled", true)) {
            logDebug("bStats metrics are disabled in config.yml.");
            return;
        }

        try {
            new Metrics(this, BSTATS_PLUGIN_ID);
            logInfo("bStats metrics started with plugin ID " + BSTATS_PLUGIN_ID + ".");
        } catch (Throwable throwable) {
            logWarning("Could not start bStats metrics: " + throwable.getMessage());
            logDebug("bStats startup failed.", throwable);
        }
    }

    private void registerCommands() {
        HistoryCommand historyCommand = new HistoryCommand(this);
        MilestoneCommand milestoneCommand = new MilestoneCommand(this);

        if (getCommand("bank") != null) {
            BankCommand bankCommand = new BankCommand(this);
            getCommand("bank").setExecutor(bankCommand);
            getCommand("bank").setTabCompleter(bankCommand);
        }

        if (getCommand("kora-admin") != null) {
            AdminCommand adminCommand = new AdminCommand(this, historyCommand, milestoneCommand);
            getCommand("kora-admin").setExecutor(adminCommand);
            getCommand("kora-admin").setTabCompleter(adminCommand);
        }

        if (getCommand("korapayments") != null) {
            PublicCommand publicCommand = new PublicCommand(this);
            getCommand("korapayments").setExecutor(publicCommand);
            getCommand("korapayments").setTabCompleter(publicCommand);
        }

        if (getCommand("topnap") != null) {
            TopCommand topCommand = new TopCommand(this);
            getCommand("topnap").setExecutor(topCommand);
            getCommand("topnap").setTabCompleter(topCommand);
        }

        if (getCommand("lichsunap") != null) getCommand("lichsunap").setExecutor(historyCommand);
        if (getCommand("mocnap") != null) {
            getCommand("mocnap").setExecutor(milestoneCommand);
            getCommand("mocnap").setTabCompleter(milestoneCommand);
        }

        if (getCommand("napthe") != null) {
            NapTheCommand napTheExecutor = new NapTheCommand(this);
            getCommand("napthe").setExecutor(napTheExecutor);
            getCommand("napthe").setTabCompleter(napTheExecutor);
        }

        if (getCommand("confirmcard") != null) getCommand("confirmcard").setExecutor(new ConfirmCardCommand(this));
        if (getCommand("cancelcard") != null) getCommand("cancelcard").setExecutor(new CancelCardCommand(this));

        if (getCommand("taokenhbanhang") != null) {
            StoreCommand storeCommand = new StoreCommand(this);
            getCommand("taokenhbanhang").setExecutor(storeCommand);
            getCommand("taokenhbanhang").setTabCompleter(storeCommand);
        }
    }

    private void registerCompatibilityListeners() {
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPluginEnable(PluginEnableEvent event) {
                if (event.getPlugin() != null
                        && "PlaceholderAPI".equalsIgnoreCase(event.getPlugin().getName())) {
                    registerPlaceholderExpansion();
                }
            }
        }, this);
    }

    private synchronized void registerPlaceholderExpansion() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            logDebug("PlaceholderAPI is not enabled; KoraPayments placeholders will be registered when PlaceholderAPI starts.");
            return;
        }

        List<String> registeredIdentifiers = new ArrayList<>();
        for (String identifier : PLACEHOLDER_IDENTIFIERS) {
            if (isPlaceholderIdentifierTracked(identifier)) {
                continue;
            }

            try {
                KPExpansion expansion = new KPExpansion(this, identifier);
                if (!expansion.register()) {
                    boolean removedStaleExpansion = tryUnregisterExistingKoraExpansion(identifier);
                    if (removedStaleExpansion) {
                        expansion = new KPExpansion(this, identifier);
                    }

                    if (!removedStaleExpansion || !expansion.register()) {
                        Object existingExpansion = findPlaceholderExpansion(identifier);
                        String owner = existingExpansion == null
                                ? "unknown expansion"
                                : existingExpansion.getClass().getName();
                        logWarning("PlaceholderAPI identifier '%" + identifier + "_%' is already registered by " + owner + ". "
                                + "KoraPayments kept running. If placeholder %" + identifier + "_*% is shown as raw text, restart the server or use the other alias.");
                        continue;
                    }
                }

                placeholderExpansions.add(expansion);
                registeredIdentifiers.add("%" + identifier + "_*");
            } catch (Throwable throwable) {
                logSevere("Failed to register PlaceholderAPI expansion '%" + identifier + "_%'.", throwable);
            }
        }

        if (!registeredIdentifiers.isEmpty()) {
            logInfo("Registered PlaceholderAPI expansions: " + String.join(", ", registeredIdentifiers));
        }
    }

    private boolean isPlaceholderIdentifierTracked(String identifier) {
        for (KPExpansion expansion : placeholderExpansions) {
            if (expansion != null && expansion.getIdentifier().equalsIgnoreCase(identifier)) {
                return true;
            }
        }
        return false;
    }

    private boolean tryUnregisterExistingKoraExpansion(String identifier) {
        Object existingExpansion = findPlaceholderExpansion(identifier);
        if (existingExpansion == null || !isKoraPlaceholderExpansion(existingExpansion)) {
            return false;
        }

        if (invokeUnregister(existingExpansion)) {
            logDebug("Removed stale KoraPayments PlaceholderAPI expansion '%" + identifier + "_%'.");
            return true;
        }

        Object manager = getPlaceholderLocalExpansionManager();
        if (manager != null && invokeManagerUnregister(manager, existingExpansion, identifier)) {
            logDebug("Removed stale KoraPayments PlaceholderAPI expansion '%" + identifier + "_%' through LocalExpansionManager.");
            return true;
        }

        return false;
    }

    private boolean isKoraPlaceholderExpansion(Object expansion) {
        if (expansion == null) {
            return false;
        }

        String className = expansion.getClass().getName();
        return className.equals(KPExpansion.class.getName()) || className.startsWith("vn.korapayments.");
    }

    private Object findPlaceholderExpansion(String identifier) {
        Object manager = getPlaceholderLocalExpansionManager();
        if (manager == null || identifier == null || identifier.isBlank()) {
            return null;
        }

        for (Method method : manager.getClass().getMethods()) {
            if (!method.getName().equals("getExpansion") || method.getParameterCount() != 1) {
                continue;
            }
            if (!String.class.equals(method.getParameterTypes()[0])) {
                continue;
            }
            try {
                Object result = unwrapOptional(method.invoke(manager, identifier));
                if (result != null) {
                    return result;
                }
            } catch (Throwable ignored) {
            }
        }

        Object expansions = invokeNoArg(manager, "getExpansions");
        if (expansions instanceof Map<?, ?> map) {
            Object direct = map.get(identifier);
            if (direct != null) {
                return unwrapOptional(direct);
            }
            for (Object value : map.values()) {
                Object unwrapped = unwrapOptional(value);
                if (identifier.equalsIgnoreCase(getExpansionIdentifier(unwrapped))) {
                    return unwrapped;
                }
            }
        } else if (expansions instanceof Collection<?> collection) {
            for (Object value : collection) {
                Object unwrapped = unwrapOptional(value);
                if (identifier.equalsIgnoreCase(getExpansionIdentifier(unwrapped))) {
                    return unwrapped;
                }
            }
        }

        return null;
    }

    private Object getPlaceholderLocalExpansionManager() {
        Plugin placeholderApi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (placeholderApi == null) {
            return null;
        }

        Object manager = invokeNoArg(placeholderApi, "getLocalExpansionManager");
        if (manager != null) {
            return manager;
        }

        try {
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPIPlugin");
            Object instance = invokeNoArg(papiClass, "getInstance");
            return invokeNoArg(instance, "getLocalExpansionManager");
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object invokeNoArg(Object target, String methodName) {
        if (target == null || methodName == null) {
            return null;
        }

        try {
            Method method = target instanceof Class<?> clazz
                    ? clazz.getMethod(methodName)
                    : target.getClass().getMethod(methodName);
            return method.invoke(target instanceof Class<?> ? null : target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object unwrapOptional(Object value) {
        if (value instanceof Optional<?> optional) {
            return optional.orElse(null);
        }
        return value;
    }

    private String getExpansionIdentifier(Object expansion) {
        Object identifier = invokeNoArg(expansion, "getIdentifier");
        return identifier == null ? "" : String.valueOf(identifier);
    }

    private boolean invokeUnregister(Object expansion) {
        if (expansion == null) {
            return false;
        }

        try {
            Method method = expansion.getClass().getMethod("unregister");
            Object result = method.invoke(expansion);
            return !(result instanceof Boolean bool) || bool;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean invokeManagerUnregister(Object manager, Object expansion, String identifier) {
        if (manager == null) {
            return false;
        }

        for (Method method : manager.getClass().getMethods()) {
            if (!method.getName().toLowerCase(Locale.ROOT).contains("unregister") || method.getParameterCount() != 1) {
                continue;
            }

            Class<?> parameterType = method.getParameterTypes()[0];
            Object argument = null;
            if (expansion != null && parameterType.isInstance(expansion)) {
                argument = expansion;
            } else if (String.class.equals(parameterType)) {
                argument = identifier;
            }

            if (argument == null) {
                continue;
            }

            try {
                Object result = method.invoke(manager, argument);
                return !(result instanceof Boolean bool) || bool;
            } catch (Throwable ignored) {
            }
        }

        return false;
    }

    private void unregisterPlaceholderExpansions() {
        if (placeholderExpansions.isEmpty()) {
            return;
        }

        for (KPExpansion expansion : new ArrayList<>(placeholderExpansions)) {
            try {
                expansion.getClass().getMethod("unregister").invoke(expansion);
            } catch (NoSuchMethodException ignored) {
                // Older PlaceholderAPI versions may not expose unregister(); the JVM will clear it on shutdown.
            } catch (Throwable throwable) {
                logDebug("Could not unregister PlaceholderAPI expansion '%" + expansion.getIdentifier() + "_%'.", throwable);
            }
        }
        placeholderExpansions.clear();
    }

    private void clearPlaceholderCaches() {
        for (KPExpansion expansion : new ArrayList<>(placeholderExpansions)) {
            try {
                expansion.clearCaches();
            } catch (Throwable throwable) {
                logDebug("Could not clear PlaceholderAPI cache for '%" + expansion.getIdentifier() + "_%'.", throwable);
            }
        }
    }

    public void processSuccessPayment(String playerName, long amount) {
        processSuccessPayment(playerName, amount, PaymentChannel.LEGACY, "", "");
    }

    public void processSuccessPayment(String playerName, long amount, PaymentChannel channel, String provider, String detail) {
        PaymentChannel safeChannel = channel == null ? PaymentChannel.LEGACY : channel;
        getDatabaseManager().addTransaction(playerName, amount, safeChannel, provider, detail);
        clearPlaceholderCaches();

        Player player = Bukkit.getPlayer(playerName);
        getMilestoneManager().handleSuccessfulPayment(player, amount);

        getLogManager().payment(playerName + " topup " + amount + " " + trPlain("general.currency")
                + " via " + safeChannel.storageKey() + (provider == null || provider.isBlank() ? "" : " (" + provider + ")"));

        if (transactionWebhookManager != null) {
            transactionWebhookManager.sendSuccess(safeChannel, playerName, amount, provider, detail);
        }
    }

    public void processManualTopup(CommandSender sender, Player target, long amount) {
        if (amount <= 0) {
            sender.sendMessage(tr("admin.amount-positive"));
            return;
        }

        int finalPoints = calculateFinalPoints(amount, PaymentChannel.MANUAL);
        int bonusPoints = calculateBonusPoints(amount, PaymentChannel.MANUAL);
        String commandTemplate = getConfig().getString("reward-command", "p give {player} {points}");
        String command = commandTemplate
                .replace("{player}", target.getName())
                .replace("{points}", String.valueOf(finalPoints));

        platformScheduler.runPlayer(target, () -> {
            dispatchConsoleCommand(command);
            sendActionBar(target, tr("manual.success-actionbar"));
            broadcast(tr("manual.broadcast", "player", target.getName(), "amount", formatMoney(amount)));
            target.sendMessage(tr("manual.target-amount", "amount", formatMoney(amount)));
            target.sendMessage(tr("manual.target-points", "points", formatMoney(finalPoints)));
            if (bonusPoints > 0) {
                target.sendMessage(tr("promotion.bonus-channel", "bonus", getPromotionPercent(PaymentChannel.MANUAL), "type", trPlain(PaymentChannel.MANUAL.languageKey())));
            }
            spawnSuccessFirework(target.getLocation());
            target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            processSuccessPayment(target.getName(), amount, PaymentChannel.MANUAL, "admin", "Admin: " + sender.getName());
            getLogManager().manual(sender.getName() + " manual topup for " + target.getName()
                    + ", amount " + amount + " " + trPlain("general.currency") + ", points " + finalPoints + ".");
            sender.sendMessage(tr("manual.sender-success", "player", target.getName(), "amount", formatMoney(amount)));
            sender.sendMessage(tr("manual.sender-points", "points", formatMoney(finalPoints)));
        });
    }

    public void handleCardResponse(Player player, int status, String message, CardRequest card) {
        switch (status) {
            case 1 -> giveCardReward(player, card);
            case 2 -> {
                player.sendMessage(tr("card.wrong-amount"));
                player.sendMessage(tr("card.wrong-amount-note"));
                logCardTransaction(player.getName() + " (WRONG_AMOUNT)", card.getAmount(), 0, 0);
            }
            case 99 -> player.sendMessage(tr("card.pending"));
            default -> player.sendMessage(tr("card.error-prefix", "message", message == null ? "Unknown" : message));
        }
    }

    public void giveCardReward(Player player, CardRequest card) {
        int amount = card.getAmount();
        double taxRate = isCardTaxesEnabled() ? cardRateManager.getDiscountRate(card.getTelco(), amount) : 0;
        int netAmount = (int) (amount * (1.0 - (taxRate / 100.0)));
        int ratio = getCardRewardRatio();
        int basePoints = netAmount / ratio;
        int bonusPoints = calculatePromotionBonusFromBasePoints(basePoints, PaymentChannel.CARD);
        int points = basePoints + bonusPoints;

        logCardTransaction(player.getName(), amount, netAmount, points);

        platformScheduler.runPlayer(player, () -> {
            for (String commandTemplate : getCardRewardCommands()) {
                String command = commandTemplate
                        .replace("%player%", player.getName())
                        .replace("%amount%", String.valueOf(amount))
                        .replace("%net_amount%", String.valueOf(netAmount))
                        .replace("%points%", String.valueOf(points));
                dispatchConsoleCommand(command);
            }

            sendActionBar(player, tr("card.success-actionbar"));
            broadcast(tr("card.success-broadcast", "player", player.getName(), "amount", formatMoney(amount)));
            player.sendMessage(tr("card.success-received", "telco", card.getTelco(), "amount", formatMoney(amount)));
            player.sendMessage(tr("card.success-points", "points", formatMoney(points)));
            if (bonusPoints > 0) {
                player.sendMessage(tr("promotion.bonus-channel", "bonus", getPromotionPercent(PaymentChannel.CARD), "type", trPlain(PaymentChannel.CARD.languageKey())));
            }

            spawnSuccessFirework(player.getLocation());
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            processSuccessPayment(player.getName(), amount, PaymentChannel.CARD, getCardProviderName(),
                    "Telco: " + card.getTelco() + " | Net: " + netAmount + " | Request: " + card.getRequestId());
        });
    }

    public void spawnSuccessFirework(Location location) {
        if (location == null || location.getWorld() == null) return;
        Firework firework = (Firework) location.getWorld().spawnEntity(location.clone().add(0, 1, 0), EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .withColor(Color.YELLOW, Color.BLUE, Color.AQUA)
                .withFade(Color.WHITE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .trail(true)
                .flicker(true)
                .build());
        meta.setPower(1);
        firework.setFireworkMeta(meta);
    }

    private void logCardTransaction(String playerName, int amount, int netAmount, int points) {
        getLogManager().cardTransaction(playerName, amount, netAmount, points);
    }

    public void resetTopNap(CommandSender sender) {
        getDatabaseManager().resetTopNap();
        clearPlaceholderCaches();
        if (milestoneManager != null) {
            milestoneManager.resetServerMilestoneRuntimeData();
        }
        sender.sendMessage(tr("milestone.reset"));
        broadcast(tr("milestone.reset-broadcast"));
        getLogManager().admin(sender.getName() + " reset topnap data.");
    }

    public boolean reloadPlugin() {
        saveDefaultConfig();
        reloadConfig();
        if (logManager != null) logManager.reloadSettings();
        if (languageManager != null) languageManager.reload();
        if (milestoneManager != null) milestoneManager.reload();

        if (cardRateManager != null && isCardTaxesEnabled()) {
            cardRateManager.invalidateCache();
        }

        if (storeManager != null) {
            storeManager.reload();
        }

        return true;
    }

    public boolean ensureFeatureAvailable(CommandSender sender) {
        return true;
    }

    public boolean isCardTaxesEnabled() {
        return getBooleanCompat("napthe.taxes.enabled", "card2k.taxes.enabled", true);
    }

    public int getCardRewardRatio() {
        int ratio = getIntCompat("napthe.rewards.ratio", "card2k.rewards.ratio", 1);
        return Math.max(1, ratio);
    }

    public List<String> getCardRewardCommands() {
        List<String> commands = getConfig().getStringList("napthe.rewards.commands");
        if (!commands.isEmpty()) return commands;
        return getConfig().getStringList("card2k.rewards.commands");
    }

    public String getCardProviderName() {
        String provider = getConfig().getString("napthe.provider", "card2k");
        return provider == null || provider.isBlank() ? "card2k" : provider.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private boolean getBooleanCompat(String primaryPath, String legacyPath, boolean fallback) {
        if (getConfig().contains(primaryPath)) return getConfig().getBoolean(primaryPath, fallback);
        return getConfig().getBoolean(legacyPath, fallback);
    }

    private int getIntCompat(String primaryPath, String legacyPath, int fallback) {
        if (getConfig().contains(primaryPath)) return getConfig().getInt(primaryPath, fallback);
        return getConfig().getInt(legacyPath, fallback);
    }

    public int calculateFinalPoints(long amount) {
        return calculateFinalPoints(amount, PaymentChannel.BANK);
    }

    public int calculateFinalPoints(long amount, PaymentChannel channel) {
        int basePoints = calculateBankBasePoints(amount);
        return basePoints + calculatePromotionBonusFromBasePoints(basePoints, channel);
    }

    public int calculateBankBasePoints(long amount) {
        int rate = Math.max(0, getConfig().getInt("conversion-rate", 1));
        long points = (amount / 1000L) * rate;
        return points > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, points);
    }

    public int calculateBonusPoints(long amount) {
        return calculateBonusPoints(amount, PaymentChannel.BANK);
    }

    public int calculateBonusPoints(long amount, PaymentChannel channel) {
        return calculatePromotionBonusFromBasePoints(calculateBankBasePoints(amount), channel);
    }

    public int calculatePromotionBonusFromBasePoints(int basePoints, PaymentChannel channel) {
        if (basePoints <= 0 || !isPromotionActive(channel)) return 0;
        int percent = getPromotionPercent(channel);
        return Math.max(0, basePoints * percent / 100);
    }

    public int applyPromotionToPoints(int basePoints, PaymentChannel channel) {
        return basePoints + calculatePromotionBonusFromBasePoints(basePoints, channel);
    }

    public boolean isPromotionActive() {
        return isPromotionActive(PaymentChannel.BANK);
    }

    public boolean isPromotionActive(PaymentChannel channel) {
        String path = resolvePromotionPath(channel);
        if (!getConfig().getBoolean(path + ".enabled", false)) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            sdf.setLenient(false);
            String rawEndDate = getConfig().getString(path + ".end-date", "01/01/2000 00:00:00");
            return new Date().before(sdf.parse(rawEndDate));
        } catch (Exception e) {
            logWarning("Invalid " + path + ".end-date. Expected format: dd/MM/yyyy HH:mm:ss");
            return false;
        }
    }

    public int getPromotionPercent(PaymentChannel channel) {
        return Math.max(0, getConfig().getInt(resolvePromotionPath(channel) + ".percent", 0));
    }

    private String resolvePromotionPath(PaymentChannel channel) {
        PaymentChannel safeChannel = channel == null ? PaymentChannel.BANK : channel;
        if (safeChannel == PaymentChannel.CARD) {
            return "napthe.promotion";
        }

        if (getConfig().contains("napbank.promotion")) {
            return "napbank.promotion";
        }

        if (getConfig().contains("promotion")) {
            return "promotion";
        }

        return safeChannel.promotionPath();
    }

    public void dispatchConsoleCommand(String command) {
        if (command == null || command.isBlank()) return;
        platformScheduler.runGlobal(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
    }

    public void broadcast(String message) {
        platformScheduler.runGlobal(() -> Bukkit.broadcastMessage(message));
    }

    public void sendActionBar(Player player, String message) {
        if (player == null) return;
        platformScheduler.runPlayer(player, () -> player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message)));
    }

    public String tr(String key, Object... replacements) {
        if (languageManager == null) return key;
        return normalizeUserFacingCommandHints(languageManager.text(key, replacements));
    }

    public List<String> trList(String key, Object... replacements) {
        if (languageManager == null) return List.of(key);
        List<String> lines = languageManager.list(key, replacements);
        for (int i = 0; i < lines.size(); i++) {
            lines.set(i, normalizeUserFacingCommandHints(lines.get(i)));
        }
        return lines;
    }

    private String normalizeUserFacingCommandHints(String message) {
        if (message == null || message.isBlank()) return message;
        return message
                .replace("/mocnap rewards", "/mocnap server")
                .replace("/mocnap " + org.bukkit.ChatColor.COLOR_CHAR + "7",
                        "/mocnap canhan " + org.bukkit.ChatColor.COLOR_CHAR + "7");
    }

    public String trPlain(String key, Object... replacements) {
        return stripColor(tr(key, replacements));
    }

    public String stripColor(String message) {
        return org.bukkit.ChatColor.stripColor(message == null ? "" : message);
    }

    public String formatMoney(long amount) {
        return String.format("%,d", amount);
    }


    public boolean isDebugMode() {
        return logManager != null && logManager.isDebugEnabled();
    }

    public void logDebug(String message) {
        logDebug(message, null);
    }

    public void logDebug(String message, Throwable throwable) {
        if (logManager != null) {
            logManager.debug(message, throwable);
        }
    }

    public void logInfo(String message) {
        if (logManager != null) {
            logManager.info(message);
        } else {
            getLogger().info(message);
        }
    }

    public void logWarning(String message) {
        logWarning(message, null);
    }

    public void logWarning(String message, Throwable throwable) {
        if (logManager != null) {
            logManager.warning(message, throwable);
        } else if (throwable == null) {
            getLogger().warning(message);
        } else {
            getLogger().log(Level.WARNING, message, throwable);
        }
    }

    public void logSevere(String message) {
        logSevere(message, null);
    }

    public void logSevere(String message, Throwable throwable) {
        if (logManager != null) {
            logManager.severe(message, throwable);
        } else if (throwable == null) {
            getLogger().severe(message);
        } else {
            getLogger().log(Level.SEVERE, message, throwable);
        }
    }

    public java.net.HttpURLConnection openJsonPostConnection(String url) throws IOException {
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        connection.setConnectTimeout(WEBHOOK_CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(WEBHOOK_READ_TIMEOUT_MS);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        return connection;
    }

    private String jsonEscape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    public static KoraPayments getInstance() { return instance; }
    public PlatformScheduler getPlatformScheduler() { return platformScheduler; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public MilestoneManager getMilestoneManager() { return milestoneManager; }
    public BankPaymentManager getBankPaymentManager() { return bankPaymentManager; }
    public LogManager getLogManager() { return logManager; }
    public AdminGUIManager getAdminGUIManager() { return adminGUIManager; }
    public CardSessionManager getCardSessionManager() { return cardSessionManager; }
    public CardListener getCardListener() { return cardListener; }
    public CardRateManager getCardRateManager() { return cardRateManager; }
    public TransactionWebhookManager getTransactionWebhookManager() { return transactionWebhookManager; }
    public StoreManager getStoreManager() { return storeManager; }
    public MilestoneGUIManager getMilestoneGuiManager() { return milestoneGuiManager; }
    public PaymentGUIManager getPaymentGuiManager() { return paymentGuiManager; }
    public Map<UUID, CardRequest> getPendingCards() { return pendingCards; }
}
