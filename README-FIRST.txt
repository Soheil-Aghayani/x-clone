X CLONE - WINDOWS PORTABLE
==========================

1. Extract the complete "X Clone" folder from the ZIP.
2. Open the folder and double-click "X Clone.exe".
3. Create an account or sign in.

No Java, Maven, IDE, or separate backend command is required.

Backend selection:
- If server-url.txt is included beside the EXE, the app uses that shared HTTPS backend.
- Otherwise, the app starts its local development backend automatically.

Local timeline, chat, profile, and media data is currently stored here:
  %USERPROFILE%\.x-clone

Local-backend account data is stored here:
  %USERPROFILE%\.x-clone-server
  (SQLite database: xclone.db)

Important:
- Do not run the EXE from inside the ZIP. Extract the whole folder first.
- Profile and media files selected from your computer remain local for now.
- Never put a Turso token in server-url.txt or anywhere in this app.
- A shared build's server-url.txt should contain only the public Koyeb HTTPS URL.

If the app closes unexpectedly, diagnostic information is written to:
  %USERPROFILE%\.x-clone\client-error.log
