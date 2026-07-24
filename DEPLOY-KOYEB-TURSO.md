# Deploy X Clone with GitHub, Koyeb, and Turso

The hosted architecture is:

```text
JavaFX desktop clients -> Koyeb HTTPS API -> Turso/libSQL
```

- **GitHub** stores the source and runs CI.
- **Koyeb** builds the Dockerfile and hosts the public Java API.
- **Turso** stores shared account and session data using SQLite-compatible SQL.

The desktop app defaults to a local embedded backend backed by SQLite. When a
remote server URL is configured, it uses the Koyeb HTTPS API instead.

## 0. Rotate the exposed database token

The original database token was pasted into a chat. Revoke it in Turso and
create a replacement before deployment:

```text
turso db tokens invalidate xclone
turso db tokens create xclone
```

If the database has a different CLI name, substitute that name. Never commit,
paste into issues, or place this token in the JavaFX client.

## 1. GitHub repository

The intended repository is:

<https://github.com/Soheil-Aghayani/x-clone>

The local checkout should use it as `origin`:

```powershell
git remote set-url origin "https://github.com/Soheil-Aghayani/x-clone.git"
git remote -v
```

After reviewing and committing the project, push the current branch:

```powershell
git push -u origin feature/front-login
```

GitHub Actions will run the Maven tests under a virtual display and verify that
the Koyeb Docker image builds.

## 2. Create the Koyeb service

1. Sign in at <https://app.koyeb.com/>.
2. Select **Create Web Service** and choose **GitHub**.
3. Grant the Koyeb GitHub App access to `Soheil-Aghayani/x-clone`.
4. Select the branch containing this `Dockerfile`.
5. Choose the **Dockerfile** builder and use `Dockerfile` as its path.
6. Choose the free instance for testing.
7. Expose port `8080` using HTTP.
8. Set the HTTP health-check path to `/health`.

## 3. Configure Turso without exposing credentials

The database address is public configuration:

```text
libsql://xclone-damoon.aws-ap-northeast-1.turso.io
```

In the Koyeb control panel:

1. Create a Koyeb Secret named `TURSO_AUTH_TOKEN`.
2. Paste the **newly rotated** Turso token into that Secret.
3. Add these service environment variables:

```text
TURSO_DATABASE_URL=libsql://xclone-damoon.aws-ap-northeast-1.turso.io
TURSO_AUTH_TOKEN={{ secret.TURSO_AUTH_TOKEN }}
```

Koyeb defines `PORT` automatically. The backend converts the `libsql://` URL to
Turso's HTTPS endpoint, sends the token only in the Bearer authorization header,
and creates the `app_users` and `app_sessions` tables automatically.

Deploy the service and verify:

```text
https://YOUR-SERVICE-YOUR-ORG.koyeb.app/health
```

Expected response:

```json
{"status":"ok"}
```

## 4. Point desktop clients to Koyeb

For a Maven development run:

```powershell
$env:XCLONE_SERVER_URL = "https://YOUR-SERVICE-YOUR-ORG.koyeb.app"
mvn javafx:run
```

For a shareable Windows ZIP:

```powershell
.\build-portable.ps1 -ServerUrl "https://YOUR-SERVICE-YOUR-ORG.koyeb.app"
```

The ZIP contains `server-url.txt` beside the executable. It contains only the
public Koyeb URL. Friends can extract the ZIP and run the application without
installing Java or Maven.

Backend URL configuration priority:

1. Java property `-Dxclone.server.url=...`
2. Environment variable `XCLONE_SERVER_URL`
3. `server-url.txt` beside the packaged executable
4. Local default `http://127.0.0.1:8080`

## 5. Current shared-data boundary

Registration, login, and sessions use the shared Turso database. The existing
timeline, follows, notifications, chats, and media still use each desktop
client's local store. Those features need API endpoints and Turso tables (with
object storage for uploaded media) before the entire social network is shared.
