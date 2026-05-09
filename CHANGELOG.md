# Changelog

All notable changes to GeoBlock are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0-beta.1] - 2026-05-06

First public beta. All core features present and stable enough for
production use; the beta channel stays in place until real-world
feedback validates the API surface.

### Features

- Country filtering in **blacklist** or **whitelist** mode
  (ISO 3166-1 alpha-2 country codes).
- IP and UUID bypass list with full IPv4/IPv6 CIDR support.
- Free GeoIP through [db-ip.com](https://db-ip.com), no account
  required (CC-BY 4.0).
- Optional MaxMind GeoLite2 provider for admins who already have a
  free MaxMind account.
- LuckPerms integration with `/op` fallback when LuckPerms is absent.
- Async Discord webhook for connection denials and lookup failures,
  with toggleable IP inclusion.
- Hot reload via `/geoblock reload` and on-demand database refresh
  via `/geoblock update`.

### Notes

- The db-ip database is refreshed once per month upstream; the plugin
  automatically falls back to the previous month if the new file is
  not yet published.
- Discord notifications are fully optional and granular (per-event
  toggle, IP hiding for privacy-conscious deployments).

### Requirements

- Paper or Purpur 1.21.x (API target `26.1.2`).
- Java 25.
