package com.goody.screensaver;

import java.util.Locale;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point for Goody's Scrolling Text Screensaver.
 *
 * <p>{@code /s} or {@code --fullscreen} starts the saver immediately.
 * {@code /c} or {@code --config} opens the preferences dialog.
 * With no arguments, the settings dialog is shown.
 * xscreensaver passes {@code -window-id}; that must be drawn into, not
 * replaced with a second full-screen window.
 */
public final class MarqueeSaver {

    private MarqueeSaver() {
    }

    public static void main(String[] args) {
        // Exclusive OpenGL Java2D on Raspberry Pi often paints a black window.
        System.setProperty("sun.java2d.opengl", "false");

        LaunchMode mode = LaunchMode.fromArgs(args);
        Long windowId = findWindowId(args);
        boolean wantConfig = hasExplicitConfigFlag(args);

        SwingUtilities.invokeLater(() -> {
            installLookAndFeel();
            ScreensaverConfig config = ScreensaverConfig.load();
            config.applyCommandLine(args);

            if (wantConfig || (mode == LaunchMode.CONFIG && windowId == null)) {
                new SettingsDialog(config).setVisible(true);
                return;
            }
            if (windowId != null) {
                if (X11WindowEmbed.show(config, windowId)) {
                    return;
                }
                System.err.println("Goody's Marquee: xscreensaver embed failed; falling back to a window.");
            }
            if (mode == LaunchMode.FULLSCREEN || mode == LaunchMode.PREVIEW || windowId != null) {
                new MarqueeFrame(config, () -> System.exit(0), true).showFullScreen();
                return;
            }
            new SettingsDialog(config).setVisible(true);
        });
    }

    private static void installLookAndFeel() {
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.contains("linux")) {
                // GTK L&F on Raspberry Pi OS often leaves Swing dialogs blank.
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } else {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
        } catch (Exception ignored) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignoredAgain) {
                // Keep the default look if nothing else loads.
            }
        }
    }

    static boolean hasExplicitConfigFlag(String[] args) {
        for (String raw : args) {
            String key = optionKey(raw);
            if (key.equalsIgnoreCase("c")
                    || key.equalsIgnoreCase("config")
                    || key.equalsIgnoreCase("prefs")
                    || key.toLowerCase(Locale.ROOT).startsWith("c:")) {
                return true;
            }
        }
        return false;
    }

    static Long findWindowId(String[] args) {
        Long fromEnv = parseWindowId(System.getenv("XSCREENSAVER_WINDOW"));
        if (fromEnv != null) {
            return fromEnv;
        }
        for (int i = 0; i < args.length; i++) {
            String raw = args[i];
            String key = optionKey(raw);
            if (!key.equalsIgnoreCase("window-id") && !key.equalsIgnoreCase("window_id")) {
                continue;
            }
            String value;
            int equals = raw.indexOf('=');
            if (equals > 0) {
                value = raw.substring(equals + 1);
            } else if (i + 1 < args.length) {
                value = args[i + 1];
            } else {
                continue;
            }
            Long parsed = parseWindowId(value);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    static Long parseWindowId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        try {
            if (value.startsWith("0x") || value.startsWith("0X")) {
                return Long.parseUnsignedLong(value.substring(2), 16);
            }
            return Long.parseUnsignedLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    static String optionKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String arg = stripPrefix(raw);
        int equals = arg.indexOf('=');
        return equals > 0 ? arg.substring(0, equals) : arg;
    }

    static String stripPrefix(String raw) {
        if (raw.startsWith("--")) {
            return raw.substring(2);
        }
        if (raw.startsWith("-") || raw.startsWith("/")) {
            return raw.substring(1);
        }
        return raw;
    }

    enum LaunchMode {
        CONFIG,
        FULLSCREEN,
        PREVIEW;

        static LaunchMode fromArgs(String[] args) {
            LaunchMode mode = CONFIG;
            for (String raw : args) {
                String arg = optionKey(raw);
                if (arg.isEmpty()) {
                    continue;
                }
                if (arg.equalsIgnoreCase("s")
                        || arg.equalsIgnoreCase("fullscreen")
                        || arg.equalsIgnoreCase("root")) {
                    mode = FULLSCREEN;
                } else if (arg.equalsIgnoreCase("c")
                        || arg.equalsIgnoreCase("config")
                        || arg.equalsIgnoreCase("prefs")
                        || arg.toLowerCase(Locale.ROOT).startsWith("c:")) {
                    mode = CONFIG;
                } else if (arg.equalsIgnoreCase("p")
                        || arg.equalsIgnoreCase("preview")
                        || arg.equalsIgnoreCase("window-id")
                        || arg.equalsIgnoreCase("window_id")) {
                    mode = PREVIEW;
                }
            }
            return mode;
        }
    }
}
