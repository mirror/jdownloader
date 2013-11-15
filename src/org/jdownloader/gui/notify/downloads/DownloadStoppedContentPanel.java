package org.jdownloader.gui.notify.downloads;

import javax.swing.JSeparator;

import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.plugins.DownloadLink;

import org.jdownloader.gui.notify.gui.CFG_BUBBLE;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.TaskColumn.ColumnHelper;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;

public class DownloadStoppedContentPanel extends DownloadStartedContentPanel {

    private Pair status;

    public DownloadStoppedContentPanel(SingleDownloadController downloadController) {
        super(downloadController);
        add(new JSeparator(), "pushx,spanx");

    }

    @Override
    protected void layoutComponents() {
        super.layoutComponents();

        if (CFG_BUBBLE.DOWNLOAD_STARTED_BUBBLE_CONTENT_STATUS_VISIBLE.isEnabled()) {
            DownloadLink downloadLink = downloadController.getDownloadLink();
            ColumnHelper ch = new ColumnHelper();
            DownloadsTableModel.getInstance().getTaskColumn().fillColumnHelper(ch, downloadLink);
            status = addPair(status, _GUI._.lit_status() + ":", ch.getIcon());
            status.setText(ch.getString());

        }
    }
}
