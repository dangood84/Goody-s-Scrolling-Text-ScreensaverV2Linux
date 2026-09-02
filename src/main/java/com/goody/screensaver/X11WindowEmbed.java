package com.goody.screensaver;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * xscreensaver preview passes {@code -window-id} of an override-redirect
 * window. A normal Swing frame is drawn behind that overlay (black Preview).
 * This class places an override-redirect popup on the same rectangle, which
 * is how the mini preview pane and the Preview button can actually show text.
 */
final class X11WindowEmbed {

    private X11WindowEmbed() {
    }

    static boolean show(ScreensaverConfig config, long windowId) {
        if (windowId == 0L) {
            return false;
        }
        Rectangle bounds = queryWindowBounds(windowId);
        if (bounds != null && bounds.width > 2 && bounds.height > 2) {
            System.err.println("Goody's Marquee: preview overlay " + bounds);
            showPopup(config, bounds, coversMostOfScreen(bounds));
            return true;
        }
        if (showEmbeddedFrame(config, windowId)) {
            return true;
        }
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .getBounds();
        System.err.println("Goody's Marquee: no window geometry for "
                + toHex(windowId) + ", using a full-screen popup");
        showPopup(config, screen, true);
        return true;
    }

    private static void showPopup(ScreensaverConfig config, Rectangle bounds, boolean fullscreenPreview) {
        var frame = new MarqueeFrame(config, () -> System.exit(0), false, fullscreenPreview);
        if (!fullscreenPreview) {
            frame.setFocusableWindowState(false);
        }
        frame.showAsPopup(bounds);
    }

    private static boolean coversMostOfScreen(Rectangle bounds) {
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .getBounds();
        long area = (long) bounds.width * bounds.height;
        long screenArea = (long) screen.width * screen.height;
        return screenArea > 0 && area * 100 / screenArea >= 70;
    }

    private static boolean showEmbeddedFrame(ScreensaverConfig config, long windowId) {
        try {
            Class<?> type = Class.forName("sun.awt.X11.XEmbeddedFrame");
            Frame frame = createEmbeddedFrame(type, windowId);
            if (frame == null) {
                return false;
            }
            frame.setLayout(new BorderLayout());
            MarqueePanel panel = new MarqueePanel(config);
            frame.add(panel, BorderLayout.CENTER);
            frame.validate();
            frame.repaint();
            panel.start();
            return true;
        } catch (Throwable ex) {
            System.err.println("Goody's Marquee: XEmbeddedFrame failed for " + toHex(windowId));
            return false;
        }
    }

    private static Frame createEmbeddedFrame(Class<?> type, long windowId) throws Exception {
        Object frame = tryConstruct(type, new Class<?>[] {long.class, boolean.class}, windowId, false);
        if (frame instanceof Frame embedded) {
            return embedded;
        }
        frame = tryConstruct(type, new Class<?>[] {long.class}, windowId);
        return frame instanceof Frame embedded ? embedded : null;
    }

    private static Object tryConstruct(Class<?> type, Class<?>[] signature, Object... args) {
        try {
            Constructor<?> ctor = type.getConstructor(signature);
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    static Rectangle queryWindowBounds(long windowId) {
        String hex = toHex(windowId);
        Path xwininfo = firstExisting("/usr/bin/xwininfo", "/bin/xwininfo");
        if (xwininfo != null) {
            Rectangle parsed = parseXwininfo(run(xwininfo.toString(), "-id", hex));
            if (parsed != null) {
                return parsed;
            }
        }
        Path xdotool = firstExisting("/usr/bin/xdotool", "/bin/xdotool");
        if (xdotool != null) {
            return parseXdotool(run(xdotool.toString(), "getwindowgeometry", "--shell", hex));
        }
        return null;
    }

    private static Path firstExisting(String... paths) {
        for (String path : paths) {
            Path candidate = Path.of(path);
            if (Files.isExecutable(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String run(String... command) {
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return "";
            }
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }
            return output.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    private static Rectangle parseXwininfo(String output) {
        Integer x = matchInt(output, "Absolute upper-left X:");
        Integer y = matchInt(output, "Absolute upper-left Y:");
        Integer width = matchInt(output, "Width:");
        Integer height = matchInt(output, "Height:");
        if (x == null || y == null || width == null || height == null) {
            return null;
        }
        return new Rectangle(x, y, width, height);
    }

    private static Rectangle parseXdotool(String output) {
        Integer x = matchAssignment(output, "X");
        Integer y = matchAssignment(output, "Y");
        Integer width = matchAssignment(output, "WIDTH");
        Integer height = matchAssignment(output, "HEIGHT");
        if (x == null || y == null || width == null || height == null) {
            return null;
        }
        return new Rectangle(x, y, width, height);
    }

    private static Integer matchInt(String output, String label) {
        for (String line : output.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(label)) {
                String number = trimmed.substring(label.length()).trim();
                try {
                    return Integer.parseInt(number);
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
        }
        return null;
    }

    private static Integer matchAssignment(String output, String key) {
        String prefix = key + "=";
        for (String line : output.split("\n")) {
            if (line.startsWith(prefix)) {
                try {
                    return Integer.parseInt(line.substring(prefix.length()).trim());
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
        }
        return null;
    }

    private static String toHex(long windowId) {
        return "0x" + Long.toHexString(windowId).toLowerCase(Locale.ROOT);
    }
}
