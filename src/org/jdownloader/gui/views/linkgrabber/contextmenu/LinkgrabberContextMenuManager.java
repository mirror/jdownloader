package org.jdownloader.gui.views.linkgrabber.contextmenu;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.BooleanLinkedMenuItemData;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuItemProperty;
import org.jdownloader.controlling.contextmenu.SeperatorData;
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
import org.jdownloader.gui.views.downloads.action.MenuManagerAction;
import org.jdownloader.gui.views.downloads.context.submenu.PriorityMenuContainer;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberPanel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.actions.AddContainerAction;
import org.jdownloader.gui.views.linkgrabber.actions.AddLinksAction;
import org.jdownloader.gui.views.linkgrabber.actions.ConfirmAction;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;

public class LinkgrabberContextMenuManager extends ContextMenuManager<CrawledPackage, CrawledLink> {

    private static final LinkgrabberContextMenuManager INSTANCE = new LinkgrabberContextMenuManager();

    /**
     * get the only existing instance of DownloadListContextMenuManager. This is a singleton
     * 
     * @return
     */
    public static LinkgrabberContextMenuManager getInstance() {
        return LinkgrabberContextMenuManager.INSTANCE;
    }

    @Override
    public String getFileExtension() {
        return ".jdLGMenu";
    }

    private LinkGrabberPanel panel;
    private LinkGrabberTable table;

    /**
     * Create a new instance of DownloadListContextMenuManager. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */

    private LinkgrabberContextMenuManager() {
        super();

    }

    private static final int VERSION = 0;

    public MenuContainerRoot createDefaultStructure() {
        MenuContainerRoot mr = new MenuContainerRoot();
        mr.setSource(VERSION);
        mr.add(new MenuItemData(new ActionData(ConfirmAction.class), MenuItemProperty.HIDE_IF_DISABLED));
        mr.add(new SeperatorData());
        mr.add(new BooleanLinkedMenuItemData(CFG_LINKGRABBER.CONTEXT_MENU_ADD_LINKS_ACTION_ALWAYS_VISIBLE, true, AddLinksAction.class));
        mr.add(new BooleanLinkedMenuItemData(CFG_LINKGRABBER.CONTEXT_MENU_ADD_LINKS_ACTION_ALWAYS_VISIBLE, true, AddContainerAction.class));
        mr.add(new SeperatorData());
        mr.add(createSettingsSubmenu());

        mr.add(SortAction.class);
        mr.add(EnabledAction.class);

        mr.add(new SeperatorData());
        mr.add(new ActionData(OpenUrlAction.class, MenuItemProperty.LINK_CONTEXT));
        mr.add(new SeperatorData());

        // addons
        mr.add(new AddonLGSubMenuLink());

        mr.add(createOthersMenu());
        // others

        mr.add(new SeperatorData());
        /* remove menu */
        mr.add(RemoveSelectionLinkgrabberAction.class);
        mr.add(createCleanupMenu());
        mr.add(new SeperatorData());
        mr.add(new MenuItemData(new ActionData(LGMenuManagerAction.class)));

        return mr;
    }

    private LinkgrabberCleanUpSubMenu createCleanupMenu() {
        LinkgrabberCleanUpSubMenu cleanup = new LinkgrabberCleanUpSubMenu();

        cleanup.add(RemoveAllLinkgrabberAction.class);
        cleanup.add(RemoveNonSelectedAction.class);
        cleanup.add(RemoveOfflineAction.class);
        cleanup.add(RemoveIncompleteArchives.class);
        cleanup.add(new SeperatorData());
        cleanup.add(ResetPopupAction.class);

        return cleanup;
    }

    private LinkGrabberMoreSubMenu createOthersMenu() {
        LinkGrabberMoreSubMenu ret = new LinkGrabberMoreSubMenu();

        ret.add(CreateDLCAction.class);
        ret.add(MergeToPackageAction.class);
        ret.add(SplitPackagesByHost.class);

        return ret;
    }

    private SettingsLGSubmenu createSettingsSubmenu() {
        SettingsLGSubmenu ret = new SettingsLGSubmenu();

        ret.add(CheckStatusAction.class);

        ret.add(URLEditorAction.class);
        ret.add(SetDownloadFolderInLinkgrabberAction.class);
        ret.add(SetDownloadPassword.class);
        ret.add(SetCommentAction.class);
        ret.add(createPriorityMenu());

        return ret;

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

    public void setPanel(LinkGrabberPanel linkGrabberPanel) {
        this.panel = linkGrabberPanel;
        table = panel.getTable();
    }

    public LinkGrabberTable getTable() {
        return table;
    }

    public LinkGrabberPanel getPanel() {
        return panel;
    }

}
