package org.jdownloader.api.myjdownloader.remotemenu;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.remoteapi.exceptions.FileNotFound404Exception;
import org.appwork.storage.JSonStorage;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.jdownloader.api.content.v2.MyJDMenuItem;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.extensions.ExtensionNotLoadedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.context.submenu.SettingsMenuContainer;
import org.jdownloader.myjdownloader.client.bindings.MenuStructure.Type;
import org.jdownloader.myjdownloader.client.bindings.interfaces.UIInterface.Context;

public class MenuManagerMYJDDownloadTableContext extends ContextMenuManager<FilePackage, DownloadLink> {

    private static final MenuManagerMYJDDownloadTableContext INSTANCE = new MenuManagerMYJDDownloadTableContext();

    /**
     * get the only existing instance of DownloadListContextMenuManager. This is a singleton
     *
     * @return
     */
    public static MenuManagerMYJDDownloadTableContext getInstance() {
        return MenuManagerMYJDDownloadTableContext.INSTANCE;
    }

    private MyJDMenuItem                                 apiStructure;
    private HashMap<String, AbstractMyJDSelectionAction> idMap;

    /**
     * Create a new instance of DownloadListContextMenuManager. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */

    private MenuManagerMYJDDownloadTableContext() {
        super();

    }

    public boolean isAcceleratorsEnabled() {
        return true;
    }

    public MenuContainerRoot createDefaultStructure() {
        MenuContainerRoot mr = new MenuContainerRoot();

        SettingsMenuContainer settings;
        settings = new SettingsMenuContainer();
        mr.add(settings);

        settings.add(CheckOnlineStatusAction.class);
        settings.add(RenameActionLink.class);

        mr.add(EnableActionLink.class);
        mr.add(ForceDownloadsAction.class);

        return mr;
    }

    @Override
    public String getFileExtension() {
        return ".myjdDL";
    }

    @Override
    public String getName() {
        return _GUI.T.MenuManagerMYJDDownloadTableContext_getName();
    }

    @Override
    protected void updateGui() {

    }

    @Override
    protected String getStorageKey() {
        return "MYJDDownloadTableContext";
    }

    @Override
    protected void onSetupMenuData(MenuContainerRoot menuData) {

        final MyJDMenuItem root = new MyJDMenuItem();
        MenuContainerRoot internalRoot = getMenuData();
        final HashMap<String, AbstractMyJDSelectionAction> map = new HashMap<String, AbstractMyJDSelectionAction>();
        new MenuBuilder() {
            protected MyJDMenuItem addContainer(MyJDMenuItem root, MenuItemData inst, int index, int size) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, ExtensionNotLoadedException {
                MyJDMenuItem submenu = new MyJDMenuItem(Type.CONTAINER, null, inst.getName(), inst.getIconKey());
                root.add(submenu);
                return submenu;
            };

            protected void addAction(MyJDMenuItem root, MenuItemData inst, int index, int size) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, ExtensionNotLoadedException {
                CustomizableAppAction action = inst.createAction();
                if (action != null && action instanceof AbstractMyJDSelectionAction) {
                    if (action != null) {
                        if (action instanceof AbstractMyJDSelectionActionLink) {
                            root.add(new MyJDMenuItem(Type.LINK, ((AbstractMyJDSelectionAction) action).getID(), inst.getName(), inst.getIconKey()));
                        } else if (action instanceof AbstractMyJDSelectionAction) {
                            root.add(new MyJDMenuItem(Type.ACTION, ((AbstractMyJDSelectionAction) action).getID(), inst.getName(), inst.getIconKey()));
                        }
                    }
                    if (!(action instanceof AbstractMyJDSelectionActionLink)) {
                        AbstractMyJDSelectionAction dupe;
                        if ((dupe = map.put(((AbstractMyJDSelectionAction) action).getID(), (AbstractMyJDSelectionAction) action)) != null) {
                            LoggerFactory.getDefaultLogger().log(new Exception("Dupe Remote Action: Same id " + dupe.getID() + " for " + dupe.getClass().getSimpleName() + "." + dupe.getName() + " and " + action.getClass().getSimpleName() + "." + action.getName()));
                        }
                    }
                }

            };
        }.createLayer(root, internalRoot);
        this.idMap = map;
        this.apiStructure = root;
    }

    public MyJDMenuItem getMenuStructure() {
        // this will validate and call onSetupMenuData
        final MyJDMenuItem root = new MyJDMenuItem();
        MenuContainerRoot internalRoot = getMenuData();
        new MenuBuilder() {
            protected MyJDMenuItem addContainer(MyJDMenuItem root, MenuItemData inst, int index, int size) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, ExtensionNotLoadedException {
                MyJDMenuItem submenu = new MyJDMenuItem(Type.CONTAINER, null, inst.getName(), inst.getIconKey());
                root.add(submenu);
                return submenu;
            };

            protected void addAction(MyJDMenuItem root, MenuItemData inst, int index, int size) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, ExtensionNotLoadedException {
                CustomizableAppAction action = inst.createAction();
                if (action != null && action instanceof AbstractMyJDSelectionAction) {
                    if (action != null) {
                        if (action instanceof AbstractMyJDSelectionActionLink) {
                            root.add(new MyJDMenuItem(Type.LINK, ((AbstractMyJDSelectionAction) action).getID(), inst.getName(), inst.getIconKey()));
                        } else if (action instanceof AbstractMyJDSelectionAction) {
                            root.add(new MyJDMenuItem(Type.ACTION, ((AbstractMyJDSelectionAction) action).getID(), inst.getName(), inst.getIconKey()));
                        }
                    }

                }

            };
        }.createLayer(root, internalRoot);
        System.out.println(JSonStorage.serializeToJson(apiStructure));
        return apiStructure;
    }

    public Object invoke(String id, SelectionInfo<?, ?> selection, Context context) throws FileNotFound404Exception {
        AbstractMyJDSelectionAction action = idMap.get(id);
        if (action == null) {
            throw new FileNotFound404Exception();
        }
        action.setSelection(selection);
        return action.performAction(this, context);
    }

}
