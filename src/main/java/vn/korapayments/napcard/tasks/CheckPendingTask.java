package vn.korapayments.napcard.tasks;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.JSONObject;
import vn.korapayments.KoraPayments;
import vn.korapayments.napcard.api.CardChargingService;
import vn.korapayments.napcard.models.CardRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public class CheckPendingTask implements Runnable {
    private final KoraPayments plugin;
    private final CardChargingService service;

    public CheckPendingTask(KoraPayments plugin) {
        this.plugin = plugin;
        this.service = plugin.getCardChargingService();
    }

    @Override
    public void run() {
        Map<UUID, CardRequest> pending = plugin.getPendingCards();
        if (pending.isEmpty()) return;

        for (Map.Entry<UUID, CardRequest> entry : new ArrayList<>(pending.entrySet())) {
            UUID playerId = entry.getKey();
            CardRequest card = entry.getValue();

            service.sendRequest(card, "check", new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    plugin.logDebug("Pending card check failed: " + e.getMessage(), e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.body() == null) return;

                    String raw = response.body().string();
                    try {
                        JSONObject json = new JSONObject(raw);
                        int status = json.optInt("status", 99);
                        String message = json.optString("message", json.optString("msg", "Unknown"));

                        if (status != 99) {
                            pending.remove(playerId);
                            plugin.getPlatformScheduler().runGlobal(() -> {
                                Player player = Bukkit.getPlayer(playerId);
                                if (player != null) {
                                    plugin.getPlatformScheduler().runPlayer(player, () ->
                                            plugin.handleCardResponse(player, status, message, card));
                                }
                            });
                        }
                    } catch (Exception e) {
                        plugin.logWarning("Invalid pending card response. Enable logging.debug for raw response.");
                        plugin.logDebug("Invalid pending card raw response: " + raw, e);
                    }
                }
            });
        }
    }
}
