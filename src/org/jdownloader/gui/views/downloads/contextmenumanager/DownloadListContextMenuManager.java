package org.jdownloader.gui.views.downloads.contextmenumanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JPopupMenu;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.context.CheckStatusAction;
import org.jdownloader.gui.views.components.packagetable.context.EnabledAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityDefaultAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityHighAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityHigherAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityHighestAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityLowerAction;
import org.jdownloader.gui.views.components.packagetable.context.SetCommentAction;
import org.jdownloader.gui.views.components.packagetable.context.SetDownloadPassword;
import org.jdownloader.gui.views.components.packagetable.context.URLEditorAction;
import org.jdownloader.gui.views.downloads.context.CreateDLCAction;
import org.jdownloader.gui.views.downloads.context.DeleteDisabledSelectedLinks;
import org.jdownloader.gui.views.downloads.context.DeleteQuickAction;
import org.jdownloader.gui.views.downloads.context.DeleteSelectedAndFailedLinksAction;
import org.jdownloader.gui.views.downloads.context.DeleteSelectedFinishedLinksAction;
import org.jdownloader.gui.views.downloads.context.DeleteSelectedOfflineLinksAction;
import org.jdownloader.gui.views.downloads.context.ForceDownloadAction;
import org.jdownloader.gui.views.downloads.context.MenuManagerAction;
import org.jdownloader.gui.views.downloads.context.NewPackageAction;
import org.jdownloader.gui.views.downloads.context.OpenDirectoryAction;
import org.jdownloader.gui.views.downloads.context.OpenFileAction;
import org.jdownloader.gui.views.downloads.context.OpenInBrowserAction;
import org.jdownloader.gui.views.downloads.context.PackageNameAction;
import org.jdownloader.gui.views.downloads.context.ResetAction;
import org.jdownloader.gui.views.downloads.context.ResumeAction;
import org.jdownloader.gui.views.downloads.context.SetDownloadFolderInDownloadTableAction;
import org.jdownloader.gui.views.downloads.context.StopsignAction;
import org.jdownloader.gui.views.linkgrabber.contextmenu.SortAction;
import org.jdownloader.logging.LogController;

public class DownloadListContextMenuManager {

    private ContextMenuConfigInterface                  config;
    private LogSource                                   logger;
    private static final DownloadListContextMenuManager INSTANCE = new DownloadListContextMenuManager();

    /**
     * get the only existing instance of DownloadListContextMenuManager. This is a singleton
     * 
     * @return
     */
    public static DownloadListContextMenuManager getInstance() {
        return DownloadListContextMenuManager.INSTANCE;
    }

    /**
     * Create a new instance of DownloadListContextMenuManager. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */

    private DownloadListContextMenuManager() {

        config = JsonConfig.create(Application.getResource("cfg/menus/DownloadListContextMenu"), ContextMenuConfigInterface.class);
        logger = LogController.getInstance().getLogger(getClass().getName());
        init();

    }

    private static final int VERSION = 0;

    public List<MenuItemData> listSpecialItems() {
        ArrayList<MenuItemData> ret = new ArrayList<MenuItemData>();

        ret.add(createSettingsMenu());
        ret.add(createMoreMenu());
        ret.add(createDeleteMenu());
        ret.add(createPriorityMenu());
        // ret.add(new AddonSubMenuLink());
        return ret;
    }

