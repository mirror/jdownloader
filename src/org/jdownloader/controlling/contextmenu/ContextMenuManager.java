package org.jdownloader.controlling.contextmenu;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.windowmanager.WindowManager;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.jdownloader.controlling.contextmenu.gui.ExtPopupMenu;
import org.jdownloader.controlling.contextmenu.gui.MenuBuilder;
import org.jdownloader.controlling.contextmenu.gui.MenuManagerDialog;
import org.jdownloader.controlling.contextmenu.gui.MenuManagerDialogInterface;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.logging.LogController;

public abstract class ContextMenuManager<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> {
    protected DelayedRunnable updateDelayer;

    public ContextMenuManager() {
        config = JsonConfig.create(Application.getResource("cfg/menus/" + getStorageKey()), ContextMenuConfigInterface.class);
        logger = LogController.getInstance().getLogger(getClass().getName());
        updateDelayer = new DelayedRunnable(1000l, 2000) {
            @Override
            public String getID() {
                return "MenuManager-" + ContextMenuManager.this.getClass();
            }

            @Override
            public void delayedrun() {
                updateGui();
            }

        };
    }

    protected abstract String getStorageKey();

    protected static MenuItemData setAccelerator(MenuItemData menuItemData, KeyStroke keyStroke) {
        menuItemData.setShortcut(keyStroke == null ? null : keyStroke.toString());
        return menuItemData;
    }

    protected static ActionData setName(ActionData actionData, String name) {
        if (StringUtils.isEmpty(name)) name = MenuItemData.EMPTY_NAME;
        actionData.setName(name);
        return actionData;
    }

    protected static MenuItemData setOptional(Class<?> class1) {
        return setOptional(new MenuItemData(new ActionData(class1)));
    }

    protected static MenuItemData setOptional(MenuItemData menuItemData) {
        menuItemData.setVisible(false);
        return menuItemData;
    }

    protected static ActionData setIconKey(ActionData putSetup, String KEY) {
        putSetup.setIconKey(KEY);
        return putSetup;
    }

    protected abstract void updateGui();

    public JPopupMenu build(SelectionInfo<PackageType, ChildrenType> si) {
        long t = System.currentTimeMillis();
        ExtPopupMenu root = new ExtPopupMenu();
        MenuContainerRoot md = getMenuData();
        new MenuBuilder(this, root, si, md).run();
        // createLayer(root, md);

        return root;
    }

    private boolean             managerVisible = false;
    protected MenuManagerDialog dialogFrame;

