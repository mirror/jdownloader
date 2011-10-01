package org.jdownloader.controlling.packagizer;

import java.util.ArrayList;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.event.predefined.changeevent.ChangeEvent;
import org.appwork.utils.event.predefined.changeevent.ChangeEventSender;

public class PackagizerController {
    private static final PackagizerController INSTANCE = new PackagizerController();

    public static PackagizerController getInstance() {
        return INSTANCE;
    }

    private PackagizerSettings        config;
    private ArrayList<PackagizerRule> list;
    private ChangeEventSender         eventSender;

    private PackagizerController() {

        config = JsonConfig.create(PackagizerSettings.class);
        eventSender = new ChangeEventSender();
        list = config.getRuleList();
        if (list == null) list = new ArrayList<PackagizerRule>();

        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void run() {
                synchronized (PackagizerController.this) {
                    config.setRuleList(list);
                }
            }

            @Override
            public String toString() {
                return "save packagizer...";
            }
        });
    }

    public ChangeEventSender getEventSender() {
        return eventSender;
    }

    public ArrayList<PackagizerRule> list() {
        synchronized (this) {
            return new ArrayList<PackagizerRule>(list);
        }
    }

    public void add(PackagizerRule linkFilter) {
        if (linkFilter == null) return;
        synchronized (this) {
            list.add(linkFilter);
            config.setRuleList(list);
        }
        getEventSender().fireEvent(new ChangeEvent(this));
    }

    public void addAll(ArrayList<PackagizerRule> all) {
        if (all == null) return;
        synchronized (this) {
            list.addAll(all);
            config.setRuleList(list);

        }
        getEventSender().fireEvent(new ChangeEvent(this));
    }

    public void remove(PackagizerRule lf) {
        if (lf == null) return;
        synchronized (this) {
            list.remove(lf);
            config.setRuleList(list);
        }
        getEventSender().fireEvent(new ChangeEvent(this));

    }

}
