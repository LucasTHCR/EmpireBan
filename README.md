# EmpireBan

Konfigurierbares Ban/Mute/Kick-System mit ID-Level-System, IP-Handling und VPN-Check für
**Spigot/Paper**, **BungeeCord** und **Velocity** — als eine einzige JAR-Datei
(`EmpireBan.jar`). Beim Laden erkennt die jeweilige Plattform automatisch nur ihren eigenen
Einstiegspunkt (Spigot liest `plugin.yml`, BungeeCord `bungee.yml`, Velocity die
`@Plugin`-annotierte Klasse) — der Rest der JAR wird ignoriert.

> **Wichtig:** Bei Proxy-Betrieb gehört die JAR **nur auf den Proxy** (Velocity oder
> BungeeCord), **nicht zusätzlich** auf die Spigot-Unterserver. Nur im reinen
> Standalone-Betrieb ohne Proxy kommt sie auf den Spigot-Server.

## Build

```bash
mvn clean package
```

Ergebnis: `target/EmpireBan.jar`.

## Funktionen

- **Ban/Mute/Kick/Warn-System** mit Historie pro Spieler
- **Konfigurierbares ID-Level-System** (`ids.yml`): beliebig viele benannte Bestrafungsgründe,
  jede mit eigenen Eskalationsstufen. Level werden automatisch anhand der Anzahl bisheriger
  Bestrafungen mit derselben ID hochgezählt (1. Verstoß = Level 1, 2. Verstoß = Level 2, ...).
  Ist das höchste konfigurierte Level erreicht, bleibt es dort.
- **IP-Handling**: erkennt, wenn ein neu verbindender Spieler dieselbe IP wie ein aktuell
  gebannter Spieler benutzt. Je nach Konfiguration wird nur das Team benachrichtigt
  (`bansys.notify`) oder der Alt-Account automatisch mitgebannt.
