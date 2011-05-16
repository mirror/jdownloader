package org.jdownloader.settings.advanced;

import java.util.ArrayList;
import java.util.HashMap;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.MethodHandler;

public class AdvancedConfigManager {
    private static final AdvancedConfigManager INSTANCE = new AdvancedConfigManager();

    public static AdvancedConfigManager getInstance() {
        return AdvancedConfigManager.INSTANCE;
    }

    private ArrayList<AdvancedConfigEntry> configInterfaces;
    private AdvancedConfigEventSender      eventSender;

    private AdvancedConfigManager() {
        configInterfaces = new ArrayList<AdvancedConfigEntry>();
        eventSender = new AdvancedConfigEventSender();
    }

    public AdvancedConfigEventSender getEventSender() {
        return eventSender;
    }

    public void register(ConfigInterface cf) {

        HashMap<String, AdvancedConfigInterfaceEntry> map = new HashMap<String, AdvancedConfigInterfaceEntry>();
        for (MethodHandler m : cf.getStorageHandler().getMap().values()) {

            AdvancedConfigInterfaceEntry f = map.get(m.getKey());
            if (f == null) {
                f = new AdvancedConfigInterfaceEntry(cf);
                map.put(m.getKey(), f);
            }
            if (m.isGetter()) {
                f.setGetter(m);
            } else {
                f.setSetter(m);
            }
            if (f.getGetter() != null & f.getSetter() != null) {
                // if
                // (f.getGetter().getMethod().getAnnotation(AboutConfig.class)
                // != null ||
                // f.getSetter().getMethod().getAnnotation(AboutConfig.class) !=
                // null) {
                configInterfaces.add(f);
                // }
            }
        }

        eventSender.fireEvent(new AdvancedConfigEvent(this, AdvancedConfigEvent.Types.UPDATED, cf));
    }

    public ArrayList<AdvancedConfigEntry> list() {

        return configInterfaces;
    }
}
