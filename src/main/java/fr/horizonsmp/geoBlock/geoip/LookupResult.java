package fr.horizonsmp.geoBlock.geoip;

public record LookupResult(String countryIso, boolean proxy) {

    public static LookupResult country(String iso) {
        return new LookupResult(iso, false);
    }

    public LookupResult withProxy(boolean isProxy) {
        return new LookupResult(this.countryIso, isProxy);
    }
}
