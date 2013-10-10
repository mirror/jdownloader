package org.jdownloader.gui.views.downloads.bottombar;

import javax.swing.JPopupMenu;

import jd.gui.swing.jdgui.interfaces.View;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.SeperatorData;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.event.GUIEventSender;
import org.jdownloader.gui.event.GUIListener;
import org.jdownloader.gui.mainmenu.ChunksEditorLink;
import org.jdownloader.gui.mainmenu.ParalellDownloadsEditorLink;
import org.jdownloader.gui.mainmenu.ParallelDownloadsPerHostEditorLink;
import org.jdownloader.gui.mainmenu.SpeedlimitEditorLink;
import org.jdownloader.gui.toolbar.action.GenericDeleteSelectedToolbarAction;
import org.jdownloader.gui.toolbar.action.ToolbarDeleteAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.action.ClearDownloadListAction;
import org.jdownloader.gui.views.downloads.action.MenuManagerAction;
import org.jdownloader.gui.views.downloads.action.RemoveAllVisibleDownloadsAction;
import org.jdownloader.gui.views.downloads.action.RemoveNonSelectedAction;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.gui.views.linkgrabber.actions.AddContainerAction;
import org.jdownloader.gui.views.linkgrabber.actions.AddLinksAction;

public class BottomBarMenuManager extends ContextMenuManager<FilePackage, DownloadLink> implements GUIListener {

    private static final BottomBarMenuManager INSTANCE = new BottomBarMenuManager();

    /**
     * get the only existing instance of DownloadListContextMenuManager. This is a singleton
     * 
     * @return
     */
    public static BottomBarMenuManager getInstance() {
        return BottomBarMenuManager.INSTANCE;
    }

    private BottomBar bottomBar;

    @Override
    public void setMenuData(MenuContainerRoot root) {
        super.setMenuData(root);
        // no delayer here.

    }

    @Override
    public String getFileExtension() {
        return ".jdDLBottomBar";
    }

    private BottomBarMenuManager() {
        super();

        GUIEventSender.getInstance().addListener(this, true);

    }

    public JPopupMenu build(SelectionInfo<FilePackage, DownloadLink> si) {
        throw new WTFException("Not Supported");

    }

    public boolean isAcceleratorsEnabled() {
        return true;
    }

    private static final int VERSION = 0;

    public MenuContainerRoot createDefaultStructure() {
        MenuContainerRoot mr = new MenuContainerRoot();
        mr.setSource(VERSION);
        mr.add(AddLinksAction.class);
        AddLinksContainer addLinks = new AddLinksContainer();

        addLinks.add(setName(new ActionData(AddLinksAction.class), _GUI._.AddOptionsAction_actionPerformed_addlinks()));
        addLinks.add(AddContainerAction.class);
        mr.add(addLinks);
        //
        mr.add(ClearDownloadListAction.class);
        DeleteContainer delete = new DeleteContainer();

        delete.add(RemoveAllVisibleDownloadsAction.class);
        delete.add(ToolbarDeleteAction.class);
        delete.add(setIconKey(new ActionData(GenericDeleteSelectedToolbarAction.class).putSetup(GenericDeleteSelectedToolbarAction.DELETE_DISABLED, true), IconKey.ICON_REMOVE_DISABLED));
        delete.add(setIconKey(new ActionData(GenericDeleteSelectedToolbarAction.class).putSetup(GenericDeleteSelectedToolbarAction.DELETE_FAILED, true), IconKey.ICON_REMOVE_FAILED));
        delete.add(setIconKey(new ActionData(GenericDeleteSelectedToolbarAction.class).putSetup(GenericDeleteSelectedToolbarAction.DELETE_FINISHED, true), IconKey.ICON_REMOVE_OK));
        delete.add(setIconKey(new ActionData(GenericDeleteSelectedToolbarAction.class).putSetup(GenericDeleteSelectedToolbarAction.DELETE_OFFLINE, true), IconKey.ICON_REMOVE_OFFLINE));
        delete.add(RemoveNonSelectedAction.class);
        mr.add(delete);
        //
        mr.add(new SearchMenuItem());
        mr.add(new HorizontalBoxItem());
        mr.add(new QuickFilterMenuItem());

        mr.add(DownloadsOverviewPanelToggleAction.class);

        QuickSettingsMenuContainer quicksettings = new QuickSettingsMenuContainer();

        quicksettings.add(new ChunksEditorLink());

        quicksettings.add(new ParalellDownloadsEditorLink());
        quicksettings.add(new ParallelDownloadsPerHostEditorLink());
        //
        quicksettings.add(new SpeedlimitEditorLink());
        quicksettings.add(new SeperatorData());
        quicksettings.add(BottomBarMenuManagerAction.class);
        mr.add(quicksettings);
        return mr;
    }

    private ActionData setName(ActionData actionData, String name) {
        actionData.setName(name);
        return actionData;
    }

    private MenuItemData optional(MenuItemData menuItemData) {
        menuItemData.setVisible(false);
        return menuItemData;
    }

    private ActionData setIconKey(ActionData putSetup, String KEY) {
        putSetup.setIconKey(KEY);
        return putSetup;
    }

    public void show() {

        new MenuManagerAction().actionPerformed(null);
    }

    @Override
    public String getName() {
        return _GUI._.BottomBarMenuManager_getName();
    }

    @Override
    public void onGuiMainTabSwitch(View oldView, View newView) {

        // MainToolBar.getInstance().updateToolbar();
    }

    @Override
    protected void updateGui() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (bottomBar != null) {
                    bottomBar.updateGui();
                    ((DownloadsTable) DownloadsTableModel.getInstance().getTable()).updateContextShortcuts();
                }
            }
        };

    }

    public void setLink(BottomBar bottomBar) {
        this.bottomBar = bottomBar;
    }

}
