package org.jdownloader.controlling.contextmenu;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.Storable;
import org.appwork.storage.config.MinTimeWeakReference;
import org.appwork.storage.config.MinTimeWeakReferenceCleanup;
import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.StringUtils;
import org.jdownloader.actions.AppAction;
import org.jdownloader.actions.ComponentProviderInterface;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.ExtensionNotLoadedException;
import org.jdownloader.images.NewTheme;

public class MenuItemData implements Storable, MinTimeWeakReferenceCleanup {

    public static final String      EMPTY_NAME = "<NO NAME>";
    private ArrayList<MenuItemData> items;
    private String                  name;
    private String                  iconKey;
    private String                  className;
    private ActionData              actionData;

    public String _getIdentifier() {
        if (actionData != null) {

            if (actionData.getData() != null) return actionData.getClazzName() + ":" + actionData.getData() + ":" + actionData.getSetup();

            return actionData.getClazzName() + ":" + actionData.getSetup();

        }
        if (getClass() != MenuContainer.class && getClass() != MenuItemData.class) { return getClass().getName(); }
        if (StringUtils.isNotEmpty(className)) return className;
        return getIconKey() + ":" + getName();

    }

    public String toString() {

        return _getIdentifier() + "";
    }

    public ActionData getActionData() {
        return actionData;
    }

    public void setActionData(ActionData actionData) {

        this.actionData = actionData;
    }

