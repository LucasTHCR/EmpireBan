<div align="center">
  <img src="icon.png" alt="EmpireBan" width="128" height="128">

  <a href="https://dc.gg/paperstream">
    <img src="https://img.shields.io/badge/Discord-dc.gg%2Fpaperstream-5865F2?style=for-the-badge&logo=discord" alt="Discord" />
  </a>
</div>

[![bStats](https://bstats.org/signatures/bukkit/EmpireBan.svg)](https://bstats.org/plugin/bukkit/EmpireBan/33126)
# EmpireBan
### *Your server. Your rules.*

> A configurable ban/mute/kick system with escalating punishment IDs, alt-account detection via IP, and VPN/proxy checking — for **Spigot/Paper**, **BungeeCord**, and **Velocity**, shipped as a single JAR.

---

## ✨ Features

| Feature | Description |
|---|---|
| **Ban/Mute/Kick/Warn** | Full punishment system with per-player history. |
| **Configurable Escalation IDs** | Define any number of named punishment reasons (`ids.yml`), each with its own escalation levels — 1st offense, 2nd offense, and so on, capped at the harshest configured level once exhausted. |
| **Alt-Account Detection** | Flags staff or auto-bans players who join from the IP of a currently banned player. |
| **VPN/Proxy/Tor Check** | Optional lookup via [vpnapi.io](https://vpnapi.io/) on join. |
| **Chat Filter & Slowmode** | Blacklist words/links and enforce a configurable chat delay, both with bypass permissions. |
| **MySQL or SQLite** | HikariCP-pooled MySQL, or zero-config SQLite by default. |
| **Action Logs** | Every punishment and revocation is logged, viewable and clearable in-game. |
| **Single Cross-Platform JAR** | One `EmpireBan.jar` — each platform only reads its own entry point and ignores the rest. |

---

## 📦 Dependencies

- **Spigot / Paper**, **BungeeCord**, or **Velocity** — `required` *(exactly one, depending on where you run it)*
- **vpnapi.io** — `optional` — [vpnapi.io](https://vpnapi.io/) *(free tier: up to 100 lookups/day)*

---

## 🔧 How It Works

EmpireBan ships as a single JAR containing all three platform integrations. On load, each platform only reads its own entry point — Spigot reads `plugin.yml`, BungeeCord reads `bungee.yml`, Velocity reads the `@Plugin`-annotated class — and the rest of the JAR is ignored.

> **Proxy setups:** install the JAR **only on the proxy** (Velocity or BungeeCord), **not** on the Spigot backend servers as well. It only belongs on a Spigot server in a pure standalone setup with no proxy in front.

Punishments are organized around **IDs** — named, reusable reasons like `griefing` or `cheating` — each with an ordered list of escalation levels. A player's Nth punishment under a given ID applies that ID's Nth level (capped at the last level once every level has been used), so a first offense might warn, a second mute, and a third ban — fully configurable per ID.

On join, EmpireBan checks the connecting player's IP against currently banned players and, depending on config, either just notifies staff or automatically bans the alt account too. If VPN checking is enabled, it also queries vpnapi.io for VPN/proxy/Tor usage on that IP.

---

## ⚙️ Configuration

<details>
<summary><b>Click to expand the full config.yml</b></summary>

```yaml
# EmpireBan - Konfiguration
# Zeiten in diesem Plugin werden intern immer in Sekunden gespeichert.
# Beispiele: 1 Tag = 86400, 1 Jahr = 31536000, -1 = Permanent

language: german

database:
  # sqlite oder mysql
  type: sqlite
  mysql:
    host: localhost
    port: 3306
    database: empireban
    username: root
    password: ''

ip-handling:
  # Wenn aktiviert werden Spieler mit einer IP eines gebannten Spielers automatisch gebannt.
  # Wenn deaktiviert wird nur das Team benachrichtigt (siehe notify-staff & Berechtigung bansys.notify).
  autoban: false
  notify-staff: true

vpn-check:
  enabled: false
  # Kostenloser Key ab 100 Joins/Tag benötigt: https://vpnapi.io/
  api-key: ''
  autoban: false

chat-filter:
  enabled: true
  blacklist:
    - 'discord.gg/'
    - 'http://'
    - 'https://'

chat-delay:
  seconds: 3

geyser:
  support: true

# Ab Minecraft 1.19.1 signieren Clients Chatnachrichten. BungeeCord kann signierte Nachrichten
# nicht selbst abfangen/canceln - dafür muss auf jedem Unterserver zusätzlich die beiliegende
# "BanSystem-SpigotChatAdapter" installiert werden, welche Chat-Events an den Proxy weiterreicht.
signed-chat-bypass: true
```

</details>

`ids.yml` holds the configurable escalation-ID system, and `messages_german.yml` holds every player- and staff-facing message (color codes via `&`, placeholders). Both are copied into the data folder from the JAR on first start.

> **Note:** EmpireBan currently ships with German player-facing messages only (`messages_german.yml`). The structure supports additional `messages_<language>.yml` files, but only German is included out of the box.

Durations are always stored internally in **seconds**: 1 day = `86400`, 1 year = `31536000`, `-1` = permanent. Commands additionally accept shorthand like `1d12h`, `30m`, `perm`.

---

## 🔑 Commands & Permissions

| Command | Permission |
|---|---|
| `/bansystem` (alias `/bansys`) | `bansys.bansys` |
| `/bansystem reload` | `bansys.reload` |
| `/bansystem ids create <id> <type> <onlyAdmins> <duration> <reason>` | `bansys.ids.create` |
| `/bansystem ids delete <id>` | `bansys.ids.delete` |
| `/bansystem ids edit <id> add lvl <lvl> <duration> <type>` | `bansys.ids.addlvl` |
| `/bansystem ids edit <id> remove lvl <lvl>` | `bansys.ids.removelvl` |
| `/bansystem ids edit <id> set lvlduration <lvl> <duration>` | `bansys.ids.setduration` |
| `/bansystem ids edit <id> set lvltype <lvl> <type>` | `bansys.ids.settype` |
| `/bansystem ids edit <id> set onlyadmins <true/false>` | `bansys.ids.setonlyadmins` |
| `/bansystem ids edit <id> set reason <reason>` | `bansys.ids.setreason` |
| `/bansystem ids show [id]` | `bansys.ids.show` |
| `/bansys logs show [page]` | `bansys.logs.show` |
| `/bansys logs clear` | `bansys.logs.clear` |
| `/ban <player> <id\|duration> [reason]` | `bansys.ban.<id>` / `bansys.ban.all` / `bansys.ban.admin` |
| `/unban <player> [reason]` | `bansys.unban` |
| `/mute <player> <id\|duration> [reason]` | `bansys.ban.<id>` / `bansys.ban.all` |
| `/unmute <player> [reason]` | `bansys.unmute` |
| `/kick <player> [reason]` | `bansys.kick` *(bypass: `bansys.kick.bypass`)* |
| `/check <player>` | `bansys.check` |
| `/history <player>` | `bansys.history.show` |
| `/deletehistory <player>` | `bansys.history.delete` |
| — | `bansys.notify` *(receives alt-account/VPN staff alerts)* |
| — | `bansys.bypasschatfilter` |
| — | `bansys.bypasschatdelay` |
| — | `bansys.ban.bypass` *(suppress kick-on-ban)* |

---

## 🧩 Known Limitations

- No GUI menu for the ban list — command-driven only.
- Signed chat (Minecraft 1.19.1+) can't be intercepted by BungeeCord/Velocity on their own — that needs the companion Spigot chat adapter on each backend server (`config.yml: signed-chat-bypass`), which isn't bundled yet.
- Geyser support is limited to the `geyser.support` config flag; no native Geyser-specific behavior beyond that.
- Only German is shipped as a message language (structure supports more, see above).

---

## 🚀 Getting Started

1. Drop `EmpireBan.jar` into your `plugins/` folder — **only on the proxy** (Velocity/BungeeCord) if you run one, otherwise on the Spigot server.
2. Start the server once to generate `config.yml`, `ids.yml`, and `messages_german.yml`.
3. Set your database (SQLite by default, or MySQL) and enable VPN checking if you want it.
4. Reload with `/bansystem reload` — no restart needed.

---

## 👥 Credits

| | |
|---|---|
| **LucasTHCR** | Creator & maintainer |

Discord: [dc.gg/paperstream](https://dc.gg/paperstream)

Open source under the MIT License.
