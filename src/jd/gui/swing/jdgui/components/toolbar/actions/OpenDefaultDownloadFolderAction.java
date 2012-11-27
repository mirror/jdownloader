package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.gui.shortcuts.ShortcutController;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.GeneralSettings;

public class OpenDefaultDownloadFolderAction extends AbstractToolbarAction {
    private static final OpenDefaultDownloadFolderAction INSTANCE = new OpenDefaultDownloadFolderAction();

    /**
     * get the only existing instance of OpenDefaultDownloadFolderAction. This is a singleton
     * 
     * @return
     */
    public static OpenDefaultDownloadFolderAction getInstance() {
        return OpenDefaultDownloadFolderAction.INSTANCE;
    }

    public boolean isDefaultVisible() {
        return false;
    }

    /**
     * Create a new instance of OpenDefaultDownloadFolderAction. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private OpenDefaultDownloadFolderAction() {

    }

    public void actionPerformed(ActionEvent e) {
        final String dlDir = JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder();
        if (dlDir == null) { return; }
        CrossSystem.showInExplorer(new File(dlDir));
    }

    @Override
    public String createIconKey() {
        return "save";
    }

    @Override
    public boolean isEnabled() {
        return CrossSystem.isOpenFileSupported();
    }

    @Override
    protected String createAccelerator() {
        return ShortcutController._.getOpenDefaultDownloadFolderAction();
    }

    @Override
    protected String createTooltip() {
        return _GUI._.action_open_dlfolder_tooltip();
    }

    @Override
    protected void doInit() {
    }

}
