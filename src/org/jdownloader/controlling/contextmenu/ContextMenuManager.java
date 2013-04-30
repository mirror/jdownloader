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

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.contextmenumanager.ActionData;
import org.jdownloader.gui.views.downloads.contextmenumanager.ContextMenuConfigInterface;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuBuilder;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuContainer;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuContainerRoot;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuExtenderHandler;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuItemData;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuLink;
import org.jdownloader.gui.views.downloads.contextmenumanager.SeperatorData;
import org.jdownloader.logging.LogController;

public abstract class ContextMenuManager<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> {
    public ContextMenuManager() {
        config = JsonConfig.create(Application.getResource("cfg/menus/" + getClass().getName()), ContextMenuConfigInterface.class);
        logger = LogController.getInstance().getLogger(getClass().getName());
    }

    public JPopupMenu build(SelectionInfo<PackageType, ChildrenType> si) {
        long t = System.currentTimeMillis();
        JPopupMenu root = new JPopupMenu();
        MenuContainerRoot md = getMenuData();
        new MenuBuilder<PackageType, ChildrenType>(this, root, si, md).run();
        // createLayer(root, md);

        return root;
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

        MenuContainerRoot ret = config.getMenu();
        MenuContainerRoot defaultMenu = setupDefaultStructure();
        if (ret == null) {
            // no customizer ever used
            ret = defaultMenu;

        } else {
            System.out.println(JSonStorage.toString(ret));
            ret.validate();

            List<MenuItemData> allItemsInMenu = ret.list();
            List<MenuItemData> allItemsInDefaultMenu = defaultMenu.list();
            HashMap<String, MenuItemData> itemsIdsInMenu = new HashMap<String, MenuItemData>();
            HashMap<String, MenuItemData> itemsInDefaultMenu = new HashMap<String, MenuItemData>();

            for (MenuItemData d : allItemsInMenu) {

                itemsIdsInMenu.put(d._getIdentifier(), d);

            }
            for (MenuItemData d : allItemsInDefaultMenu) {

                itemsInDefaultMenu.put(d._getIdentifier(), d);

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
                            ret.add(path);
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
                exHandler.updateMenuModel(this, ret);
            }

        }
        return ret;
    }

    public void setMenuData(MenuContainerRoot root) {

        if (JSonStorage.toString(root).equals(JSonStorage.toString(setupDefaultStructure()))) {
            root = null;
        }
        if (root == null) {

            config.setMenu(null);
            config.setUnusedItems(null);
            menuData = setupDefaultStructure();

        } else {
            menuData = root;

            config.setMenu(root);
            config.setUnusedItems(getUnused(root));
        }

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

        IO.secureWrite(saveTo, JSonStorage.toString(new MenuStructure(root, getUnused(root))).getBytes("UTF-8"));
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

}
