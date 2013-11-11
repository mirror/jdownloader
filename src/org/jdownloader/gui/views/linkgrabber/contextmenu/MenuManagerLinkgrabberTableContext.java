package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.SeperatorData;
import org.jdownloader.controlling.contextmenu.TableContext;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.context.CheckStatusAction;
import org.jdownloader.gui.views.components.packagetable.context.EnabledAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityDefaultAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityHighAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityHigherAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityHighestAction;
import org.jdownloader.gui.views.components.packagetable.context.PriorityLowerAction;
import org.jdownloader.gui.views.components.packagetable.context.RenameAction;
import org.jdownloader.gui.views.components.packagetable.context.SetCommentAction;
import org.jdownloader.gui.views.components.packagetable.context.SetDownloadPassword;
import org.jdownloader.gui.views.components.packagetable.context.URLEditorAction;
import org.jdownloader.gui.views.downloads.action.MenuManagerAction;
import org.jdownloader.gui.views.downloads.context.submenu.PriorityMenuContainer;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberPanel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.gui.views.linkgrabber.actions.ResetAction;

public class MenuManagerLinkgrabberTableContext extends ContextMenuManager<CrawledPackage, CrawledLink> {

    private static final MenuManagerLinkgrabberTableContext INSTANCE = new MenuManagerLinkgrabberTableContext();

    /**
     * get the only existing instance of DownloadListContextMenuManager. This is a singleton
     * 
     * @return
     */
    public static MenuManagerLinkgrabberTableContext getInstance() {
        return MenuManagerLinkgrabberTableContext.INSTANCE;
    }

    @Override
    public String getFileExtension() {
        return ".jdLGMenu";
    }

    @Override
    protected String getStorageKey() {
        return "LinkgrabberContext";
    }

    private LinkGrabberPanel panel;
    private LinkGrabberTable table;

    /**
     * Create a new instance of DownloadListContextMenuManager. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */

    private MenuManagerLinkgrabberTableContext() {
        super();

    }

    @Override
    public MenuContainerRoot getMenuData() {
        return super.getMenuData();
    }

    public MenuContainerRoot createDefaultStructure() {
        MenuContainerRoot mr = new MenuContainerRoot();

        mr.add(AddLinksContextMenuAction.class);
        mr.add(AddContainerContextMenuAction.class);

        mr.add(new SeperatorData());
        mr.add(new MenuItemData(new ActionData(ConfirmLinksContextAction.class)));
        mr.add(new MenuItemData(new ActionData(ConfirmLinksContextAction.class).putSetup(ConfirmLinksContextAction.SELECTION_ONLY, false).putSetup(TableContext.ITEM_VISIBLE_FOR_EMPTY_SELECTION, true)));

        mr.add(new SeperatorData());

        mr.add(new SeperatorData());
        mr.add(createSettingsSubmenu());

        mr.add(SortAction.class);
        mr.add(EnabledAction.class);

        mr.add(new SeperatorData());
        mr.add(new ActionData(OpenInBrowserAction.class));
        mr.add(new SeperatorData());

        mr.add(createOthersMenu());
        // others

        mr.add(new SeperatorData());
        /* remove menu */

        ActionData ad = new ActionData(GenericDeleteFromLinkgrabberContextAction.class);
        ad.putSetup(GenericDeleteFromLinkgrabberContextAction.DELETE_ALL, true);
        ad.putSetup(GenericDeleteFromLinkgrabberContextAction.ONLY_SELECTED_ITEMS, true);
        ad.putSetup(TableContext.ITEM_VISIBLE_FOR_EMPTY_SELECTION, false);
        mr.add(setShortcut(new MenuItemData(setIconKey(ad, IconKey.ICON_DELETE)), KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)));

        mr.add(createCleanupMenu());
        mr.add(new SeperatorData());
        mr.add(new ActionData(PropertiesAction.class));
        mr.add(new SeperatorData());
        mr.add(new MenuItemData(new ActionData(LGMenuManagerAction.class)));

        return mr;
    }

    private MenuItemData setShortcut(MenuItemData menuItemData, KeyStroke keyStroke) {
        menuItemData.setShortcut(keyStroke == null ? null : keyStroke.toString());
        return menuItemData;
    }

    private LinkgrabberCleanUpSubMenu createCleanupMenu() {
        LinkgrabberCleanUpSubMenu cleanup = new LinkgrabberCleanUpSubMenu();

        cleanup.add((new MenuItemData(setIconKey(new ActionData(GenericDeleteFromLinkgrabberContextAction.class).putSetup(GenericDeleteFromLinkgrabberContextAction.DELETE_ALL, true).putSetup(TableContext.ITEM_VISIBLE_FOR_EMPTY_SELECTION, true).putSetup(GenericDeleteFromLinkgrabberContextAction.ONLY_SELECTED_ITEMS, false), IconKey.ICON_RESET))));
        cleanup.add((new MenuItemData(setIconKey(new ActionData(GenericDeleteFromLinkgrabberContextAction.class).putSetup(GenericDeleteFromLinkgrabberContextAction.DELETE_DISABLED, true), IconKey.ICON_REMOVE_DISABLED))));

        cleanup.add((new MenuItemData(setIconKey(new ActionData(GenericDeleteFromLinkgrabberContextAction.class).putSetup(GenericDeleteFromLinkgrabberContextAction.DELETE_OFFLINE, true), IconKey.ICON_REMOVE_OFFLINE))));

        cleanup.add(RemoveNonSelectedContextAction.class);

        cleanup.add(RemoveIncompleteArchives.class);
        cleanup.add(new SeperatorData());
        cleanup.add(setName(new ActionData(ResetAction.class), _GUI._.ResetPopupAction_ResetPopupAction_()));

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

        ret.add(RenameAction.class);
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

        new MenuManagerAction().actionPerformed(null);
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

    public boolean isAcceleratorsEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return _GUI._.LinkgrabberContextMenuManager_getName();
    }

    @Override
    protected void updateGui() {
        ((LinkGrabberTable) LinkGrabberTableModel.getInstance().getTable()).updateContextShortcuts();

    }

}
