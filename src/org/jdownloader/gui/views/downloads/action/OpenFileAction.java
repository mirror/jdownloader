package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.io.File;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

public class OpenFileAction extends CustomizableTableContextAppAction<FilePackage, DownloadLink> {

    private static final long   serialVersionUID = 1901008532686173167L;

    private File                file             = null;
    private final static String NAME             = _GUI.T.gui_table_contextmenu_openfile();

    public OpenFileAction() {
        super();
        setIconKey(IconKey.ICON_FILE);
        setName(NAME);
    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        final SelectionInfo<FilePackage, DownloadLink> selection = getSelection();
        file = null;
        if (hasSelection(selection)) {
            if (selection.isLinkContext()) {
                this.file = new File(selection.getContextLink().getFileOutput());
            } else {
                this.file = new File(selection.getFirstPackage().getView().getDownloadDirectory());
                // Do not show for packages
                setVisible(false);
            }
        }
    }

    public OpenFileAction(File file) {
        super();
        this.file = file;
        setIconKey(IconKey.ICON_FILE);
        setName(NAME);
    }

    @Override
    public boolean isEnabled() {
        final File lFile = file;
        return CrossSystem.isOpenFileSupported() && lFile != null && lFile.exists();
    }

    public void actionPerformed(ActionEvent e) {
        if (isEnabled()) {
            File lFile = file;
            while (!lFile.exists()) {
                File p = lFile.getParentFile();
                if (p == null || p.equals(lFile)) {
                    return;
                }
                lFile = p;
            }
            CrossSystem.openFile(lFile);
        }
    }

}