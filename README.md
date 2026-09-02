# Goody's Marquee for Linux and Raspberry Pi OS

Linux desktops do not share one screensaver API. This port keeps the Java 21 Swing app and registers it where third-party savers usually live:

- **MATE / Cinnamon / XFCE** — `.desktop` file in `~/.local/share/applications/screensavers/`
- **xscreensaver** — a program line in `~/.xscreensaver` (added by `./install.sh` once that file exists)
- **Raspberry Pi OS** — same, plus notes for Wayland vs X11

There is no official GNOME Shell screensaver-plugin slot comparable to a Windows `.scr`. On GNOME, use the settings shortcut or run `--fullscreen` from a session idle hook if you want.

## Requirements

- Java 21 or later (`sudo apt install default-jre` on Debian / Raspberry Pi OS)

## Install

```bash
chmod +x build-jar.sh install.sh uninstall.sh goodys-marquee-screensaver
./install.sh
```

Then pick **Goody's Marquee** in your desktop's screensaver settings. For message, font, colors, and speed, open **Goody's Marquee Settings** from the application menu (xscreensaver's own Settings button does not show those options).

## Raspberry Pi OS

Bookworm and later default to Wayland (labwc), where XScreensaver settings often do nothing. Switch to X11 first:

1. `sudo apt install xscreensaver default-jre`
2. **raspi-config → Advanced Options → Wayland → X11**, then reboot
3. Open **XScreensaver settings** once (this creates `~/.xscreensaver`)
4. Run `./install.sh` again so it can register **Goody's Marquee**
5. Close and reopen XScreensaver settings, then pick **Goody's Marquee**
6. Open **Goody's Marquee Settings** from the application menu to edit text, font, colors, and speed

If you already opened XScreensaver settings before re-running the installer, that last `./install.sh` is the step that makes it appear in the list.

## Note on the old jar

`GoodysScreensaverV2.jar` was built with a broken `Main-Class`. `./build-jar.sh` writes `GoodysMarquee.jar` with `com.goody.screensaver.MarqueeSaver`.
