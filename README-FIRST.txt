X CLONE - WINDOWS PORTABLE
==========================

1. Extract the complete "X Clone" folder from the ZIP.
2. Open the folder and double-click "X Clone.exe".
3. Create an account or sign in.

No Java, Maven, IDE, or separate backend command is required.

Backend selection:
- This build connects to https://x-clone.alirezalotfimoghaddam.ir by default.
- A server-url.txt beside the EXE can override that address for another HTTPS backend.

Client-only preferences, drafts, chat, and cached media are stored here:
  %USERPROFILE%\.x-clone

Local-backend account and social data is stored here:
  %USERPROFILE%\.x-clone-server
  (SQLite database: xclone.db)

Important:
- Do not run the EXE from inside the ZIP. Extract the whole folder first.
- Core profiles, posts, interactions, follows, and notifications are shared when this
  build is configured with a public backend.
- Selected PNG, JPEG, and GIF files are uploaded to the backend and can be viewed
  by other users connected to the same public service.
- Sign-in sessions are restored securely and checked with the server at startup.
- Never put a Turso token in server-url.txt or anywhere in this app.
- A shared build's server-url.txt should contain only a public HTTPS backend URL.

If the app closes unexpectedly, diagnostic information is written to:
  %USERPROFILE%\.x-clone\client-error.log
