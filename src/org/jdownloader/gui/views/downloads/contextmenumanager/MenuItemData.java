package org.jdownloader.gui.views.downloads.contextmenumanager;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenuItem;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.Storable;
import org.appwork.utils.StringUtils;
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
        real = null;
        this.actionData = actionData;
    }

    public String getClassName() {
        if (StringUtils.isNotEmpty(className)) return className;
        if (getClass() == MenuItemData.class) return null;
        return getClass().getName();
    }

    public void setClassName(String className) {
        real = null;
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
        real = null;
        this.type = type;
    }

    public MenuItemData(/* Storable */) {
        items = new ArrayList<MenuItemData>();
    }

    public ArrayList<MenuItemData> getItems() {
        return items;
    }

    public void setItems(ArrayList<MenuItemData> items) {
        real = null;
        this.items = items;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        real = null;
        this.name = name;
    }

    public String getIconKey() {
        return iconKey;
    }

    public void setIconKey(String iconKey) {
        real = null;
        this.iconKey = iconKey;
    }

    public void add(MenuItemData child) {
        items.add(child);
    }

    /**
     * add A path
     * 
     * @param path
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public void add(List<MenuItemData> path) {
        try {
            MenuItemData lastNode = createInstance(path.get(path.size() - 1));

            for (int i = path.size() - 2; i >= 0; i--) {

                MenuItemData node = path.get(i);

                MenuItemData ret;

                ret = createInstance(node);
                if (ret instanceof MenuContainerRoot) break;
                ret.setItems(new ArrayList<MenuItemData>());
                ret.add(lastNode);
                lastNode = ret;

            }
            MenuItemData addAt = this;
            addBranch(this, lastNode);

        } catch (Exception e) {
            throw new WTFException(e);
        }
    }

    private void addBranch(MenuItemData menuItemData, MenuItemData lastNode) {
        boolean added = false;
        for (MenuItemData mu : menuItemData.getItems()) {
            if (mu.getActionData() != null) continue;
            if (StringUtils.equals(mu.getClassName(), lastNode.getClassName())) {
                // subfolder found

                if (lastNode.getItems() != null && lastNode.getItems().size() > 0) {

                    addBranch(mu, lastNode.getItems().get(0));
                    added = true;
                } else {
                    return;
                }

            }
        }
        if (!added) {
            menuItemData.getItems().add(lastNode);

        }
    }

    public HashSet<MenuItemProperty> getProperties() {
        return properties;
    }

    public void setProperties(HashSet<MenuItemProperty> properties) {
        real = null;
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
            if (className == null || getClass().getName().equals(className)) return this;

            MenuItemData ret = createInstance(this);

            real = ret;
            return ret;
        } catch (Throwable e) {
            throw new WTFException(e);
        }
    }

    private MenuItemData createInstance(MenuItemData menuItemData) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (menuItemData.getClassName() == null) return menuItemData;
        MenuItemData ret = (MenuItemData) Class.forName(menuItemData.getClassName()).newInstance();
        ret.setIconKey(menuItemData.getIconKey());
        ret.setName(menuItemData.getName());
        ret.setItems(menuItemData.getItems());
        ret.setType(menuItemData.getType());
        ret.setProperties(menuItemData.getProperties());
        return ret;

    }

    public JComponent createItem(SelectionInfo<?, ?> selection) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ActionClassNotAvailableException {

        if (actionData == null) {
            //
            throw new WTFException("No ACTION");
        }

        return new JMenuItem(createAction(selection));

    }

    public AppAction createAction(SelectionInfo<?, ?> selection) throws ActionClassNotAvailableException {
        if (actionData == null) {
            //
            throw new WTFException("No ACTION");
        }
        Class<?> clazz = actionData._getClazz();

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

        for (MenuItemProperty p : inst.mergeProperties()) {
            switch (p) {
            case LINK_CONTEXT:
                if (!selection.isLinkContext()) return false;
                break;
            case PACKAGE_CONTEXT:
                if (!selection.isPackageContext()) return false;
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
        System.out.println(1);
        JComponent it;
        try {
            it = createItem(selection);
        } catch (ActionClassNotAvailableException e) {
            return null;
        }
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

    public List<MenuItemData> list() {
        List<MenuItemData> set = new ArrayList<MenuItemData>();
        set.add(this);
        if (getItems() != null) {
            for (MenuItemData d : getItems()) {
                set.addAll(d.list());
            }

        }
        return set;
    }

    public List<List<MenuItemData>> listPathes() {
        List<List<MenuItemData>> set = new ArrayList<List<MenuItemData>>();

        if (getItems() != null) {
            for (MenuItemData d : getItems()) {
                for (List<MenuItemData> p : d.listPathes()) {
                    ArrayList<MenuItemData> newPath = new ArrayList<MenuItemData>();
                    newPath.add(this);
                    newPath.addAll(p);
                    set.add(newPath);
                }
            }

        } else {
            ArrayList<MenuItemData> newPath = new ArrayList<MenuItemData>();
            newPath.add(this);
            set.add(newPath);
        }
        return set;
    }
}
