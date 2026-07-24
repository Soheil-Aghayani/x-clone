# X Clone

X Clone is a JavaFX desktop social-network client inspired by X. It includes
authentication, profiles, a timeline, replies, reposts, likes, bookmarks,
notifications, search, chat screens, GIF/image media, Persian text support, and
portable Windows packaging.

## Architecture

```text
JavaFX desktop app
        |
        | HTTPS JSON API
        v
Koyeb Java backend
        |
        | Turso SQL over HTTP
        v
Turso/libSQL (SQLite-compatible)
```

Local development starts an embedded HTTP backend automatically and saves
accounts in `%USERPROFILE%\.x-clone-server\xclone.db` using SQLite. A hosted
backend uses `TURSO_DATABASE_URL` and `TURSO_AUTH_TOKEN`.

The Turso token must exist only in the Koyeb Secret store. Never put it in the
desktop client, `server-url.txt`, source code, screenshots, issues, or commits.

## Requirements for source builds

- JDK 21
- Maven 3.9+

Run locally:

```powershell
git clone https://github.com/Soheil-Aghayani/x-clone.git
cd x-clone
mvn clean javafx:run
```

Run the tests:

```powershell
mvn test
```

## Hosted deployment

The repository contains:

- `Dockerfile` for the Koyeb backend
- `.github/workflows/ci.yml` for Maven and Docker CI
- `.env.example` with secret-free configuration examples
- `DEPLOY-KOYEB-TURSO.md` with the complete deployment procedure

See [Deploy with Koyeb and Turso](DEPLOY-KOYEB-TURSO.md).

## Windows portable build

Build a local-only package:

```powershell
.\build-portable.ps1
```

Build a package connected to Koyeb:

```powershell
.\build-portable.ps1 -ServerUrl "https://YOUR-SERVICE-YOUR-ORG.koyeb.app"
```

The shareable file is written to `dist\X-Clone-Windows-Portable.zip`.

## Configuration

Backend:

| Variable | Purpose |
| --- | --- |
| `PORT` | HTTP port; supplied by Koyeb |
| `TURSO_DATABASE_URL` | Hosted `libsql://` database address |
| `TURSO_AUTH_TOKEN` | Turso database token, stored as a Koyeb Secret |

Desktop client:

| Setting | Purpose |
| --- | --- |
| `XCLONE_SERVER_URL` | Public Koyeb HTTPS URL |
| `server-url.txt` | Portable-build alternative to the environment variable |

## Current limitation

Accounts and sessions are ready for the shared backend. Posts, follows,
notifications, chat messages, and media are still maintained by the local
desktop stores. They must be moved behind authenticated backend APIs before all
users see the same complete social network.
