package com.goody.screensaver;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

/**
 * Mutable settings for the scrolling marquee. Values are persisted with
 * {@link Preferences} so they reload automatically on the next launch.
 */
public final class ScreensaverConfig {

    private static final Logger LOG = Logger.getLogger(ScreensaverConfig.class.getName());
    private static final Preferences PREFS = Preferences.userNodeForPackage(ScreensaverConfig.class);

    private static final String KEY_MESSAGE = "message";
    private static final String KEY_FONT_FAMILY = "fontFamily";
    private static final String KEY_FONT_SIZE = "fontSize";
    private static final String KEY_BOLD = "bold";
    private static final String KEY_ITALIC = "italic";
    private static final String KEY_UNDERLINE = "underline";
    private static final String KEY_TEXT_COLOR = "textColor";
    private static final String KEY_BACKGROUND_COLOR = "backgroundColor";
    private static final String KEY_PIXELS_PER_SECOND = "pixelsPerSecond";

    private static final String DEFAULT_MESSAGE = "Goody's Scrolling Text Screensaver  •  Smooth Graphics2D marquee";
    private static final int DEFAULT_FONT_SIZE = 64;
    private static final boolean DEFAULT_BOLD = true;
    private static final boolean DEFAULT_ITALIC = false;
    private static final boolean DEFAULT_UNDERLINE = true;
    private static final int DEFAULT_TEXT_COLOR = new Color(255, 214, 102).getRGB();
    private static final int DEFAULT_BACKGROUND_COLOR = new Color(12, 18, 36).getRGB();
    private static final int DEFAULT_PIXELS_PER_SECOND = 140;

    private String message = DEFAULT_MESSAGE;
    private String fontFamily = pickDefaultFontFamily();
    private int fontSize = DEFAULT_FONT_SIZE;
    private boolean bold = DEFAULT_BOLD;
    private boolean italic = DEFAULT_ITALIC;
    private boolean underline = DEFAULT_UNDERLINE;
    private Color textColor = new Color(DEFAULT_TEXT_COLOR, true);
    private Color backgroundColor = new Color(DEFAULT_BACKGROUND_COLOR, true);
    private int pixelsPerSecond = DEFAULT_PIXELS_PER_SECOND;

    public static ScreensaverConfig load() {
        var config = new ScreensaverConfig();
        config.message = PREFS.get(KEY_MESSAGE, config.message);
        config.fontFamily = PREFS.get(KEY_FONT_FAMILY, config.fontFamily);
        config.fontSize = PREFS.getInt(KEY_FONT_SIZE, config.fontSize);
        config.bold = PREFS.getBoolean(KEY_BOLD, config.bold);
        config.italic = PREFS.getBoolean(KEY_ITALIC, config.italic);
        config.underline = PREFS.getBoolean(KEY_UNDERLINE, config.underline);
        config.textColor = new Color(PREFS.getInt(KEY_TEXT_COLOR, config.textColor.getRGB()), true);
        config.backgroundColor = new Color(PREFS.getInt(KEY_BACKGROUND_COLOR, config.backgroundColor.getRGB()), true);
        config.pixelsPerSecond = PREFS.getInt(KEY_PIXELS_PER_SECOND, config.pixelsPerSecond);
        return config;
    }

    public void save() {
        PREFS.put(KEY_MESSAGE, message);
        PREFS.put(KEY_FONT_FAMILY, fontFamily);
        PREFS.putInt(KEY_FONT_SIZE, fontSize);
        PREFS.putBoolean(KEY_BOLD, bold);
        PREFS.putBoolean(KEY_ITALIC, italic);
        PREFS.putBoolean(KEY_UNDERLINE, underline);
        PREFS.putInt(KEY_TEXT_COLOR, textColor.getRGB());
        PREFS.putInt(KEY_BACKGROUND_COLOR, backgroundColor.getRGB());
        PREFS.putInt(KEY_PIXELS_PER_SECOND, pixelsPerSecond);
        try {
            PREFS.flush();
        } catch (BackingStoreException ex) {
            LOG.log(Level.WARNING, "Unable to persist screensaver preferences", ex);
        }
    }

