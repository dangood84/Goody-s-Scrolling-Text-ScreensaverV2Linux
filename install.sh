#!/bin/sh
set -e
ROOT="$(CDPATH= cd -- "$(dirname "$0")" && pwd)"

if ! command -v java >/dev/null 2>&1; then
    echo "Java 21+ is required. On Debian / Raspberry Pi OS:" >&2
    echo "  sudo apt install default-jre" >&2
    exit 1
fi

echo "Building jar..."
chmod +x "$ROOT/build-jar.sh"
"$ROOT/build-jar.sh"

PREFIX="${XDG_DATA_HOME:-$HOME/.local/share}/goodys-marquee"
APPDIR="${XDG_DATA_HOME:-$HOME/.local/share}/applications"
SAVERDIR="$APPDIR/screensavers"

mkdir -p "$PREFIX" "$SAVERDIR" "$APPDIR"
cp "$ROOT/GoodysMarquee.jar" "$PREFIX/GoodysMarquee.jar"
cp "$ROOT/goodys-marquee-screensaver" "$PREFIX/goodys-marquee-screensaver"
cp "$ROOT/goodys-marquee-screensaver.xml" "$PREFIX/goodys-marquee-screensaver.xml"
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
Categories=Settings;DesktopSettings;Utility;
Icon=preferences-desktop-screensaver
StartupNotify=false
Terminal=false
EOF

XSCREENSAVER_RC="$HOME/.xscreensaver"
MARKER="goodys-marquee-screensaver"
ENTRY="                                \"Goody's Marquee\"  $PREFIX/goodys-marquee-screensaver --fullscreen \\n\\"
if [ ! -f "$XSCREENSAVER_RC" ]; then
    echo "No $XSCREENSAVER_RC yet. Open XScreensaver settings once to create it, then run ./install.sh again."
    echo "Or add this line just below programs: in that file:"
    echo "$ENTRY"
elif grep -q "$MARKER" "$XSCREENSAVER_RC"; then
    echo "Already listed in $XSCREENSAVER_RC"
else
    tmp="$(mktemp)"
    awk_status=0
    awk -v prefix="$PREFIX" '
        {
            print
            if (!done && $0 ~ /^programs:/) {
                printf "                                \"Goody'\''s Marquee\"  %s/goodys-marquee-screensaver --fullscreen \\n\\\n", prefix
                done = 1
            }
        }
        END { if (!done) exit 2 }
    ' "$XSCREENSAVER_RC" > "$tmp" || awk_status=$?
    if [ "$awk_status" -eq 0 ]; then
        mv "$tmp" "$XSCREENSAVER_RC"
        echo "Registered in $XSCREENSAVER_RC"
        if command -v xscreensaver-command >/dev/null 2>&1; then
            xscreensaver-command -restart >/dev/null 2>&1 || true
        fi
    else
        rm -f "$tmp"
        echo "Could not find a programs: list in $XSCREENSAVER_RC. Add this line manually:"
        echo "$ENTRY"
    fi
fi

echo "Installed to $PREFIX"
echo "Screensaver desktop file: $SAVERDIR/goodys-marquee.desktop"
echo "Settings: application menu → Goody's Marquee Settings, or XScreensaver's Settings button after the XML is installed."
echo

XSCREENSAVER_XML_DIR="/usr/share/xscreensaver/config"
if [ -d "$XSCREENSAVER_XML_DIR" ]; then
    echo "Installing XScreensaver Settings panel (may ask for your sudo password)..."
    if sudo cp "$PREFIX/goodys-marquee-screensaver.xml" "$XSCREENSAVER_XML_DIR/goodys-marquee-screensaver.xml"; then
        echo "XScreensaver Settings should now show message, font, colors, and speed."
        echo "Close and reopen XScreensaver settings if it was already open."
    else
        echo "To enable the Settings button, run:"
        echo "  sudo cp $PREFIX/goodys-marquee-screensaver.xml $XSCREENSAVER_XML_DIR/"
    fi
fi
echo
echo "MATE / Cinnamon / XFCE: open Screensaver settings and pick Goody's Marquee."
echo "Raspberry Pi OS (Wayland): install xscreensaver, or switch to X11 in raspi-config for classic saver support:"
echo "  sudo apt install xscreensaver default-jre"
echo "  raspi-config → Advanced Options → Wayland → X11 (optional)"
