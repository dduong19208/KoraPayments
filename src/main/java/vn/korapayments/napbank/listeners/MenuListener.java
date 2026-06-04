package vn.korapayments.napbank.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import vn.korapayments.KoraPayments;
import vn.korapayments.common.manager.MenuHolder;
import vn.korapayments.common.manager.PaymentGUIManager;

import java.util.List;

public class MenuListener implements Listener {
    private static final int[] MILESTONE_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final KoraPayments plugin;

    public MenuListener(KoraPayments plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();

        if (!(holder instanceof MenuHolder menuHolder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        switch (menuHolder.getMenuId()) {
            case "admin_dashboard", "history" -> {
                return;
            }
            case "milestones" -> handleMilestoneClick(player, slot, false);
            case "server_milestones" -> handleMilestoneClick(player, slot, true);
            case PaymentGUIManager.MENU_CARD_PROVIDER -> handleCardProviderClick(player, menuHolder, slot);
            case PaymentGUIManager.MENU_CARD_AMOUNT -> handleCardAmountClick(player, menuHolder, slot);
            case PaymentGUIManager.MENU_BANK_AMOUNT -> handleBankAmountClick(player, menuHolder, slot);
            case "topnap" -> handleTopClick(player, menuHolder, slot);
            default -> {
                return;
            }
        }
    }


    private void handleTopClick(Player player, MenuHolder holder, int slot) {
        String action = holder.getSlotValue(slot);
        if (action == null || action.isBlank()) {
            return;
        }

        String mode = holder.getData("mode");
        if (mode == null || mode.isBlank()) {
            mode = "all";
        }

        int page = parsePositiveInt(holder.getData("page"), 1);
        int targetPage = switch (action) {
            case "previous" -> Math.max(1, page - 1);
            case "next" -> page + 1;
            default -> page;
        };

        final String finalMode = mode;
        final int finalPage = targetPage;
        plugin.getPlatformScheduler().runPlayer(player, () -> player.performCommand("topnap " + finalMode + " " + finalPage));
    }

    private int parsePositiveInt(String input, int fallback) {
        if (input == null || input.isBlank()) {
            return fallback;
        }
        try {
            return Math.max(1, Integer.parseInt(input));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void handleCardProviderClick(Player player, MenuHolder holder, int slot) {
        if (slot == 40) {
            player.closeInventory();
            return;
        }

        String telco = holder.getSlotValue(slot);
        if (telco == null || telco.isBlank()) {
            return;
        }

        plugin.getPaymentGuiManager().openCardAmountMenu(player, telco);
    }

    private void handleCardAmountClick(Player player, MenuHolder holder, int slot) {
        if (slot == 45) {
            plugin.getPaymentGuiManager().openCardProviderMenu(player);
            return;
        }
        if (slot == 53) {
            player.closeInventory();
            return;
        }

        String amount = holder.getSlotValue(slot);
        String telco = holder.getData("telco");
        if (amount == null || amount.isBlank() || telco == null || telco.isBlank()) {
            return;
        }

        player.closeInventory();
        plugin.getPlatformScheduler().runPlayer(player, () -> player.performCommand("napthe " + telco + " " + amount));
    }

    private void handleBankAmountClick(Player player, MenuHolder holder, int slot) {
        if (slot == 53) {
            player.closeInventory();
            return;
        }

        String amount = holder.getSlotValue(slot);
        if (amount == null || amount.isBlank()) {
            return;
        }

        player.closeInventory();
        plugin.getPlatformScheduler().runPlayer(player, () -> player.performCommand("bank " + amount));
    }

    private void handleMilestoneClick(Player player, int slot, boolean serverMilestone) {
        int milestoneIndex = getMilestoneIndex(slot);
        if (milestoneIndex == -1) {
            return;
        }

        List<Long> milestones = serverMilestone
                ? plugin.getMilestoneManager().getServerMilestones()
                : plugin.getMilestoneManager().getPersonalMilestones();

        if (milestoneIndex >= milestones.size()) {
            return;
        }

        long milestone = milestones.get(milestoneIndex);
        boolean claimed = serverMilestone
                ? plugin.getMilestoneManager().claimServerMilestone(player, milestone)
                : plugin.getMilestoneManager().claimPersonalMilestone(player, milestone);

        if (!claimed || plugin.getMilestoneGuiManager() == null) {
            return;
        }

        if (serverMilestone) {
            plugin.getMilestoneGuiManager().openServerMilestoneRewards(player);
        } else {
            plugin.getMilestoneGuiManager().openMilestones(player);
        }
    }

    private int getMilestoneIndex(int slot) {
        for (int i = 0; i < MILESTONE_SLOTS.length; i++) {
            if (MILESTONE_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }
}
