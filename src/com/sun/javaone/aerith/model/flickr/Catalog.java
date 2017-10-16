package com.sun.javaone.aerith.model.flickr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.photosets.PhotosetsInterface;
import com.flickr4java.flickr.people.PeopleInterface;
import com.flickr4java.flickr.people.User;
import com.sun.javaone.aerith.model.FlickrService;
import org.xml.sax.SAXException;

public class Catalog {

    private Photoset[] randomPicks;

    private final List<User> users = new ArrayList<>();

    public Photoset[] getRandomPicks() {
        return randomPicks;
    }

    public void addUsers(User... list) {
        for (User user : list) {
            System.out.println("adding user: " + user);
            users.add(user);
        }
    }

    public User[] getUsers() {
        return users.toArray(new User[0]);
    }

    @SuppressWarnings("unchecked")
    public void prefetch() {
        randomPicks = new Photoset[5];
        PhotosetsInterface photosetsInterface = FlickrService.getPhotosetsInterface();

        try {
            User user = users.get(0);
            Collection photosets = photosetsInterface.getList(user.getId()).getPhotosets();
            List<Photoset> shuffledSets = new ArrayList<>(photosets);
            Collections.shuffle(shuffledSets);
            int i = 0;
            for (Photoset set : shuffledSets) {
                final Photoset info = photosetsInterface.getInfo(set.getId());
                final PeopleInterface peopleInterface = FlickrService.getPeopleInterface();
                final User owner = peopleInterface.getInfo(info.getOwner().getId());
                set.setOwner(owner);
                randomPicks[i++] = set;
                if (i >= randomPicks.length) {
                    break;
                }
            }
          } catch (FlickrException e) {
          }
    }

}