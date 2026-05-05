package fr.horizonsmp.geoBlock.listener;

import java.util.Locale;
import java.util.Optional;

public record ConnectionDecision(
        boolean allowed,
        Optional<DenialReason> reason,
        Optional<String> countryIso,
        boolean proxy
) {

    public static ConnectionDecision allow() {
        return new ConnectionDecision(true, Optional.empty(), Optional.empty(), false);
    }

    public static ConnectionDecision allow(String countryIso, boolean proxy) {
        return new ConnectionDecision(true, Optional.empty(),
                Optional.ofNullable(countryIso), proxy);
    }

    public static ConnectionDecision deny(DenialReason reason, String countryIso, boolean proxy) {
        return new ConnectionDecision(false, Optional.of(reason),
                Optional.ofNullable(countryIso), proxy);
    }

    public static ConnectionDecision deny(DenialReason reason) {
        return new ConnectionDecision(false, Optional.of(reason), Optional.empty(), false);
    }

    public String countryDisplayName() {
        return countryIso
                .map(iso -> {
                    String name = new Locale.Builder().setRegion(iso).build()
                            .getDisplayCountry(Locale.ENGLISH);
                    return (name == null || name.isBlank()) ? iso : name;
                })
                .orElse("unknown");
    }
}
