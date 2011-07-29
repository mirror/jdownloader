package org.jdownloader.gui.views.linkgrabber;

import org.jdownloader.gui.views.downloads.columns.AddedDateColumn;
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

public class LinkGrabberTableModel extends LinkTableModel {

    private static final long serialVersionUID = -198189279671615981L;

    public LinkGrabberTableModel() {
        super("LinkGrabberTableModel");

    }

    @Override
    protected void initColumns() {
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

    }

    /**
     * we want to return to default sort after each start
     */
    protected boolean isSortStateSaverEnabled() {
        return true;
    }

    // /**
    // * @return
    // */
    // protected ExtColumn<PackageLinkNode> getDefaultSortColumn() {
    // downloadOrder.setSortOrderIdentifier(ExtColumn.SORT_DESC);
    // return downloadOrder;
    // }

}