    public MenuContainerRoot setupDefaultStructure() {
        MenuContainerRoot mr = new MenuContainerRoot();
        mr.setSource(VERSION);
        // mr.add()
        mr.add(createSettingsMenu());

        mr.add(new SeparatorData());
        mr.add(new MenuItemData(get(OpenFileAction.class), MenuItemProperty.HIDE_IF_OUTPUT_NOT_EXISTING, MenuItemProperty.HIDE_IF_OPENFILE_IS_UNSUPPORTED));
        mr.add(new MenuItemData(get(OpenDirectoryAction.class), MenuItemProperty.HIDE_IF_OPENFILE_IS_UNSUPPORTED));
        mr.add(new MenuItemData(get(SortAction.class)));
        mr.add(new MenuItemData(get(EnabledAction.class)));
        mr.add(new SeparatorData());

        mr.add(new MenuItemData(get(ForceDownloadAction.class)));
        mr.add(new MenuItemData(get(StopsignAction.class)));
        mr.add(new SeparatorData());

        mr.add(new AddonSubMenuLink());

        // /* addons */

        mr.add(createMoreMenu());

        mr.add(new SeparatorData());
        mr.add(new MenuItemData(get(DeleteQuickAction.class)));
        mr.add(createDeleteMenu());

        mr.add(new SeparatorData());

        mr.add(new MenuItemData(get(MenuManagerAction.class)));
        return mr;
    }

    private MenuItemData createDeleteMenu() {
        DeleteMenuContainer delete = new DeleteMenuContainer();

        delete.add(new MenuItemData(get(DeleteDisabledSelectedLinks.class)));
        delete.add(new MenuItemData(get(DeleteSelectedAndFailedLinksAction.class)));
        delete.add(new MenuItemData(get(DeleteSelectedFinishedLinksAction.class)));
        delete.add(new MenuItemData(get(DeleteSelectedOfflineLinksAction.class)));
        return delete;
    }

    private MenuItemData createMoreMenu() {
        MoreMenuContainer more = new MoreMenuContainer();
        more.add(new MenuItemData(get(ResumeAction.class)));
        more.add(new MenuItemData(get(ResetAction.class)));
        more.add(new SeparatorData());
        more.add(new MenuItemData(get(NewPackageAction.class)));
        more.add(new MenuItemData(get(CreateDLCAction.class)));
        return more;
    }

    private MenuItemData createSettingsMenu() {
        SettingsMenuContainer settings;
        settings = new SettingsMenuContainer();

        settings.add(new MenuItemData(get(CheckStatusAction.class)));
        settings.add(new MenuItemData(get(OpenInBrowserAction.class), MenuItemProperty.HIDE_IF_DISABLED));
        settings.add(new MenuItemData(get(URLEditorAction.class)));
        settings.add(new SeparatorData());
        settings.add(new MenuItemData(get(PackageNameAction.class)));
        settings.add(new MenuItemData(get(SetDownloadFolderInDownloadTableAction.class)));
        settings.add(new MenuItemData(get(SetDownloadPassword.class)));

        settings.add(createPriorityMenu());
        return settings;

    }

    private MenuItemData createPriorityMenu() {
        PriorityMenuContainer priority;
        priority = new PriorityMenuContainer();
        priority.add(new MenuItemData(get(PriorityLowerAction.class)));
        priority.add(new MenuItemData(get(PriorityDefaultAction.class)));
        priority.add(new MenuItemData(get(PriorityHighAction.class)));
        priority.add(new MenuItemData(get(PriorityHigherAction.class)));
        priority.add(new MenuItemData(get(PriorityHighestAction.class)));
        return priority;
    }

    private ActionData get(Class<?> class1) {
        return map.get(class1);
    }

