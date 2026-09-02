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
 */
public final class MarqueeSaver {

    private MarqueeSaver() {
    }

    public static void main(String[] args) {
        // Exclusive OpenGL Java2D on Raspberry Pi often paints a black window.
        System.setProperty("sun.java2d.opengl", "false");

        LaunchMode mode = LaunchMode.fromArgs(args);
        boolean xscreensaver = launchedByXscreensaver(args);

        SwingUtilities.invokeLater(() -> {
            installLookAndFeel();
            ScreensaverConfig config = ScreensaverConfig.load();
            if (mode == LaunchMode.CONFIG) {
                new SettingsDialog(config).setVisible(true);
            } else {
                new MarqueeFrame(config, () -> System.exit(0), !xscreensaver).showFullScreen();
            }
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

    static boolean launchedByXscreensaver(String[] args) {
        if (System.getenv("XSCREENSAVER_WINDOW") != null) {
            return true;
        }
        for (String raw : args) {
            String key = optionKey(raw);
            if (key.equalsIgnoreCase("window-id")
                    || key.equalsIgnoreCase("window_id")
                    || key.equalsIgnoreCase("root")) {
                return true;
            }
        }
        return false;
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
