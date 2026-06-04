package vn.korapayments.common.manager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import vn.korapayments.KoraPayments;
import vn.korapayments.napcard.api.CardChargingService;
import vn.korapayments.common.model.PaymentChannel;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PaymentGUIManager {
    public static final String MENU_CARD_PROVIDER = "napthe_provider";
    public static final String MENU_CARD_AMOUNT = "napthe_amount";
    public static final String MENU_BANK_AMOUNT = "bank_amount";

    private static final int[] TELCO_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            20, 21, 22, 23, 24,
            29, 30, 31, 32, 33
    };

    private static final int[] AMOUNT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private static final List<String> DEFAULT_TELCOS = List.of(
            "VIETTEL", "MOBIFONE", "VINAPHONE", "ZING", "GARENA", "GATE", "VCOIN", "SCOIN"
    );

    private static final List<Integer> DEFAULT_CARD_AMOUNTS = List.of(
            10000, 20000, 30000, 50000, 100000, 200000, 300000, 500000, 1000000, 2000000, 5000000
    );

    private static final List<Long> DEFAULT_BANK_AMOUNTS = List.of(
            10000L, 20000L, 30000L, 50000L, 100000L, 200000L, 300000L, 500000L, 1000000L, 2000000L, 5000000L
    );

    private final KoraPayments plugin;

    public PaymentGUIManager(KoraPayments plugin) {
        this.plugin = plugin;
    }

    public void openCardProviderMenu(Player player) {
        CardChargingService service = new CardChargingService(plugin);
        String providerName = service.getProviderDisplayName();
        List<String> telcos = getCardTelcos();

        MenuHolder holder = new MenuHolder(MENU_CARD_PROVIDER);
        Inventory inv = Bukkit.createInventory(holder, 45, plugin.tr("gui.card-provider-title"));
        fillModernBorder(inv);

        inv.setItem(4, GUIUtils.item(
                Material.NETHER_STAR,
                plugin.tr("gui.card-provider-info-title"),
                plugin.tr("gui.card-provider-info-provider", "provider", providerName),
                plugin.tr("gui.card-provider-info-step"),
                " ",
                plugin.tr("gui.card-provider-info-hint")
        ));

        for (int i = 0; i < telcos.size() && i < TELCO_SLOTS.length; i++) {
            String telco = normalizeTelco(telcos.get(i));
            int slot = TELCO_SLOTS[i];
            holder.setSlotValue(slot, telco);
            inv.setItem(slot, GUIUtils.item(
                    getTelcoMaterial(telco),
                    plugin.tr("gui.card-telco-name", "telco", formatTelcoName(telco)),
                    plugin.tr("gui.card-telco-provider", "provider", providerName),
                    plugin.tr("gui.card-telco-range"),
                    " ",
                    plugin.tr("gui.click-select")
            ));
        }

        inv.setItem(40, closeItem());
        player.openInventory(inv);
    }

    public void openCardAmountMenu(Player player, String telco) {
        String normalizedTelco = normalizeTelco(telco);
        CardChargingService service = new CardChargingService(plugin);
        String providerName = service.getProviderDisplayName();
        List<Integer> amounts = getCardAmounts();
        boolean cardPromotionActive = plugin.isPromotionActive(PaymentChannel.CARD);
        int cardPromotionPercent = plugin.getPromotionPercent(PaymentChannel.CARD);

        MenuHolder holder = new MenuHolder(MENU_CARD_AMOUNT);
        holder.setData("telco", normalizedTelco);
        Inventory inv = Bukkit.createInventory(holder, 54, plugin.tr("gui.card-amount-title"));
        fillModernBorder(inv);

        inv.setItem(4, GUIUtils.item(
                getTelcoMaterial(normalizedTelco),
                plugin.tr("gui.card-amount-info-title", "telco", formatTelcoName(normalizedTelco)),
                plugin.tr("gui.card-provider-info-provider", "provider", providerName),
                plugin.tr("gui.card-amount-info-step"),
                " ",
                plugin.tr("gui.card-amount-info-hint")
        ));

        for (int i = 0; i < amounts.size() && i < AMOUNT_SLOTS.length; i++) {
            int amount = amounts.get(i);
            int slot = AMOUNT_SLOTS[i];
            holder.setSlotValue(slot, String.valueOf(amount));

            List<String> lore = new ArrayList<>();
            lore.add(plugin.tr("gui.card-amount-lore-amount", "amount", plugin.formatMoney(amount)));
            double discount = plugin.isCardTaxesEnabled()
                    ? plugin.getCardRateManager().getDiscountRate(normalizedTelco, amount)
                    : 0.0D;
            int netAmount = (int) (amount * (1.0 - (discount / 100.0)));
            int basePoints = netAmount / plugin.getCardRewardRatio();
            int points = plugin.applyPromotionToPoints(basePoints, PaymentChannel.CARD);
            if (plugin.isCardTaxesEnabled()) {
                lore.add(plugin.tr("gui.card-amount-lore-tax", "tax", formatPercent(discount)));
            }
            lore.add(plugin.tr("gui.card-amount-lore-receive", "amount", plugin.formatMoney(netAmount)));
            lore.add(plugin.tr("gui.card-amount-lore-points", "points", plugin.formatMoney(points)));
            if (cardPromotionActive) {
                lore.add(plugin.tr("gui.card-amount-lore-promotion", "bonus", cardPromotionPercent));
            }
            lore.add(" ");
            lore.add(plugin.tr("gui.click-select"));

            inv.setItem(slot, GUIUtils.item(
                    Material.GOLD_INGOT,
                    plugin.tr("gui.amount-item-name", "amount", plugin.formatMoney(amount)),
                    lore.toArray(new String[0])
            ));
        }

        inv.setItem(45, backItem());
        inv.setItem(49, GUIUtils.item(
                Material.BOOK,
                plugin.tr("gui.card-help-title"),
                plugin.tr("gui.card-help-line-1"),
                plugin.tr("gui.card-help-line-2"),
                plugin.tr("gui.card-help-line-3")
        ));
        inv.setItem(53, closeItem());
        player.openInventory(inv);
    }

    public void openBankAmountMenu(Player player) {
        List<Long> amounts = getBankAmounts();
        MenuHolder holder = new MenuHolder(MENU_BANK_AMOUNT);
        Inventory inv = Bukkit.createInventory(holder, 54, plugin.tr("gui.bank-amount-title"));
        fillModernBorder(inv);

        long maxAmount = plugin.getConfig().getLong("napbank.max-amount", 0L);
        boolean bankPromotionActive = plugin.isPromotionActive(PaymentChannel.BANK);
        int bankPromotionPercent = plugin.getPromotionPercent(PaymentChannel.BANK);
        inv.setItem(4, GUIUtils.item(
                Material.EMERALD,
                plugin.tr("gui.bank-info-title"),
                plugin.tr("gui.bank-info-provider", "provider", getBankProviderDisplayName()),
                plugin.tr("gui.bank-info-min", "amount", plugin.formatMoney(getBankMinAmount())),
                plugin.tr("gui.bank-info-max", "amount", maxAmount > 0 ? plugin.formatMoney(maxAmount) : plugin.trPlain("general.no-limit")),
                " ",
                plugin.tr("gui.bank-info-hint")
        ));

        if (amounts.isEmpty()) {
            inv.setItem(22, GUIUtils.item(
                    Material.BARRIER,
                    plugin.tr("gui.no-data"),
                    plugin.tr("gui.bank-no-amounts")
            ));
        } else {
            for (int i = 0; i < amounts.size() && i < AMOUNT_SLOTS.length; i++) {
                long amount = amounts.get(i);
                int slot = AMOUNT_SLOTS[i];
                holder.setSlotValue(slot, String.valueOf(amount));
                int points = plugin.calculateFinalPoints(amount, PaymentChannel.BANK);
                inv.setItem(slot, GUIUtils.item(
                        Material.EMERALD,
                        plugin.tr("gui.amount-item-name", "amount", plugin.formatMoney(amount)),
                        plugin.tr("gui.bank-amount-lore-amount", "amount", plugin.formatMoney(amount)),
                        plugin.tr("gui.bank-amount-lore-points", "points", plugin.formatMoney(points)),
                        bankPromotionActive ? plugin.tr("gui.bank-amount-lore-promotion", "bonus", bankPromotionPercent) : " ",
                        " ",
                        plugin.tr("gui.click-select")
                ));
            }
        }

        inv.setItem(49, GUIUtils.item(
                Material.MAP,
                plugin.tr("gui.bank-help-title"),
                plugin.tr("gui.bank-help-line-1"),
                plugin.tr("gui.bank-help-line-2"),
                plugin.tr("gui.bank-help-line-3")
        ));
        inv.setItem(53, closeItem());
        player.openInventory(inv);
    }

    public List<String> getCardTelcos() {
        CardChargingService service = new CardChargingService(plugin);
        String provider = service.getProvider();
        List<String> configured = getConfiguredStringList("napthe.gui.providers." + provider + ".telcos");
        if (configured.isEmpty()) {
            configured = getConfiguredStringList("napthe.gui.telcos");
        }
        if (configured.isEmpty()) {
            configured = DEFAULT_TELCOS;
        }

        Set<String> unique = new LinkedHashSet<>();
        for (String telco : configured) {
            String normalized = normalizeTelco(telco);
            if (!normalized.isBlank()) unique.add(normalized);
        }
        return new ArrayList<>(unique);
    }

    public List<Integer> getCardAmounts() {
        return getConfiguredIntList("napthe.gui.amounts", DEFAULT_CARD_AMOUNTS);
    }

    public List<Long> getBankAmounts() {
        long minAmount = getBankMinAmount();
        long maxAmount = plugin.getConfig().getLong("napbank.max-amount", 0L);
        List<Long> configured = getConfiguredLongList("napbank.gui.amounts", DEFAULT_BANK_AMOUNTS);
        List<Long> filtered = new ArrayList<>();
        for (long amount : configured) {
            if (amount < minAmount) continue;
            if (maxAmount > 0 && amount > maxAmount) continue;
            filtered.add(amount);
        }
        return filtered;
    }

    public boolean isCardTelcoAllowed(String telco) {
        String normalized = normalizeTelco(telco);
        return getCardTelcos().contains(normalized);
    }

    public boolean isCardAmountAllowed(int amount) {
        return getCardAmounts().contains(amount);
    }

    private void fillModernBorder(Inventory inv) {
        ItemStack black = GUIUtils.item(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack cyan = GUIUtils.item(Material.CYAN_STAINED_GLASS_PANE, " ");
        GUIUtils.fillBorder(inv, black);
        if (inv.getSize() >= 9) {
            inv.setItem(0, cyan);
            inv.setItem(8, cyan);
            inv.setItem(inv.getSize() - 9, cyan);
            inv.setItem(inv.getSize() - 1, cyan);
        }
    }

    private ItemStack closeItem() {
        return GUIUtils.item(
                Material.BARRIER,
                plugin.tr("gui.close-name"),
                plugin.tr("gui.close-lore")
        );
    }

    private ItemStack backItem() {
        return GUIUtils.item(
                Material.ARROW,
                plugin.tr("gui.back-name"),
                plugin.tr("gui.back-lore")
        );
    }

    private Material getTelcoMaterial(String telco) {
        return switch (normalizeTelco(telco)) {
            case "VIETTEL" -> Material.RED_CONCRETE;
            case "MOBIFONE" -> Material.BLUE_CONCRETE;
            case "VINAPHONE" -> Material.GREEN_CONCRETE;
            case "VIETNAMOBILE", "VNMOBI" -> Material.YELLOW_CONCRETE;
            case "ZING" -> Material.ORANGE_CONCRETE;
            case "GARENA" -> Material.FIRE_CHARGE;
            case "GATE" -> Material.GOLD_BLOCK;
            case "VCOIN" -> Material.PURPLE_CONCRETE;
            case "SCOIN" -> Material.AMETHYST_SHARD;
            default -> Material.PAPER;
        };
    }

    private String formatTelcoName(String telco) {
        String normalized = normalizeTelco(telco);
        return switch (normalized) {
            case "MOBIFONE" -> "MobiFone";
            case "VINAPHONE" -> "VinaPhone";
            case "VIETNAMOBILE", "VNMOBI" -> "Vietnamobile";
            case "VCOIN" -> "VCoin";
            case "SCOIN" -> "SCoin";
            default -> normalized;
        };
    }

    private String normalizeTelco(String telco) {
        if (telco == null) return "";
        return telco.trim()
                .replace(" ", "")
                .replace("-", "")
                .replace(".", "")
                .toUpperCase(Locale.ROOT);
    }

    private String formatPercent(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001) {
            return String.valueOf((int) Math.rint(value));
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private long getBankMinAmount() {
        return Math.max(1L, plugin.getConfig().getLong("napbank.min-amount", 2000L));
    }

    private String getBankProviderDisplayName() {
        String provider = plugin.getConfig().getString("napbank.provider", "payos");
        if (provider == null) return "PayOS";
        return provider.equalsIgnoreCase("sepay") ? "SePay" : "PayOS";
    }

    private List<String> getConfiguredStringList(String path) {
        List<String> output = new ArrayList<>();
        List<?> raw = plugin.getConfig().getList(path);
        if (raw == null) return output;
        for (Object value : raw) {
            if (value == null) continue;
            String text = String.valueOf(value).trim();
            if (!text.isBlank()) output.add(text);
        }
        return output;
    }

    private List<Integer> getConfiguredIntList(String path, List<Integer> fallback) {
        Set<Integer> unique = new LinkedHashSet<>();
        List<?> raw = plugin.getConfig().getList(path);
        if (raw != null) {
            for (Object value : raw) {
                Integer parsed = parseInt(value);
                if (parsed != null && parsed > 0) unique.add(parsed);
            }
        }
        if (unique.isEmpty()) unique.addAll(fallback);
        return new ArrayList<>(unique);
    }

    private List<Long> getConfiguredLongList(String path, List<Long> fallback) {
        Set<Long> unique = new LinkedHashSet<>();
        List<?> raw = plugin.getConfig().getList(path);
        if (raw != null) {
            for (Object value : raw) {
                Long parsed = parseLong(value);
                if (parsed != null && parsed > 0) unique.add(parsed);
            }
        }
        if (unique.isEmpty()) unique.addAll(fallback);
        return new ArrayList<>(unique);
    }

    private Integer parseInt(Object value) {
        Long parsed = parseLong(value);
        if (parsed == null || parsed > Integer.MAX_VALUE) return null;
        return parsed.intValue();
    }

    private Long parseLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).replace(",", "").trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
