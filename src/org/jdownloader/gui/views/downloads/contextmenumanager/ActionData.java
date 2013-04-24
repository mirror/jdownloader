package org.jdownloader.gui.views.downloads.contextmenumanager;

import java.util.HashSet;

import org.appwork.storage.Storable;

public class ActionData implements Storable {
    private HashSet<MenuItemProperty> properties;
    private Class<?>                  clazz;

    public Class<?> _getClazz() throws ActionClassNotAvailableException {
        if (clazz == null) {
            try {
                clazz = Class.forName(getClazzName());
            } catch (ClassNotFoundException e) {
                throw new ActionClassNotAvailableException(getClazzName());
            }
        }
        return clazz;
    }

    private String clazzName;
    private int    version = -1;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public ActionData(/* Storable */) {

    }

    public ActionData(int versionNumber, Class<?> class1, MenuItemProperty... ps) {
        version = versionNumber;
        this.clazz = class1;
        this.clazzName = class1.getName();
        properties = new HashSet<MenuItemProperty>();
        for (MenuItemProperty ap : ps) {
            properties.add(ap);
        }
    }

    public String getClazzName() {
        return clazzName;
    }

    public void setClazzName(String clazzName) {
        this.clazzName = clazzName;
    }

    public HashSet<MenuItemProperty> getProperties() {
        return properties;
    }

    public void setProperties(HashSet<MenuItemProperty> properties) {
        this.properties = properties;
    }
}
