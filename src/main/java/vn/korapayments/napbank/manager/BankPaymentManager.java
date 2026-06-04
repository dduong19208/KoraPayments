package vn.korapayments.napbank.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import vn.korapayments.KoraPayments;
import vn.korapayments.common.model.PaymentChannel;
import vn.korapayments.common.scheduler.PlatformScheduler.ScheduledTask;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class BankPaymentManager {
    private static final String PROVIDER_PAYOS = "payos";
    private static final String PROVIDER_SEPAY = "sepay";
    private static final String PAYOS_PAYMENT_REQUESTS_API = "https://api-merchant.payos.vn/v2/payment-requests";
    private static final String SEPAY_TRANSACTIONS_API = "https://my.sepay.vn/userapi/transactions/list";
    private static final String QUICK_CHART_QR_API = "https://quickchart.io/qr";

    private final KoraPayments plugin;
    private final Map<UUID, ScheduledTask> activeTasks = new ConcurrentHashMap<>();
    private final Map<Long, TransactionState> pendingStates = new ConcurrentHashMap<>();

    // BỘ NHỚ ĐỆM BẢO MẬT: Lưu trữ ID giao dịch SePay đã xử lý để chống replay attack.
    private final Set<String> processedSePayTransactions = ConcurrentHashMap.newKeySet();

    public BankPaymentManager(KoraPayments plugin) {
        this.plugin = plugin;
    }

    private record TransactionState(String provider, String description, long createdAtMillis) {}

    public record PaymentOrder(String provider,
                               long orderCode,
                               long amount,
                               String payerName,
                               String bankCode,
                               String bankName,
                               String accountNumber,
                               String accountName,
                               String description,
                               String qrCode,
                               String qrImageUrl,
                               String checkoutUrl) {
        public JsonObject toJsonObject() {
            JsonObject data = new JsonObject();
            data.addProperty("provider", safe(provider));
            data.addProperty("orderCode", orderCode);
            data.addProperty("amount", amount);
            data.addProperty("payerName", safe(payerName));
            data.addProperty("bin", safe(bankCode));
            data.addProperty("bankName", safe(bankName));
            data.addProperty("accountNumber", safe(accountNumber));
            data.addProperty("accountName", safe(accountName));
            data.addProperty("description", safe(description));
            data.addProperty("qrCode", safe(qrCode));
            data.addProperty("qrImageUrl", safe(qrImageUrl));
            data.addProperty("checkoutUrl", safe(checkoutUrl));
            data.addProperty("qrImage", true);
            return data;
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }
    }

    public void createTransaction(Player player, long amount, TransactionCallback callback) {
        createPaymentOrder(player.getName(), amount, new PaymentOrderCallback() {
            @Override
            public void onSuccess(PaymentOrder order) {
                callback.onSuccess(order.toJsonObject(), order.orderCode());
            }

            @Override
            public void onFailure(String message) {
                sendCreateTransactionError(player, message);
            }
        });
    }

    public void createPaymentOrder(String payerName, long amount, PaymentOrderCallback callback) {
        String safePayerName = sanitizePayerName(payerName);
        if (PROVIDER_SEPAY.equals(getProvider())) {
            createSePayPaymentOrder(safePayerName, amount, callback);
        } else {
            createPayOSPaymentOrder(safePayerName, amount, callback);
        }
    }

    private void createPayOSPaymentOrder(String payerName, long amount, PaymentOrderCallback callback) {
        if (!hasPayOSCredentials()) {
            plugin.logWarning("PayOS createTransaction aborted: missing client-id/api-key/checksum-key in config.yml");
            callback.onFailure(plugin.tr("bank.payos-missing"));
            return;
        }

        long orderCode = buildOrderCode();
        String description = buildPaymentDescription("payos.payment-format", payerName, orderCode, "{playername} THANH TOAN", 25);
        pendingStates.put(orderCode, new TransactionState(PROVIDER_PAYOS, description, System.currentTimeMillis()));

        plugin.getPlatformScheduler().runAsync(() -> {
            try (CloseableHttpClient client = createHttpClient()) {
                HttpPost post = new HttpPost(PAYOS_PAYMENT_REQUESTS_API);
                JsonObject body = new JsonObject();
                body.addProperty("orderCode", orderCode);
                body.addProperty("amount", amount);
                body.addProperty("description", description);
                body.addProperty("cancelUrl", "https://google.com");
                body.addProperty("returnUrl", "https://google.com");
                body.addProperty("signature", generatePayOSSignature(amount, "https://google.com", description, orderCode, "https://google.com"));
                post.setHeader("x-client-id", getPayOSClientId());
                post.setHeader("x-api-key", getPayOSApiKey());
                post.setEntity(new StringEntity(body.toString(), ContentType.APPLICATION_JSON));

                client.execute(post, response -> {
                    String res = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    JsonObject json = JsonParser.parseString(res).getAsJsonObject();
                    String code = getJsonString(json, "code", "unknown");
                    if ("00".equals(code)) {
                        JsonObject data = json.has("data") && json.get("data").isJsonObject()
                                ? json.getAsJsonObject("data") : new JsonObject();
                        callback.onSuccess(buildPayOSOrder(payerName, amount, orderCode, description, data));
                        return null;
                    }

                    String message = getJsonString(json, "desc", getJsonString(json, "message", "PayOS rejected the transaction"));
                    plugin.logWarning("PayOS createTransaction failed for " + payerName
                            + " code=" + code + " message=" + message);
                    plugin.logDebug("PayOS createTransaction raw response: " + res);
                    pendingStates.remove(orderCode);
                    callback.onFailure(plugin.tr("bank.create-provider-failed", "message", message, "code", code));
                    return null;
                });
            } catch (Exception e) {
                pendingStates.remove(orderCode);
                plugin.logWarning("PayOS createTransaction crashed for " + payerName, e);
                callback.onFailure(plugin.tr("bank.create-failed"));
            }
        });
    }

    private PaymentOrder buildPayOSOrder(String payerName, long amount, long orderCode, String fallbackDescription, JsonObject data) {
        String description = getJsonString(data, "description", fallbackDescription);
        String qrCode = getJsonString(data, "qrCode", getJsonString(data, "qr_code", ""));
        String checkoutUrl = getJsonString(data, "checkoutUrl", getJsonString(data, "checkout_url", ""));
        String bankCode = getJsonString(data, "bin", getJsonString(data, "bankCode", ""));
        String bankName = getJsonString(data, "bankName", getJsonString(data, "bank", "PayOS"));
        String accountNumber = getJsonString(data, "accountNumber", getJsonString(data, "account_number", ""));
        String accountName = getJsonString(data, "accountName", getJsonString(data, "account_name", ""));
        String qrSource = !qrCode.isBlank() ? qrCode : checkoutUrl;
        return new PaymentOrder(PROVIDER_PAYOS, orderCode, amount, payerName, bankCode, bankName,
                accountNumber, accountName, description, qrSource, buildQrImageUrl(qrSource), checkoutUrl);
    }

    private void createSePayPaymentOrder(String payerName, long amount, PaymentOrderCallback callback) {
        if (!hasSePayCredentials()) {
            plugin.logWarning("SePay createTransaction aborted: missing api-token/bank-code/account-number/account-name in config.yml");
            callback.onFailure(plugin.tr("bank.sepay-missing"));
            return;
        }

        long orderCode = buildOrderCode();
        String description = buildPaymentDescription("sepay.payment-format", payerName, orderCode, "KP{ordercode}", 50);
        pendingStates.put(orderCode, new TransactionState(PROVIDER_SEPAY, description, System.currentTimeMillis()));

        String qrUrl = buildSePayQrImageUrl(amount, description);
        PaymentOrder order = new PaymentOrder(
                PROVIDER_SEPAY,
                orderCode,
                amount,
                payerName,
                getSePayBankCode(),
                getSePayBankName(),
                getSePayAccountNumber(),
                getSePayAccountName(),
                description,
                qrUrl,
                forceVietQrOnlyUrl(qrUrl),
                ""
        );
        callback.onSuccess(order);
    }

    public void startPolling(Player player, long orderCode, long amount, int mapId) {
        cancelPolling(player);
        TransactionState state = pendingStates.getOrDefault(orderCode,
                new TransactionState(getProvider(), String.valueOf(orderCode), System.currentTimeMillis()));
        AtomicBoolean paid = new AtomicBoolean(false);
        AtomicReference<ScheduledTask> taskReference = new AtomicReference<>();
        int timeoutSeconds = Math.max(60, plugin.getConfig().getInt("napbank.timeout-seconds", 600));
        int pollEverySeconds = Math.max(5, plugin.getConfig().getInt("napbank.poll-every-seconds", 10));
        int[] timeLeft = {timeoutSeconds};

        Runnable polling = () -> {
            ScheduledTask self = taskReference.get();
            if (paid.get()) {
                cleanupTask(player, orderCode, self);
                return;
            }

            if (timeLeft[0] <= 0) {
                stopAndClean(player, mapId, plugin.tr("bank.expired"), orderCode, self);
                sendFailureWebhook(player.getName(), amount, "timeout");
                return;
            }

            if (!player.isOnline()) {
                cleanupTask(player, orderCode, self);
                return;
            }

            int minutes = timeLeft[0] / 60;
            int seconds = timeLeft[0] % 60;
            String timer = String.format("%02d:%02d", minutes, seconds);
            plugin.sendActionBar(player, plugin.tr("bank.actionbar-scanning",
                    "time", timer,
                    "amount", plugin.formatMoney(amount)));

            timeLeft[0]--;

            if (timeLeft[0] % pollEverySeconds == 0) {
                if (PROVIDER_SEPAY.equals(state.provider())) {
                    checkSePayPayment(player, orderCode, amount, mapId, self, paid, state);
                } else {
                    checkPayOSPayment(player, orderCode, amount, mapId, self, paid, state);
                }
            }
        };

        ScheduledTask task = plugin.getPlatformScheduler().runTimerAsync(polling, 1L, 20L);
        taskReference.set(task);
        activeTasks.put(player.getUniqueId(), task);
    }

    public ScheduledTask startExternalPolling(String ownerKey, long orderCode, long amount, ExternalPaymentCallback callback) {
        TransactionState state = pendingStates.getOrDefault(orderCode,
                new TransactionState(getProvider(), String.valueOf(orderCode), System.currentTimeMillis()));
        int timeoutSeconds = Math.max(60, plugin.getStoreManager() == null
                ? plugin.getConfig().getInt("napbank.timeout-seconds", 600)
                : plugin.getStoreManager().getOrderTimeoutSeconds());
        int pollEverySeconds = Math.max(5, plugin.getStoreManager() == null
                ? plugin.getConfig().getInt("napbank.poll-every-seconds", 10)
                : plugin.getStoreManager().getPollEverySeconds());
        long expiresAt = System.currentTimeMillis() + timeoutSeconds * 1000L;
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<ScheduledTask> taskReference = new AtomicReference<>();

        Runnable polling = () -> {
            ScheduledTask self = taskReference.get();
            if (completed.get()) {
                if (self != null) self.cancel();
                return;
            }

            if (System.currentTimeMillis() > expiresAt) {
                if (completed.compareAndSet(false, true)) {
                    pendingStates.remove(orderCode);
                    if (self != null) self.cancel();
                    callback.onExpired();
                }
                return;
            }

            try {
                boolean paid = PROVIDER_SEPAY.equals(state.provider())
                        ? isSePayPaid(amount, state)
                        : isPayOSPaid(orderCode);
                if (paid && completed.compareAndSet(false, true)) {
                    pendingStates.remove(orderCode);
                    if (self != null) self.cancel();
                    callback.onPaid();
                }
            } catch (Exception e) {
                plugin.logDebug("External bank payment polling skipped for " + ownerKey + ": " + e.getMessage(), e);
            }
        };

        ScheduledTask task = plugin.getPlatformScheduler().runTimerAsync(polling, 20L, pollEverySeconds * 20L);
        taskReference.set(task);
        return () -> {
            if (completed.compareAndSet(false, true)) {
                pendingStates.remove(orderCode);
                ScheduledTask current = taskReference.get();
                if (current != null) current.cancel();
            }
        };
    }

    public void cancelExternalTransaction(long orderCode) {
        pendingStates.remove(orderCode);
    }

    private void checkPayOSPayment(Player player, long orderCode, long amount, int mapId, ScheduledTask task, AtomicBoolean paid, TransactionState state) {
        try {
            if (isPayOSPaid(orderCode)) {
                markPaymentSuccess(player, amount, mapId, task, paid, orderCode, state);
            }
        } catch (Exception e) {
            plugin.logDebug("PayOS checkPayment skipped: " + e.getMessage(), e);
        }
    }

    private boolean isPayOSPaid(long orderCode) throws Exception {
        try (CloseableHttpClient client = createHttpClient()) {
            HttpGet get = new HttpGet(PAYOS_PAYMENT_REQUESTS_API + "/" + orderCode);
            get.setHeader("x-client-id", getPayOSClientId());
            get.setHeader("x-api-key", getPayOSApiKey());

            return client.execute(get, response -> {
                String res = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                JsonObject json = JsonParser.parseString(res).getAsJsonObject();
                JsonObject data = json.has("data") && json.get("data").isJsonObject()
                        ? json.getAsJsonObject("data") : null;
                return data != null && "PAID".equalsIgnoreCase(getJsonString(data, "status", ""));
            });
        }
    }

    private void checkSePayPayment(Player player, long orderCode, long amount, int mapId,
                                   ScheduledTask task, AtomicBoolean paid, TransactionState state) {
        try {
            if (isSePayPaid(amount, state)) {
                markPaymentSuccess(player, amount, mapId, task, paid, orderCode, state);
            }
        } catch (Exception e) {
            plugin.logDebug("SePay checkPayment skipped: " + e.getMessage(), e);
        }
    }

    private boolean isSePayPaid(long amount, TransactionState state) throws Exception {
        try (CloseableHttpClient client = createHttpClient()) {
            int limit = Math.max(1, Math.min(100, plugin.getConfig().getInt("sepay.transaction-lookup-limit", 20)));
            String minDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Math.max(0, state.createdAtMillis() - 60000L)));
            String url = SEPAY_TRANSACTIONS_API
                    + "?account_number=" + enc(getSePayAccountNumber())
                    + "&amount_in=" + amount
                    + "&limit=" + limit
                    + "&transaction_date_min=" + enc(minDate);

            HttpGet get = new HttpGet(url);
            get.setHeader("Content-Type", "application/json");
            get.setHeader("Authorization", "Bearer " + getSePayApiToken());

            return client.execute(get, response -> {
                String res = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                JsonObject json = JsonParser.parseString(res).getAsJsonObject();
                if (json.has("transactions") && json.get("transactions").isJsonArray()) {
                    JsonArray transactions = json.getAsJsonArray("transactions");
                    for (JsonElement element : transactions) {
                        if (!element.isJsonObject()) continue;
                        JsonObject transaction = element.getAsJsonObject();
                        String transactionId = getJsonString(transaction, "id", getJsonString(transaction, "reference_number", ""));
                        if (!transactionId.isEmpty() && processedSePayTransactions.contains(transactionId)) {
                            continue;
                        }

                        if (isMatchingSePayTransaction(transaction, amount, state.description())) {
                            if (!transactionId.isEmpty()) {
                                processedSePayTransactions.add(transactionId);
                            }
                            return true;
                        }
                    }
                } else if (json.has("status") && json.get("status").getAsInt() != 200) {
                    plugin.logWarning("SePay transaction lookup failed. Enable logging.debug for raw response.");
                    plugin.logDebug("SePay transaction lookup raw response: " + res);
                }
                return false;
            });
        }
    }

    private boolean isMatchingSePayTransaction(JsonObject transaction, long amount, String description) {
        long amountIn = getMoneyLong(transaction, "amount_in", getMoneyLong(transaction, "transferAmount", 0L));
        if (amountIn != amount) return false;

        String target = normalizeTransferContent(description);
        String content = normalizeTransferContent(getJsonString(transaction, "transaction_content", ""));
        String code = normalizeTransferContent(getJsonString(transaction, "code", ""));
        String contentAlt = normalizeTransferContent(getJsonString(transaction, "content", ""));
        return !target.isBlank() && (content.contains(target) || code.contains(target) || contentAlt.contains(target));
    }

    private void markPaymentSuccess(Player player, long amount, int mapId, ScheduledTask task, AtomicBoolean paid, long orderCode, TransactionState state) {
        if (paid.compareAndSet(false, true)) {
            executeSuccess(player, amount, orderCode, state);
            removeQRMap(player, mapId);
            activeTasks.remove(player.getUniqueId());
            pendingStates.remove(orderCode);
            if (task != null) task.cancel();
        }
    }

    private void executeSuccess(Player player, long amount, long orderCode, TransactionState state) {
        int finalPoints = plugin.calculateFinalPoints(amount, PaymentChannel.BANK);
        int bonusPoints = plugin.calculateBonusPoints(amount, PaymentChannel.BANK);
        plugin.getLogManager().log("BANK_SUCCESS: " + player.getName() + " topup " + amount + " " + plugin.trPlain("general.currency") + ".");

        String commandTemplate = plugin.getConfig().getString("reward-command", "p give {player} {points}");
        String command = commandTemplate
                .replace("{player}", player.getName())
                .replace("{points}", String.valueOf(finalPoints));

        plugin.getPlatformScheduler().runPlayer(player, () -> {
            plugin.dispatchConsoleCommand(command);
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(plugin.tr("bank.success-actionbar")));
            plugin.broadcast(plugin.tr("bank.success-broadcast", "player", player.getName(), "amount", plugin.formatMoney(amount)));
            player.sendMessage(plugin.tr("bank.success-points", "points", plugin.formatMoney(finalPoints)));
            if (bonusPoints > 0) {
                player.sendMessage(plugin.tr("promotion.bonus-channel", "bonus", plugin.getPromotionPercent(PaymentChannel.BANK), "type", plugin.trPlain(PaymentChannel.BANK.languageKey())));
            }
            plugin.spawnSuccessFirework(player.getLocation());
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            plugin.processSuccessPayment(player.getName(), amount, PaymentChannel.BANK, state.provider(),
                    "Order: " + orderCode + " | Content: " + state.description());
        });
    }

    private void sendFailureWebhook(String playerName, long amount, String reason) {
        if (plugin.getTransactionWebhookManager() != null) {
            plugin.getTransactionWebhookManager().sendFailure(PaymentChannel.BANK, playerName, amount, reason);
        }
    }

    public void cancelPolling(Player player) {
        ScheduledTask task = activeTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            removeAnyQRMap(player);
        }
    }

    private void removeQRMap(Player player, int mapId) {
        plugin.getPlatformScheduler().runPlayer(player, () -> {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == Material.FILLED_MAP) {
                    MapMeta meta = (MapMeta) item.getItemMeta();
                    if (meta != null && meta.hasMapView() && meta.getMapView().getId() == mapId) {
                        player.getInventory().remove(item);
                        break;
                    }
                }
            }
        });
    }

    private void removeAnyQRMap(Player player) {
        plugin.getPlatformScheduler().runPlayer(player, () -> {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == Material.FILLED_MAP) player.getInventory().remove(item);
            }
        });
    }

    private void sendPlayerMessage(Player player, String message) {
        plugin.getPlatformScheduler().runPlayer(player, () -> player.sendMessage(message));
    }

    private void sendCreateTransactionError(Player player, String message) {
        sendPlayerMessage(player, message);
    }

    private void stopAndClean(Player player, int mapId, String message, long orderCode, ScheduledTask task) {
        if (message != null) sendPlayerMessage(player, message);
        removeQRMap(player, mapId);
        cleanupTask(player, orderCode, task);
    }

    private void cleanupTask(Player player, long orderCode, ScheduledTask task) {
        activeTasks.remove(player.getUniqueId());
        pendingStates.remove(orderCode);
        if (task != null) task.cancel();
    }

    private long buildOrderCode() {
        long now = System.currentTimeMillis();
        return Math.abs(now % 9000000000L) + 1000000000L;
    }

    private String buildPaymentDescription(String configPath, String payerName, long orderCode, String defaultFormat, int maxLength) {
        String format = plugin.getConfig().getString(configPath, defaultFormat);
        String raw = format
                .replace("{playername}", payerName)
                .replace("{player}", payerName)
                .replace("{ordercode}", String.valueOf(orderCode))
                .replace("{code}", String.valueOf(orderCode));
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9 ]", " ")
                .trim()
                .replaceAll("\\s+", " ");
        String code = String.valueOf(orderCode);
        if (normalized.isBlank()) {
            normalized = "KP" + code;
        }

        // Giữ mã đơn trong nội dung chuyển khoản để tránh giao dịch không khớp khi format quá dài.
        if (!normalized.contains(code) && maxLength >= code.length() + 2) {
            normalized = "KP " + code;
        }

        if (normalized.length() > maxLength) {
            if (normalized.contains(code) && maxLength >= code.length() + 2) {
                int roomForPrefix = Math.max(0, maxLength - code.length() - 1);
                String prefix = normalized.substring(0, Math.min(roomForPrefix, normalized.indexOf(code))).trim();
                normalized = (prefix.isBlank() ? "KP" : prefix) + " " + code;
                if (normalized.length() > maxLength) {
                    normalized = normalized.substring(normalized.length() - maxLength).trim();
                }
            } else {
                normalized = normalized.substring(0, maxLength).trim();
            }
        }
        return normalized;
    }

    private String buildSePayQrImageUrl(long amount, String description) {
        String template = "compact2";
        String base = "https://img.vietqr.io/image/"
                + encPath(getSePayBankCode())
                + "-" + encPath(getSePayAccountNumber())
                + "-" + template
                + ".png";

        return base
                + "?amount=" + amount
                + "&addInfo=" + enc(description)
                + "&accountName=" + enc(getSePayAccountName());
    }

    private String buildQrImageUrl(String qrContentOrUrl) {
        String value = qrContentOrUrl == null ? "" : qrContentOrUrl.trim();
        if (value.isBlank()) return "";
        String lower = value.toLowerCase(Locale.ROOT);
        if ((lower.startsWith("http://") || lower.startsWith("https://"))
                && (lower.endsWith(".png") || lower.contains(".png?") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp"))) {
            return forceVietQrOnlyUrl(value);
        }
        return QUICK_CHART_QR_API + "?size=512&margin=2&text=" + enc(value);
    }

    private String forceVietQrOnlyUrl(String imageUrl) {
        if (imageUrl == null) return "";
        String lower = imageUrl.toLowerCase(Locale.ROOT);
        if (!lower.contains("img.vietqr.io/image/") || !lower.contains(".png")) {
            return imageUrl;
        }

        int pngIndex = lower.indexOf(".png");
        int lastDashBeforePng = imageUrl.lastIndexOf('-', pngIndex);
        if (lastDashBeforePng < 0) {
            return imageUrl;
        }

        return imageUrl.substring(0, lastDashBeforePng + 1) + "qr_only" + imageUrl.substring(pngIndex);
    }

    private String generatePayOSSignature(long amount, String cancelUrl, String description, long orderCode, String returnUrl) throws Exception {
        String data = "amount=" + amount + "&cancelUrl=" + cancelUrl + "&description=" + description + "&orderCode=" + orderCode + "&returnUrl=" + returnUrl;
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(getPayOSChecksumKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    private CloseableHttpClient createHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(5))
                .setConnectionRequestTimeout(Timeout.ofSeconds(5))
                .setResponseTimeout(Timeout.ofSeconds(10))
                .build();
        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    private String getProvider() {
        String provider = plugin.getConfig().getString("napbank.provider", PROVIDER_PAYOS);
        if (provider == null || provider.isBlank()) return PROVIDER_PAYOS;
        provider = provider.trim().toLowerCase(Locale.ROOT);
        return PROVIDER_SEPAY.equals(provider) ? PROVIDER_SEPAY : PROVIDER_PAYOS;
    }

    private String getPayOSClientId() { return plugin.getConfig().getString("payos.client-id", ""); }
    private String getPayOSApiKey() { return plugin.getConfig().getString("payos.api-key", ""); }
    private String getPayOSChecksumKey() { return plugin.getConfig().getString("payos.checksum-key", ""); }

    private boolean hasPayOSCredentials() {
        return !getPayOSClientId().isBlank() && !getPayOSApiKey().isBlank() && !getPayOSChecksumKey().isBlank();
    }

    private String getSePayApiToken() { return plugin.getConfig().getString("sepay.api-token", ""); }
    private String getSePayBankCode() { return plugin.getConfig().getString("sepay.bank-code", ""); }
    private String getSePayBankName() { return plugin.getConfig().getString("sepay.bank-name", getSePayBankCode()); }
    private String getSePayAccountNumber() { return plugin.getConfig().getString("sepay.account-number", ""); }
    private String getSePayAccountName() { return plugin.getConfig().getString("sepay.account-name", ""); }

    private boolean hasSePayCredentials() {
        return !getSePayApiToken().isBlank()
                && !getSePayBankCode().isBlank()
                && !getSePayAccountNumber().isBlank()
                && !getSePayAccountName().isBlank();
    }

    private String getJsonString(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return fallback;
        try {
            return object.get(key).getAsString();
        } catch (Exception e) {
            return fallback;
        }
    }

    private long getMoneyLong(JsonObject object, String key, long fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return fallback;
        try {
            return new BigDecimal(object.get(key).getAsString()).longValue();
        } catch (Exception e) {
            return fallback;
        }
    }

    private String normalizeTransferContent(String value) {
        if (value == null) return "";
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return ascii.replaceAll("[^a-zA-Z0-9]", "").toUpperCase(Locale.ROOT);
    }

    private String sanitizePayerName(String value) {
        if (value == null || value.isBlank()) return "Player";
        String normalized = value.trim().replaceAll("[^a-zA-Z0-9_ ]", "").trim();
        return normalized.isBlank() ? "Player" : normalized;
    }

    private String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String encPath(String value) {
        return enc(value).replace("+", "%20");
    }

    public interface TransactionCallback {
        void onSuccess(JsonObject data, long orderCode);
    }

    public interface PaymentOrderCallback {
        void onSuccess(PaymentOrder order);
        void onFailure(String message);
    }

    public interface ExternalPaymentCallback {
        void onPaid();
        void onExpired();
    }
}
