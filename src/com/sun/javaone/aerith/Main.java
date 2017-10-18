package com.sun.javaone.aerith;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.jdesktop.swingx.mapviewer.LocalResponseCache;
import com.sun.javaone.aerith.ui.MainFrame;
import com.sun.javaone.aerith.ui.TransitionManager;
import java.net.URL;
import java.net.URLClassLoader;

public class Main {
    private Main() {
    }

    public static void main(String[] args) {

        // Log the application classpath for debugging purposes
        System.out.println("----- Application Classpath -----");
        final ClassLoader cl = ClassLoader.getSystemClassLoader();
        final URL[] urls = ((URLClassLoader)cl).getURLs();
        for (URL url: urls){
            System.out.println(url.getFile());
        }

        LocalResponseCache.installResponseCache();

        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                } catch (ClassNotFoundException e) {
                } catch (InstantiationException e) {
                } catch (IllegalAccessException e) {
                } catch (UnsupportedLookAndFeelException e) {
                }

                MainFrame frame = TransitionManager.createMainFrame();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
