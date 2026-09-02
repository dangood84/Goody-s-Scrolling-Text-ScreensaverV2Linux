package com.goody.screensaver;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

/**
 * Mutable settings for the scrolling marquee. Values are persisted to
 * {@code ~/.config/goodys-marquee/config.properties} and Java Preferences.
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
        config.readConfigFile();
        if (config.message == null || config.message.isBlank()) {
            config.message = DEFAULT_MESSAGE;
            config.save();
        }
        return config;
    }

    /**
     * Text actually drawn on the marquee. Blank saved values fall back to the
     * default so xscreensaver's empty {@code --message} does not hide the preview.
     */
    public String displayMessage() {
        return message == null || message.isBlank() ? DEFAULT_MESSAGE : message;
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
        writeConfigFile();
    }

    /**
     * XScreensaver Settings reads command-line flags from {@code ~/.xscreensaver},
     * not our config file. Keep that program line in sync so the panel shows
     * the same message, font, colors, and speed.
     */
    public void syncXscreensaverCommand() {
        Path rc = Path.of(System.getProperty("user.home"), ".xscreensaver");
        if (!Files.isRegularFile(rc)) {
            return;
        }
        try {
            String text = Files.readString(rc);
            String marker = "goodys-marquee-screensaver";
            int markerAt = text.indexOf(marker);
            if (markerAt < 0) {
                return;
            }
            int lineStart = text.lastIndexOf('\n', markerAt - 1) + 1;
            int lineEnd = text.indexOf('\n', markerAt);
            if (lineEnd < 0) {
                lineEnd = text.length();
            }
            String line = text.substring(lineStart, lineEnd);
            int pathStart = markerAt - lineStart;
            while (pathStart > 0 && !Character.isWhitespace(line.charAt(pathStart - 1))) {
                pathStart--;
            }
            String leading = line.substring(0, pathStart);
            String binary = line.substring(pathStart, markerAt - lineStart + marker.length());
            boolean continuation = line.contains("\\n\\");
            String updated = leading + binary + " " + xscreensaverArgs();
            if (continuation) {
                updated += " \\n\\";
            }
            if (updated.equals(line)) {
                return;
            }
            String rebuilt = text.substring(0, lineStart) + updated + text.substring(lineEnd);
            Files.writeString(rc, rebuilt);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Unable to update ~/.xscreensaver", ex);
        }
    }

    /**
     * Reads flags from the Goody's line in {@code ~/.xscreensaver} so the
     * Accessories settings window matches what XScreensaver last wrote.
     */
    public void applyXscreensaverFile() {
        Path rc = Path.of(System.getProperty("user.home"), ".xscreensaver");
        if (!Files.isRegularFile(rc)) {
            return;
        }
        try {
            String text = Files.readString(rc);
            String marker = "goodys-marquee-screensaver";
            int markerAt = text.indexOf(marker);
            if (markerAt < 0) {
                return;
            }
            int lineStart = text.lastIndexOf('\n', markerAt - 1) + 1;
            int lineEnd = text.indexOf('\n', markerAt);
            if (lineEnd < 0) {
                lineEnd = text.length();
            }
            String line = text.substring(lineStart, lineEnd);
            int localMarker = markerAt - lineStart;
            int argsStart = localMarker + marker.length();
            if (argsStart > line.length()) {
                return;
            }
            String args = line.substring(argsStart).trim();
            if (args.endsWith("\\n\\")) {
                args = args.substring(0, args.length() - 3).trim();
            }
            applyCommandLine(tokenizeCommandLine(args).toArray(String[]::new));
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Unable to read ~/.xscreensaver", ex);
        }
    }

    private String xscreensaverArgs() {
        // XScreensaver omits flags that match XML defaults, and only honors
        // one of arg-set / arg-unset per checkbox. Match that so Close can
        // round-trip Bold/Italic/Underline, size, and the message.
        StringBuilder out = new StringBuilder("--fullscreen");
        out.append(" --message ").append(quoteForXscreensaver(asciiSafe(displayMessage())));
        out.append(" --font-family ").append(quoteForXscreensaver(asciiSafe(fontFamily)));
        if (fontSize != DEFAULT_FONT_SIZE) {
            out.append(" --font-size ").append(fontSize);
        }
        if (!bold) {
            out.append(" --no-bold");
        }
        if (italic) {
            out.append(" --italic");
        }
        if (!underline) {
            out.append(" --no-underline");
        }
        out.append(" --text-color ").append(toHex(textColor));
        out.append(" --background-color ").append(toHex(backgroundColor));
        if (pixelsPerSecond != DEFAULT_PIXELS_PER_SECOND) {
            out.append(" --speed ").append(pixelsPerSecond);
        }
        return out.toString();
    }

    /**
     * Matches xscreensaver's shell_quotify(): quote when the value has spaces,
     * quotes, or other specials. Avoid non-ASCII, which can break ~/.xscreensaver.
     */
    private static String quoteForXscreensaver(String value) {
        boolean needQuotes = value.isEmpty();
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '!' || ch == '"' || ch == '$') {
                needQuotes = true;
                out.append('\\').append(ch);
            } else if (ch <= ' ' || ch >= 127 || ch == '\'' || ch == '#' || ch == '%'
                    || ch == '&' || ch == '(' || ch == ')' || ch == '*') {
                needQuotes = true;
                out.append(ch);
            } else {
                out.append(ch);
            }
        }
        return needQuotes ? "\"" + out + "\"" : out.toString();
    }

    private static String asciiSafe(String value) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\'') {
                continue;
            }
            out.append(ch < 127 ? ch : '-');
        }
        return out.toString();
    }

    static java.util.List<String> tokenizeCommandLine(String cmd) {
        var tokens = new java.util.ArrayList<String>();
        int i = 0;
        int n = cmd.length();
        while (i < n) {
            while (i < n && Character.isWhitespace(cmd.charAt(i))) {
                i++;
            }
            if (i >= n) {
                break;
            }
            char q = cmd.charAt(i);
            if (q == '"' || q == '\'' || q == '`') {
                i++;
                var token = new StringBuilder();
                while (i < n && cmd.charAt(i) != q) {
                    if (cmd.charAt(i) == '\\' && i + 1 < n) {
                        i++;
                    }
                    token.append(cmd.charAt(i));
                    i++;
                }
                if (i < n && cmd.charAt(i) == q) {
                    i++;
                }
                tokens.add(token.toString());
            } else {
                int start = i;
                while (i < n && !Character.isWhitespace(cmd.charAt(i))
                        && cmd.charAt(i) != '"' && cmd.charAt(i) != '\'' && cmd.charAt(i) != '`') {
                    i++;
                }
                tokens.add(cmd.substring(start, i));
            }
        }
        return tokens;
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
        if (looksLikeXscreensaverSettings(args)) {
            // Omitted flags mean XML defaults, not "keep the last Java Save".
            setMessage(DEFAULT_MESSAGE);
            setFontSize(DEFAULT_FONT_SIZE);
            setBold(DEFAULT_BOLD);
            setItalic(DEFAULT_ITALIC);
            setUnderline(DEFAULT_UNDERLINE);
            setPixelsPerSecond(DEFAULT_PIXELS_PER_SECOND);
            changed = true;
        }
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
                    if (!isPlaceholder(value)) {
                        setMessage(value);
                        changed = true;
                    }
                } else if (key.equalsIgnoreCase("font-family") || key.equalsIgnoreCase("fontFamily")) {
                    if (!isPlaceholder(value)) {
                        setFontFamily(value);
                        changed = true;
                    }
                } else if (key.equalsIgnoreCase("font-size") || key.equalsIgnoreCase("fontSize")) {
                    setFontSize(parseCliInt(value));
                    changed = true;
                } else if (key.equalsIgnoreCase("speed") || key.equalsIgnoreCase("pixels-per-second")) {
                    setPixelsPerSecond(parseCliInt(value));
                    changed = true;
                } else if (key.equalsIgnoreCase("text-color") || key.equalsIgnoreCase("textColor")) {
                    if (!isPlaceholder(value)) {
                        setTextColor(parseColor(value));
                        changed = true;
                    }
                } else if (key.equalsIgnoreCase("background-color")
                        || key.equalsIgnoreCase("backgroundColor")
                        || key.equalsIgnoreCase("bg-color")) {
                    if (!isPlaceholder(value)) {
                        setBackgroundColor(parseColor(value));
                        changed = true;
                    }
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

    private static boolean looksLikeXscreensaverSettings(String[] args) {
        for (String raw : args) {
            String key = MarqueeSaver.optionKey(raw);
            if (key.equalsIgnoreCase("message")
                    || key.equalsIgnoreCase("font-family")
                    || key.equalsIgnoreCase("fontFamily")
                    || key.equalsIgnoreCase("font-size")
                    || key.equalsIgnoreCase("fontSize")
                    || key.equalsIgnoreCase("bold")
                    || key.equalsIgnoreCase("no-bold")
                    || key.equalsIgnoreCase("italic")
                    || key.equalsIgnoreCase("no-italic")
                    || key.equalsIgnoreCase("underline")
                    || key.equalsIgnoreCase("no-underline")
                    || key.equalsIgnoreCase("text-color")
                    || key.equalsIgnoreCase("textColor")
                    || key.equalsIgnoreCase("background-color")
                    || key.equalsIgnoreCase("backgroundColor")
                    || key.equalsIgnoreCase("speed")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPlaceholder(String value) {
        return value == null || value.isBlank() || value.equals("%");
    }

    /** XScreensaver sliders often pass {@code 64.000} rather than {@code 64}. */
    private static int parseCliInt(String value) {
        return (int) Math.round(Double.parseDouble(value.trim()));
    }

    static Path configFile() {
        String xdg = System.getenv("XDG_CONFIG_HOME");
        Path dir = xdg == null || xdg.isBlank()
                ? Path.of(System.getProperty("user.home"), ".config", "goodys-marquee")
                : Path.of(xdg, "goodys-marquee");
        return dir.resolve("config.properties");
    }

    private void readConfigFile() {
        Path file = configFile();
        if (!Files.isRegularFile(file)) {
            return;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Unable to read " + file, ex);
            return;
        }
        message = props.getProperty(KEY_MESSAGE, message);
        fontFamily = props.getProperty(KEY_FONT_FAMILY, fontFamily);
        fontSize = parseIntProperty(props, KEY_FONT_SIZE, fontSize);
        bold = parseBooleanProperty(props, KEY_BOLD, bold);
        italic = parseBooleanProperty(props, KEY_ITALIC, italic);
        underline = parseBooleanProperty(props, KEY_UNDERLINE, underline);
        pixelsPerSecond = parseIntProperty(props, KEY_PIXELS_PER_SECOND, pixelsPerSecond);
        Color loadedText = parseStoredColor(props.getProperty(KEY_TEXT_COLOR));
        if (loadedText != null) {
            textColor = loadedText;
        }
        Color loadedBackground = parseStoredColor(props.getProperty(KEY_BACKGROUND_COLOR));
        if (loadedBackground != null) {
            backgroundColor = loadedBackground;
        }
    }

    private void writeConfigFile() {
        Path file = configFile();
        Properties props = new Properties();
        props.setProperty(KEY_MESSAGE, message == null ? "" : message);
        props.setProperty(KEY_FONT_FAMILY, fontFamily);
        props.setProperty(KEY_FONT_SIZE, Integer.toString(fontSize));
        props.setProperty(KEY_BOLD, Boolean.toString(bold));
        props.setProperty(KEY_ITALIC, Boolean.toString(italic));
        props.setProperty(KEY_UNDERLINE, Boolean.toString(underline));
        props.setProperty(KEY_TEXT_COLOR, toHex(textColor));
        props.setProperty(KEY_BACKGROUND_COLOR, toHex(backgroundColor));
        props.setProperty(KEY_PIXELS_PER_SECOND, Integer.toString(pixelsPerSecond));
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                props.store(out, "Goody's Marquee");
            }
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Unable to write " + file, ex);
        }
    }

    private static int parseIntProperty(Properties props, String key, int fallback) {
        try {
            return Integer.parseInt(props.getProperty(key, Integer.toString(fallback)).trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static boolean parseBooleanProperty(Properties props, String key, boolean fallback) {
        String raw = props.getProperty(key);
        return raw == null ? fallback : Boolean.parseBoolean(raw);
    }

    private static Color parseStoredColor(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return parseColor(raw);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String toHex(Color color) {
        return String.format("#%06X", color.getRGB() & 0xFFFFFF);
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
