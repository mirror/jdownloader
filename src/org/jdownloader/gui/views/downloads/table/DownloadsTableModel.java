package org.jdownloader.gui.views.downloads.table;

import java.util.ArrayList;

import jd.controlling.DownloadController;
import jd.controlling.IOEQ;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PackageLinkNode;

import org.appwork.utils.swing.table.ExtTableModel;
import org.jdownloader.gui.views.downloads.columns.AddedDateColumn;
import org.jdownloader.gui.views.downloads.columns.FileColumn;
import org.jdownloader.gui.views.downloads.columns.FinishedDateColumn;
import org.jdownloader.gui.views.downloads.columns.HosterIconColumn;
import org.jdownloader.gui.views.downloads.columns.ListOrderIDColumn;
import org.jdownloader.gui.views.downloads.columns.LoadedColumn;
import org.jdownloader.gui.views.downloads.columns.PriorityColumn;
import org.jdownloader.gui.views.downloads.columns.RemainingColumn;
import org.jdownloader.gui.views.downloads.columns.SizeColumn;

public class DownloadsTableModel extends ExtTableModel<PackageLinkNode> {

    public DownloadsTableModel() {
        super("downloadstable");
    }

    @Override
    protected void initColumns() {
        this.addColumn(new FileColumn());
        this.addColumn(new SizeColumn());
        this.addColumn(new RemainingColumn());
        this.addColumn(new LoadedColumn());
        this.addColumn(new HosterIconColumn());
        this.addColumn(new AddedDateColumn());
        this.addColumn(new FinishedDateColumn());
        this.addColumn(new PriorityColumn());
        this.addColumn(new ListOrderIDColumn());
    }

    public void recreateModel() {
        IOEQ.add(new Runnable() {

            public void run() {
                ArrayList<PackageLinkNode> newtableData = new ArrayList<PackageLinkNode>(tableData.size());
                synchronized (DownloadController.getInstance().getPackages()) {
                    for (FilePackage fp : DownloadController.getInstance().getPackages()) {
                        newtableData.add(fp);
                        if (fp.isExpanded()) {
                            for (DownloadLink dl : fp.getDownloadLinkList()) {
                                newtableData.add(dl);
                            }
                        }
                    }
                }
                ArrayList<PackageLinkNode> selected = DownloadsTableModel.this.getSelectedObjects();
                tableData = newtableData;
                DownloadsTableModel.this.fireTableStructureChanged();
                DownloadsTableModel.this.setSelectedObjects(selected);
            }

        }, true);
    }

}
