package org.jdownloader.gui.views.downloads.table;

import jd.controlling.downloadcontroller.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel;
import org.jdownloader.gui.views.downloads.columns.AddedDateColumn;
import org.jdownloader.gui.views.downloads.columns.AvailabilityColumn;
import org.jdownloader.gui.views.downloads.columns.ConnectionColumn;
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

    private static final long serialVersionUID = -198189279671615981L;

    public DownloadsTableModel() {
        super(DownloadController.getInstance(), "downloadstable3");

    }

    @Override
    protected void initColumns() {
        // addColumn(new ExtTextColumn<PackageLinkNode>("TEst") {
        //
        // @Override
        // public String getStringValue(PackageLinkNode value) {
        // return value + "";
        // }
        // });
        this.addColumn(new FileColumn());
        this.addColumn(new SizeColumn());
        this.addColumn(new HosterColumn());
        addColumn(new ConnectionColumn());
        this.addColumn(new TaskColumn());
        this.addColumn(new RemainingColumn());
        // this.addColumn(new CommentColumn());

        this.addColumn(new AddedDateColumn());
        this.addColumn(new FinishedDateColumn());

        addColumn(new SpeedColumn());
        addColumn(new ETAColumn());

        this.addColumn(new LoadedColumn());
        this.addColumn(new ProgressColumn());

        this.addColumn(new PriorityColumn());
        this.addColumn(new StopSignColumn());
        this.addColumn(new AvailabilityColumn());
        // reset sort

    }

}
