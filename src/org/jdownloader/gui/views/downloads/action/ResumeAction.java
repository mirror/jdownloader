package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.controlling.IOEQ;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class ResumeAction extends AppAction {

    private static final long                              serialVersionUID = 8087143123808363305L;

    private final SelectionInfo<FilePackage, DownloadLink> si;

    public ResumeAction(SelectionInfo<FilePackage, DownloadLink> si) {
        this.si = si;
        setIconKey("resume");
        setName(_GUI._.gui_table_contextmenu_resume());
    }

    @Override
    public boolean isEnabled() {
        return !si.isEmpty();
    }

    public void actionPerformed(ActionEvent e) {
        IOEQ.add(new Runnable() {
            public void run() {
                for (DownloadLink link : si.getChildren()) {
                    if (!link.getLinkStatus().isPluginActive() && (link.getLinkStatus().isFailed() || link.getLinkStatus().hasStatus(LinkStatus.TEMP_IGNORE))) {
                        link.getLinkStatus().reset(true);
                        DownloadWatchDog.getInstance().removeIPBlockTimeout(link);
                        DownloadWatchDog.getInstance().removeTempUnavailTimeout(link);
                    }
                }
            }
        });
    }
}