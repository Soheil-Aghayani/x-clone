X CLONE - WINDOWS PORTABLE
==========================

1. Extract the complete "X Clone" folder from the ZIP.
2. Open the folder and double-click "X Clone.exe".
3. Create an account or sign in.

No Java, Maven, IDE, or separate backend command is required.

Backend selection:
- If server-url.txt is included beside the EXE, the app uses that shared HTTPS backend.
- Otherwise, the app starts its local development backend automatically.

Client-only preferences, drafts, chat, and cached media are stored here:
  %USERPROFILE%\.x-clone

Local-backend account and social data is stored here:
  %USERPROFILE%\.x-clone-server
  (SQLite database: xclone.db)

Important:
- Do not run the EXE from inside the ZIP. Extract the whole folder first.
- Core profiles, posts, interactions, follows, and notifications are shared when this
  build is configured with a public backend.
- Files selected from your computer remain local until cloud media uploads are added.
- Never put a Turso token in server-url.txt or anywhere in this app.
- A shared build's server-url.txt should contain only the public Render HTTPS URL.

If the app closes unexpectedly, diagnostic information is written to:
  %USERPROFILE%\.x-clone\client-error.log
