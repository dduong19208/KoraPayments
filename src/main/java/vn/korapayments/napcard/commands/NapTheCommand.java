package vn.korapayments.napcard.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import vn.korapayments.KoraPayments;
import vn.korapayments.napcard.manager.CardSessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class NapTheCommand implements CommandExecutor, TabCompleter {
    private final KoraPayments plugin;

    public NapTheCommand(KoraPayments plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.tr("general.player-only"));
            return true;
        }
        if (!plugin.ensureFeatureAvailable(player)) return true;

        if (args.length >= 1 && args[0].equalsIgnoreCase("gui")) {
            plugin.getPaymentGuiManager().openCardProviderMenu(player);
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.tr("card.usage"));
            return true;
        }

        String telco = normalizeTelco(args[0]);
        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.tr("card.amount-number"));
            return true;
        }

        CardSessionManager.Session session = new CardSessionManager.Session(telco, amount);

        if (args.length >= 4) {
            session.serial = args[2];
            session.pin = args[3];
            session.step = CardSessionManager.Step.CONFIRM;
            plugin.getCardSessionManager().start(player.getUniqueId(), session);
            plugin.getCardListener().showConfirmation(player, session);
            return true;
        }

        plugin.getCardSessionManager().start(player.getUniqueId(), session);
        player.sendMessage("");
        player.sendMessage(plugin.tr("card.start", "telco", telco, "amount", plugin.formatMoney(amount)));
        player.sendMessage(plugin.tr("card.enter-serial"));
        player.sendMessage("");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("gui");
            suggestions.addAll(plugin.getPaymentGuiManager().getCardTelcos());
            return suggestions.stream()
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("gui")) {
            return plugin.getPaymentGuiManager().getCardAmounts().stream()
                    .map(String::valueOf)
                    .filter(s -> s.startsWith(args[1]))
                    .collect(Collectors.toList());
        }
        if (args.length == 3) return List.of("<serial>");
        if (args.length == 4) return List.of("<code>");
        return new ArrayList<>();
    }

    private String normalizeTelco(String telco) {
        if (telco == null) return "";
        return telco.trim()
                .replace(" ", "")
                .replace("-", "")
                .replace(".", "")
                .toUpperCase(Locale.ROOT);
    }
}
