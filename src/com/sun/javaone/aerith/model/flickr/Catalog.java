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

/**
 *
 * @author aerith
 */
public class Catalog {

    private Photoset[] randomPicks;

    private final List<User> users = new ArrayList<>();

    public Photoset[] getRandomPicks() {
        return randomPicks;
    }

    public void addUsers(User... list) {
        for (User user : list) {
            System.out.println("Adding user > " + user.getUsername() + " < id > " + user.getId());
            users.add(user);
        }
        System.out.println("Catalog UserList size > " + users.size());
    }

    public User[] getUsers() {
        return users.toArray(new User[0]);
    }

    @SuppressWarnings("unchecked")
    public void prefetch() {
        randomPicks = new Photoset[5];

        final PhotosetsInterface photosetsInterface = FlickrService.getPhotosetsInterface();
        try {
            final User user = users.get(0);
            final Collection photosets = photosetsInterface.getList(user.getId()).getPhotosets();
            final List<Photoset> shuffledSets = new ArrayList<>(photosets);
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
              e.printStackTrace();
          }
    }

}