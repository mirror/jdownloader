package org.jdownloader.settings.advanced;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;

public class AdvancedConfigManager {
    private static final AdvancedConfigManager INSTANCE = new AdvancedConfigManager();

    public static AdvancedConfigManager getInstance() {
        return AdvancedConfigManager.INSTANCE;
    }

    private ArrayList<ConfigInterface> configInterfaces;
    private AdvancedConfigEventSender  eventSender;

    private AdvancedConfigManager() {
        configInterfaces = new ArrayList<ConfigInterface>();
        eventSender = new AdvancedConfigEventSender();
    }

    public void register(ConfigInterface cf) {
        configInterfaces.add(cf);
        eventSender.fireEvent(new AdvancedConfigEvent(this, AdvancedConfigEvent.Types.UPDATED, cf));
    }
}
