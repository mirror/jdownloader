package org.jdownloader.controlling.packagizer;

import java.util.ArrayList;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.storage.config.JsonConfig;

public class PackagizerController {
    private static final PackagizerController INSTANCE = new PackagizerController();

    public static PackagizerController getInstance() {
        return INSTANCE;
    }

    private PackagizerSettings        config;
    private ArrayList<PackagizerRule> list;

    private PackagizerController() {

        config = JsonConfig.create(PackagizerSettings.class);
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
    }

    public void remove(PackagizerRule lf) {
        if (lf == null) return;
        synchronized (this) {
            list.remove(lf);
            config.setRuleList(list);
        }
    }

}
