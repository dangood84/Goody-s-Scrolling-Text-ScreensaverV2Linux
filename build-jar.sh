#!/bin/sh
set -e
ROOT="$(CDPATH= cd -- "$(dirname "$0")" && pwd)"
mkdir -p "$ROOT/out"
javac --release 21 -encoding UTF-8 -d "$ROOT/out" "$ROOT"/src/main/java/com/goody/screensaver/*.java
jar cfe "$ROOT/GoodysMarquee.jar" com.goody.screensaver.MarqueeSaver -C "$ROOT/out" .
echo "Built $ROOT/GoodysMarquee.jar"
