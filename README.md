# TV Home

An ad-free home screen for the Google TV Streamer, in cream, forest green, chocolate and marble, with a soft evening palette after dark.

Setup takes about 20 minutes, once. You need the TV remote and your Pixel phone. No computer.

## Part 1 – Install TV Home on the Streamer (5 minutes)

1. On the Streamer, install **Downloader** from the Play Store.
2. Let Downloader install apps: Settings → Apps → Security & restrictions → Unknown sources → **Downloader** → on.
3. Open Downloader and enter:
   `https://github.com/LPgifthub/tv-screen/releases/latest/download/tv-home.apk`

   **Shortcut:** don't type it with the remote. Open the **Google Home** app on your Pixel, open the TV remote, tap the keyboard icon, and paste the address. Copy it from this page on your phone.
4. Press **Install**, then **Open**. TV Home appears. Press **Setup** to see its status page.

From now on TV Home updates itself: when the **Setup** button says **update ready**, open Setup and press **Install update**.

## Part 2 – Make the Home button open TV Home (10 minutes, once)

Google TV only allows this through a one-off command. Your Pixel can send it over Wi-Fi.

**On the TV**
1. Settings → System → About → click **Android TV OS build** 7 times ("You are now a developer").
2. Settings → System → Developer options → **Wireless debugging** → on.
3. Choose **Pair device with pairing code**. Leave that screen showing: it gives an IP address, a port and a 6-digit code.

**On your Pixel** (same Wi-Fi as the TV)
4. Install **Bugjaeger Mobile ADB** from the Play Store.
5. In Bugjaeger, choose to pair over Wi-Fi with a pairing code, and enter the IP, **pairing port** and 6-digit code from the TV's pairing box.
6. Then connect. **Watch out: the connect port is different from the pairing port.** Use the IP and port shown on the main Wireless debugging screen (not the pairing box). If anything fails, close the pairing box and open it again: the code and port change each time.
7. Once connected, open Bugjaeger's **shell** and paste this whole line (open this page on your phone to copy it):

   ```
   cmd package set-home-activity uk.lloyd.tvhome/.MainActivity; appops set uk.lloyd.tvhome ACCESS_RESTRICTED_SETTINGS allow; pm grant uk.lloyd.tvhome android.permission.WRITE_SECURE_SETTINGS
   ```

**Check it worked**
8. Press Home on the remote. TV Home should open.
9. In TV Home → **Setup**, all three Home button lines should show ✓:
   - Default home app: **TV Home ✓**
   - Backup service: **Running ✓**
   - Backup switches itself back on: **Yes ✓**
10. Turn **Wireless debugging** off again (Settings → System → Developer options). It's only needed for setup.

What the line does: makes TV Home the default home app; allows the backup Home button service (Android greys it out for apps not from the Play Store); and lets TV Home switch that backup back on after a restart (Android TV 14 can switch it off).

**No Pixel to hand?** Any laptop with Google's free "platform-tools" works: `adb pair IP:PORT`, then `adb connect IP:PORT` (the address shown under Wireless debugging), then `adb shell "<the line above>"`.

## Part 3 – Football scores (5 minutes, optional)

Scores come from football-data.org, which has a proper free plan for apps like this. FotMob has no public API and its terms don't allow other apps to pull its data, so it isn't used.

1. Register for a free key at **football-data.org**. It arrives by email.
2. TV Home → **Setup** → **Add football scores key**, and enter it. The Google TV remote in the **Google Home** app on your phone has a keyboard, so you can paste it rather than type it with the remote.

The **Next up** card then shows Norwich, Forest, England and the Premier League: the next fixture, the live score with the minute while a match is on, and the full-time result ("Won 2–1 v Leeds") for a few hours afterwards. It updates every minute during matches. Free-plan scores can lag a few minutes behind live TV. England only shows during the World Cup and the Euros on the free plan.

## Evening colours

From 9pm to 7am the screen switches to a soft, darker palette. Change it in **Setup**: automatic, always day, or always evening.

## If something isn't right

| What you see | What to do |
| --- | --- |
| Home still opens Google's screen | Check Setup. If "Default home app" isn't TV Home, run Part 2 again. The backup service should still catch Home in the meantime. |
| A main button says "Not installed" | Press it: it opens that app in the Play Store. |
| Next up says "Add free key in Setup" | Do Part 3. |
| Next up says "Key not accepted" | Re-enter the key in Setup; check it against the email. |
| Weather says "updating" | It retries every 2 minutes until the internet is up, then every 30 minutes. |
| Pairing or connecting fails in Bugjaeger | Check both are on the same Wi-Fi. Use the pairing port to pair and the main Wireless debugging port to connect. Reopen the pairing box for a fresh code. |
| TV Home closes itself repeatedly | After three crashes in three minutes, TV Home opens Google's home screen instead so the TV is never stuck. Tell Claude what you were doing. |
| "All home controls" does nothing | Expected for now; it's finished in Stage 2. Use the remote's dashboard to reach the Google Home panel. |

## To remove TV Home

Uninstall it (Settings → Apps → TV Home → Uninstall). The TV goes straight back to Google's home screen. Nothing of Google's is ever switched off or removed.

## Stages

- [x] Stage 1: home screen, six app buttons, clock, weather, all apps, Home button, self-update, evening palette, fixtures and live scores
- [ ] Stage 2: scenes and home controls
- [ ] Stage 3: outdoor camera tile and doorbell pop-up
- [ ] Stage 4: calendar

Every change to this code is built automatically by GitHub (Actions tab) and published to the download link above.