    private void init() {

        add(new ActionData(0, CheckStatusAction.class));
        add(new ActionData(0, OpenInBrowserAction.class));
        add(new ActionData(0, URLEditorAction.class));
        add(new ActionData(0, PackageNameAction.class, MenuItemProperty.PACKAGE_CONTEXT));
        add(new ActionData(0, SetDownloadFolderInDownloadTableAction.class));
        add(new ActionData(0, SetDownloadPassword.class));
        add(new ActionData(0, SetCommentAction.class));

        // Priority
        add(new ActionData(0, PriorityLowerAction.class));
        add(new ActionData(0, PriorityDefaultAction.class));
        add(new ActionData(0, PriorityHighAction.class));
        add(new ActionData(0, PriorityHigherAction.class));
        add(new ActionData(0, PriorityHighestAction.class));

        add(new ActionData(0, OpenFileAction.class, MenuItemProperty.LINK_CONTEXT));
        add(new ActionData(0, OpenDirectoryAction.class));

        add(new ActionData(0, SortAction.class));
        add(new ActionData(0, EnabledAction.class));
        add(new ActionData(0, ForceDownloadAction.class));
        add(new ActionData(0, StopsignAction.class));
        add(new ActionData(0, ResumeAction.class));
        add(new ActionData(0, ResetAction.class));
        add(new ActionData(0, NewPackageAction.class));
        add(new ActionData(0, CreateDLCAction.class));
        add(new ActionData(0, DeleteQuickAction.class));
        add(new ActionData(0, DeleteDisabledSelectedLinks.class));
        add(new ActionData(0, DeleteSelectedAndFailedLinksAction.class));
        add(new ActionData(0, DeleteSelectedFinishedLinksAction.class));
        add(new ActionData(0, DeleteSelectedOfflineLinksAction.class));
        add(new ActionData(1, MenuManagerAction.class));
    }

    private HashMap<Class<?>, ActionData> map = new HashMap<Class<?>, ActionData>();
    private MenuContainerRoot             menuData;

    private void add(ActionData actionData) {

        try {
            map.put(actionData._getClazz(), actionData);
        } catch (ActionClassNotAvailableException e) {
            logger.log(e);
        }
    }

    public void show() {
    }

    public JPopupMenu build(SelectionInfo<FilePackage, DownloadLink> si) {
        long t = System.currentTimeMillis();
        JPopupMenu root = new JPopupMenu();
        MenuContainerRoot md = getMenuData();
        new MenuBuilder(root, si, md).run();
        // createLayer(root, md);

        return root;
    }