    public String getClassName() {
        if (StringUtils.isNotEmpty(className)) return className;
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

    private Type                                                                 type    = Type.ACTION;

    private boolean                                                              validated;

    private Exception                                                            validateException;

    private MenuContainerRoot                                                    root;

    private String                                                               mnemonic;

    private String                                                               shortcut;

    private NullsafeAtomicReference<MinTimeWeakReference<CustomizableAppAction>> action  = new NullsafeAtomicReference<MinTimeWeakReference<CustomizableAppAction>>();
    private boolean                                                              visible = true;

    public void setMnemonic(String mnemonic) {
        this.mnemonic = mnemonic;
    }

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

    public MenuItemData(ActionData actionData) {
        this();
        setActionData(actionData);
    }

    public MenuItemData(Class<? extends AppAction> class1) {
        this(new ActionData(class1));
    }

    public MenuItemData createValidatedItem() throws InstantiationException, IllegalAccessException, ClassNotFoundException, ExtensionNotLoadedException {

        if (className == null || getClass().getName().equals(className)) {
            this._setValidated(true);
            return this;
        }

        MenuItemData ret = createInstance(this);
        ret._setValidated(true);
        return ret;

    }

    protected MenuItemData createInstance(MenuItemData menuItemData) throws InstantiationException, IllegalAccessException, ClassNotFoundException, ExtensionNotLoadedException {

        if (menuItemData.getClassName() == null) return menuItemData;

        MenuItemData ret = null;

        String packageName = AbstractExtension.class.getPackage().getName();
        if (menuItemData.getClassName().startsWith(packageName)) {

            ret = (MenuItemData) ExtensionController.getInstance().loadClass(menuItemData.getClassName()).newInstance();

        } else {
            ret = (MenuItemData) Class.forName(menuItemData.getClassName()).newInstance();
        }
        ret.setVisible(menuItemData.isVisible());
        if (getActionData() != null) ret.setActionData(getActionData());
        ret.setIconKey(menuItemData.getIconKey());
        ret.setName(menuItemData.getName());
        ret.setItems(menuItemData.getItems());
        ret.setType(menuItemData.getType());

        return ret;

    }

    public JComponent createItem() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, SecurityException, ExtensionNotLoadedException {

        if (actionData == null) {
            //
            throw new WTFException("No ACTION");
        }
        final CustomizableAppAction action = createAction();
        action.requestUpdate(this);
        if (!isVisible()) return null;
        if (!action.isVisible()) return null;
        if (StringUtils.isNotEmpty(getShortcut())) {
            action.setAccelerator(KeyStroke.getKeyStroke(getShortcut()));
        }

        if (action instanceof ComponentProviderInterface) { return ((ComponentProviderInterface) action).createComponent(this); }
        JMenuItem ret = action.isToggle() ? new JCheckBoxMenuItem(action) : new JMenuItem(action);

        ret.getAccessibleContext().setAccessibleName(action.getName());
        ret.getAccessibleContext().setAccessibleDescription(action.getTooltipText());
        if (StringUtils.isNotEmpty(name)) {
            ret.setText(name);
        }
        if (StringUtils.isNotEmpty(iconKey)) {
            ret.setIcon(NewTheme.I().getIcon(iconKey, 20));
        }
        return ret;

    }

    @SuppressWarnings("unchecked")
    public CustomizableAppAction createAction() throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ExtensionNotLoadedException {
        if (!validated) {
            //
            throw new WTFException();
        }
        if (actionData == null) {
            //
            throw new WTFException("No ACTION");
        }
        CustomizableAppAction ret = null;
        MinTimeWeakReference<CustomizableAppAction> minWeakAction = this.action.get();
        if (minWeakAction != null && (ret = minWeakAction.get()) != null) return ret;
        // if (action != null && action instanceof AbstractSelectionContextAction) {
        // ((AbstractSelectionContextAction) action).setSelection(selection);
        // if (action instanceof CachableInterface) {
        // if (StringUtils.isNotEmpty(actionData.getData())) {
        // ((CachableInterface) action).setData(actionData.getData());
        // }
        // }
        // fill(action.getClass(), action);
        // return customize(action);
        // } else if (action != null && action instanceof CachableInterface) {
        // // no need to set selection. action does not need any selection
        //
        // if (StringUtils.isNotEmpty(actionData.getData())) {
        // ((CachableInterface) action).setData(actionData.getData());
        // }
        // fill(action.getClass(), action);
        // return customize(action);
        // } else if (action != null) {
        // System.out.println("Please Update Action " + action.getClass().getName());
        // }

        Class<?> clazz = actionData._getClazz();
        Constructor<?> c = clazz.getConstructor(new Class[] {});
        ret = (CustomizableAppAction) c.newInstance(new Object[] {});
        ret.setMenuItemData(this);
        ret.initContextDefaults();
        ret.loadContextSetups();
        minWeakAction = new MinTimeWeakReference<CustomizableAppAction>(ret, 30 * 1000l, "MenuItemAction", this);
        action.set(minWeakAction);
        return (ret);
    }

    public JComponent addTo(JComponent root) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, SecurityException, ExtensionNotLoadedException {

        JComponent it;

        it = createItem();
        if (it == null) return null;
        root.add(it);
        return it;

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
        ArrayList<MenuItemData> newPath = new ArrayList<MenuItemData>();
        newPath.add(this);

        set.add(newPath);
        if (getItems() != null) {
            for (MenuItemData d : getItems()) {
                for (List<MenuItemData> p : d.listPathes()) {
                    newPath = new ArrayList<MenuItemData>();
                    newPath.add(this);
                    newPath.addAll(p);
                    set.add(newPath);
                }
            }

        }
        return set;
    }

    public String _getDescription() {
        if (getActionData() != null) {
            try {
                return createAction().getTooltipText();
            } catch (Exception e) {

            }
        }
        return null;
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public Collection<String> _getItemIdentifiers() {
        HashSet<String> ret = new HashSet<String>();
        for (MenuItemData mid : getItems()) {
            ret.add(mid._getIdentifier());
        }
        return ret;
    }

    public void _setValidated(boolean b) {
        validated = true;
    }

    public boolean _isValidated() {
        return validated;
    }

    public void _setValidateException(Exception e) {
        this.validateException = e;
    }

    public Exception _getValidateException() {
        return validateException;
    }

    public MenuContainerRoot _getRoot() {
        return root;
    }

    public void _setRoot(MenuContainerRoot root) {
        this.root = root;
    }

    public String getShortcut() {
        return shortcut;
    }

    public void setShortcut(String shortcut) {
        this.shortcut = shortcut;
    }

    // public String _getShortcut() {
    // if (StringUtils.isNotEmpty(shortcut)) { return shortcut; }
    // if (getActionData() != null) {
    // try {
    // return createAction(null).getShortCutString();
    // } catch (Exception e) {
    //
    // }
    // }
    // return null;
    // }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean b) {
        visible = b;

    }

    public void clearCachedAction() {
        MinTimeWeakReference<CustomizableAppAction> old = action.getAndSet(null);
        if (old != null) old.clearReference();
    }

    @Override
    public void onMinTimeWeakReferenceCleanup(MinTimeWeakReference<?> minTimeWeakReference) {
        action.compareAndSet((MinTimeWeakReference<CustomizableAppAction>) minTimeWeakReference, null);
    }

}
