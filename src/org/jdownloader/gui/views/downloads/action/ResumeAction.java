package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.actions.SelectionAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class ResumeAction extends SelectionAppAction<FilePackage, DownloadLink> {

    private static final long serialVersionUID = 8087143123808363305L;

    public ResumeAction(SelectionInfo<FilePackage, DownloadLink> si) {
        super(si);
        setIconKey("resume");
        setName(_GUI._.gui_table_contextmenu_resume());
    }

    @Override
    public boolean isEnabled() {
        return hasSelection() && super.isEnabled();
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        DownloadWatchDog.getInstance().resume(getSelection().getChildren());
    }
}