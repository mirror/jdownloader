package org.jdownloader.gui.toolbar;

import javax.swing.JPopupMenu;

import jd.controlling.IOEQ;
import jd.gui.swing.jdgui.components.toolbar.MainToolBar;
import jd.gui.swing.jdgui.components.toolbar.actions.AutoReconnectToggleAction;
import jd.gui.swing.jdgui.components.toolbar.actions.ClipBoardToggleAction;
import jd.gui.swing.jdgui.components.toolbar.actions.ExitToolbarAction;
import jd.gui.swing.jdgui.components.toolbar.actions.GlobalPremiumSwitchToggleAction;
import jd.gui.swing.jdgui.components.toolbar.actions.OpenDefaultDownloadFolderAction;
import jd.gui.swing.jdgui.components.toolbar.actions.PauseDownloadsAction;
import jd.gui.swing.jdgui.components.toolbar.actions.ReconnectAction;
import jd.gui.swing.jdgui.components.toolbar.actions.ShowSettingsAction;
import jd.gui.swing.jdgui.components.toolbar.actions.StartDownloadsAction;
import jd.gui.swing.jdgui.components.toolbar.actions.StopDownloadsAction;
import jd.gui.swing.jdgui.components.toolbar.actions.UpdateAction;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.exceptions.WTFException;
import org.appwork.scheduler.DelayedRunnable;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuExtenderHandler;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuItemProperty;
import org.jdownloader.controlling.contextmenu.SeperatorData;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.action.MenuManagerAction;

public class MainToolbarManager extends ContextMenuManager<FilePackage, DownloadLink> {

    private static final MainToolbarManager INSTANCE = new MainToolbarManager();

    /**
     * get the only existing instance of DownloadListContextMenuManager. This is a singleton
     * 
     * @return
     */
    public static MainToolbarManager getInstance() {
        return MainToolbarManager.INSTANCE;
    }

    private DelayedRunnable updateDelayer;

    @Override
    public void setMenuData(MenuContainerRoot root) {
        super.setMenuData(root);

        updateDelayer.resetAndStart();
    }

    @Override
    public synchronized void registerExtender(MenuExtenderHandler handler) {
        super.registerExtender(handler);
        updateDelayer.resetAndStart();
    }

    @Override
    public void unregisterExtender(MenuExtenderHandler handler) {
        super.unregisterExtender(handler);
        updateDelayer.resetAndStart();
    }

    @Override
    public String getFileExtension() {
        return ".jdToolbar";
    }

    /**
     * Create a new instance of DownloadListContextMenuManager. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */

    private MainToolbarManager() {
        super();
        updateDelayer = new DelayedRunnable(IOEQ.TIMINGQUEUE, 1000l, 2000) {

            @Override
            public void delayedrun() {
                MainToolBar.getInstance().updateToolbar();
            }

        };

    }

    public JPopupMenu build(SelectionInfo<FilePackage, DownloadLink> si) {
        throw new WTFException("Not Supported");

    }

    private static final int VERSION = 0;

    public MenuContainerRoot createDefaultStructure() {
        MenuContainerRoot mr = new MenuContainerRoot();
        mr.setSource(VERSION);

        mr.add(StartDownloadsAction.class);

        mr.add(PauseDownloadsAction.class);
        mr.add(StopDownloadsAction.class);

        mr.add(ClipBoardToggleAction.class);
        mr.add(AutoReconnectToggleAction.class);
        mr.add(GlobalPremiumSwitchToggleAction.class);
        mr.add(new SeperatorData());

        mr.add(ReconnectAction.class);
        mr.add(UpdateAction.class);
        mr.add(new MenuItemData(OpenDefaultDownloadFolderAction.class, MenuItemProperty.ALWAYS_HIDDEN));

        mr.add(new MenuItemData(ShowSettingsAction.class, MenuItemProperty.ALWAYS_HIDDEN));
        mr.add(new MenuItemData(ExitToolbarAction.class, MenuItemProperty.ALWAYS_HIDDEN));
        return mr;
    }

    public void show() {

        new MenuManagerAction(null).actionPerformed(null);
    }

    public boolean supportsProperty(MenuItemProperty property) {
        switch (property) {

        case HIDE_IF_DISABLED:
        case HIDE_IF_OPENFILE_IS_UNSUPPORTED:
        case HIDE_IF_OUTPUT_NOT_EXISTING:
        case LINK_CONTEXT:
        case PACKAGE_CONTEXT:
            return false;

        default:
            return true;
        }

    }

    @Override
    public String getName() {
        return _GUI._.MainToolbarManager_getName();
    }

}
