package org.jdownloader.controlling.contextmenu;

import java.util.HashMap;
import java.util.HashSet;

import org.appwork.storage.Storable;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.ExtensionNotLoadedException;

public class ActionData implements Storable {
    private HashSet<MenuItemProperty> properties;
    private String                    jsonData;

    private Class<?>                  clazz;
    private static final String       PACKAGE_NAME = AbstractExtension.class.getPackage().getName() + ".";
    private String                    data;

    public void setData(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public Class<?> _getClazz() throws ClassNotFoundException, ExtensionNotLoadedException {

        if (clazz == null) {

            if (_isExtensionAction()) {

                clazz = ExtensionController.getInstance().loadClass(getClazzName());

            } else {
                clazz = Class.forName(getClazzName());
            }

        } else if (_isExtensionAction()) {
            clazz = ExtensionController.getInstance().loadClass(getClazzName());
        }
        return clazz;
    }

    private boolean _isExtensionAction() {

        String cn = getClazzName();
        int i = cn.lastIndexOf(".");
        String pkg = i >= 0 ? cn.substring(0, i) : "";
        boolean ret = pkg.startsWith(PACKAGE_NAME);
        return ret;
    }

    private String                  clazzName;
    private String                  name;
    private String                  iconKey;
    private HashMap<String, Object> setup;

    public ActionData(/* Storable */) {

    }

    public ActionData(Class<?> class1, MenuItemProperty... ps) {
        this(class1, null, ps);
    }

    public ActionData(Class<?> class1, String data, MenuItemProperty... ps) {
        this.data = data;
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

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getIconKey() {
        return iconKey;
    }

    public void setIconKey(String iconKey) {
        this.iconKey = iconKey;
    }

    public void putSetup(String key, Object value) {

        if (setup == null) {
            setup = new HashMap<String, Object>();
        }
        setup.put(key, value);
    }

    public HashMap<String, Object> getSetup() {
        return setup;
    }

    public void setSetup(HashMap<String, Object> setup) {
        this.setup = setup;
    }

    public Object fetchSetup(String name2) {
        if (setup == null) return null;
        return setup.get(name2);
    }

}
