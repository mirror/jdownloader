package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.io.File;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class OpenDirectoryAction extends CustomizableTableContextAppAction<FilePackage, DownloadLink> {

    private static final long   serialVersionUID = 3656369075540437063L;

    private File                directory        = null;

    private File                file             = null;
    private final static String NAME             = _GUI._.gui_table_contextmenu_downloaddir();

    public OpenDirectoryAction() {
        super();
        setIconKey("package_open");
        setName(NAME);
        setTooltipText("Open the current downloaddir in explorer");
    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        file = null;
        directory = null;
        final SelectionInfo<FilePackage, DownloadLink> selection = getSelection();
        if (hasSelection(selection)) {
            this.directory = new File(selection.getFirstPackage().getView().getDownloadDirectory());
            if (selection.isLinkContext()) {
                this.file = new File(selection.getContextLink().getFileOutput());
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (isEnabled()) {
            if (file == null) {
                CrossSystem.openFile(directory);
            } else {
                CrossSystem.showInExplorer(file);
            }
        }
    }

    @Override
    public boolean isEnabled() {
        final File lDirectory = directory;
        final File lFile = file;
        return CrossSystem.isOpenFileSupported() && (lDirectory != null && lDirectory.exists() || lFile != null && lFile.exists());
    }

}