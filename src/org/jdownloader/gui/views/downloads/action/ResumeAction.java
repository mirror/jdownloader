package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

public class ResumeAction extends CustomizableTableContextAppAction<FilePackage, DownloadLink> {

    private static final long   serialVersionUID = 8087143123808363305L;

    private final static String NAME             = _GUI.T.gui_table_contextmenu_resume();

    public ResumeAction() {
        setIconKey(IconKey.ICON_RESUME);
        setName(NAME);
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) {
            return;
        }
        DownloadWatchDog.getInstance().resume(getSelection().getChildren());
    }
}