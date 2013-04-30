package org.jdownloader.gui.views.downloads.contextmenumanager;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.gui.views.components.packagetable.context.CheckStatusAction;
import org.jdownloader.gui.views.components.packagetable.context.EnabledAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityDefaultAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityHighAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityHigherAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityHighestAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityLowerAction;
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

public class DownloadListContextMenuManager extends ContextMenuManager<FilePackage, DownloadLink> {

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
        super();

    }

    private static final int VERSION = 0;

    public MenuContainerRoot createDefaultStructure() {
        MenuContainerRoot mr = new MenuContainerRoot();
        mr.setSource(VERSION);
        // mr.add()
        mr.add(createSettingsMenu());

        mr.add(new SeperatorData());
        mr.add(new MenuItemData(new ActionData(OpenFileAction.class), MenuItemProperty.HIDE_IF_OUTPUT_NOT_EXISTING, MenuItemProperty.HIDE_IF_OPENFILE_IS_UNSUPPORTED));
        mr.add(new MenuItemData(new ActionData(OpenDirectoryAction.class), MenuItemProperty.HIDE_IF_OPENFILE_IS_UNSUPPORTED));
        mr.add(new MenuItemData(new ActionData(SortAction.class)));
        mr.add(new MenuItemData(new ActionData(EnabledAction.class)));
        mr.add(new SeperatorData());

        mr.add(new MenuItemData(new ActionData(ForceDownloadAction.class)));
        mr.add(new MenuItemData(new ActionData(StopsignAction.class)));
        mr.add(new SeperatorData());

        mr.add(new AddonSubMenuLink());

        // /* addons */

        mr.add(createMoreMenu());

        mr.add(new SeperatorData());
        mr.add(new MenuItemData(new ActionData(DeleteQuickAction.class)));
        mr.add(createDeleteMenu());

        mr.add(new SeperatorData());

        mr.add(new MenuItemData(new ActionData(MenuManagerAction.class)));

        return mr;
    }

    private MenuItemData createDeleteMenu() {
        DeleteMenuContainer delete = new DeleteMenuContainer();

        delete.add(new MenuItemData(new ActionData(DeleteDisabledSelectedLinks.class)));
        delete.add(new MenuItemData(new ActionData(DeleteSelectedAndFailedLinksAction.class)));
        delete.add(new MenuItemData(new ActionData(DeleteSelectedFinishedLinksAction.class)));
        delete.add(new MenuItemData(new ActionData(DeleteSelectedOfflineLinksAction.class)));
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
        settings.add(new MenuItemData(new ActionData(OpenInBrowserAction.class), MenuItemProperty.HIDE_IF_DISABLED));
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

    public void show() {

        new MenuManagerAction(null).actionPerformed(null);
    }

}
