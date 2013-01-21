package org.jdownloader.api.config;

import org.appwork.remoteapi.QueryResponseMap;
import org.appwork.storage.Storable;
import org.appwork.storage.config.handler.KeyHandler;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;
import org.jdownloader.settings.advanced.AdvancedConfigInterfaceEntry;

public class ConfigEntryAPIStorable implements Storable {

    public ConfigEntryAPIStorable(/* Storable */) {
    }

    public ConfigEntryAPIStorable(AdvancedConfigEntry ace) {
        KeyHandler<?> kh = ((AdvancedConfigInterfaceEntry) ace).getKeyHandler();
        this.key = kh.getKey();
        this.interfaceKey = kh.getStorageHandler().getConfigInterface().getName();
    }

    private String           key;
    private String           interfaceKey;
    private QueryResponseMap infoMap;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getInterfaceKey() {
        return interfaceKey;
    }

    public void setInterfaceKey(String interfaceKey) {
        this.interfaceKey = interfaceKey;
    }

    public QueryResponseMap getInfoMap() {
        return infoMap;
    }

    public void setInfoMap(QueryResponseMap infoMap) {
        this.infoMap = infoMap;
    }
}
