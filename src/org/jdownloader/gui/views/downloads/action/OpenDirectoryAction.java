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

public class OpenDirectoryAction extends CustomizableTableContextAppAction<FilePackage, DownloadLink> {

    private static final long   serialVersionUID = 3656369075540437063L;

    private volatile File       directory        = null;
    private volatile File       file             = null;

    private final static String NAME             = _GUI.T.gui_table_contextmenu_downloaddir();

    public OpenDirectoryAction() {
        super();
        setIconKey(IconKey.ICON_PACKAGE_OPEN);
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
            if (file != null) {
                CrossSystem.showInExplorer(file);
            } else {
                CrossSystem.openFile(getDirectory(directory));
            }
        }
    }

    private File getDirectory(File directory) {
        if (directory != null) {
            if (directory.exists()) {
                return directory;
            }
            directory = directory.getParentFile();
            if (directory != null && directory.exists()) {
                return directory;
            }
        }
        return null;
    }

    @Override
    public boolean isEnabled() {
        if (CrossSystem.isOpenFileSupported()) {
            final File lFile = file;
            if (lFile != null && lFile.exists()) {
                return true;
            } else {
                file = null;
            }
            final File lDirectory = getDirectory(directory);
            if (lDirectory != null && lDirectory.exists()) {
                directory = lDirectory;
                return true;
            } else {
                directory = null;
            }
        }
        return false;
    }
}