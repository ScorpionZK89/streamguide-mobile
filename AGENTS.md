# StreamGuide repository workflow

- Keep signing keys and passwords outside this repository. Never commit `.jks`, `.keystore`, or secret values.
- After a completed change, run the relevant tests and a debug build before publishing.
- Commit the completed change to `main` and push it to `origin` so GitHub remains the shared source of truth.
- A push to `main` automatically publishes a signed APK as a GitHub Release. Do not create an extra version tag unless explicitly requested.
- Preserve working IPTV, network, database, and playback behavior unless the task specifically requires changing it.
