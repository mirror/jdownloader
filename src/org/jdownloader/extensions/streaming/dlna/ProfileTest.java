package org.jdownloader.extensions.streaming.dlna;

import org.jdownloader.extensions.streaming.dlna.profiles.Profile;

public class ProfileTest {
    public static void main(String[] args) {
        long t = System.currentTimeMillis();
        Profile.init();
        System.out.println("LoadTime in MS: " + (System.currentTimeMillis() - t));
        System.out.println(" profiles: " + Profile.ALL_PROFILES.size());

        for (Profile p : Profile.ALL_PROFILES) {
            System.out.println(p);
        }
    }

}
