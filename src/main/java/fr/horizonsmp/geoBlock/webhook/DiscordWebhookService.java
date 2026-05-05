package fr.horizonsmp.geoBlock.webhook;

import fr.horizonsmp.geoBlock.config.PluginConfig;
import fr.horizonsmp.geoBlock.listener.ConnectionDecision;
import fr.horizonsmp.geoBlock.listener.DenialReason;
import fr.horizonsmp.geoBlock.listener.LookupStatus;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DiscordWebhookService {

    private static final int COLOR_DENIAL = 0xE74C3C;
    private static final int COLOR_WARNING = 0xF1C40F;

    private final HttpClient http;
    private final Supplier<PluginConfig> configSupplier;
    private final Logger logger;

    public DiscordWebhookService(Supplier<PluginConfig> configSupplier, Logger logger) {
        this.configSupplier = configSupplier;
        this.logger = logger;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void notifyDenial(String playerName, UUID uuid, String ipAddress, ConnectionDecision decision) {
        PluginConfig config = configSupplier.get();
        PluginConfig.Discord discord = config.discord();
        if (!discord.isEnabled()) {
            return;
        }
        send(discord, buildDenialPayload(playerName, uuid, ipAddress, decision, discord));
    }

    public void notifyLookupFailure(String playerName,
                                    UUID uuid,
                                    String ipAddress,
                                    LookupStatus status,
                                    boolean playerAllowed) {
        PluginConfig config = configSupplier.get();
        PluginConfig.Discord discord = config.discord();
        if (!discord.isEnabled() || !discord.notifyLookupFailure()) {
            return;
        }
        send(discord, buildLookupFailurePayload(playerName, uuid, ipAddress, status, playerAllowed, discord));
    }

    private void send(PluginConfig.Discord discord, String body) {
        URI target;
        try {
            target = new URI(discord.webhookUrl());
        } catch (URISyntaxException e) {
            logger.warning("Discord webhook URL is invalid: " + e.getMessage());
            return;
        }

        HttpRequest request = HttpRequest.newBuilder(target)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("User-Agent", "GeoBlock/Discord")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        http.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        logger.log(Level.WARNING, "Discord webhook delivery failed", throwable);
                        return;
                    }
                    int status = response.statusCode();
                    if (status >= 300) {
                        logger.warning("Discord webhook returned HTTP " + status);
                    }
                });
    }

    private static String buildDenialPayload(String playerName,
                                             UUID uuid,
                                             String ipAddress,
                                             ConnectionDecision decision,
                                             PluginConfig.Discord discord) {
        List<String> fields = new ArrayList<>();
        fields.add(field("Player", formatPlayer(playerName, uuid), true));

        if (discord.includeIp() && ipAddress != null && !ipAddress.isBlank()) {
            fields.add(field("IP", "`" + ipAddress + "`", true));
        }

        String iso = decision.countryIso().orElse("--");
        fields.add(field("Country", decision.countryDisplayName() + " (" + iso + ")", true));

        DenialReason reason = decision.reason().orElse(DenialReason.LOOKUP_FAILED);
        fields.add(field("Reason", humanReadableReason(reason), false));

        if (decision.proxy()) {
            fields.add(field("Proxy/VPN", "yes", true));
        }

        return wrapEmbed(discord, "Connection refused", COLOR_DENIAL, fields);
    }

    private static String buildLookupFailurePayload(String playerName,
                                                    UUID uuid,
                                                    String ipAddress,
                                                    LookupStatus status,
                                                    boolean playerAllowed,
                                                    PluginConfig.Discord discord) {
        List<String> fields = new ArrayList<>();
        fields.add(field("Player", formatPlayer(playerName, uuid), true));

        if (discord.includeIp() && ipAddress != null && !ipAddress.isBlank()) {
            fields.add(field("IP", "`" + ipAddress + "`", true));
        }

        fields.add(field("Status", status.description(), false));
        fields.add(field("Connection", playerAllowed ? "allowed (fail-open)" : "refused (fail-closed)", true));

        return wrapEmbed(discord, "GeoIP lookup did not resolve a country", COLOR_WARNING, fields);
    }

    private static String wrapEmbed(PluginConfig.Discord discord,
                                    String title,
                                    int color,
                                    List<String> fields) {
        StringBuilder fieldsJson = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                fieldsJson.append(',');
            }
            fieldsJson.append(fields.get(i));
        }

        StringBuilder payload = new StringBuilder();
        payload.append('{');
        if (discord.username() != null && !discord.username().isBlank()) {
            payload.append("\"username\":\"").append(escape(discord.username())).append("\",");
        }
        payload.append("\"embeds\":[{")
                .append("\"title\":\"").append(escape(title)).append("\",")
                .append("\"color\":").append(color).append(',')
                .append("\"timestamp\":\"").append(Instant.now().toString()).append("\",")
                .append("\"fields\":[").append(fieldsJson).append("]")
                .append("}]")
                .append('}');
        return payload.toString();
    }

    private static String formatPlayer(String playerName, UUID uuid) {
        String line = (playerName == null || playerName.isBlank() ? "unknown" : playerName);
        if (uuid != null) {
            line += " (`" + uuid + "`)";
        }
        return line;
    }

    private static String field(String name, String value, boolean inline) {
        return "{\"name\":\"" + escape(name)
                + "\",\"value\":\"" + escape(value)
                + "\",\"inline\":" + inline + "}";
    }

    private static String humanReadableReason(DenialReason reason) {
        return switch (reason) {
            case COUNTRY_BLACKLISTED -> "Country is blacklisted";
            case COUNTRY_NOT_WHITELISTED -> "Country is not whitelisted";
            case VPN_DETECTED -> "VPN or proxy detected";
            case LOOKUP_FAILED -> "GeoIP lookup failed";
        };
    }

    private static String escape(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length() + 2);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
