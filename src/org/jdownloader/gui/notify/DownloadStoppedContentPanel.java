package org.jdownloader.gui.notify;

import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.plugins.DownloadLink;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.TaskColumn.ColumnHelper;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;

public class DownloadStoppedContentPanel extends DownloadStartedContentPanel {

    public DownloadStoppedContentPanel(SingleDownloadController downloadController) {
        super(downloadController);
        add(new JSeparator(), "pushx,spanx");
        DownloadLink downloadLink = downloadController.getDownloadLink();
        add(createHeaderLabel(_GUI._.lit_status() + ":"));

        ColumnHelper ch = new ColumnHelper();
        DownloadsTableModel.getInstance().getTaskColumn().fillColumnHelper(ch, downloadLink);
        add(new JLabel(ch.getString(), ch.getIcon(), SwingConstants.LEFT));
    }

}
