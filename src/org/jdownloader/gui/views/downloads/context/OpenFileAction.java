package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.ImageIcon;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class OpenFileAction extends AppAction {

    private static final long serialVersionUID = 1901008532686173167L;

    private File              file;

    public OpenFileAction(final SelectionInfo<FilePackage, DownloadLink> si) {
        this.file = si == null ? null : new File(si.getContextLink().getFileOutput());

        ImageIcon img;
        if (si != null) {
            if (si.isLinkContext()) {
                img = (si.getLink().getIcon());
            } else {
                img = (si.getFirstPackage().isExpanded() ? NewTheme.I().getIcon("tree_package_open", 32) : NewTheme.I().getIcon("tree_package_closed", 32));
            }
            this.setSmallIcon(img);
        } else {
            this.setSmallIcon(NewTheme.I().getIcon("file", 20));
        }
        setName(_GUI._.gui_table_contextmenu_openfile());

    }

    public OpenFileAction(File file) {
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