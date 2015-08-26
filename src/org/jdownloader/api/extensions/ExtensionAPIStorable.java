package org.jdownloader.api.extensions;

import org.appwork.storage.Storable;
import org.jdownloader.myjdownloader.client.json.AbstractJsonData;

public class ExtensionAPIStorable extends AbstractJsonData implements Storable {
    private String  id;
    private boolean installed;
    private boolean enabled;
    private String  name;
    private String  iconKey;
    private String  description;
    private String  configInterface;

    public ExtensionAPIStorable(/* Storable */) {
    }

    public boolean isInstalled() {
        return installed;
    }

    public void setInstalled(boolean installed) {
        this.installed = installed;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIconKey() {
        return iconKey;
    }

    public void setIconKey(String iconKey) {
        this.iconKey = iconKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConfigInterface() {
        return configInterface;
    }

    public void setConfigInterface(String configInterface) {
        this.configInterface = configInterface;
    }

    public void setId(String id) {
        this.id = id;
    }

}
