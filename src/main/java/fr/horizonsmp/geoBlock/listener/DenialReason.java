package fr.horizonsmp.geoBlock.listener;

public enum DenialReason {
    COUNTRY_BLACKLISTED("kick.blocked"),
    COUNTRY_NOT_WHITELISTED("kick.not-whitelisted"),
    VPN_DETECTED("kick.vpn"),
    LOOKUP_FAILED("kick.lookup-failed");

    private final String messageKey;

    DenialReason(String messageKey) {
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
