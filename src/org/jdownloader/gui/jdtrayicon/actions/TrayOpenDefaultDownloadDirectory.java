package org.jdownloader.gui.jdtrayicon.actions;

import jd.gui.swing.jdgui.components.toolbar.actions.OpenDefaultDownloadFolderAction;

import org.jdownloader.gui.jdtrayicon.translate._TRAY;

public class TrayOpenDefaultDownloadDirectory extends OpenDefaultDownloadFolderAction {

    public TrayOpenDefaultDownloadDirectory() {
        super();
        setName(_TRAY._.popup_downloadfolder());
    }

}