    public ScreensaverConfig copy() {
        var copy = new ScreensaverConfig();
        copy.message = message;
        copy.fontFamily = fontFamily;
        copy.fontSize = fontSize;
        copy.bold = bold;
        copy.italic = italic;
        copy.underline = underline;
        copy.textColor = textColor;
        copy.backgroundColor = backgroundColor;
        copy.pixelsPerSecond = pixelsPerSecond;
        return copy;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message == null ? "" : message;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily == null || fontFamily.isBlank() ? "Serif" : fontFamily;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = Math.clamp(fontSize, 8, 300);
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public boolean isUnderline() {
        return underline;
    }

    public void setUnderline(boolean underline) {
        this.underline = underline;
    }

    public Color getTextColor() {
        return textColor;
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor == null ? Color.WHITE : textColor;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor == null ? Color.BLACK : backgroundColor;
    }

    public int getPixelsPerSecond() {
        return pixelsPerSecond;
    }

    public void setPixelsPerSecond(int pixelsPerSecond) {
        this.pixelsPerSecond = Math.clamp(pixelsPerSecond, 10, 600);
    }

    static String[] availableFontFamilies() {
        String[] families = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        Arrays.sort(families, String.CASE_INSENSITIVE_ORDER);
        return families;
    }

    /**
     * Applies xscreensaver XML / command-line overrides and saves if anything
     * changed. Unknown flags (including {@code -window-id}) are ignored.
     */
    public void applyCommandLine(String[] args) {
        if (args == null || args.length == 0) {
            return;
        }
        boolean changed = false;
        for (int i = 0; i < args.length; i++) {
            String raw = args[i];
            String key = MarqueeSaver.optionKey(raw);
            if (key.isEmpty()) {
                continue;
            }
            if (key.equalsIgnoreCase("window-id") || key.equalsIgnoreCase("window_id")) {
                if (!raw.contains("=") && i + 1 < args.length) {
                    i++;
                }
                continue;
            }
            if (key.equalsIgnoreCase("bold")) {
                setBold(true);
                changed = true;
                continue;
            }
            if (key.equalsIgnoreCase("no-bold")) {
                setBold(false);
                changed = true;
                continue;
            }
            if (key.equalsIgnoreCase("italic")) {
                setItalic(true);
                changed = true;
                continue;
            }
            if (key.equalsIgnoreCase("no-italic")) {
                setItalic(false);
                changed = true;
                continue;
            }
            if (key.equalsIgnoreCase("underline")) {
                setUnderline(true);
                changed = true;
                continue;
            }
            if (key.equalsIgnoreCase("no-underline")) {
                setUnderline(false);
                changed = true;
                continue;
            }
            String value = flagValue(raw, args, i);
            if (value == null) {
                continue;
            }
            if (!raw.contains("=") && i + 1 < args.length && args[i + 1].equals(value)) {
                i++;
            }
            try {
                if (key.equalsIgnoreCase("message")) {
                    setMessage(value);
                    changed = true;
                } else if (key.equalsIgnoreCase("font-family") || key.equalsIgnoreCase("fontFamily")) {
                    setFontFamily(value);
                    changed = true;
                } else if (key.equalsIgnoreCase("font-size") || key.equalsIgnoreCase("fontSize")) {
                    setFontSize(Integer.parseInt(value.trim()));
                    changed = true;
                } else if (key.equalsIgnoreCase("speed") || key.equalsIgnoreCase("pixels-per-second")) {
                    setPixelsPerSecond(Integer.parseInt(value.trim()));
                    changed = true;
                } else if (key.equalsIgnoreCase("text-color") || key.equalsIgnoreCase("textColor")) {
                    setTextColor(parseColor(value));
                    changed = true;
                } else if (key.equalsIgnoreCase("background-color")
                        || key.equalsIgnoreCase("backgroundColor")
                        || key.equalsIgnoreCase("bg-color")) {
                    setBackgroundColor(parseColor(value));
                    changed = true;
                }
            } catch (RuntimeException ex) {
                LOG.log(Level.WARNING, "Ignoring screensaver option --" + key + " " + value, ex);
            }
        }
        if (changed) {
            save();
        }
    }

    private static String flagValue(String raw, String[] args, int index) {
        int equals = raw.indexOf('=');
        if (equals > 0) {
            return raw.substring(equals + 1);
        }
        if (index + 1 < args.length && !args[index + 1].startsWith("-")) {
            return args[index + 1];
        }
        return null;
    }

    private static Color parseColor(String raw) {
        String hex = raw.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        } else if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }
        if (hex.length() == 6) {
            return new Color(Integer.parseInt(hex, 16));
        }
        if (hex.length() == 8) {
            return new Color((int) Long.parseLong(hex, 16), true);
        }
        throw new IllegalArgumentException("Not a hex color: " + raw);
    }

    private static String pickDefaultFontFamily() {
        List<String> available = Arrays.stream(availableFontFamilies())
                .map(family -> family.toLowerCase(Locale.ROOT))
                .toList();
        return Stream.of("Georgia", "Palatino", "Times New Roman", "Serif")
                .filter(candidate -> available.contains(candidate.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse("Serif");
    }
}