    public void openGui() {
        new Thread("Manager") {

            public void run() {
                if (managerVisible) {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            WindowManager.getInstance().setZState(dialogFrame.getDialog(), FrameState.TO_FRONT_FOCUSED);

                        }
                    };
                    return;
                }
                managerVisible = true;
                try {
                    UIOManager.I().show(MenuManagerDialogInterface.class, dialogFrame = new MenuManagerDialog(ContextMenuManager.this));
                } finally {
                    managerVisible = false;
                }
            }
        }.start();

    }

    public LogSource getLogger() {
        return logger;
    }

    MenuContainerRoot          menuData;
    ContextMenuConfigInterface config;
    LogSource                  logger;

    public List<ActionData> list() {

        HashSet<String> unique = new HashSet<String>();
        ArrayList<ActionData> ret = new ArrayList<ActionData>();
        for (MenuItemData mid : setupDefaultStructure().list()) {
            if (mid.getActionData() != null && unique.add(mid.getActionData().getClazzName())) {
                ret.add(mid.getActionData());
            }
        }
        return ret;
    }

    ArrayList<MenuExtenderHandler> extender = new ArrayList<MenuExtenderHandler>();

    // public void addExtensionAction(MenuContainerRoot parent, int index, MenuExtenderHandler extender, ExtensionContextMenuItem
    // archiveSubMenu) {
    // if (extender == null) throw new NullPointerException();
    // archiveSubMenu.setOwner(extender.getClass().getSimpleName());
    //
    // if (archiveSubMenu instanceof MenuLink) {
    // addSpecial(archiveSubMenu);
    // }
    // parent.getItems().add(index, archiveSubMenu);
    //
    // }

    public synchronized MenuContainerRoot getMenuData() {
        long t = System.currentTimeMillis();
        if (menuData != null) return menuData;
        try {
            convertOldFiles();
            MenuContainerRoot ret = config.getMenu();

            MenuContainerRoot defaultMenu = setupDefaultStructure();
            if (ret == null) {
                // no customizer ever used
                ret = defaultMenu;

            } else {
                ret.validate();

                List<MenuItemData> allItemsInMenu = ret.list();
                List<MenuItemData> allItemsInDefaultMenu = defaultMenu.list();
                HashMap<String, MenuItemData> itemsIdsInMenu = new HashMap<String, MenuItemData>();
                HashMap<String, MenuItemData> itemsInDefaultMenu = new HashMap<String, MenuItemData>();

                for (MenuItemData d : allItemsInDefaultMenu) {

                    itemsInDefaultMenu.put(d._getIdentifier(), d);

                }
                for (MenuItemData d : allItemsInMenu) {

                    itemsIdsInMenu.put(d._getIdentifier(), d);

                }
                ArrayList<String> unused = config.getUnusedItems();
                if (unused == null) {
                    unused = new ArrayList<String>();
                }

                ArrayList<MenuItemData> newActions = new ArrayList<MenuItemData>();
                HashSet<String> idsInUnusedList = new HashSet<String>(unused);

                // find new or updated actions

                for (Entry<String, MenuItemData> e : itemsInDefaultMenu.entrySet()) {

                    if (!idsInUnusedList.contains(e.getKey())) {
                        // not in unused list
                        if (!itemsIdsInMenu.containsKey(e.getKey())) {
                            // not in menu itself
                            // this is a new action
                            newActions.add(e.getValue());
                        }

                    }
                }

                if (newActions.size() > 0) {

                    List<List<MenuItemData>> pathes = defaultMenu.listPathes();
                    // HashSet<Class<?>> actionClassesInDefaultTree = new HashSet<Class<?>>();
                    // // HashMap<MenuItemData,> actionClassesInDefaultTree = new HashSet<Class<?>>();
                    //
                    // System.out.println(pathes);
                    // for (List<MenuItemData> path : pathes) {
                    //
                    // MenuItemData d = path.get(path.size() - 1);
                    // if (d.getActionData() != null) {
                    // if (d.getActionData().getClazzName() != null) {
                    // try {
                    // actionClassesInDefaultTree.add(d.getActionData()._getClazz());
                    // } catch (Exception e1) {
                    // logger.log(e1);
                    // }
                    // }
                    // }
                    // }
                    HashSet<String> itemsInSubmenuItems = new HashSet<String>();

                    for (MenuItemData ad : newActions) {
                        if (ad.getItems() != null) {
                            for (MenuItemData mid : ad.list()) {
                                if (mid == ad) continue;
                                // newActions.remove(mid);
                                itemsInSubmenuItems.add(mid._getIdentifier());
                            }
                        }

                    }
                    for (MenuItemData ad : newActions) {
                        if (itemsInSubmenuItems.contains(ad._getIdentifier())) continue;
                        for (List<MenuItemData> path : pathes) {
                            if (StringUtils.equals(path.get(path.size() - 1)._getIdentifier(), ad._getIdentifier())) {
                                try {
                                    ret.add(path);
                                } catch (Throwable e) {
                                    logger.log(e);

                                }
                            }

                        }

                    }
                    // neworUpdate.add(new SeparatorData());
                    // neworUpdate.add(new MenuItemData(new ActionData(0,MenuManagerAction.class)));

                }

            }
            ret.validate();
            System.out.println("Set menu Data");
            menuData = ret;
            System.out.println(System.currentTimeMillis() - t);
            return ret;
        } catch (Exception e) {
            logger.log(e);
            try {
                menuData = setupDefaultStructure();
                menuData.validate();
                return menuData;
            } catch (Exception e1) {
                logger.log(e1);
                menuData = new MenuContainerRoot();
                menuData.validate();
                return menuData;
            }
        }

    }

    private void convertOldFiles() {

    }

    abstract public MenuContainerRoot createDefaultStructure();

    // public void extend(JComponent root, ExtensionContextMenuItem<?> inst, SelectionInfo<?, ?> selection, MenuContainerRoot menuData) {
    // synchronized (extender) {
    // for (MenuExtenderHandler exHandler : extender) {
    // exHandler.extend(root, inst, selection, menuData);
    //
    // }
    // }
    // }

    public MenuContainerRoot setupDefaultStructure() {
        MenuContainerRoot ret = createDefaultStructure();

        synchronized (extender) {
            for (MenuExtenderHandler exHandler : extender) {
                MenuItemData r = exHandler.updateMenuModel(this, ret);
                if (r != null) ret.addBranch(ret, r);
            }

        }
        return ret;
    }

    public void setMenuData(MenuContainerRoot root) {
        String rootString = JSonStorage.serializeToJson(root);
        String orgDefString = JSonStorage.serializeToJson(setupDefaultStructure());
        MenuContainerRoot def = JSonStorage.restoreFromString(orgDefString, new TypeRef<MenuContainerRoot>() {
        });
        def.validate();
        String defaultString = JSonStorage.serializeToJson(def);
        if (rootString.equals(defaultString)) {
            root = null;
        }
        if (root == null) {

            config.setMenu(null);
            config.setUnusedItems(null);
            menuData = setupDefaultStructure();
            menuData.validate();

        } else {
            menuData = root;

            config.setMenu(root);
            config.setUnusedItems(getUnused(root));
        }
        updateGui();

    }

    private ArrayList<String> getUnused(MenuContainerRoot root) {
        ArrayList<String> list = new ArrayList<String>();

        List<MenuItemData> allItemsInMenu = root.list();

        HashSet<String> actionClassesInMenu = new HashSet<String>();

        for (MenuItemData d : allItemsInMenu) {
            actionClassesInMenu.add(d._getIdentifier());
        }
        // HashSet<String> actionClassesInDefaultMenu = new HashSet<String>();
        for (MenuItemData e : setupDefaultStructure().list()) {
            if (actionClassesInMenu.add(e._getIdentifier())) {
                list.add(e._getIdentifier());
            }

        }

        return list;
    }

    public void saveTo(MenuContainerRoot root, File saveTo) throws UnsupportedEncodingException, IOException {

        IO.secureWrite(saveTo, JSonStorage.serializeToJson(new MenuStructure(root, getUnused(root))).getBytes("UTF-8"));
    }

    public MenuStructure readFrom(File file) throws IOException {
        return JSonStorage.restoreFromString(IO.readFileToString(file), new TypeRef<MenuStructure>() {
        });
    }

    public synchronized void registerExtender(MenuExtenderHandler handler) {
        synchronized (extender) {
            extender.remove(handler);
            extender.add(handler);

            menuData = null;

        }
        updateDelayer.resetAndStart();

    }

    public void refresh() {
        updateDelayer.resetAndStart();
    }

    public void unregisterExtender(MenuExtenderHandler handler) {
        synchronized (extender) {
            extender.remove(handler);

            menuData = null;

        }
        updateDelayer.resetAndStart();
    }

    public ArrayList<MenuExtenderHandler> listExtender() {
        synchronized (extender) {
            return new ArrayList<MenuExtenderHandler>(extender);
        }
    }

    public List<MenuItemData> listSpecialItems() {

        HashSet<MenuItemData> specials = new HashSet<MenuItemData>();

        for (MenuItemData mid : setupDefaultStructure().list()) {
            if (mid instanceof MenuContainerRoot) continue;
            if (StringUtils.isEmpty(mid.getName())) continue;
            if (MenuContainer.class.isAssignableFrom(mid.getClass().getSuperclass())) {
                specials.add(mid);
            } else if (mid instanceof MenuLink) {
                specials.add(mid);
            }

        }
        ArrayList<MenuItemData> ret = new ArrayList<MenuItemData>(specials);

        ret.add(0, new SeperatorData());
        return ret;
    }

    public abstract String getFileExtension();

    public abstract String getName();

    public boolean isAcceleratorsEnabled() {
        return false;
    }

}
