package org.jdownloader.gui.views.downloads.contextmenumanager;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.SeperatorData;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.mainmenu.container.OptionalContainer;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.context.CheckStatusAction;
import org.jdownloader.gui.views.components.packagetable.context.EnabledAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityDefaultAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityHighAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityHigherAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityHighestAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityLowerAction;
import org.jdownloader.gui.views.components.packagetable.context.RenameAction;
import org.jdownloader.gui.views.components.packagetable.context.SetDownloadPassword;
import org.jdownloader.gui.views.components.packagetable.context.URLEditorAction;
import org.jdownloader.gui.views.downloads.action.CollapseExpandContextAction;
import org.jdownloader.gui.views.downloads.action.CopyGenericContextAction;
import org.jdownloader.gui.views.downloads.action.CreateDLCAction;
import org.jdownloader.gui.views.downloads.action.ForceDownloadAction;
import org.jdownloader.gui.views.downloads.action.GenericDeleteFromDownloadlistAction;
import org.jdownloader.gui.views.downloads.action.GenericDeleteFromDownloadlistContextAction;
import org.jdownloader.gui.views.downloads.action.MenuManagerAction;
import org.jdownloader.gui.views.downloads.action.NewPackageAction;
import org.jdownloader.gui.views.downloads.action.OpenDirectoryAction;
import org.jdownloader.gui.views.downloads.action.OpenFileAction;
import org.jdownloader.gui.views.downloads.action.OpenInBrowserAction;
import org.jdownloader.gui.views.downloads.action.PackageNameAction;
import org.jdownloader.gui.views.downloads.action.PropertiesAction;
import org.jdownloader.gui.views.downloads.action.ResetAction;
import org.jdownloader.gui.views.downloads.action.ResumeAction;
import org.jdownloader.gui.views.downloads.action.SetDownloadFolderInDownloadTableAction;
import org.jdownloader.gui.views.downloads.action.SkipAction;
import org.jdownloader.gui.views.downloads.action.StopsignAction;
import org.jdownloader.gui.views.downloads.context.submenu.DeleteMenuContainer;
import org.jdownloader.gui.views.downloads.context.submenu.MoreMenuContainer;
import org.jdownloader.gui.views.downloads.context.submenu.PriorityMenuContainer;
import org.jdownloader.gui.views.downloads.context.submenu.SettingsMenuContainer;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.gui.views.linkgrabber.bottombar.IncludedSelectionSetup;
import org.jdownloader.gui.views.linkgrabber.contextmenu.AddContainerContextMenuAction;
import org.jdownloader.gui.views.linkgrabber.contextmenu.AddLinksContextMenuAction;
import org.jdownloader.gui.views.linkgrabber.contextmenu.SortAction;

public class MenuManagerDownloadTableContext extends ContextMenuManager<FilePackage, DownloadLink> {

    private static final MenuManagerDownloadTableContext INSTANCE = new MenuManagerDownloadTableContext();

    /**
     * get the only existing instance of DownloadListContextMenuManager. This is a singleton
     * 
     * @return
     */
    public static MenuManagerDownloadTableContext getInstance() {
        return MenuManagerDownloadTableContext.INSTANCE;
    }

    /**
     * Create a new instance of DownloadListContextMenuManager. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */

    private MenuManagerDownloadTableContext() {
        super();

    }

    public boolean isAcceleratorsEnabled() {
        return true;
    }

    public MenuContainerRoot createDefaultStructure() {
        MenuContainerRoot mr = new MenuContainerRoot();
        mr.add(AddLinksContextMenuAction.class);
        mr.add(AddContainerContextMenuAction.class);

        mr.add(new SeperatorData());
        // mr.add()
        mr.add(createSettingsMenu());

        mr.add(new SeperatorData());
        mr.add(new DownloadsTablePluginLink());
        mr.add(new SeperatorData());
        mr.add(new MenuItemData(new ActionData(OpenFileAction.class)));
        mr.add(new MenuItemData(new ActionData(OpenDirectoryAction.class)));
        mr.add(new MenuItemData(new ActionData(SortAction.class)));
        mr.add(new MenuItemData(new ActionData(EnabledAction.class)));
        mr.add(new MenuItemData(new ActionData(SkipAction.class)));
        mr.add(new SeperatorData());

        mr.add(new MenuItemData(new ActionData(ForceDownloadAction.class)));
        mr.add(new MenuItemData(new ActionData(StopsignAction.class)));
        mr.add(new SeperatorData());

        mr.add(createMoreMenu());

        mr.add(new SeperatorData());
        mr.add(setAccelerator(new MenuItemData(setName(setIconKey(new ActionData(GenericDeleteFromDownloadlistContextAction.class).putSetup(GenericDeleteFromDownloadlistAction.DELETE_ALL, true), IconKey.ICON_DELETE), _GUI._.DeleteQuickAction_DeleteQuickAction_object_())), CrossSystem.getDeleteShortcut()));

        mr.add(createDeleteMenu());
        mr.add(new SeperatorData());
        mr.add(PropertiesAction.class);
        mr.add(new SeperatorData());

        mr.add(new MenuItemData(new ActionData(MenuManagerAction.class)));
        OptionalContainer opt;
        mr.add(opt = new OptionalContainer(false));
        opt.add(CollapseExpandContextAction.class);
        opt.add(CopyGenericContextAction.class);
        return mr;
    }

