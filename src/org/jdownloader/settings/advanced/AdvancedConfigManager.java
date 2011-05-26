package org.jdownloader.settings.advanced;

import java.util.ArrayList;
import java.util.HashMap;


import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.KeyHandler;
import org.jdownloader.settings.AboutConfig;
import org.jdownloader.settings.GeneralSettings;

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
        this.register(JsonConfig.create(GeneralSettings.class));
    }

    public AdvancedConfigEventSender getEventSender() {
        return eventSender;
    }

    public void register(ConfigInterface cf) {

        HashMap<KeyHandler, Boolean> map = new HashMap<KeyHandler, Boolean>();

        for (KeyHandler m : cf.getStorageHandler().getMap().values()) {

            if (map.containsKey(m)) continue;

            if (m.getAnnotation(AboutConfig.class) != null) {
                if (m.getSetter() == null) {
                    throw new RuntimeException("Setter for " + m.getGetter().getMethod() + " missing");
                } else if (m.getGetter() == null) {
                    throw new RuntimeException("Getter for " + m.getSetter().getMethod() + " missing");
                } else {
                    configInterfaces.add(new AdvancedConfigInterfaceEntry(cf, m));
                    map.put(m, true);
                }
            }

        }

        eventSender.fireEvent(new AdvancedConfigEvent(this, AdvancedConfigEvent.Types.UPDATED, cf));
    }

    public ArrayList<AdvancedConfigEntry> list() {

        return configInterfaces;
    }
}
