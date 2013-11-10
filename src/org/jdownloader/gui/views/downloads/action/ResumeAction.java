package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.controlling.contextmenu.CustomizableSelectionAppAction;
import org.jdownloader.gui.translate._GUI;

public class ResumeAction extends CustomizableSelectionAppAction<FilePackage, DownloadLink> {

    private static final long serialVersionUID = 8087143123808363305L;

    public ResumeAction() {

        setIconKey("resume");
        setName(_GUI._.gui_table_contextmenu_resume());
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        DownloadWatchDog.getInstance().resume(getSelection().getChildren());
    }
}