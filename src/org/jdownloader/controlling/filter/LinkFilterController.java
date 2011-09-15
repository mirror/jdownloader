package org.jdownloader.controlling.filter;

import java.util.ArrayList;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.storage.config.JsonConfig;

public class LinkFilterController {
    private static final LinkFilterController INSTANCE = new LinkFilterController();

    /**
     * get the only existing instance of LinkFilterController. This is a
     * singleton
     * 
     * @return
     */
    public static LinkFilterController getInstance() {
        return LinkFilterController.INSTANCE;
    }

    private ArrayList<FilterRule> filter;
    private LinkFilterSettings    config;

    /**
     * Create a new instance of LinkFilterController. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private LinkFilterController() {
        config = JsonConfig.create(LinkFilterSettings.class);
        filter = config.getFilterList();
        if (filter == null) filter = new ArrayList<FilterRule>();
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void run() {
                synchronized (LinkFilterController.this) {
                    config.setFilterList(filter);
                }
            }

            @Override
            public String toString() {
                return "save filters...";
            }
        });
    }

    public boolean isBlacklist() {
        return config.isBlackList();
    }

    public void setBlacklist(boolean b) {
        config.setBlackList(b);
    }

    public ArrayList<FilterRule> list() {
        synchronized (this) {
            return new ArrayList<FilterRule>(filter);
        }
    }

    public void add(FilterRule linkFilter) {
        if (linkFilter == null) return;
        synchronized (this) {
            filter.add(linkFilter);
            config.setFilterList(filter);
        }
    }

    public void remove(FilterRule lf) {
        if (lf == null) return;
        synchronized (this) {
            filter.remove(lf);
            config.setFilterList(filter);
        }
    }

    public String test(String filter2) {
        return null;
    }

}
