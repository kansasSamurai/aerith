package com.sun.javaone.aerith.model;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.Transport;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.contacts.ContactsInterface;
import com.flickr4java.flickr.favorites.FavoritesInterface;
import com.flickr4java.flickr.people.PeopleInterface;
import com.flickr4java.flickr.people.User;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.photosets.PhotosetsInterface;
import com.flickr4java.flickr.tags.TagsInterface;
import com.flickr4java.flickr.test.TestInterface;

import org.xml.sax.SAXException;

/**
 * This service encapsulates the features provided by the Flickr API.
 *
 * @author Rick Wellman
 */
public class FlickrService {

    private static Flickr flickr = null;

    private FlickrService() {
    }

    /**
     * Made private (because it should be).
     *
     * @return
     */
    private synchronized static Flickr getFlickr() {
        if (flickr == null) {
            //Flickr.tracing = false;
            //flickr = new Flickr("b5c9fb7ef8f08b9c8ae7c72a606b7d1d"); //("af1e08e71047433b04fe4bcf4397c0b6");
            final Transport t = new REST();
            flickr = new Flickr("b5c9fb7ef8f08b9c8ae7c72a606b7d1d", "991e942c7474bf75", t);

            final TestInterface testInterface = flickr.getTestInterface();
            try {
                // Collection results = testInterface.echo(Collections.EMPTY_LIST);
                Collection results = testInterface.echo(Collections.EMPTY_MAP);
            } catch (FlickrException ex) {
                Logger.getLogger(FlickrService.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
            }

        }

        return flickr;
    }

    public static ContactsInterface getContactsInterface() {
        return getFlickr().getContactsInterface();
    }

    public static PeopleInterface getPeopleInterface() {
        return getFlickr().getPeopleInterface();
    }

    public static FavoritesInterface getFavoritesInterface() {
        return getFlickr().getFavoritesInterface();
    }

    public static PhotosetsInterface getPhotosetsInterface() {
        return getFlickr().getPhotosetsInterface();
    }

    public static PhotosInterface getPhotosInterface() {
        return getFlickr().getPhotosInterface();
    }

    public static TagsInterface getTagsInterface() {
        return getFlickr().getTagsInterface();
    }

    @SuppressWarnings("unchecked")
    public static Photoset[] getAlbums(User user) {
        PhotosetsInterface photosetsInterface = FlickrService.getPhotosetsInterface();
        try {
            return ((Collection<Photoset>) photosetsInterface.getList(user.getId()).getPhotosets()).toArray(new Photoset[0]);
        } catch (FlickrException e) {
            e.printStackTrace();
        }

        return new Photoset[0];
    }

    public static BufferedImage getBuddyIcon(User contact) {
        final int iconServer = contact.getIconServer();
        try {
            String dynamicImage = "http://static.flickr.com/" + iconServer + "/buddyicons/" + contact.getId() + ".jpg";
            String staticImage = "http://www.flickr.com/images/buddyicon.jpg";
            final String url = (iconServer > 0) ? dynamicImage : staticImage;
            return ImageIO.read(new URL(url));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
