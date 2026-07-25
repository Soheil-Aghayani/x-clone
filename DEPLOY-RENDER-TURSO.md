# Deploy X Clone with GitHub, Render, and Turso

The public architecture is:

```text
JavaFX desktop apps -> Render HTTPS API -> Turso/libSQL
```

- GitHub stores the source and runs continuous integration.
- Render builds the repository's Dockerfile and hosts the public Java API.
- Turso stores accounts and shared social data using SQLite-compatible SQL.

The desktop app still works without cloud deployment: it starts a local backend with SQLite. A public Render URL is required only when people on different computers should share the same network.

## 1. Protect the Turso token

If a token was ever pasted into chat, a screenshot, or a public location, revoke it in Turso and create a new one before deployment. Never commit the token and never place it in the JavaFX client.

The database URL is not the secret:

```text
libsql://xclone-damoon.aws-ap-northeast-1.turso.io
```

The long authentication token is the secret.

## 2. Confirm GitHub CI

Repository:

<https://github.com/Soheil-Aghayani/x-clone>

Push the branch you intend to deploy. GitHub Actions should run the Maven tests and verify the Docker build:

```powershell
git push -u origin feature/front-login
```

CI verifies the build. Render's GitHub integration handles deployment when auto-deploy is enabled.

## 3. Create the Render web service

1. Open the Render dashboard.
2. Choose **New** and then **Web Service**.
3. Connect the `Soheil-Aghayani/x-clone` GitHub repository.
4. Use these values:

| Render field | Value |
| --- | --- |
| Name | `x-clone` or another unique name |
| Language / Runtime | `Docker` |
| Branch | `feature/front-login` until the finished work is merged to `main` |
| Region | The closest available region |
| Root Directory | Leave empty |
| Dockerfile Path | `./Dockerfile` |
| Health Check Path | `/health` |

Select a free instance if it is available for your account.

## 4. Add the environment variables correctly

Render shows a **key** field on the left and a **value** field on the right. Add two separate rows:

| Key | Value |
| --- | --- |
| `TURSO_DATABASE_URL` | `libsql://xclone-damoon.aws-ap-northeast-1.turso.io` |
| `TURSO_AUTH_TOKEN` | Your newly rotated Turso token |

Do not click **Generate** for `TURSO_AUTH_TOKEN`. Generate creates an unrelated random value; Render needs the token issued by Turso.

Render supplies `PORT` automatically, so it normally does not need to be added manually.

Optional:

| Key | Value |
| --- | --- |
| `XCLONE_NPC_INTERVAL_SECONDS` | `900` for one shared demo activity slot every 15 minutes |
| `XCLONE_ADMIN_USERNAME` | `potato` if that existing account should receive the database `admin` role |

Fake content is disabled by default. After signing in with the configured administrator, open **More → Settings and privacy** to enable or disable the shared demo network. The setting is stored in Turso, applies to every user, and cannot be changed by a normal account.

Click **Deploy Web Service** and wait for the first build to finish.

## 5. Verify the public backend

Render gives the service a URL similar to:

```text
https://x-clone-xxxx.onrender.com
```

Open this address in a browser:

```text
https://x-clone-xxxx.onrender.com/health
```

Expected response:

```json
{"status":"ok"}
```

On a free service, the first request after a period of inactivity can take roughly a minute while the service wakes up. The desktop client allows a longer initial request for this reason.

If deployment fails, check **Logs** in Render first. The most common causes are:

- an expired or incorrect Turso token
- the database URL and token entered in the same field
- the wrong Git branch
- a token copied with extra spaces or quotation marks

## 6. Connect your own desktop app

For a Maven development run:

```powershell
$env:XCLONE_SERVER_URL = "https://x-clone.alirezalotfimoghaddam.ir"
mvn javafx:run
```

The desktop client already uses that domain by default. The environment variable
is only needed when testing a different deployment. To remove an override later:

```powershell
Remove-Item Env:XCLONE_SERVER_URL
```

## 7. Build a ZIP for other people

```powershell
.\build-portable.ps1
```

That command embeds `https://x-clone.alirezalotfimoghaddam.ir`. Use
`-ServerUrl "https://another-host.example"` only for a different backend.

Send:

```text
dist\X-Clone-Windows-Portable.zip
```

The recipient should:

1. Extract the entire ZIP.
2. Open the extracted `X Clone` folder.
3. Run `X Clone.exe`.
4. Create an account or sign in.

The generated `server-url.txt` contains only the public Render address. It must never contain the Turso token.

Backend URL priority:

1. Java property `-Dxclone.server.url=...`
2. Environment variable `XCLONE_SERVER_URL`
3. `server-url.txt` beside the packaged executable
4. Hosted default `https://x-clone.alirezalotfimoghaddam.ir`

The repository's `launch.ps1` explicitly selects `http://127.0.0.1:8080` for
local backend development; portable builds do not use that local address.

## 8. Understand the current deployment limitation

Render auto-deploy works only after the service is connected to the correct GitHub repository and branch. Until that is configured, pushing code updates GitHub and runs CI but does not update the public backend.

Core social data is shared through Turso: accounts, sessions, profiles, posts, replies, quotes, likes, reposts, bookmarks, follows, notifications, media metadata, and uploaded PNG/JPEG/GIF bytes. Chat, drafts, polls, temporary cache, and some personal preferences remain local.
