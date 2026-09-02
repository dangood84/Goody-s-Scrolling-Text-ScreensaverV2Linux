#!/bin/sh
set -e
ROOT="$(CDPATH= cd -- "$(dirname "$0")" && pwd)"

if ! command -v java >/dev/null 2>&1; then
    echo "Java 21+ is required. On Debian / Raspberry Pi OS:" >&2
    echo "  sudo apt install default-jre" >&2
    exit 1
fi

if [ ! -f "$ROOT/GoodysMarquee.jar" ]; then
    echo "Building jar..."
    chmod +x "$ROOT/build-jar.sh"
    "$ROOT/build-jar.sh"
fi

PREFIX="${XDG_DATA_HOME:-$HOME/.local/share}/goodys-marquee"
APPDIR="${XDG_DATA_HOME:-$HOME/.local/share}/applications"
SAVERDIR="$APPDIR/screensavers"

mkdir -p "$PREFIX" "$SAVERDIR" "$APPDIR"
cp "$ROOT/GoodysMarquee.jar" "$PREFIX/GoodysMarquee.jar"
cp "$ROOT/goodys-marquee-screensaver" "$PREFIX/goodys-marquee-screensaver"
chmod +x "$PREFIX/goodys-marquee-screensaver"

cat > "$SAVERDIR/goodys-marquee.desktop" << EOF
[Desktop Entry]
Type=Application
Name=Goody's Marquee
Comment=Scrolling text screensaver
Exec=$PREFIX/goodys-marquee-screensaver --fullscreen
TryExec=java
Categories=Screensaver;
StartupNotify=false
Terminal=false
EOF

cat > "$APPDIR/goodys-marquee-settings.desktop" << EOF
[Desktop Entry]
Type=Application
Name=Goody's Marquee Settings
Comment=Configure Goody's scrolling text screensaver
Exec=$PREFIX/goodys-marquee-screensaver --config
TryExec=java
Categories=Settings;DesktopSettings;
StartupNotify=false
Terminal=false
EOF

XSCREENSAVER_RC="$HOME/.xscreensaver"
MARKER="GoodysMarquee"
if [ -f "$XSCREENSAVER_RC" ] && ! grep -q "$MARKER" "$XSCREENSAVER_RC"; then
    echo "Add this line to the programs: list in $XSCREENSAVER_RC :"
    echo "  -                   \"Goody's Marquee\"  $PREFIX/goodys-marquee-screensaver --fullscreen \\n\\"
elif [ ! -f "$XSCREENSAVER_RC" ]; then
    echo "xscreensaver is optional. If you use it, add this program entry after installing xscreensaver:"
    echo "  -                   \"Goody's Marquee\"  $PREFIX/goodys-marquee-screensaver --fullscreen \\n\\"
fi

echo "Installed to $PREFIX"
echo "Screensaver desktop file: $SAVERDIR/goodys-marquee.desktop"
echo "Settings: application menu → Goody's Marquee Settings"
echo
echo "MATE / Cinnamon / XFCE: open Screensaver settings and pick Goody's Marquee."
echo "Raspberry Pi OS (Wayland): install xscreensaver, or switch to X11 in raspi-config for classic saver support:"
echo "  sudo apt install xscreensaver default-jre"
echo "  raspi-config → Advanced Options → Wayland → X11 (optional)"
