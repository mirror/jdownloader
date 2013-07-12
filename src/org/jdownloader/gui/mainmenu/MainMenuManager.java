package org.jdownloader.gui.mainmenu;

import javax.swing.JPopupMenu;

import jd.controlling.IOEQ;
import jd.gui.swing.jdgui.menu.JDMenuBar;
import jd.gui.swing.jdgui.menu.actions.AboutAction;
import jd.gui.swing.jdgui.menu.actions.ExitAction;
import jd.gui.swing.jdgui.menu.actions.KnowledgeAction;
import jd.gui.swing.jdgui.menu.actions.LatestChangesAction;
import jd.gui.swing.jdgui.menu.actions.RestartAction;
import jd.gui.swing.jdgui.menu.actions.SettingsAction;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.exceptions.WTFException;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuExtenderHandler;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.SeperatorData;
import org.jdownloader.gui.mainmenu.action.AddLinksMenuAction;
import org.jdownloader.gui.mainmenu.action.LogSendAction;
import org.jdownloader.gui.mainmenu.container.AboutMenuContainer;
import org.jdownloader.gui.mainmenu.container.CaptchaQuickSettingsContainer;
import org.jdownloader.gui.mainmenu.container.ExtensionsMenuContainer;
import org.jdownloader.gui.mainmenu.container.ExtensionsMenuWindowContainer;
import org.jdownloader.gui.mainmenu.container.FileMenuContainer;
import org.jdownloader.gui.mainmenu.container.OptionalContainer;
import org.jdownloader.gui.mainmenu.container.SettingsMenuContainer;
import org.jdownloader.gui.toolbar.action.CaptchaDialogsToogleAction;
import org.jdownloader.gui.toolbar.action.CaptchaExchangeToogleAction;
import org.jdownloader.gui.toolbar.action.JAntiCaptchaToogleAction;
import org.jdownloader.gui.toolbar.action.RemoteCaptchaToogleAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.context.RenameAction;
import org.jdownloader.gui.views.downloads.action.MenuManagerAction;
import org.jdownloader.gui.views.linkgrabber.actions.AddContainerAction;

public class MainMenuManager extends ContextMenuManager<FilePackage, DownloadLink> {

    private static final MainMenuManager INSTANCE = new MainMenuManager();

    /**
     * get the only existing instance of DownloadListContextMenuManager. This is a singleton
     * 
     * @return
     */
    public static MainMenuManager getInstance() {
        return MainMenuManager.INSTANCE;
    }

    private DelayedRunnable updateDelayer;

    @Override
    public void setMenuData(MenuContainerRoot root) {
        super.setMenuData(root);

        // no delayer here.
        JDMenuBar.getInstance().updateLayout();
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
        return ".jdmenu";
    }

    /**
     * Create a new instance of DownloadListContextMenuManager. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */

    private MainMenuManager() {
        super();
        updateDelayer = new DelayedRunnable(IOEQ.TIMINGQUEUE, 1000l, 2000) {

            @Override
            public void delayedrun() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        JDMenuBar.getInstance().updateLayout();
                    }
                };

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

        mr.add(createFileMenu());
        mr.add(createSettingsMenu());
        mr.add(createAddonsMenu());
        // mr.add(createAddonsMenu());
        mr.add(createAboutMenu());

        OptionalContainer opt;
        mr.add(opt = new OptionalContainer(false));

        opt.add(RenameAction.class);

        CaptchaQuickSettingsContainer ocr;
        opt.add(ocr = new CaptchaQuickSettingsContainer());
        ocr.add(CaptchaExchangeToogleAction.class);
        ocr.add(JAntiCaptchaToogleAction.class);
        ocr.add(RemoteCaptchaToogleAction.class);
        ocr.add(CaptchaDialogsToogleAction.class);
        // // add(new EditMenu());
        // add(new SettingsMenu());
        // add(AddonsMenu.getInstance());
        //

        return mr;
    }

    public AboutMenuContainer createAboutMenu() {
        AboutMenuContainer ret = new AboutMenuContainer();
        ret.add(LatestChangesAction.class);
        ret.add(KnowledgeAction.class);
        ret.add(LogSendAction.class);
        ret.add(new SeperatorData());
        ret.add(AboutAction.class);
        return ret;
    }

    public MenuItemData createAddonsMenu() {
        ExtensionsMenuContainer ret = new ExtensionsMenuContainer();
        ExtensionsMenuWindowContainer windows = new ExtensionsMenuWindowContainer();
        ret.add(windows);
        return ret;
    }

    public SettingsMenuContainer createSettingsMenu() {
        // add(new SettingsMenu());

        SettingsMenuContainer ret = new SettingsMenuContainer();

        ret.add(SettingsAction.class);
        ret.add(new SeperatorData());
        // add(new ChunksEditor());
        // add(new ParalellDownloadsEditor());
        // add(new ParallelDownloadsPerHostEditor());
        // add(new SpeedlimitEditor());

        ret.add(new ChunksEditorLink());

        ret.add(new ParalellDownloadsEditorLink());
        ret.add(new ParallelDownloadsPerHostEditorLink());
        //
        ret.add(new SpeedlimitEditorLink());

        return ret;
    }

    public FileMenuContainer createFileMenu() {
        FileMenuContainer ret = new FileMenuContainer();
        // add(new FileMenu());3
        ret.add(AddLinksMenuAction.class);
        ret.add(AddContainerAction.class);
        ret.add(new SeperatorData());
        ret.add(RestartAction.class);
        ret.add(new ActionData(ExitAction.class).putSetup(ExitAction.HIDE_ON_MAC, true));

        return ret;
    }

    public void show() {

        new MenuManagerAction().actionPerformed(null);
    }

    public boolean isAcceleratorsEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return _GUI._.MainMenuManager_getName();
    }

}
