package com.goody.screensaver;

import java.awt.Cursor;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.Locale;
import javax.swing.JFrame;

/**
 * Undecorated full-screen window that hosts the scrolling marquee.
 * A key or click exits after a short grace period. Mouse-move exit is
 * used for standalone launches only; xscreensaver generates motion events
 * that would otherwise dismiss the preview immediately.
 */
public final class MarqueeFrame extends JFrame {

    private static final int MOUSE_MOVE_EXIT_PIXELS = 12;
    private static final long CLICK_GRACE_MS = 400L;
    private static final long MOVE_GRACE_MS = 1200L;

    private final GraphicsDevice device;
    private final Runnable onExit;
    private final boolean exitOnMouseMove;
    private final boolean exitOnClick;
    private boolean exited;
    private Point firstMousePoint;
    private long shownAtMillis;

    public MarqueeFrame(ScreensaverConfig config, Runnable onExit) {
        this(config, onExit, true, true);
    }

    public MarqueeFrame(ScreensaverConfig config, Runnable onExit, boolean exitOnMouseMove) {
        this(config, onExit, exitOnMouseMove, true);
    }

    public MarqueeFrame(
            ScreensaverConfig config,
            Runnable onExit,
            boolean exitOnMouseMove,
            boolean exitOnClick) {
        super("Goody's Scrolling Text Screensaver");
        this.onExit = onExit;
        this.exitOnMouseMove = exitOnMouseMove;
        this.exitOnClick = exitOnClick;
        this.device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        setUndecorated(true);
        setResizable(false);
        setAlwaysOnTop(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setCursor(invisibleCursor());
        setFocusable(true);

        MarqueePanel panel = new MarqueePanel(config);
        setContentPane(panel);
        bindWakeListeners(panel);
        if (!exitOnClick) {
            // Mini xscreensaver preview should not steal keyboard from the settings UI.
            setFocusable(false);
        }
    }

    public void showFullScreen() {
        shownAtMillis = System.currentTimeMillis();
        GraphicsConfiguration gc = device.getDefaultConfiguration();
        Rectangle bounds = gc.getBounds();
        setBounds(bounds);

        // Exclusive AWT fullscreen on Raspberry Pi X11 frequently stays black
        // until the window is torn down. A maximized undecorated window paints.
        if (!isLinux() && device.isFullScreenSupported()) {
            setVisible(true);
            device.setFullScreenWindow(this);
        } else {
            setExtendedState(MAXIMIZED_BOTH);
            setVisible(true);
        }
        toFront();
        requestFocus();
        getContentPane().requestFocusInWindow();
    }

    /**
     * Override-redirect popup used to sit on top of xscreensaver's black
     * preview overlay, sized to the target window.
     */
    public void showAsPopup(Rectangle bounds) {
        shownAtMillis = System.currentTimeMillis();
        try {
            setType(Type.POPUP);
        } catch (IllegalComponentStateException ignored) {
            // Window already realized; always-on-top still helps.
        }
        setAlwaysOnTop(true);
        setBounds(bounds);
        setVisible(true);
        toFront();
        if (exitOnClick) {
            requestFocus();
            getContentPane().requestFocusInWindow();
        }
    }

    private void bindWakeListeners(MarqueePanel panel) {
        if (exitOnClick) {
            KeyAdapter keys = new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent event) {
                    if (!stillInGracePeriod(CLICK_GRACE_MS)) {
                        exitScreensaver();
                    }
                }

                @Override
                public void keyTyped(KeyEvent event) {
                    if (!stillInGracePeriod(CLICK_GRACE_MS)) {
                        exitScreensaver();
                    }
                }
            };
            addKeyListener(keys);
            panel.addKeyListener(keys);

            MouseAdapter clicks = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    if (!stillInGracePeriod(CLICK_GRACE_MS)) {
                        exitScreensaver();
                    }
                }
            };
            addMouseListener(clicks);
            panel.addMouseListener(clicks);
        }

        if (!exitOnMouseMove) {
            return;
        }

        MouseMotionAdapter mouse = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                onMouseMoved(event.getPoint());
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                onMouseMoved(event.getPoint());
            }
        };
        addMouseMotionListener(mouse);
        panel.addMouseMotionListener(mouse);
    }

    private boolean stillInGracePeriod(long graceMs) {
        return System.currentTimeMillis() - shownAtMillis < graceMs;
    }

    private void onMouseMoved(Point point) {
        if (stillInGracePeriod(MOVE_GRACE_MS)) {
            firstMousePoint = point;
            return;
        }
        if (firstMousePoint == null) {
            firstMousePoint = point;
            return;
        }
        int dx = Math.abs(point.x - firstMousePoint.x);
        int dy = Math.abs(point.y - firstMousePoint.y);
        if (dx >= MOUSE_MOVE_EXIT_PIXELS || dy >= MOUSE_MOVE_EXIT_PIXELS) {
            exitScreensaver();
        }
    }

    private void exitScreensaver() {
        if (exited) {
            return;
        }
        exited = true;

        if (getContentPane() instanceof MarqueePanel panel) {
            panel.stop();
        }
        if (device.getFullScreenWindow() == this) {
            device.setFullScreenWindow(null);
        }
        dispose();
        onExit.run();
    }

    private static boolean isLinux() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux");
    }

    private static Cursor invisibleCursor() {
        try {
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            return Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(0, 0), "hidden");
        } catch (RuntimeException ex) {
            return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        }
    }
}
