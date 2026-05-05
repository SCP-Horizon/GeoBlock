package fr.horizonsmp.geoBlock.listener;

import fr.horizonsmp.geoBlock.i18n.Messages;
import fr.horizonsmp.geoBlock.webhook.DiscordWebhookService;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.Map;

public final class PreLoginListener implements Listener {

    private final ConnectionGuard guard;
    private final Messages messages;
    private final DiscordWebhookService webhook;

    public PreLoginListener(ConnectionGuard guard,
                            Messages messages,
                            DiscordWebhookService webhook) {
        this.guard = guard;
        this.messages = messages;
        this.webhook = webhook;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        ConnectionDecision decision = guard.evaluate(event.getUniqueId(), event.getAddress());
        if (decision.allowed()) {
            return;
        }
        DenialReason reason = decision.reason().orElse(DenialReason.LOOKUP_FAILED);
        Component kickMessage = messages.get(reason.messageKey(), Map.of(
                "country", decision.countryDisplayName(),
                "countryCode", decision.countryIso().orElse("--"),
                "ip", event.getAddress() == null ? "" : event.getAddress().getHostAddress(),
                "uuid", event.getUniqueId() == null ? "" : event.getUniqueId().toString(),
                "name", event.getName() == null ? "" : event.getName()
        ));
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickMessage);
        webhook.notifyDenial(
                event.getName(),
                event.getUniqueId(),
                event.getAddress() == null ? null : event.getAddress().getHostAddress(),
                decision
        );
    }
}
