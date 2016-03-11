package jd.plugins.components;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Random;

/**
 * a way to switch api servers designed for zevera but can be used with any that share api server names.
 *
 * @author raztoki
 *
 */
public class ZeveraApiTracker {

    public ZeveraApiTracker() {

    }

    private final static ArrayList<String> APIS = new ArrayList<String>();

    static {
        APIS.add("api.");
        APIS.add("api1.");
        APIS.add("api2.");
        APIS.add("api24.");
        APIS.add("api64.");
    }

    private final LinkedHashMap<String, Integer> failures = new LinkedHashMap<String, Integer>();

    private String getRandomApi() {
        return APIS.get(new Random().nextInt(APIS.size()));
    }

    private String  current   = null;
    private boolean hasFailed = false;

    public final String get() {
        synchronized (failures) {
            if (current != null && !hasFailed) {
                // reuse the last server that hasn't failed.
                return current;
            } else if (current == null && failures.isEmpty()) {
                // virgin
                current = getRandomApi();
                hasFailed = false;
                return current;
            } else if (failures.size() != APIS.size()) {
                // if not all within, choose a random server not within
                current = getRandomApi();
                while (failures.containsKey(current)) {
                    current = getRandomApi();
                }
                hasFailed = false;
                return current;
            } else {
                // failures contains each server, lets choose a server with the least amount of failures!
                int failure = 0;
                String server = null;
                for (final Entry<String, Integer> e : failures.entrySet()) {
                    if (server == null) {
                        server = e.getKey();
                        failure = e.getValue();
                    } else if (e.getValue() < failure) {
                        server = e.getKey();
                        failure = e.getValue();
                    }
                }
                current = server;
                hasFailed = false;
                return server;
            }
        }
    }

    public final void setFailure() {
        synchronized (failures) {
            final int currentFailure = failures.containsKey(current) ? failures.get(current) : 0;
            failures.put(current, currentFailure + 1);
            current = null;
            hasFailed = true;
        }
    }

}
