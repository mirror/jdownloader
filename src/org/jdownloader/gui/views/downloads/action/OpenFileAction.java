package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.ImageIcon;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.translate._GUI;

public class OpenFileAction extends CustomizableTableContextAppAction<FilePackage, DownloadLink> {

    private static final long serialVersionUID = 1901008532686173167L;

    private File              file;

    public OpenFileAction() {

        ImageIcon img;

        setIconKey("file");
        setName(_GUI._.gui_table_contextmenu_openfile());

    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        if (hasSelection()) {
            if (getSelection().isLinkContext()) {
                this.file = getSelection() == null ? null : new File(getSelection().getContextLink().getFileOutput());

            } else {
                this.file = getSelection() == null ? null : new File(getSelection().getContextPackage().getDownloadDirectory());
                // Do not show for packages
                setVisible(false);
            }

        }
    }

    public OpenFileAction(File file) {
        super();
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