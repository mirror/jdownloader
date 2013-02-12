package org.jdownloader.gui.views.downloads.table;

import jd.controlling.downloadcontroller.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel;
import org.jdownloader.gui.views.components.packagetable.columns.ChecksumColumn;
import org.jdownloader.gui.views.components.packagetable.columns.CommentColumn;
import org.jdownloader.gui.views.components.packagetable.columns.DownloadPasswordColumn;
import org.jdownloader.gui.views.downloads.columns.AddedDateColumn;
import org.jdownloader.gui.views.downloads.columns.AvailabilityColumn;
import org.jdownloader.gui.views.downloads.columns.ConnectionColumn;
import org.jdownloader.gui.views.downloads.columns.DownloadFolderColumn;
import org.jdownloader.gui.views.downloads.columns.DurationColumn;
import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.gui.views.downloads.columns.FileColumn;
import org.jdownloader.gui.views.downloads.columns.FinishedDateColumn;
import org.jdownloader.gui.views.downloads.columns.HosterColumn;
import org.jdownloader.gui.views.downloads.columns.LoadedColumn;
import org.jdownloader.gui.views.downloads.columns.PriorityColumn;
import org.jdownloader.gui.views.downloads.columns.ProgressColumn;
import org.jdownloader.gui.views.downloads.columns.RemainingColumn;
import org.jdownloader.gui.views.downloads.columns.SizeColumn;
import org.jdownloader.gui.views.downloads.columns.SpeedColumn;
import org.jdownloader.gui.views.downloads.columns.StopSignColumn;
import org.jdownloader.gui.views.downloads.columns.TaskColumn;

public class DownloadsTableModel extends PackageControllerTableModel<FilePackage, DownloadLink> {

    private static final long                serialVersionUID = -198189279671615981L;

    private static final DownloadsTableModel INSTANCE         = new DownloadsTableModel();

    public static DownloadsTableModel getInstance() {
        return INSTANCE;
    }

    private StopSignColumn stopSignColumn;

    protected FileColumn   expandCollapse;

    private PriorityColumn priorityColumn;

    private DownloadsTableModel() {
        super(DownloadController.getInstance(), "downloadstable3");

    }

    @Override
    protected void initColumns() {
        this.addColumn(expandCollapse = new FileColumn());
        this.addColumn(new SizeColumn());
        this.addColumn(new HosterColumn());
        this.addColumn(new ConnectionColumn());
        this.addColumn(new TaskColumn());
        this.addColumn(new RemainingColumn());

        this.addColumn(new AddedDateColumn());
        this.addColumn(new FinishedDateColumn());
        this.addColumn(new DurationColumn());

        this.addColumn(new SpeedColumn());
        this.addColumn(new ETAColumn());

        this.addColumn(new LoadedColumn());
        this.addColumn(new ProgressColumn());

        this.addColumn(priorityColumn = new PriorityColumn());
        this.addColumn(new AvailabilityColumn());
        this.addColumn(new DownloadFolderColumn());
        this.addColumn(new CommentColumn());
        this.addColumn(new DownloadPasswordColumn());
        this.addColumn(new ChecksumColumn());

        this.addColumn(stopSignColumn = new StopSignColumn());

        // reset sort

    }

    public void setStopSignColumnVisible(boolean b) {
        if (stopSignColumn != null) this.setColumnVisible(stopSignColumn, b);
    }

    public void setPriorityColumnVisible(boolean b) {
        if (priorityColumn != null) this.setColumnVisible(priorityColumn, b);
    }

}
