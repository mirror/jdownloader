package org.jdownloader.api.config;

import org.appwork.remoteapi.QueryResponseMap;
import org.appwork.storage.Storable;
import org.appwork.storage.config.handler.KeyHandler;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;
import org.jdownloader.settings.advanced.AdvancedConfigInterfaceEntry;

public class ConfigInterfaceAPIStorable implements Storable {

    public ConfigInterfaceAPIStorable(/* Storable */) {
    }

    public ConfigInterfaceAPIStorable(AdvancedConfigEntry ace) {
        KeyHandler<?> kh = ((AdvancedConfigInterfaceEntry) ace).getKeyHandler();
        this.key = kh.getStorageHandler().getConfigInterface().getName();
        this.name = key.substring(key.lastIndexOf(".") + 1, key.length());

        this.infoMap = new QueryResponseMap();
        infoMap.put("settingsCount", 1);
    }

    private String           key;
    private String           name;
    private QueryResponseMap infoMap;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public QueryResponseMap getInfoMap() {
        return infoMap;
    }

    public void setInfoMap(QueryResponseMap infoMap) {
        this.infoMap = infoMap;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ConfigInterfaceAPIStorable other = (ConfigInterfaceAPIStorable) obj;
        if (key == null) {
            if (other.key != null) return false;
        } else if (!key.equals(other.key)) return false;
        return true;
    }

}