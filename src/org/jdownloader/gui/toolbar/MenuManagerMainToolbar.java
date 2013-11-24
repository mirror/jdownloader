package org.jdownloader.gui.toolbar;

import javax.swing.JPopupMenu;

import jd.gui.swing.jdgui.components.toolbar.MainToolBar;
import jd.gui.swing.jdgui.components.toolbar.actions.AutoReconnectToggleAction;
import jd.gui.swing.jdgui.components.toolbar.actions.ClipBoardToggleAction;
import jd.gui.swing.jdgui.components.toolbar.actions.ExitToolbarAction;
import jd.gui.swing.jdgui.components.toolbar.actions.GlobalPremiumSwitchToggleAction;
import jd.gui.swing.jdgui.components.toolbar.actions.OpenDefaultDownloadFolderAction;
import jd.gui.swing.jdgui.components.toolbar.actions.PauseDownloadsAction;
import jd.gui.swing.jdgui.components.toolbar.actions.ReconnectAction;
import jd.gui.swing.jdgui.components.toolbar.actions.ShowSettingsAction;
import jd.gui.swing.jdgui.components.toolbar.actions.SilentModeToggleAction;
import jd.gui.swing.jdgui.components.toolbar.actions.StartDownloadsAction;
import jd.gui.swing.jdgui.components.toolbar.actions.StopDownloadsAction;
import jd.gui.swing.jdgui.components.toolbar.actions.StopDownloadsButFinishRunningOnesAction;
import jd.gui.swing.jdgui.components.toolbar.actions.UpdateAction;
import jd.gui.swing.jdgui.menu.actions.KnowledgeAction;
import jd.gui.swing.jdgui.menu.actions.LatestChangesAction;
import jd.gui.swing.jdgui.menu.actions.RestartAction;
import jd.gui.swing.jdgui.menu.actions.SettingsAction;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Application;
import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.SeperatorData;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.mainmenu.ChunksEditorLink;
import org.jdownloader.gui.mainmenu.ParalellDownloadsEditorLink;
import org.jdownloader.gui.mainmenu.ParallelDownloadsPerHostEditorLink;
import org.jdownloader.gui.mainmenu.SpeedlimitEditorLink;
import org.jdownloader.gui.mainmenu.action.AddLinksMenuAction;
import org.jdownloader.gui.mainmenu.action.LogSendAction;
import org.jdownloader.gui.mainmenu.container.CaptchaQuickSettingsContainer;
import org.jdownloader.gui.mainmenu.container.OptionalContainer;
import org.jdownloader.gui.mainmenu.container.SettingsMenuContainer;
import org.jdownloader.gui.toolbar.action.CaptchaDialogsToogleAction;
import org.jdownloader.gui.toolbar.action.CaptchaExchangeToogleAction;
import org.jdownloader.gui.toolbar.action.CollapseExpandAllAction;
import org.jdownloader.gui.toolbar.action.GenericDeleteFromTableToolbarAction;
import org.jdownloader.gui.toolbar.action.JAntiCaptchaToogleAction;
import org.jdownloader.gui.toolbar.action.MoveDownAction;
import org.jdownloader.gui.toolbar.action.MoveToBottomAction;
import org.jdownloader.gui.toolbar.action.MoveToTopAction;
import org.jdownloader.gui.toolbar.action.MoveUpAction;
import org.jdownloader.gui.toolbar.action.RemoteCaptchaToogleAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.context.RenameAction;
import org.jdownloader.gui.views.downloads.action.MenuManagerAction;
import org.jdownloader.gui.views.downloads.context.submenu.DeleteMenuContainer;
import org.jdownloader.gui.views.linkgrabber.actions.AddContainerAction;

public class MenuManagerMainToolbar extends ContextMenuManager<FilePackage, DownloadLink> {

    private static final MenuManagerMainToolbar INSTANCE = new MenuManagerMainToolbar();

    /**
     * get the only existing instance of DownloadListContextMenuManager. This is a singleton
     * 
     * @return
     */
    public static MenuManagerMainToolbar getInstance() {
        return MenuManagerMainToolbar.INSTANCE;
    }

    @Override
    protected String getStorageKey() {
        return "MainToolbar";
    }

    @Override
    public void setMenuData(MenuContainerRoot root) {
        super.setMenuData(root);
        // no delayer here.

    }

    @Override
    public String getFileExtension() {
        return ".jdToolbar";
    }

    /**
     * Create a new instance of DownloadListContextMenuManager. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */

    private MenuManagerMainToolbar() {
        super();

    }

    public JPopupMenu build(SelectionInfo<FilePackage, DownloadLink> si) {
        throw new WTFException("Not Supported");

    }

    public boolean isAcceleratorsEnabled() {
        return true;
    }

