# GeoBlock

A Paper plugin that filters incoming connections by country using a local
MMDB database, with bypass support for trusted IPs and UUIDs and Discord
webhook notifications.

## Features

- Country-based filtering in `blacklist` or `whitelist` mode (ISO 3166-1
  alpha-2 codes).
- IP and UUID bypass list with IPv4/IPv6 CIDR support.
- Permission integration through the standard Bukkit API: works with
  LuckPerms when present, falls back to vanilla `/op` otherwise.
- Async Discord webhook for connection denials and lookup failures, with
  toggleable IP inclusion for privacy-conscious deployments.
- Hot configuration reload via `/geoblock reload` and on-demand database
  refresh via `/geoblock update`.

## GeoIP providers

GeoBlock supports two interchangeable sources for the country database,
selected through `geoip.provider` in `config.yml`.

| Provider  | Account | Update cadence | Notes                                  |
|-----------|---------|----------------|----------------------------------------|
| `db-ip`   | none    | monthly        | **default**, free, CC-BY 4.0           |
| `maxmind` | free    | weekly         | requires a MaxMind license key         |

The plugin downloads the database automatically on enable and refreshes
it on the configured interval. `/geoblock update` triggers an immediate
download regardless of the schedule.

This product includes IP-Country data created by db-ip.com, made
available under a Creative Commons Attribution 4.0 International License
(<https://db-ip.com>).

## Build

```sh
./gradlew build
```

The shaded jar is produced at `build/libs/GeoBlock-<version>.jar` with all
runtime dependencies relocated under `fr.horizonsmp.geoBlock.lib.*` to
avoid clashes with other plugins.

## Run a development server

```sh
./gradlew runServer
```

Boots a Paper development server with the plugin loaded.

## Requirements

- Java 25
- Paper 1.21.x (Minecraft `26.1.2` API target)

## Configuration

After the first run, edit the files generated under `plugins/GeoBlock/`:

- `config.yml` — filtering mode, country list, GeoIP provider and
  Discord options. Each option is documented inline.
- `messages.yml` — user-facing strings (kick reasons, command output).
- `whitelist.yml` — bypass list (managed through commands).

The plugin reads `config.yml`, `messages.yml` and `whitelist.yml` at
startup and through `/geoblock reload`.

## Commands

| Command                                | Permission                | Description                                |
|----------------------------------------|---------------------------|--------------------------------------------|
| `/geoblock reload`                     | `geoblock.command.reload` | Reload all configuration files             |
| `/geoblock update`                     | `geoblock.command.update` | Force an immediate database download       |
| `/geoblock bypass add ip <ip\|cidr>`   | `geoblock.command.bypass` | Add an IP or CIDR to the bypass list       |
| `/geoblock bypass add uuid <uuid>`     | `geoblock.command.bypass` | Add a player UUID to the bypass list       |
| `/geoblock bypass remove ip <ip>`      | `geoblock.command.bypass` | Remove an IP/CIDR entry                    |
| `/geoblock bypass remove uuid <uuid>`  | `geoblock.command.bypass` | Remove a UUID entry                        |
| `/geoblock bypass list`                | `geoblock.command.bypass` | Show every bypass entry                    |
| `/geoblock help`                       | `geoblock.admin`          | Print the command summary                  |

`geoblock.admin` (default `op`) implies the child permissions, so
operators get full access on a server without LuckPerms.

## License

See LICENSE file.
