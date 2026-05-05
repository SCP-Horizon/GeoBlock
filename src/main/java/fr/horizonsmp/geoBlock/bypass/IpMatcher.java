package fr.horizonsmp.geoBlock.bypass;

import java.net.InetAddress;
import java.util.Optional;

public final class IpMatcher {

    private final byte[] networkBytes;
    private final int prefixLength;

    private IpMatcher(byte[] networkBytes, int prefixLength) {
        this.networkBytes = networkBytes;
        this.prefixLength = prefixLength;
    }

    public boolean matches(InetAddress address) {
        if (address == null) {
            return false;
        }
        byte[] candidate = address.getAddress();
        if (candidate.length != networkBytes.length) {
            return false;
        }
        int fullBytes = prefixLength / 8;
        int remainingBits = prefixLength % 8;
        for (int i = 0; i < fullBytes; i++) {
            if (candidate[i] != networkBytes[i]) {
                return false;
            }
        }
        if (remainingBits == 0) {
            return true;
        }
        int mask = 0xFF << (8 - remainingBits);
        return (candidate[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
    }

    public int prefixLength() {
        return prefixLength;
    }

    public boolean isExactHost() {
        return prefixLength == networkBytes.length * 8;
    }

    public static Optional<IpMatcher> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        int slash = trimmed.indexOf('/');
        String addressPart = slash < 0 ? trimmed : trimmed.substring(0, slash);
        String prefixPart = slash < 0 ? null : trimmed.substring(slash + 1);

        InetAddress address;
        try {
            address = InetAddress.ofLiteral(addressPart);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        byte[] bytes = address.getAddress();
        int maxPrefix = bytes.length * 8;
        int prefix = maxPrefix;
        if (prefixPart != null) {
            try {
                prefix = Integer.parseInt(prefixPart);
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
            if (prefix < 0 || prefix > maxPrefix) {
                return Optional.empty();
            }
        }

        applyMask(bytes, prefix);
        return Optional.of(new IpMatcher(bytes, prefix));
    }

    private static void applyMask(byte[] bytes, int prefix) {
        int fullBytes = prefix / 8;
        int remainingBits = prefix % 8;
        if (remainingBits != 0) {
            int mask = 0xFF << (8 - remainingBits);
            bytes[fullBytes] = (byte) (bytes[fullBytes] & mask);
            fullBytes++;
        }
        for (int i = fullBytes; i < bytes.length; i++) {
            bytes[i] = 0;
        }
    }
}