    public synchronized MenuContainerRoot getMenuData() {
        long t = System.currentTimeMillis();
        if (menuData != null) return menuData;
        MenuContainerRoot ret = config.getMenuStructure();

        if (ret == null) {
            // no customizer ever used
            ret = setupDefaultStructure();

        } else {
            List<MenuItemData> allItemsInMenu = ret.list();

            HashSet<Class<?>> actionClassesInMenu = new HashSet<Class<?>>();
            NewActionsContainer neworUpdate = new NewActionsContainer();
            for (MenuItemData d : allItemsInMenu) {
                if (d.getActionData() != null) {
                    if (d.getActionData().getClazzName() != null) {
                        try {
                            actionClassesInMenu.add(d.getActionData()._getClazz());
                        } catch (Exception e1) {
                            logger.log(e1);
                        }
                    }
                }
                if (StringUtils.equals(d.getClassName(), NewActionsContainer.class.getName())) {
                    neworUpdate = (NewActionsContainer) d.lazyReal();
                }
            }
            ArrayList<ActionData> unused = config.getUnusedActions();
            if (unused == null) {
                unused = new ArrayList<ActionData>();
            }
            ArrayList<ActionData> updatedActions = new ArrayList<ActionData>();
            ArrayList<ActionData> newActions = new ArrayList<ActionData>();
            HashSet<Class<?>> actionClassesInUnusedLIst = new HashSet<Class<?>>();

            // find all updated in unused list
            for (ActionData unusedAction : unused) {
                try {
                    ActionData newAd = map.get(unusedAction._getClazz());
                    actionClassesInUnusedLIst.add(unusedAction._getClazz());
                    if (newAd == null) {
                        // action is not available any more
                    } else if (newAd.getVersion() > unusedAction.getVersion()) {
                        // action has been updated
                        if (newAd._getClazz() == MenuManagerAction.class) continue;
                        updatedActions.add(newAd);
                    }

                } catch (ActionClassNotAvailableException e) {
                    logger.log(e);
                }

            }

            // find new or updated actions

            for (Entry<Class<?>, ActionData> e : map.entrySet()) {
                if (e.getKey() == MenuManagerAction.class) continue;
                if (!actionClassesInUnusedLIst.contains(e.getKey())) {
                    // not in unused list
                    if (!actionClassesInMenu.contains(e.getKey())) {
                        // not in menu itself
                        // this is a new action
                        newActions.add(e.getValue());
                    }

                }
            }

            System.out.println(1);
            if (updatedActions.size() > 0 || newActions.size() > 0) {
                MenuContainerRoot defaultTree = setupDefaultStructure();
                List<List<MenuItemData>> pathes = defaultTree.listPathes();
                HashSet<Class<?>> actionClassesInDefaultTree = new HashSet<Class<?>>();
                // HashMap<MenuItemData,> actionClassesInDefaultTree = new HashSet<Class<?>>();

                System.out.println(pathes);
                for (List<MenuItemData> path : pathes) {

                    MenuItemData d = path.get(path.size() - 1);
                    if (d.getActionData() != null) {
                        if (d.getActionData().getClazzName() != null) {
                            try {
                                actionClassesInDefaultTree.add(d.getActionData()._getClazz());
                            } catch (Exception e1) {
                                logger.log(e1);
                            }
                        }
                    }
                }

                for (ActionData ad : updatedActions) {
                    try {
                        for (List<MenuItemData> path : pathes) {
                            MenuItemData d = path.get(path.size() - 1);
                            if (d.getActionData() != null && d.getActionData()._getClazz() == ad._getClazz()) {

                                neworUpdate.add(path);

                            }

                        }

                    } catch (ActionClassNotAvailableException e1) {
                        logger.log(e1);
                    }
                }

                for (ActionData ad : newActions) {
                    try {
                        for (List<MenuItemData> path : pathes) {
                            MenuItemData d = path.get(path.size() - 1);
                            if (d.getActionData() != null && d.getActionData()._getClazz() == ad._getClazz()) {

                                neworUpdate.add(path);

                            }

                        }

                    } catch (ActionClassNotAvailableException e1) {
                        logger.log(e1);
                    }
                }
                neworUpdate.add(new SeparatorData());
                neworUpdate.add(new MenuItemData(get(MenuManagerAction.class)));
                ret.getItems().add(0, neworUpdate);

            }

        }
        menuData = ret;
        System.out.println(System.currentTimeMillis() - t);
        return ret;
    }

    public List<ActionData> list() {
        ArrayList<ActionData> ret = new ArrayList<ActionData>();
        for (Entry<Class<?>, ActionData> set : map.entrySet()) {
            ret.add(set.getValue());
        }
        return ret;
    }

    public void setMenuData(MenuContainerRoot root) {

        if (JSonStorage.toString(root).equals(JSonStorage.toString(setupDefaultStructure()))) {
            root = null;
        }
        if (root == null) {

            config.setMenuStructure(null);
            config.setUnusedActions(null);
            menuData = setupDefaultStructure();

        } else {
            menuData = root;

            ArrayList<ActionData> list = new ArrayList<ActionData>();

            List<MenuItemData> allItemsInMenu = root.list();

            HashSet<Class<?>> actionClassesInMenu = new HashSet<Class<?>>();

            for (MenuItemData d : allItemsInMenu) {
                if (d.getActionData() != null) {
                    if (d.getActionData().getClazzName() != null) {
                        try {
                            actionClassesInMenu.add(d.getActionData()._getClazz());
                        } catch (Exception e1) {
                            logger.log(e1);
                        }
                    }
                }
            }
            for (Entry<Class<?>, ActionData> e : map.entrySet()) {
                if (!actionClassesInMenu.contains(e.getKey())) {
                    list.add(e.getValue());
                }
            }

            config.setMenuStructure(root);
            config.setUnusedActions(list);
        }

    }

}
