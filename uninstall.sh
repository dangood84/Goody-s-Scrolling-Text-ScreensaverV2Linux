#!/bin/sh
set -e
PREFIX="${XDG_DATA_HOME:-$HOME/.local/share}/goodys-marquee"
APPDIR="${XDG_DATA_HOME:-$HOME/.local/share}/applications"
rm -rf "$PREFIX"
rm -f "$APPDIR/screensavers/goodys-marquee.desktop"
rm -f "$APPDIR/goodys-marquee-settings.desktop"
if [ -f "$HOME/.xscreensaver" ]; then
    if grep -q "goodys-marquee-screensaver" "$HOME/.xscreensaver"; then
        tmp="$(mktemp)"
        grep -v "goodys-marquee-screensaver" "$HOME/.xscreensaver" > "$tmp"
        mv "$tmp" "$HOME/.xscreensaver"
    fi
fi
echo "Removed Goody's Marquee from $PREFIX"
