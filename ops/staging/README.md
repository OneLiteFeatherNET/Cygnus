# Staging-Deploy

`deploy-staging.sh` läuft auf dem Staging-Server als Benutzer `cloud` und holt die
neuesten Snapshot-Builds von Cygnus aus dem Reposilite-Repo
`onelitefeather-snapshots`. Der Server ist von GitHub aus nicht erreichbar, deshalb
zieht er die Builds selbst.

## Was das Skript macht

1. Ermittelt den Kanal: die erste offene PR mit Label `staging` ergibt `pr-<n>`,
   sonst `main`.
2. Sucht in den Maven-Metadaten von `cygnus-game` die neueste Version für diesen
   Kanal. Gibt es für eine PR noch keinen Snapshot, fällt es auf `main` zurück.
3. Lädt `cygnus-game` und `cygnus-bridge` herunter, prüft beide Jars und die
   `Implementation-Version` des Game-Jars.
4. Ersetzt im CloudNet-Template `Slender-Staging/current` die `application.jar`
   und das Bridge-Extension-Jar.
5. Stoppt alle laufenden `Slender-Staging-<n>`-Services. CloudNet startet frische
   Services aus dem Template.

Ist der Stand schon ausgerollt (Marker in `~/.local/state/cygnus-staging/deployed`),
beendet sich das Skript ohne Ausgabe.

## Voraussetzungen

`curl`, `jq`, `unzip`, `flock`, `screen`. `xmllint` ist optional, ohne es nutzt das
Skript `sed`.

## Konfiguration

Datei `~/.config/cygnus-staging/env` (Rechte `600`):

```sh
REPO_USER=<reposilite-lesebenutzer>
REPO_TOKEN=<reposilite-lesetoken>
# optional, erhöht das GitHub-Limit für Abfragen:
# GITHUB_TOKEN=<github-token-mit-leserecht>
```

```sh
chmod 600 ~/.config/cygnus-staging/env
```

Weitere Variablen (`CLOUDNET_DIR`, `TASK`, `TEMPLATE_DIR`, `SCREEN_SESSION` und so
weiter) lassen sich in der Umgebung überschreiben. Die Defaults stehen am Anfang des
Skripts.

## Installation

```sh
mkdir -p ~/cygnus-staging
cp ops/staging/deploy-staging.sh ~/cygnus-staging/
chmod +x ~/cygnus-staging/deploy-staging.sh
~/cygnus-staging/deploy-staging.sh --dry-run   # zeigt nur, was passieren würde
```

Crontab des Benutzers `cloud`:

```cron
*/2 * * * * $HOME/cygnus-staging/deploy-staging.sh >> $HOME/cygnus-staging/deploy.log 2>&1
```

## PR für Staging auswählen

Label `staging` auf die PR setzen. Der GitHub-Workflow `staging-snapshot.yml` baut
die PR dann als `pr-<n>` und veröffentlicht sie. Das Label darf nur an einer PR
hängen. Setzt man es an einer zweiten PR, entfernt der Workflow es von der ersten.

Wird das Label entfernt, zieht der Server beim nächsten Lauf wieder `main`.