    public MenuContainerRoot createDefaultStructure() {
        MenuContainerRoot mr = new MenuContainerRoot();

        mr.add(StartDownloadsAction.class);

        mr.add(PauseDownloadsAction.class);
        mr.add(StopDownloadsAction.class);
        mr.add(optional(new MenuItemData(new ActionData(StopDownloadsButFinishRunningOnesAction.class))));
        mr.add(new SeperatorData());
        mr.add(new MenuItemData(new ActionData(MoveToTopAction.class)));
        mr.add(new MenuItemData(new ActionData(MoveUpAction.class)));
        mr.add(new MenuItemData(new ActionData(MoveDownAction.class)));
        mr.add(new MenuItemData(new ActionData(MoveToBottomAction.class)));
        mr.add(new SeperatorData());
        mr.add(ClipBoardToggleAction.class);
        mr.add(AutoReconnectToggleAction.class);
        mr.add(GlobalPremiumSwitchToggleAction.class);
        mr.add(SilentModeToggleAction.class);
        mr.add(new SeperatorData());

        mr.add(ReconnectAction.class);
        mr.add(UpdateAction.class);
        // if (!Application.isJared(MainToolbarManager.class)) {
        // MenuContainer opt;
        // mr.add(opt = new MenuContainer("Dialog Debug", "menu"));
        // opt.add(ShowInputDialogDebugAction.class);
        //
        // }
        OptionalContainer opt;
        mr.add(opt = new OptionalContainer(false));
        if (!Application.isJared(MainToolBar.class)) {
            opt.add(DoAnyThingForADeveloperAction.class);
        }
        opt.add(new MenuItemData(OpenDefaultDownloadFolderAction.class));
        opt.add(new MenuItemData(ShowSettingsAction.class));
        opt.add(new MenuItemData(ExitToolbarAction.class));
        opt.add(AddLinksMenuAction.class);
        opt.add(AddContainerAction.class);
        opt.add(SetProxySetupAction.class);
        opt.add(RestartAction.class);
        opt.add(SettingsAction.class);

        SettingsMenuContainer ret = new SettingsMenuContainer();
        ret.setName(_GUI._.quicksettings_SettingsMenuContainer());
        opt.add(ret);

        ret.add(new ChunksEditorLink());

        ret.add(new ParalellDownloadsEditorLink());
        ret.add(new ParallelDownloadsPerHostEditorLink());
        //
        ret.add(new SpeedlimitEditorLink());
        opt.add(LatestChangesAction.class);
        opt.add(KnowledgeAction.class);
        opt.add(LogSendAction.class);
        opt.add(RenameAction.class);

        CaptchaQuickSettingsContainer ocr;
        opt.add(ocr = new CaptchaQuickSettingsContainer());
        ocr.add(CaptchaExchangeToogleAction.class);
        ocr.add(JAntiCaptchaToogleAction.class);
        ocr.add(RemoteCaptchaToogleAction.class);

        mr.add(createDeleteMenu());

        ocr.add(CaptchaDialogsToogleAction.class);
        opt.add(CollapseExpandAllAction.class);
        return mr;
    }

    private MenuItemData optional(MenuItemData menuItemData) {
        menuItemData.setVisible(false);
        return menuItemData;
    }

    private MenuItemData createDeleteMenu() {
        DeleteMenuContainer delete = new DeleteMenuContainer();
        delete.setVisible(false);
        delete.add(setIconKey(new ActionData(GenericDeleteFromTableToolbarAction.class).putSetup(GenericDeleteFromTableToolbarAction.DELETE_ALL, true), IconKey.ICON_RESET));
        delete.add(setIconKey(new ActionData(GenericDeleteFromTableToolbarAction.class).putSetup(GenericDeleteFromTableToolbarAction.DELETE_DISABLED, true), IconKey.ICON_REMOVE_DISABLED));
        delete.add(setIconKey(new ActionData(GenericDeleteFromTableToolbarAction.class).putSetup(GenericDeleteFromTableToolbarAction.DELETE_FAILED, true), IconKey.ICON_REMOVE_FAILED));
        delete.add(setIconKey(new ActionData(GenericDeleteFromTableToolbarAction.class).putSetup(GenericDeleteFromTableToolbarAction.DELETE_FINISHED, true), IconKey.ICON_REMOVE_OK));
        delete.add(setIconKey(new ActionData(GenericDeleteFromTableToolbarAction.class).putSetup(GenericDeleteFromTableToolbarAction.DELETE_OFFLINE, true), IconKey.ICON_REMOVE_OFFLINE));

        // delete.add(new MenuItemData(new ActionData(DeleteSelectedAndFailedLinksAction.class)));
        // delete.add(new MenuItemData(new ActionData(DeleteSelectedFinishedLinksAction.class)));
        // delete.add(new MenuItemData(new ActionData(DeleteSelectedOfflineLinksAction.class)));
        return delete;
    }

    protected static ActionData setIconKey(ActionData putSetup, String KEY) {
        putSetup.setIconKey(KEY);
        return putSetup;
    }

    public void show() {

        new MenuManagerAction().actionPerformed(null);
    }

    @Override
    public String getName() {
        return _GUI._.MainToolbarManager_getName();
    }

    @Override
    protected void updateGui() {
        MainToolBar.getInstance().updateToolbar();
    }

}
