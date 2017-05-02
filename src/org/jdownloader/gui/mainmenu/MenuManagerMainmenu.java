package org.jdownloader.gui.mainmenu;

import javax.swing.JPopupMenu;

import jd.gui.swing.jdgui.menu.JDMenuBar;
import jd.gui.swing.jdgui.menu.actions.AboutAction;
import jd.gui.swing.jdgui.menu.actions.ExitAction;
import jd.gui.swing.jdgui.menu.actions.KnowledgeAction;
import jd.gui.swing.jdgui.menu.actions.LatestChangesAction;
import jd.gui.swing.jdgui.menu.actions.MyJDownloaderTabAction;
import jd.gui.swing.jdgui.menu.actions.RestartAction;
import jd.gui.swing.jdgui.menu.actions.SettingsAccountUsageRulesAction;
import jd.gui.swing.jdgui.menu.actions.SettingsAction;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.SeparatorData;
import org.jdownloader.gui.mainmenu.action.AddLinksMenuAction;
import org.jdownloader.gui.mainmenu.action.LogSendAction;
import org.jdownloader.gui.mainmenu.container.AboutMenuContainer;
import org.jdownloader.gui.mainmenu.container.BackupMenuContainer;
import org.jdownloader.gui.mainmenu.container.CaptchaQuickSettingsContainer;
import org.jdownloader.gui.mainmenu.container.ExtensionsMenuContainer;
import org.jdownloader.gui.mainmenu.container.ExtensionsMenuWindowContainer;
import org.jdownloader.gui.mainmenu.container.FileMenuContainer;
import org.jdownloader.gui.mainmenu.container.OptionalContainer;
import org.jdownloader.gui.mainmenu.container.SettingsMenuContainer;
import org.jdownloader.gui.toolbar.action.CaptchaModeChangeAction;
import org.jdownloader.gui.toolbar.action.CaptchaToogle9KWAction;
import org.jdownloader.gui.toolbar.action.CaptchaToogleAntiCaptchaAction;
import org.jdownloader.gui.toolbar.action.CaptchaToogleBrowserSolverAction;
import org.jdownloader.gui.toolbar.action.CaptchaToogleCheapCaptchaAction;
import org.jdownloader.gui.toolbar.action.CaptchaToogleDBCAction;
import org.jdownloader.gui.toolbar.action.CaptchaToogleDialogAction;
import org.jdownloader.gui.toolbar.action.CaptchaToogleEndCaptchaAction;
import org.jdownloader.gui.toolbar.action.CaptchaToogleImageTyperzAction;
import org.jdownloader.gui.toolbar.action.CaptchaToogleJACAction;
import org.jdownloader.gui.toolbar.action.CaptchaToogleMyJDAutoAction;
import org.jdownloader.gui.toolbar.action.CaptchaToogleMyJDRemoteAction;
import org.jdownloader.gui.toolbar.action.CaptchaToogleTwoCaptchaAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.context.RenameAction;
import org.jdownloader.gui.views.downloads.action.MenuManagerAction;
import org.jdownloader.gui.views.linkgrabber.actions.AddContainerAction;

public class MenuManagerMainmenu extends ContextMenuManager<FilePackage, DownloadLink> {

    private static final MenuManagerMainmenu INSTANCE = new MenuManagerMainmenu();

    /**
     * get the only existing instance of DownloadListContextMenuManager. This is a singleton
     *
     * @return
     */
    public static MenuManagerMainmenu getInstance() {
        return MenuManagerMainmenu.INSTANCE;
    }

    @Override
    protected String getStorageKey() {
        return "MainMenu";
    }

    @Override
    public String getFileExtension() {
        return ".jdmenu";
    }

    /**
     * Create a new instance of DownloadListContextMenuManager. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */

    private MenuManagerMainmenu() {
        super();

    }

    public JPopupMenu build(SelectionInfo<FilePackage, DownloadLink> si) {
        throw new WTFException("Not Supported");

    }

    public MenuContainerRoot createDefaultStructure() {
        MenuContainerRoot mr = new MenuContainerRoot();

        mr.add(createFileMenu());
        if (!CrossSystem.isMac()) {
            mr.add(createSettingsMenu());
        }
        mr.add(createAddonsMenu());
        // mr.add(createAddonsMenu());
        mr.add(createAboutMenu());

        OptionalContainer opt;
        mr.add(opt = new OptionalContainer(false));

        opt.add(RenameAction.class);

        CaptchaQuickSettingsContainer ocr;
        opt.add(ocr = new CaptchaQuickSettingsContainer());
        ocr.add(CaptchaModeChangeAction.class);
        ocr.add(CaptchaToogleAntiCaptchaAction.class);
        ocr.add(CaptchaToogleTwoCaptchaAction.class);
        ocr.add(CaptchaToogle9KWAction.class);
        ocr.add(CaptchaToogleDBCAction.class);
        ocr.add(CaptchaToogleCheapCaptchaAction.class);
        ocr.add(CaptchaToogleImageTyperzAction.class);
        ocr.add(CaptchaToogleEndCaptchaAction.class);
        ocr.add(CaptchaToogleDialogAction.class);
        ocr.add(CaptchaToogleBrowserSolverAction.class);
        ocr.add(CaptchaToogleJACAction.class);
        ocr.add(CaptchaToogleMyJDAutoAction.class);
        ocr.add(CaptchaToogleMyJDRemoteAction.class);
        // HorizontalBoxItem h = new HorizontalBoxItem();
        // h.setVisible(true);
        // mr.add(h);

        return mr;
    }

    public AboutMenuContainer createAboutMenu() {
        AboutMenuContainer ret = new AboutMenuContainer();
        ret.add(LatestChangesAction.class);
        ret.add(KnowledgeAction.class);
        ret.add(LogSendAction.class);
        ret.add(new SeparatorData());
        ret.add(CheckForUpdatesAction.class);
        ret.add(AboutAction.class);
        ret.add(DonateAction.class);
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
        ret.add(MyJDownloaderTabAction.class);
        ret.add(hide(new MenuItemData(SettingsAccountUsageRulesAction.class)));
        ret.add(new SeparatorData());
        // add(new ChunksEditor());
        // add(new ParalellDownloadsEditor());
        // add(new ParallelDownloadsPerHostEditor());
        // add(new SpeedlimitEditor());

        if (!CrossSystem.isMac()) {
            ret.add(new ChunksEditorLink());

            ret.add(new ParalellDownloadsEditorLink());
            ret.add(new ParallelDownloadsPerHostEditorLink());
            //
            ret.add(new SpeedlimitEditorLink());
        }
        return ret;
    }

    private MenuItemData hide(MenuItemData menuItemData) {
        menuItemData.setVisible(false);
        return menuItemData;
    }

    public FileMenuContainer createFileMenu() {
        FileMenuContainer ret = new FileMenuContainer();
        // add(new FileMenu());3
        ret.add(AddLinksMenuAction.class);
        ret.add(AddContainerAction.class);
        ret.add(new SeparatorData());
        BackupMenuContainer backup = new BackupMenuContainer();

        backup.add(BackupCreateAction.class);
        backup.add(BackupRestoreAction.class);
        ret.add(backup);
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
        return _GUI.T.MainMenuManager_getName();
    }

    @Override
    protected void updateGui() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                JDMenuBar.getInstance().updateLayout();
            }
        };

    }

}
