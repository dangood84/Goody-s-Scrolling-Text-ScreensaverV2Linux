package com.goody.screensaver;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.lang.reflect.Constructor;
import javax.swing.SwingUtilities;

/**
 * Draws the marquee inside an existing X11 window, which is how xscreensaver
 * preview, Preview, and blanking all work. A separate Swing window sits
 * behind xscreensaver's overlay and looks like a black screen.
 */
final class X11WindowEmbed {

    private X11WindowEmbed() {
    }

    static boolean show(ScreensaverConfig config, long windowId) {
        if (windowId == 0L) {
            return false;
        }
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
            SwingUtilities.invokeLater(panel::requestFocusInWindow);
            return true;
        } catch (Throwable ex) {
            System.err.println("Goody's Marquee: cannot draw in xscreensaver window " + toHex(windowId));
            ex.printStackTrace(System.err);
            return false;
        }
    }

    private static Frame createEmbeddedFrame(Class<?> type, long windowId) throws Exception {
        // xscreensaver does not speak XEmbed; a plain X parent handle works.
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

    private static String toHex(long windowId) {
        return "0x" + Long.toHexString(windowId);
    }
}
