package com.sun.javaone.aerith.ui;

import java.awt.BorderLayout;
import java.util.concurrent.ExecutionException;
import javax.swing.Box;
import javax.swing.JPanel;

import com.flickr4java.flickr.people.User;
import com.flickr4java.flickr.photosets.Photoset;
import com.sun.javaone.aerith.model.FlickrService;
import com.sun.javaone.aerith.util.Bundles;

import org.jdesktop.fuse.ResourceInjector;
import org.jdesktop.swingx.util.SwingWorker;

/**
 * AlbumsPanel
 *
 * @author Rick Wellman
 * @author aerith
 */
class AlbumsPanel extends JPanel {

    private final BackgroundTitle backgroundTitle;

    private final AlbumSelector albumSelector;

    AlbumsPanel() {
        ResourceInjector.get().inject(this);

        setOpaque(false);
        setLayout(new BorderLayout());

        albumSelector = new AlbumSelector();
        backgroundTitle = new BackgroundTitle(Bundles.getMessage(getClass(), "TXT_SelectAlbum", ""));

        add(BorderLayout.NORTH, backgroundTitle);
        add(BorderLayout.WEST, Box.createHorizontalStrut(60));
        add(BorderLayout.CENTER, albumSelector);
        add(BorderLayout.SOUTH, Box.createVerticalStrut(18));
        add(BorderLayout.EAST, Box.createHorizontalStrut(60));
    }

    void setContact(User contact) {
        backgroundTitle.setText(Bundles.getMessage(getClass(), "TXT_SelectAlbum", contact.getUsername()));
        new AlbumsFetcher(contact).execute();
    }

    private class AlbumsFetcher extends SwingWorker<Photoset[], Object> {

        private final User contact;

        AlbumsFetcher(User contact) {
            this.contact = contact;
        }

        @Override
        protected Photoset[] doInBackground() throws Exception {
            return FlickrService.getAlbums(contact);
        }

        @Override
        protected void done() {
            albumSelector.setContact(contact);
            albumSelector.removeAllAlbums();
            try {
                for (Photoset set : get()) {
                    albumSelector.addAlbum(set);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            albumSelector.defaultSelection();
        }
    }
}
