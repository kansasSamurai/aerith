package com.sun.javaone.aerith.model;

import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.beans.Statement;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.people.User;
import com.flickr4java.flickr.people.PeopleInterface;
import com.sun.javaone.aerith.model.flickr.Catalog;
import com.sun.javaone.aerith.ui.PhotoWrapper;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdesktop.swingx.mapviewer.GeoPosition;

/**
 *
 * @author aerith
 */
public class DataManager {

    @SuppressWarnings("unchecked")
    public static Catalog get(String userName) {

        final Catalog catalog = new Catalog();
        try {
            PeopleInterface peopleInterface = FlickrService.getPeopleInterface();
//            ContactsInterface contactsInterface = FlickrService.getContactsInterface();

            User mainUser = peopleInterface.findByUsername(userName);
            mainUser = peopleInterface.getInfo(mainUser.getId());
            catalog.addUsers(mainUser);
//            catalog.addUsers(users.subList(0, users.size() < 5 ? users.size() : 5).toArray(new User[0]));
            catalog.prefetch();

//            Collection<Contact> contacts = contactsInterface.getPublicList(mainUser.getId());
//            for (Contact contact : contacts) {
//                User user = peopleInterface.getInfo(contact.getId());
//                users.add(user);
//            }

            // It appears that this list is not used? rlw
            final List<User> users = new ArrayList<>(6);
            Collections.shuffle(users);

        } catch (FlickrException e) {
            e.printStackTrace();
        }

        return catalog;
    }

    public static String serializeTrip(Trip t) throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final XMLEncoder encoder = new XMLEncoder(baos);
            encoder.setPersistenceDelegate(GeneralPath.class, new GeneralPathDelegate());
            encoder.setPersistenceDelegate(GeoPosition.class, new GeoPositionDelegate());
            encoder.setPersistenceDelegate(PhotoWrapper.class, new PhotoWrapperDelegate());
            encoder.writeObject(t);
            encoder.close();
        final String xml = baos.toString();
        baos.close();

        return xml;
    }

    public static Trip deserializeTrip(String xml) throws IOException {
        ByteArrayInputStream in = null;
        XMLDecoder decoder = null;
        try {
            in = new ByteArrayInputStream(xml.getBytes());
            decoder = new XMLDecoder(in);
            final Trip t = (Trip)decoder.readObject();

            return t;
        } catch (Exception ex) {
            Logger.getLogger(DataManager.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (decoder != null) decoder.close();
            if (in != null) in.close();
        }
        return null;
    }

    public static final class GeoPositionDelegate extends DefaultPersistenceDelegate {
        public GeoPositionDelegate() {
            super(new String[] {"latitude", "longitude"});
        }
    }

    public static final class PhotoWrapperDelegate extends DefaultPersistenceDelegate {
        public PhotoWrapperDelegate() {
            super(new String[] {"flickrPhoto"});
        }
    }

    public static final class GeneralPathDelegate extends PersistenceDelegate {
        protected Expression instantiate(Object oldInstance, Encoder out) {
            return new Expression(oldInstance, GeneralPath.class, "new", new Object[0]);
        }
        protected void initialize(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
            GeneralPath a = (GeneralPath)oldInstance;

            AffineTransform tx = new AffineTransform();
            PathIterator itr = a.getPathIterator(tx);

            out.writeStatement(new Statement(a, "setWindingRule", new Object[] {a.getWindingRule()}));

            while (!itr.isDone()) {
                float[] segment = new float[6]; //must use floats because lineTo etc use floats
                int pathType = itr.currentSegment(segment);

                switch (pathType) {
                    case PathIterator.SEG_CLOSE:
                        out.writeStatement(new Statement(a, "closePath", new Object[0]));
                        break;
                    case PathIterator.SEG_CUBICTO:
                        out.writeStatement(new Statement(a, "curveTo", new Object[] {segment[0], segment[1], segment[2], segment[3], segment[4], segment[5]}));
                        break;
                    case PathIterator.SEG_LINETO:
                        out.writeStatement(new Statement(a, "lineTo", new Object[] {segment[0], segment[1]}));
                        break;
                    case PathIterator.SEG_MOVETO:
                        out.writeStatement(new Statement(a, "moveTo", new Object[] {segment[0], segment[1]}));
                        break;
                    case PathIterator.SEG_QUADTO:
                        out.writeStatement(new Statement(a, "quadTo", new Object[] {segment[0], segment[1], segment[2], segment[3]}));
                        break;
                }
                itr.next();
            }
        }
    }
}
