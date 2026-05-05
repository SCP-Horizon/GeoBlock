package fr.horizonsmp.geoBlock.listener;

public enum LookupStatus {
    OK,
    PRIVATE,
    DB_MISSING,
    LOOKUP_FAILED;

    public boolean isFailure() {
        return this != OK;
    }

    public String description() {
        return switch (this) {
            case OK -> "Country resolved";
            case PRIVATE -> "Private or local address (skipped)";
            case DB_MISSING -> "GeoIP database is not loaded";
            case LOOKUP_FAILED -> "Address not found in database or lookup error";
        };
    }
}
