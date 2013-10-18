package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.io.File;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.actions.AbstractSelectionContextAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class OpenDirectoryAction extends AbstractSelectionContextAction<FilePackage, DownloadLink> {

    private static final long serialVersionUID = 3656369075540437063L;

    private File              directory;

    private File              file             = null;

    public OpenDirectoryAction(SelectionInfo<FilePackage, DownloadLink> si) {
        super(si);

        setIconKey("package_open");
        setName(_GUI._.gui_table_contextmenu_downloaddir());
        setTooltipText("Open the current downloaddir in explorer");
    }

    @Override
    public void setSelection(SelectionInfo<FilePackage, DownloadLink> selection) {
        super.setSelection(selection);
        file = null;
        directory = null;
        if (hasSelection()) {
            this.directory = new File(getSelection().getContextPackage().getDownloadDirectory());
            if (getSelection().isLinkContext()) {

                this.file = new File(getSelection().getContextLink().getFileOutput());

            } else {
                this.directory = new File(getSelection().getContextPackage().getDownloadDirectory());

            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        if (file == null) {
            CrossSystem.openFile(directory);
        } else {
            CrossSystem.showInExplorer(file);
        }
        //
    }

    @Override
    public boolean isEnabled() {
        return CrossSystem.isOpenFileSupported() && directory != null && directory.exists();
    }

}