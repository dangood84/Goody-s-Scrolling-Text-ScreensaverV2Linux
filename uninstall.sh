#!/bin/sh
set -e
PREFIX="${XDG_DATA_HOME:-$HOME/.local/share}/goodys-marquee"
APPDIR="${XDG_DATA_HOME:-$HOME/.local/share}/applications"
rm -rf "$PREFIX"
rm -f "$APPDIR/screensavers/goodys-marquee.desktop"
rm -f "$APPDIR/goodys-marquee-settings.desktop"
echo "Removed Goody's Marquee from $PREFIX"
