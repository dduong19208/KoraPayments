package vn.korapayments.napcard.api;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import vn.korapayments.KoraPayments;
import vn.korapayments.napcard.models.CardRequest;
import vn.korapayments.napcard.utils.HashUtils;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class CardChargingService {
    private static final String PROVIDER_CARD2K = "card2k";
    private static final String PROVIDER_GACHTHEFAST = "gachthefast";

    private final KoraPayments plugin;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build();

    public CardChargingService(KoraPayments plugin) {
        this.plugin = plugin;
    }

    public String getProvider() {
        String provider = plugin.getConfig().getString("napthe.provider", PROVIDER_CARD2K);
        if (provider == null || provider.isBlank()) {
            return PROVIDER_CARD2K;
        }
        provider = provider.trim().toLowerCase(Locale.ROOT);
        if (PROVIDER_GACHTHEFAST.equals(provider)) {
            return PROVIDER_GACHTHEFAST;
        }
        return PROVIDER_CARD2K;
    }

    public String getProviderDisplayName() {
        return PROVIDER_GACHTHEFAST.equals(getProvider()) ? "GachTheFast" : "Card2K";
    }

    public boolean hasCredentials() {
        ProviderConfig config = getProviderConfig();
        return !config.partnerId().isBlank() && !config.partnerKey().isBlank() && !config.domain().isBlank();
    }

    public void sendRequest(CardRequest card, String command, Callback callback) {
        ProviderConfig config = getProviderConfig();
        String sign = HashUtils.md5(config.partnerKey() + card.getCode() + card.getSerial());
        if (sign == null) {
            callback.onFailure(null, new IOException("Cannot create card signature"));
            return;
        }

        try {
            Request request;
            if ("GET".equalsIgnoreCase(config.method())) {
                HttpUrl.Builder urlBuilder = HttpUrl.parse(config.url()).newBuilder()
                        .addQueryParameter("telco", card.getTelco())
                        .addQueryParameter("code", card.getCode())
                        .addQueryParameter("serial", card.getSerial())
                        .addQueryParameter("amount", String.valueOf(card.getAmount()))
                        .addQueryParameter("request_id", card.getRequestId())
                        .addQueryParameter("partner_id", config.partnerId())
                        .addQueryParameter("sign", sign)
                        .addQueryParameter("command", command);

                request = new Request.Builder()
                        .url(urlBuilder.build())
                        .get()
                        .addHeader("Accept", "application/json")
                        .build();
            } else {
                RequestBody body = new FormBody.Builder()
                        .add("telco", card.getTelco())
                        .add("code", card.getCode())
                        .add("serial", card.getSerial())
                        .add("amount", String.valueOf(card.getAmount()))
                        .add("request_id", card.getRequestId())
                        .add("partner_id", config.partnerId())
                        .add("sign", sign)
                        .add("command", command)
                        .build();

                request = new Request.Builder()
                        .url(config.url())
                        .post(body)
                        .addHeader("Accept", "application/json")
                        .build();
            }

            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            callback.onFailure(null, new IOException("Cannot build card request", e));
        }
    }

    private ProviderConfig getProviderConfig() {
        String provider = getProvider();
        String basePath = PROVIDER_GACHTHEFAST.equals(provider) ? "gachthefast" : "card2k";
        String defaultDomain = PROVIDER_GACHTHEFAST.equals(provider) ? "gachthefast.com" : "card2k.com";
        String defaultMethod = PROVIDER_GACHTHEFAST.equals(provider) ? "GET" : "POST";

        String domain = plugin.getConfig().getString(basePath + ".api.domain", defaultDomain);
        String endpoint = plugin.getConfig().getString(basePath + ".api.endpoint", "/chargingws/v2");
        String method = plugin.getConfig().getString(basePath + ".api.method", defaultMethod);
        String partnerId = plugin.getConfig().getString(basePath + ".api.partner_id", "");
        String partnerKey = plugin.getConfig().getString(basePath + ".api.partner_key", "");

        return new ProviderConfig(partnerId, partnerKey, domain, endpoint, method, buildUrl(domain, endpoint));
    }

    private String buildUrl(String domain, String endpoint) {
        String base = domain == null ? "" : domain.trim();
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            base = "https://" + base;
        }
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String path = endpoint == null || endpoint.isBlank() ? "/chargingws/v2" : endpoint.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path;
    }

    private record ProviderConfig(String partnerId, String partnerKey, String domain, String endpoint, String method, String url) {}
}
