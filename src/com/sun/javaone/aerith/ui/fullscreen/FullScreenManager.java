package com.sun.javaone.aerith.ui.fullscreen;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.DisplayMode;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import javax.swing.JFrame;

import com.sun.javaone.aerith.g2d.GraphicsUtil;
import org.jdesktop.swingx.mapviewer.LocalResponseCache;

/**
 *
 * @author Rick Wellman
 * @author aerith
 */
public class FullScreenManager {

    private BufferStrategy strategy;
    private Frame frame;
    private FullScreenManager.EscapeKeyListener escapeListener;
    private final FullScreenRenderer renderer;

    public FullScreenManager(FullScreenRenderer renderer) {
        this.renderer = renderer;
    }

    public void enterFullScreen() {
        final GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice device = environment.getDefaultScreenDevice();

        try {
            frame = new JFrame();
            frame.setUndecorated(true);
            frame.setIgnoreRepaint(true);

            device.setFullScreenWindow(frame);

            frame.setBackground(Color.BLACK);
            frame.createBufferStrategy(2);
            this.strategy = frame.getBufferStrategy();

            run();
        } finally {
            device.setFullScreenWindow(null);
        }
    }

    private void run() {
        setupEscapeKey();

        renderer.start();
        while (!renderer.isDone()) {
            Graphics g = null;
            try {
                g = strategy.getDrawGraphics();
                if (!strategy.contentsLost()) {
                    renderer.render(g, frame.getBounds());
                }
            } finally {
                if (g != null) {
                    g.dispose();
                }
            }
            strategy.show();
        }
        renderer.end();
        frame.dispose();

        removeEscapeKey();
    }

    private void removeEscapeKey() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(escapeListener);
    }

    private void setupEscapeKey() {
        escapeListener = new EscapeKeyListener();
        Toolkit.getDefaultToolkit().addAWTEventListener(escapeListener, KeyEvent.KEY_EVENT_MASK);
    }

    // Cancel when the ESC key is pressed.
    private class EscapeKeyListener implements AWTEventListener {
        @Override public void eventDispatched(AWTEvent event) {
            final KeyEvent keyEvent = (KeyEvent) event;
            if (keyEvent.getID() == KeyEvent.KEY_RELEASED
                    && keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE) {
                renderer.cancel();
            }
        }
    }

    public static void launch(URL baseDir) throws Exception {
        LocalResponseCache.installResponseCache();

        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = environment.getDefaultScreenDevice();
        DisplayMode displayMode = device.getDisplayMode();
        BufferedImage image;

        final Rectangle rectDesktop = new Rectangle(0, 0, displayMode.getWidth(), displayMode.getHeight());

        final Robot robot = new Robot();
        image = robot.createScreenCapture(rectDesktop);
        image = GraphicsUtil.toCompatibleImage(image);

        final FullScreenRenderer renderer = new IndyFullScreenRenderer(image, baseDir);
        final FullScreenManager manager = new FullScreenManager(renderer);
        manager.enterFullScreen();
    }

    public static void main(String... args) {
        try {
            launch(new File(".").toURI().toURL());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
