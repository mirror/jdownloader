package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.controlling.IOEQ;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.UserIO;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.actions.SelectionAppAction;
import org.jdownloader.controlling.DownloadLinkAggregator;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class ResetAction extends SelectionAppAction<FilePackage, DownloadLink> {

    private static final long serialVersionUID = -5583373118359478729L;

    public ResetAction(SelectionInfo<FilePackage, DownloadLink> si) {
        super(si);
        setIconKey("undo");
        setName(_GUI._.gui_table_contextmenu_reset());
    }

    public void actionPerformed(ActionEvent e) {
        IOEQ.add(new Runnable() {
            public void run() {

                DownloadLinkAggregator agg = new DownloadLinkAggregator();
                agg.setLocalFileUsageEnabled(true);
                agg.update(getSelection().getChildren());

                if (UserIO.isOK(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.gui_downloadlist_reset2(agg.getTotalCount(), SizeFormatter.formatBytes(agg.getBytesLoaded()), agg.getLocalFileCount())))) {
                    for (DownloadLink link : getSelection().getChildren()) {
                        if (link.getLinkStatus().isPluginActive()) {
                            /*
                             * download is still active, let DownloadWatchdog handle the reset
                             */
                            DownloadWatchDog.getInstance().resetSingleDownloadController(link.getDownloadLinkController());
                        } else {
                            /* we can do the reset ourself */
                            DownloadWatchDog.getInstance().removeIPBlockTimeout(link);
                            DownloadWatchDog.getInstance().removeTempUnavailTimeout(link);
                            link.reset();
                        }
                    }
                }
            }
        });
    }
}