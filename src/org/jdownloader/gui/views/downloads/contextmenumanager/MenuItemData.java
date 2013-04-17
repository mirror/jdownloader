package org.jdownloader.gui.views.downloads.contextmenumanager;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.JComponent;
import javax.swing.JMenuItem;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.Storable;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.views.SelectionInfo;

public class MenuItemData implements Storable {
    private HashSet<MenuItemProperty> properties;

    private ArrayList<MenuItemData>   items;
    private String                    name;
    private String                    iconKey;
    private String                    className;
    private ActionData                actionData;

    public ActionData getActionData() {
        return actionData;
    }

    public void setActionData(ActionData actionData) {
        this.actionData = actionData;
    }

    public String getClassName() {
        if (getClass() == MenuItemData.class) return null;
        return getClass().getName();
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public static enum Type {
        ACTION,
        CONTAINER;
    }

    private Type         type = Type.ACTION;

    private MenuItemData real;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public MenuItemData(/* Storable */) {
        items = new ArrayList<MenuItemData>();
    }

    public ArrayList<MenuItemData> getItems() {
        return items;
    }

    public void setItems(ArrayList<MenuItemData> items) {
        this.items = items;
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

    public void add(MenuItemData child) {
        items.add(child);
    }

    public HashSet<MenuItemProperty> getProperties() {
        return properties;
    }

    public void setProperties(HashSet<MenuItemProperty> properties) {
        this.properties = properties;
    }

    public MenuItemData(MenuItemProperty... ps) {
        properties = new HashSet<MenuItemProperty>();
        for (MenuItemProperty p : ps) {
            properties.add(p);
        }
    }

    public MenuItemData(ActionData actionData, MenuItemProperty... itemProperties) {
        this(itemProperties);
        setActionData(actionData);
    }

    public MenuItemData lazyReal() {
        try {
            if (real != null) return real;
            if (getClassName() == className || className == null) return this;

            MenuItemData ret = (MenuItemData) Class.forName(className).newInstance();
            ret.setIconKey(getIconKey());
            ret.setName(getName());
            ret.setItems(getItems());
            ret.setType(getType());
            ret.setProperties(getProperties());
            real = ret;
            return ret;
        } catch (Throwable e) {
            throw new WTFException(e);
        }
    }

    public JComponent createItem(SelectionInfo<?, ?> selection) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        if (actionData == null) {
            //
            throw new WTFException("No ACTION");
        }

        return new JMenuItem(createAction(selection));

    }

    public AppAction createAction(SelectionInfo<?, ?> selection) {
        if (actionData == null) {
            //
            throw new WTFException("No ACTION");
        }
        Class<?> clazz = actionData._getClazz();
        if (clazz == null) {
            //
            throw new WTFException("InValid ActionClass " + actionData.getClazzName());
        }
        if (selection == null) {

            try {
                Constructor<?> c = clazz.getConstructor(new Class[] {});
                AppAction action = (AppAction) c.newInstance(new Object[] {});
                return action;
            } catch (Exception e) {

            }

        }
        try {
            Constructor<?> c = clazz.getConstructor(new Class[] { SelectionInfo.class });
            AppAction action = (AppAction) c.newInstance(new Object[] { selection });
            return action;
        } catch (Exception e) {
            throw new WTFException(e);
        }

    }

    protected boolean showItem(MenuItemData inst, SelectionInfo<?, ?> selection) {
        if (inst.mergeProperties() == null) return true;
        for (MenuItemProperty p : inst.getProperties()) {
            switch (p) {
            case LINK_CONTEXT:
                if (selection.isLinkContext()) return false;
                break;
            case PACKAGE_CONTEXT:
                if (selection.isPackageContext()) return false;
                break;
            case HIDE_IF_DISABLED:
                break;
            case HIDE_IF_OPENFILE_IS_UNSUPPORTED:
                if (!CrossSystem.isOpenFileSupported()) return false;
                break;
            case HIDE_IF_OUTPUT_NOT_EXISTING:
                if (selection == null) return false;

                File file = null;

                if (selection.isLinkContext()) {
                    if (selection.getContextLink() instanceof DownloadLink) {
                        file = new File(((DownloadLink) selection.getContextLink()).getFileOutput());
                    } else {
                        throw new WTFException("TODO");
                    }

                } else {
                    if (selection.getContextPackage() instanceof FilePackage) {
                        file = new File(((FilePackage) selection.getContextPackage()).getDownloadDirectory());
                    } else {
                        throw new WTFException("TODO");
                    }

                }
                if (file == null || !file.exists()) return false;

            }
        }
        return true;
    }

    public JComponent addTo(JComponent root, SelectionInfo<?, ?> selection) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        if (!showItem(this, selection)) return null;
        JComponent it = createItem(selection);
        if (it == null) return null;
        if (!it.isEnabled() && mergeProperties().contains(MenuItemProperty.HIDE_IF_DISABLED)) return null;

        root.add(it);
        return it;

    }

    public HashSet<MenuItemProperty> mergeProperties() {
        HashSet<MenuItemProperty> ret = new HashSet<MenuItemProperty>();
        if (getProperties() != null) ret.addAll(getProperties());
        if (actionData != null && actionData.getProperties() != null) ret.addAll(actionData.getProperties());
        return ret;
    }
}
