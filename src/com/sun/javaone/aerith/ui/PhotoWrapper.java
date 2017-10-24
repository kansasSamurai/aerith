package com.sun.javaone.aerith.ui;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.flickr4java.flickr.photos.Photo;
import com.sun.javaone.aerith.g2d.GraphicsUtil;
import java.io.IOException;

/**
 * PhotoWrapper
 *
 * @author Rick Wellman
 * @author aerith
 */
public class PhotoWrapper implements Runnable {

    private static final int ICONSIZE_1 = 133;

    private static final int ICONSIZE_2 = 50;

    private static final ExecutorService service = Executors.newCachedThreadPool(
    new ThreadFactory() {
        private int count = 0;

        @Override
        public Thread newThread(Runnable r) {
            final Thread t = new Thread(r, "wrapper-pool-" + count++);
            t.setDaemon(true);
            return t;
        }
    });

    private Photo flickrPhoto;

    private BufferedImage smallSquareImage = null;
    private boolean smallSquareImageLoaded = false;

    private BufferedImage image = null;
    private boolean imageLoaded = false;

    private Icon icon;

    private final PropertyChangeSupport support;

    public PhotoWrapper(Photo photo) {
        this.support = new PropertyChangeSupport(this);
        this.setFlickrPhoto(photo);
        service.submit(this);
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        this.support.addPropertyChangeListener(pcl);
    }

    @Override
    public void run() {
        String url;

        url = getFlickrPhoto().getSmallSquareUrl();
        try {
            System.out.println("PhotoWrapper.run() > " + url);
            smallSquareImage = GraphicsUtil.toCompatibleImage(getFlickrPhoto().getSmallSquareImage());
            smallSquareImageLoaded = true;
            support.firePropertyChange("smallSquareImageLoaded", false, true);
        } catch (IOException ex) {
            System.out.println("PhotoWrapper  ex   > " + url);
            ex.printStackTrace();
        } catch (Exception ex) {
            System.out.println("PhotoWrapper  ex   > " + url);
            ex.printStackTrace();
        }

        final BufferedImage scaled = GraphicsUtil.createThumbnail(smallSquareImage, ICONSIZE_2);
        icon = new ImageIcon(scaled);

        url = getFlickrPhoto().getSmallUrl();
        try {
            System.out.println("PhotoWrapper.run() > " + url);
            image = GraphicsUtil.toCompatibleImage(getFlickrPhoto().getSmallImage());
            imageLoaded = true;
            support.firePropertyChange("imageLoaded", false, true);
        } catch (IOException ex) {
            System.out.println("PhotoWrapper  ex   > " + url);
            ex.printStackTrace();
        } catch (Exception ex) {
            System.out.println("PhotoWrapper  ex   > " + url);
            ex.printStackTrace();
        }

    }

    public Icon getIcon() {
        return icon;
    }

    public boolean isSmallSquareImageLoaded() {
        return smallSquareImageLoaded;
    }

    public Photo getFlickrPhoto() {
        return flickrPhoto;
    }

    public void setFlickrPhoto(Photo flickrPhoto) {
        this.flickrPhoto = flickrPhoto;
    }

    public BufferedImage getSmallSquareImage() {
        return smallSquareImage;
    }

    public BufferedImage getImage() {
        return image;
    }

    public boolean isImageLoaded() {
        return imageLoaded;
    }

}