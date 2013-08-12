package jd.gui.swing.jdgui.components.toolbar.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.gui.toolbar.action.ToolBarAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.settings.GeneralSettings;

public class OpenDefaultDownloadFolderAction extends ToolBarAction {

    public OpenDefaultDownloadFolderAction(SelectionInfo<?, ?> selection) {
        setIconKey("save");
    }

    public void actionPerformed(ActionEvent e) {
        final String dlDir = JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder();
        if (dlDir == null) { return; }
        /* we want to open the dlDir and not its parent folder/select it */
        CrossSystem.openFile(new File(dlDir));
    }

    @Override
    public boolean isEnabled() {
        return CrossSystem.isOpenFileSupported();
    }

    @Override
    protected String createTooltip() {
        return _GUI._.action_open_dlfolder_tooltip();
    }

}
