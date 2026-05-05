package fr.horizonsmp.geoBlock.listener;

import java.util.Locale;
import java.util.Optional;

public record ConnectionDecision(
        boolean allowed,
        Optional<DenialReason> reason,
        Optional<String> countryIso,
        boolean proxy,
        LookupStatus lookupStatus
) {

    public static ConnectionDecision allow(LookupStatus status) {
        return new ConnectionDecision(true, Optional.empty(), Optional.empty(), false, status);
    }

    public static ConnectionDecision allow(String countryIso, boolean proxy) {
        return new ConnectionDecision(true, Optional.empty(),
                Optional.ofNullable(countryIso), proxy, LookupStatus.OK);
    }

    public static ConnectionDecision deny(DenialReason reason, String countryIso, boolean proxy) {
        return new ConnectionDecision(false, Optional.of(reason),
                Optional.ofNullable(countryIso), proxy, LookupStatus.OK);
    }

    public static ConnectionDecision deny(DenialReason reason, LookupStatus status) {
        return new ConnectionDecision(false, Optional.of(reason),
                Optional.empty(), false, status);
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
