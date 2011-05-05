package jd.controlling.reconnect;

import org.appwork.storage.Storable;

public class ReconnectPluginInfo implements Storable {
    public ReconnectPluginInfo() {
        // required by Storable
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    private String icon;
    private String className;
}
