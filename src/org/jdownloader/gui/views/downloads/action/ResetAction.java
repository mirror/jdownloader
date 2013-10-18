package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.controlling.TaskQueue;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.UserIO;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.actions.AbstractSelectionContextAction;
import org.jdownloader.controlling.DownloadLinkAggregator;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class ResetAction extends AbstractSelectionContextAction<FilePackage, DownloadLink> {

    private static final long serialVersionUID = -5583373118359478729L;

    public ResetAction(SelectionInfo<FilePackage, DownloadLink> si) {
        super(si);
        setIconKey("undo");
        setName(_GUI._.gui_table_contextmenu_reset());
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
            @Override
            protected Void run() throws RuntimeException {
                DownloadLinkAggregator agg = new DownloadLinkAggregator();
                agg.setLocalFileUsageEnabled(true);
                agg.update(getSelection().getChildren());
                if (UserIO.isOK(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.gui_downloadlist_reset2(agg.getTotalCount(), SizeFormatter.formatBytes(agg.getBytesLoaded()), agg.getLocalFileCount())))) {
                    DownloadWatchDog.getInstance().reset(getSelection().getChildren());

                }
                return null;
            };
        });
    }
}