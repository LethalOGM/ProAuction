# ProAuction

ProAuction is a modern, configurable auction house plugin for Paper servers.
It is designed to be stable, lightweight, and expandable, with support for
multiple currencies and GUI-based interaction.

This project focuses on long-term maintainability and clean architecture.

---

## ✨ Features

- GUI-based auction browsing
- Purchase confirmation GUI
- Multi-currency support
  - Vault (money)
  - Rubies / shards-based economy
- Sell, cancel, and manage auctions
- Expired listings with reclaim support
- Configurable banned items
- YAML-based storage (no database required)
- Fully configurable GUI and messages
- GitHub-based update checker

---

## 📦 Commands

| Command | Description |
|------|------------|
| `/ah` | Open the auction house GUI |
| `/ah sell <price> <currency>` | Sell the item in hand |
| `/ah cancel <id>` | Cancel an active listing |
| `/ah expired` | View expired listings |
| `/ah expired claim <id>` | Claim an expired item |
| `/ah reload` | Reload configuration |

---

## 💰 Currencies

Currencies are defined using a provider system.

### Built-in support
- **Vault** – Standard economy plugins
- **Rubies** – Custom shard-based currency

JOIN DISCORD FOR MORE
