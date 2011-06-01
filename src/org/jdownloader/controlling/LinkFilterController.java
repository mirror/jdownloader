package org.jdownloader.controlling;

import java.util.ArrayList;

import jd.gui.swing.jdgui.views.settings.components.LinkgrabberFilter.LinkFilter;

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

    private ArrayList<LinkFilter> filter;
    private LinkFilterSettings    config;

    /**
     * Create a new instance of LinkFilterController. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private LinkFilterController() {
        config = JsonConfig.create(LinkFilterSettings.class);
        filter = config.getFilterList();
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void run() {
                config.setFilterList(filter);
            }
        });
    }

    public boolean isBlacklist() {
        return config.isBlackList();
    }

    public void setBlacklist(boolean b) {
        config.setBlackList(b);
    }

    public ArrayList<LinkFilter> list() {
        return new ArrayList<LinkFilter>(filter);
    }

    public void add(LinkFilter linkFilter) {
        filter.add(linkFilter);
        save();
    }

    private void save() {
        JsonConfig.create(LinkFilterSettings.class).setFilterList(filter);
    }

    public void remove(LinkFilter lf) {
        filter.remove(lf);
        save();
    }

    public String test(String filter2) {
        return null;
    }

}
