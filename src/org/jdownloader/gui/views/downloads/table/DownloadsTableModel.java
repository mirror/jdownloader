package org.jdownloader.gui.views.downloads.table;

import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel;
import org.jdownloader.gui.views.components.packagetable.columns.ChecksumColumn;
import org.jdownloader.gui.views.components.packagetable.columns.CommentColumn;
import org.jdownloader.gui.views.components.packagetable.columns.DownloadPasswordColumn;
import org.jdownloader.gui.views.components.packagetable.columns.FileTypeColumn;
import org.jdownloader.gui.views.components.packagetable.columns.HasCaptchaColumn;
import org.jdownloader.gui.views.components.packagetable.columns.LinkIDColumn;
import org.jdownloader.gui.views.downloads.columns.AddedDateColumn;
import org.jdownloader.gui.views.downloads.columns.AvailabilityColumn;
import org.jdownloader.gui.views.downloads.columns.CandidateAccountColumn;
import org.jdownloader.gui.views.downloads.columns.CandidateGatewayColumn;
import org.jdownloader.gui.views.downloads.columns.ConnectionColumn;
import org.jdownloader.gui.views.downloads.columns.DownloadFolderColumn;
import org.jdownloader.gui.views.downloads.columns.DurationColumn;
import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.gui.views.downloads.columns.EnabledDisabledColumn;
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
import org.jdownloader.gui.views.linkgrabber.columns.PartColumn;
import org.jdownloader.gui.views.linkgrabber.columns.UrlColumn;
import org.jdownloader.settings.staticreferences.CFG_GUI;

import jd.controlling.downloadcontroller.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

public class DownloadsTableModel extends PackageControllerTableModel<FilePackage, DownloadLink> {

    private static final long                serialVersionUID = -198189279671615981L;

    private static final DownloadsTableModel INSTANCE         = new DownloadsTableModel();

    public static DownloadsTableModel getInstance() {
        return INSTANCE;
    }

    private StopSignColumn     stopSignColumn;

    protected FileColumn       expandCollapse;

    private PriorityColumn     priorityColumn;

    private TaskColumn         taskColumn;

    private AvailabilityColumn available;

    private DownloadsTableModel() {
        super(DownloadController.getInstance(), "downloadstable3");
    }

    @Override
    protected void initColumns() {
        this.addColumn(expandCollapse = new FileColumn());
        addColumn(new PartColumn());
        this.addColumn(new SizeColumn());
        this.addColumn(new HosterColumn());
        this.addColumn(new UrlColumn());
        this.addColumn(new ConnectionColumn());
        this.addColumn(new CandidateGatewayColumn());
        this.addColumn(new CandidateAccountColumn());

        this.addColumn(taskColumn = new TaskColumn());
        this.addColumn(new RemainingColumn());
        this.addColumn(new EnabledDisabledColumn());

        this.addColumn(new AddedDateColumn());
        this.addColumn(new FinishedDateColumn());
        this.addColumn(new DurationColumn());

        this.addColumn(new SpeedColumn());
        this.addColumn(new ETAColumn());

        this.addColumn(new LoadedColumn());
        this.addColumn(new ProgressColumn());

        this.addColumn(priorityColumn = new PriorityColumn());
        this.addColumn(available = new AvailabilityColumn());
        this.addColumn(new DownloadFolderColumn());
        this.addColumn(new CommentColumn());
        this.addColumn(new LinkIDColumn());
        this.addColumn(new DownloadPasswordColumn());
        this.addColumn(new ChecksumColumn());
        this.addColumn(new FileTypeColumn());
        this.addColumn(new HasCaptchaColumn());
        this.addColumn(stopSignColumn = new StopSignColumn());

        // reset sort

    }

    public TaskColumn getTaskColumn() {
        return taskColumn;
    }

    public void setAvailableColumnVisible(boolean b) {
        if (available != null) {
            this.setColumnVisible(available, b);
        }
    }

    public void setStopSignColumnVisible(boolean b) {
        if (!CFG_GUI.CFG.isDownloadControlColumnAutoShowEnabled()) {
            return;
        }
        if (stopSignColumn != null) {
            this.setColumnVisible(stopSignColumn, b);
        }
    }

    public void setPriorityColumnVisible(boolean b) {
        if (!CFG_GUI.CFG.isPriorityColumnAutoShowEnabled()) {
            return;
        }
        if (priorityColumn != null) {
            this.setColumnVisible(priorityColumn, b);
        }
    }

    @Override
    protected int[] getScrollPositionFromConfig() {
        return CFG_GUI.CFG.getDownloadListScrollPosition();

    }

}
