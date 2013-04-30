package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.io.File;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class OpenDirectoryAction extends AppAction {

    private static final long serialVersionUID = 3656369075540437063L;

    private File              directory;

    public OpenDirectoryAction(SelectionInfo<FilePackage, DownloadLink> si) {
        if (si != null) {
            this.directory = new File(si.getContextPackage().getDownloadDirectory());
        }
        setIconKey("package_open");
        setName(_GUI._.gui_table_contextmenu_downloaddir());
        setTooltipText("Open the current downloaddir in explorer");
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        CrossSystem.openFile(directory);
    }

    @Override
    public boolean isEnabled() {
        return CrossSystem.isOpenFileSupported() && directory != null && directory.exists();
    }

}