    private MenuItemData createDeleteMenu() {
        DeleteMenuContainer delete = new DeleteMenuContainer();

        delete.add(setIconKey(new ActionData(GenericDeleteFromDownloadlistContextAction.class).putSetup(GenericDeleteFromDownloadlistAction.DELETE_DISABLED, true), IconKey.ICON_REMOVE_DISABLED));
        delete.add(setIconKey(new ActionData(GenericDeleteFromDownloadlistContextAction.class).putSetup(GenericDeleteFromDownloadlistAction.DELETE_FAILED, true), IconKey.ICON_REMOVE_FAILED));
        delete.add(setIconKey(new ActionData(GenericDeleteFromDownloadlistContextAction.class).putSetup(GenericDeleteFromDownloadlistAction.DELETE_FINISHED, true), IconKey.ICON_REMOVE_OK));
        delete.add(setIconKey(new ActionData(GenericDeleteFromDownloadlistContextAction.class).putSetup(GenericDeleteFromDownloadlistAction.DELETE_OFFLINE, true), IconKey.ICON_REMOVE_OFFLINE));
        delete.add(setIconKey(new ActionData(GenericDeleteFromDownloadlistContextAction.class).putSetup(GenericDeleteFromDownloadlistContextAction.DELETE_ALL, true).putSetup(IncludedSelectionSetup.INCLUDE_UNSELECTED_LINKS, true).putSetup(IncludedSelectionSetup.INCLUDE_SELECTED_LINKS, false), IconKey.ICON_OK));

        return delete;
    }

    private MenuItemData createMoreMenu() {
        MoreMenuContainer more = new MoreMenuContainer();
        more.add(new MenuItemData(new ActionData(ResumeAction.class)));
        more.add(new MenuItemData(new ActionData(ResetAction.class)));
        more.add(new SeperatorData());
        more.add(new MenuItemData(new ActionData(NewPackageAction.class)));
        more.add(new MenuItemData(new ActionData(CreateDLCAction.class)));
        return more;
    }

    private MenuItemData createSettingsMenu() {
        SettingsMenuContainer settings;
        settings = new SettingsMenuContainer();

        settings.add(new MenuItemData(new ActionData(CheckStatusAction.class)));

        settings.add(RenameAction.class);
        settings.add(new MenuItemData(new ActionData(OpenInBrowserAction.class)));
        settings.add(new MenuItemData(new ActionData(URLEditorAction.class)));
        settings.add(new SeperatorData());
        settings.add(new MenuItemData(new ActionData(PackageNameAction.class)));
        settings.add(new MenuItemData(new ActionData(SetDownloadFolderInDownloadTableAction.class)));
        settings.add(new MenuItemData(new ActionData(SetDownloadPassword.class)));

        settings.add(createPriorityMenu());
        return settings;

    }

    private MenuItemData createPriorityMenu() {
        PriorityMenuContainer priority;
        priority = new PriorityMenuContainer();
        priority.add(new MenuItemData(new ActionData(PriorityLowerAction.class)));
        priority.add(new MenuItemData(new ActionData(PriorityDefaultAction.class)));
        priority.add(new MenuItemData(new ActionData(PriorityHighAction.class)));
        priority.add(new MenuItemData(new ActionData(PriorityHigherAction.class)));
        priority.add(new MenuItemData(new ActionData(PriorityHighestAction.class)));
        return priority;
    }

    @Override
    public String getFileExtension() {
        return ".jdDLMenu";
    }

    @Override
    public String getName() {
        return _GUI._.DownloadListContextMenuManager_getName();
    }

    @Override
    protected void updateGui() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                ((DownloadsTable) DownloadsTableModel.getInstance().getTable()).updateContextShortcuts();
            }
        };

    }

    @Override
    protected String getStorageKey() {
        return "DownloadTableContext";
    }

}
