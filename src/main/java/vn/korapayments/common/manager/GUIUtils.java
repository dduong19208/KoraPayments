package vn.korapayments.common.manager;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class GUIUtils {
    private GUIUtils() {}

    public static ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(new ArrayList<>(Arrays.asList(lore)));
            meta.addItemFlags(
                    ItemFlag.HIDE_ATTRIBUTES,
                    ItemFlag.HIDE_ADDITIONAL_TOOLTIP
            );
            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack emptyPane() {
        return item(Material.BLACK_STAINED_GLASS_PANE, " ");
    }

    public static void fillBorder(Inventory inv, ItemStack item) {
        int size = inv.getSize();

        for (int i = 0; i < 9; i++) {
            inv.setItem(i, item);
        }

        for (int i = size - 9; i < size; i++) {
            inv.setItem(i, item);
        }

        int rows = size / 9;
        for (int row = 1; row < rows - 1; row++) {
            inv.setItem(row * 9, item);
            inv.setItem(row * 9 + 8, item);
        }
    }

    public static String formatMoney(long amount) {
        return String.format("%,d", amount);
    }

    public static List<String> lore(String... lines) {
        return new ArrayList<>(Arrays.asList(lines));
    }
}