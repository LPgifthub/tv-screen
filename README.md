# TV Home

An ad-free home screen for the Google TV Streamer, in cream, forest green, chocolate and marble, with a soft evening palette after dark, live football scores, and automatic updates.

**Setup: about 15 minutes, once. You need the TV remote and your Pixel. No computer.**
TV Home guides you itself: its Setup page ticks each step off as it's done and shows a QR code for the next one.

---

## Step 1 – Install TV Home (5 minutes)

1. On the Streamer, install **Downloader** from the Play Store.
2. Let Downloader install apps: Settings → Apps → Security & restrictions → Unknown sources → **Downloader** → on.
3. Open Downloader and enter this address:

   `https://github.com/LPgifthub/tv-screen/releases/latest/download/tv-home.apk`

   **Don't type it with the remote.** On your Pixel, open the **Google Home** app → your TV → remote → keyboard icon, and paste it. Copy it from this page on your phone.
4. Press **Install**, then **Open**. TV Home opens on its **Setup** checklist.

## Step 2 – Make the Home button open TV Home (10 minutes)

This is the only fiddly bit: Google TV only lets you change the home screen with a one-off command, sent from your Pixel over Wi-Fi.

**On the TV**
1. Settings → System → About → click **Android TV OS build** 7 times ("You are now a developer").
2. Settings → System → Developer options → **Wireless debugging** → on.
3. Choose **Pair device with pairing code**. Leave that box showing: it has an IP address, a **pairing port** and a 6-digit code.

**On your Pixel** (same Wi-Fi as the TV)
4. Install **Bugjaeger Mobile ADB** from the Play Store.
5. In Bugjaeger, pair over Wi-Fi with a pairing code: enter the IP, the **pairing port** and the code.
6. Then connect using the IP and port on the **main Wireless debugging screen**. ⚠️ That's a different port from the pairing box. If anything fails, close the pairing box and open it again: the code and port change each time.
7. Get the command: point your Pixel camera at the **QR code on TV Home's Setup page** and tap **Copy text**. (Or copy it from below.)
8. Open Bugjaeger's **shell**, paste, and run it.

```
cmd package set-home-activity uk.lloyd.tvhome/.MainActivity; appops set uk.lloyd.tvhome ACCESS_RESTRICTED_SETTINGS allow; pm grant uk.lloyd.tvhome android.permission.WRITE_SECURE_SETTINGS
```

9. Watch TV Home's Setup page: within a few seconds **Home button** turns green with three ✓.
10. Turn **Wireless debugging** off again. It's only needed for setup.

What the command does: it makes TV Home the default home app. It also allows the backup Home button service, which Android greys out for apps not from the Play Store. Finally, it lets TV Home switch that backup back on after a restart, because Android TV 14 can turn it off.

**No Pixel to hand?** Any laptop with Google's free "platform-tools": run `adb pair IP:PAIRINGPORT`, then `adb connect IP:PORT`, then `adb shell "<the command above>"`.

## Step 3 – Football scores (5 minutes, optional)

1. When Step 2 is done, Setup's QR code switches to football-data.org. Scan it, sign up free, and the key arrives by email.
2. Setup → **Add football scores key**. Paste it using the Google Home app's TV remote keyboard on your phone.

**Next up** then shows Norwich, Forest, England and the Premier League:
- the next fixture;
- the live score and minute during a match, checked every minute;
- the full-time result for a few hours afterwards.

Free-plan scores can lag a few minutes behind live TV, and England only appears during the World Cup and the Euros. Why not FotMob? It has no public API, and its terms don't allow other apps to use its data.

---

## Automatic from then on

| What | How it works |
| --- | --- |
| **Updates** | TV Home checks GitHub every 6 hours. The first update asks you to confirm once. After that, new builds install by themselves, but only while TV Home is on screen, never during a programme. You can switch this off in Setup. |
| **Evening colours** | Soft dark palette from 9pm to 7am. Setup lets you choose automatic, always day, or always evening. |
| **Home button after restarts** | If Android switches the backup service off, TV Home switches it back on at start-up. |
| **Weather** | Horsford, every 30 minutes. It retries every 2 minutes until the internet is up after a restart. |
| **New apps** | They appear in All apps as soon as you come back to TV Home. |
| **Safety net** | If TV Home crashes 3 times within 3 minutes, Google's home screen opens instead, so the TV is never stuck. |

## If something isn't right

| What you see | What to do |
| --- | --- |
| Home still opens Google's screen | Open Setup. If "default home app" has no ✓, run Step 2 again. The backup service catches Home in the meantime. |
| Bugjaeger won't pair or connect | Check both devices are on the same Wi-Fi. Pair with the pairing port, connect with the main port. Reopen the pairing box for a fresh code. |
| A main button says "Not installed" | Press it: it opens that app in the Play Store. |
| Next up says "Add free key in Setup" | Do Step 3. |
| Next up says "Key not accepted" | Re-enter the key in Setup and check it against the email. |
| An update needs confirming | Normal for the first update only. Press Install. |
| TV Home keeps closing | The safety net opens Google's home screen. Tell Claude what you were doing. |
| "All home controls" does nothing | Expected until Stage 2. Use the remote's dashboard for the Google Home panel. |

## Removing TV Home

Settings → Apps → TV Home → Uninstall. The TV goes straight back to Google's home screen. Nothing of Google's is ever switched off or removed.

## Looking ahead (2027)

Google is introducing developer verification for apps installed outside the Play Store. It started in four countries in September 2026 and reaches the UK in 2027. When it arrives, register TV Home with a free "limited distribution" Android Developer Console account (email only, up to 20 devices) so updates keep installing. Apps installed over ADB are exempt either way.

## Stages

- [x] Stage 1: home screen, six app buttons, clock, weather, all apps, Home button, guided setup, automatic updates, evening palette, fixtures and live scores
- [ ] Stage 2: scenes and home controls
- [ ] Stage 3: outdoor camera tile and doorbell pop-up
- [ ] Stage 4: calendar

Every change to the code is built automatically by GitHub (Actions tab) and published to the download link above.
