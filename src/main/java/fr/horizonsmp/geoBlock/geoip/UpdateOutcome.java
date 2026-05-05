package fr.horizonsmp.geoBlock.geoip;

public enum UpdateOutcome {
    SUCCESS,
    NO_LICENSE,
    FAILED;

    public String messageKey() {
        return switch (this) {
            case SUCCESS -> "command.update.success";
            case NO_LICENSE -> "command.update.no-license";
            case FAILED -> "command.update.failed";
        };
    }
}
