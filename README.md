# GeoBlock

A Paper plugin that filters incoming connections by country using a local
MaxMind GeoLite2 database, with bypass support for trusted IPs and UUIDs,
optional VPN/proxy detection, and Discord webhook notifications.

## Features (planned)

- Country-based filtering in `blacklist` or `whitelist` mode (ISO 3166-1 alpha-2).
- IP and UUID bypass list (CIDR ranges supported, IPv4 + IPv6).
- Permission integration through the standard Bukkit API
  (works with LuckPerms or vanilla `/op`).
- Optional VPN/proxy detection through MaxMind GeoIP2 Anonymous-IP
  database (paid product, separate from the free GeoLite2 country DB).
- Async Discord webhook for connection denials.
- Hot configuration reload via `/geoblock reload`.

## Build

```sh
./gradlew build
```

The shadow jar is produced under `build/libs/GeoBlock-<version>.jar` with all
runtime dependencies relocated under `fr.horizonsmp.geoBlock.lib.*`.

## Run a test server

```sh
./gradlew runServer
```

This starts a Paper development server with the plugin loaded.

## Requirements

- Java 25
- Paper 1.21.x (Minecraft `26.1.2` API target)
- A MaxMind GeoLite2 license key (free) for automatic database updates,
  or a manually provided MMDB file under the plugin data folder.

## License

See LICENSE file.