- **VPN-Check** über [vpnapi.io](https://vpnapi.io/): erkennt VPN/Proxy/Tor-Verbindungen beim
  Login und benachrichtigt das Team optional.
- **Chatfilter** (Blacklist-Wörter/Links) und **Chatverzögerung** (Slowmode) mit
  Bypass-Rechten.
- **MySQL- und SQLite-Unterstützung** (HikariCP-Connection-Pool), SQLite ist der
  Zero-Config-Standard.
- **Aktionslogs**: jede Bestrafung/Aufhebung wird protokolliert, einsehbar über
  `/bansys logs show` bzw. löschbar über `/bansys logs clear`.
- Deutsche Standard-Nachrichten (`messages_german.yml`), inklusive des klassischen
  Ban-Screens ("Verbindung unterbrochen ... Du kannst Im Forum ein Entbannungsantrag
  stellen!").

## Konfigurationsdateien

Werden beim ersten Start automatisch aus den JAR-Ressourcen ins Datenverzeichnis kopiert:

| Datei | Inhalt |
|---|---|
| `config.yml` | Datenbank (MySQL/SQLite), IP-Handling, VPN-Check, Chatfilter, Chatdelay, Sprache |
| `ids.yml` | Das konfigurierbare ID-Level-System |
| `messages_german.yml` | Alle Spieler-/Team-Nachrichten (Platzhalter, Farbcodes mit `&`) |

Zeitangaben sind intern immer in **Sekunden**: 1 Tag = `86400`, 1 Jahr = `31536000`,
`-1` = permanent. Befehle akzeptieren zusätzlich Kurzformen wie `1d12h`, `30m`, `perm`.

## Befehle & Berechtigungen

| Befehl | Berechtigung |
|---|---|
| `/bansystem` (Alias `/bansys`) | `bansys.bansys` |
| `/bansystem reload` | `bansys.reload` |
| `/bansystem ids create <ID> <Type> <OnlyAdmins> <duration> <reason>` | `bansys.ids.create` |
| `/bansystem ids delete <ID>` | `bansys.ids.delete` |
| `/bansystem ids edit <ID> add lvl <lvl> <Duration> <Type>` | `bansys.ids.addlvl` |
| `/bansystem ids edit <ID> remove lvl <lvl>` | `bansys.ids.removelvl` |
| `/bansystem ids edit <ID> set lvlduration <lvl> <Duration>` | `bansys.ids.setduration` |
| `/bansystem ids edit <ID> set lvltype <lvl> <Type>` | `bansys.ids.settype` |
| `/bansystem ids edit <ID> set onlyadmins <True/False>` | `bansys.ids.setonlyadmins` |
| `/bansystem ids edit <ID> set reason <reason>` | `bansys.ids.setreason` |
| `/bansystem ids show [ID]` | `bansys.ids.show` |
| `/bansys logs show [Seite]` | `bansys.logs.show` |
| `/bansys logs clear` | `bansys.logs.clear` |
| `/ban <Spieler> <ID\|Dauer> [Grund]` | `bansys.ban.<ID>` / `bansys.ban.all` / `bansys.ban.admin` |
| `/unban <Spieler> [Grund]` | `bansys.unban` |
| `/mute <Spieler> <ID\|Dauer> [Grund]` | `bansys.ban.<ID>` / `bansys.ban.all` |
| `/unmute <Spieler> [Grund]` | `bansys.unmute` |
| `/kick <Spieler> [Grund]` | `bansys.kick` (Bypass: `bansys.kick.bypass`) |
| `/check <Spieler>` | `bansys.check` |
| `/history <Spieler>` | `bansys.history.show` |
| `/deletehistory <Spieler>` | `bansys.history.delete` |
| Team-Benachrichtigungen (Alt-Account/VPN-Erkennung) | `bansys.notify` |
| Chatfilter-Bypass | `bansys.bypasschatfilter` |
| Chatdelay-Bypass | `bansys.bypasschatdelay` |
| Ban-Bypass (Kick beim Bannen unterdrücken) | `bansys.ban.bypass` |

## Architektur (für Weiterentwicklung)

Alles unter `de.empireblocks.empireban.core` ist plattformunabhängig (kein Bukkit-/Bungee-/
Velocity-Import):

- `db/` — HikariCP + reines JDBC, dialektbewusstes Schema für MySQL/SQLite
  (`eb_punishments`, `eb_logs`, `eb_ips`)
- `manager/` — Geschäftslogik: `PunishmentManager`, `BanIdManager`, `IpManager`,
  `HistoryManager`, `LogManager`
- `model/` — `Punishment`, `BanId`, `IdLevel`, `PunishmentType`
- `config/` — eigener SnakeYAML-Wrapper (`YamlDocument`) statt Bukkit-YAML, damit derselbe
  Code auf allen drei Plattformen läuft
- `platform/PlatformAdapter` — Interface, das jede Plattform implementiert (Nachrichten
  senden, kicken, Berechtigungen prüfen, Scheduler)

Die drei Plattform-Pakete (`spigot`, `bungeecord`, `velocity`) enthalten nur dünne Adapter,
Listener und Befehle, die den gemeinsamen Core aufrufen.

## Bekannte Einschränkungen / offene Punkte

- Kein GUI-Menü für die Ban-Liste (rein befehlsbasiert)
- Signierter Chat (ab Minecraft 1.19.1) kann von BungeeCord/Velocity selbst nicht abgefangen
  werden — dafür wäre ein zusätzlicher Chat-Adapter auf den Unterservern nötig
  (`config.yml: signed-chat-bypass`), der aktuell noch nicht mitgeliefert wird
- Kein natives Geyser-spezifisches Verhalten über den reinen `geyser.support`-Config-Flag
  hinaus
- Nur Deutsch als Sprachdatei vorhanden (Struktur ist mehrsprachenfähig,
  `messages_<sprache>.yml`)
