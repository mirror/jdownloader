package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.ImageIcon;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.actions.SelectionAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class OpenFileAction extends SelectionAppAction<FilePackage, DownloadLink> {

    private static final long serialVersionUID = 1901008532686173167L;

    private File              file;

    public OpenFileAction(final SelectionInfo<FilePackage, DownloadLink> si) {
        super(si);
        ImageIcon img;

        setIconKey("file");
        setName(_GUI._.gui_table_contextmenu_openfile());

    }

    @Override
    public void setSelection(SelectionInfo<FilePackage, DownloadLink> selection) {
        super.setSelection(selection);
        if (getSelection() != null) {
            if (getSelection().isLinkContext()) {
                this.file = getSelection() == null ? null : new File(getSelection().getContextLink().getFileOutput());

            } else {
                this.file = getSelection() == null ? null : new File(getSelection().getContextPackage().getDownloadDirectory());

            }

        }

    }

    public OpenFileAction(File file) {
        super(null);
        this.file = file;
    }

    @Override
    public boolean isEnabled() {
        return file != null && file.exists();
    }

    public void actionPerformed(ActionEvent e) {
        while (!file.exists()) {
            File p = file.getParentFile();
            if (p == null || p.equals(file)) return;
            file = p;
        }
        CrossSystem.openFile(file);

    }

}