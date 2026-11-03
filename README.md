# TV Home

An ad-free home screen for the Google TV Streamer, in cream, forest green, chocolate and marble.

## First install (no computer needed)

1. On the Streamer, install **Downloader** from the Play Store.
2. Allow Downloader to install apps: Settings → Apps → Security & restrictions → Unknown sources → Downloader → on.
3. Open Downloader and enter:
   `https://github.com/LPgifthub/tv-screen/releases/latest/download/tv-home.apk`
4. Press **Install**, then **Open**.

After that, TV Home updates itself: **Setup → Install update** (it shows "update ready" when there's a new build).

## Make the Home button open TV Home (one-time)

Google TV only lets you change the home app with a one-off command. You can type it on the TV itself:

1. Turn on Developer options: Settings → System → About → click **Android TV OS build** 7 times.
2. Settings → System → Developer options → turn on **Wireless debugging**.
3. Install a free on-TV ADB app from the Play Store (for example **ADB TV**) and connect it to the TV itself.
4. Run:

   ```
   cmd package set-home-activity uk.lloyd.tvhome/.MainActivity
   ```

5. Press Home on the remote. TV Home's **Setup** page should now say **Default home app: TV Home ✓**.

**Backup method** (only if the command above doesn't stick): run

```
appops set uk.lloyd.tvhome ACCESS_RESTRICTED_SETTINGS allow
```

then Setup → **Open Accessibility settings** → **TV Home – Home button** → on. Android TV may switch this off after a restart, which is why it's the backup.

Google's own home screen stays installed either way, so the Google Home features keep working. We never disable it.

## To remove it

Uninstall TV Home (Settings → Apps). The TV goes back to Google's home screen. If you used the backup method, switch the Accessibility setting off first.

## Stages

- [x] Stage 1: home screen, six app buttons, clock, weather, all apps, Home button, self-update
- [ ] Stage 2: scenes and home controls
- [ ] Stage 3: outdoor camera tile and doorbell pop-up
- [ ] Stage 4: calendar and football fixtures

Every change to this code is built automatically by GitHub (Actions tab) and published to the download link above.
