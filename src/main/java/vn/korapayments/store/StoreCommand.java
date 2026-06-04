package vn.korapayments.store;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import vn.korapayments.KoraPayments;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class StoreCommand implements CommandExecutor, TabCompleter {
    private final KoraPayments plugin;

    public StoreCommand(KoraPayments plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("korapayments.admin")) {
            sender.sendMessage(plugin.tr("general.no-permission"));
            return true;
        }

        if (!plugin.ensureFeatureAvailable(sender)) return true;

        String sub = args.length == 0 ? "publish" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "publish", "post", "send", "tao", "create" -> plugin.getStoreManager().publishStorefront(sender);
            case "reload" -> plugin.getStoreManager().reloadFromCommand(sender);
            case "status" -> plugin.getStoreManager().sendStatus(sender);
            default -> sender.sendMessage(plugin.tr("store.usage"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("korapayments.admin")) return List.of();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return List.of("publish", "reload", "status").stream()
                    .filter(value -> value.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
