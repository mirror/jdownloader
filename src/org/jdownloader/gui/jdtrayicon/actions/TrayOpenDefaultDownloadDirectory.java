package org.jdownloader.gui.jdtrayicon.actions;

import jd.gui.swing.jdgui.components.toolbar.actions.OpenDefaultDownloadFolderAction;

import org.jdownloader.gui.jdtrayicon.translate._TRAY;
import org.jdownloader.gui.views.SelectionInfo;

public class TrayOpenDefaultDownloadDirectory extends OpenDefaultDownloadFolderAction {

    public TrayOpenDefaultDownloadDirectory(SelectionInfo<?, ?> selection) {
        super(selection);
        setName(_TRAY._.popup_downloadfolder());
    }

}
