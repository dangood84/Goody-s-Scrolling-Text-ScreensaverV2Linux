# Goody's Marquee for Linux and Raspberry Pi OS

Linux desktops do not share one screensaver API. This port keeps the Java 21 Swing app and registers it where third-party savers usually live:

- **MATE / Cinnamon / XFCE** — `.desktop` file in `~/.local/share/applications/screensavers/`
- **xscreensaver** — a program line in `~/.xscreensaver` (printed by the installer)
- **Raspberry Pi OS** — same, plus notes for Wayland vs X11

There is no official GNOME Shell screensaver-plugin slot comparable to a Windows `.scr`. On GNOME, use the settings shortcut or run `--fullscreen` from a session idle hook if you want.

## Requirements

- Java 21 or later (`sudo apt install default-jre` on Debian / Raspberry Pi OS)

## Install

```bash
chmod +x build-jar.sh install.sh uninstall.sh goodys-marquee-screensaver
./install.sh
```

Then pick **Goody's Marquee** in your desktop's screensaver settings, or open **Goody's Marquee Settings** from the application menu.

## Raspberry Pi OS

Bookworm and later default to Wayland (labwc), which has weak third-party screensaver support. Practical options:

1. `sudo apt install xscreensaver default-jre`, then add the program line `install.sh` prints to `~/.xscreensaver`
2. Or **raspi-config → Advanced Options → Wayland → X11**, reboot, then use XFCE/MATE/xscreensaver as on a desktop

## Note on the old jar

`GoodysScreensaverV2.jar` was built with a broken `Main-Class`. `./build-jar.sh` writes `GoodysMarquee.jar` with `com.goody.screensaver.MarqueeSaver`.
