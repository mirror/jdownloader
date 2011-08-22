package org.jdownloader.gui.views.downloads.table;

import java.util.ArrayList;

import javax.swing.Icon;

import jd.controlling.DownloadController;
import jd.plugins.FilePackage;
import jd.plugins.PackageLinkNode;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.utils.logging.Log;
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
import org.jdownloader.gui.views.linkgrabber.LinkTableModel;

public class DownloadsTableModel extends LinkTableModel {

    private static final long   serialVersionUID   = -198189279671615981L;

    private static final String SORT_DOWNLOADORDER = "DOWNLOAD";

    public DownloadsTableModel() {
        super("downloadstable3");

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

        // reset sort
        //
        try {
            getStorage().put(ExtTableModel.SORT_ORDER_ID_KEY, (String) null);
            getStorage().put(ExtTableModel.SORTCOLUMN_KEY, (String) null);
        } catch (final Exception e) {
            Log.exception(e);
        }

    }

    /**
     * we want to return to default sort after each start
     */
    protected boolean isSortStateSaverEnabled() {
        return false;
    }

    // /**
    // * @return
    // */
    protected ExtColumn<PackageLinkNode> getDefaultSortColumn() {
        return null;
    }

    @Override
    public String getNextSortIdentifier(String sortOrderIdentifier) {
        if (sortOrderIdentifier == null || sortOrderIdentifier.equals(ExtColumn.SORT_ASC)) {
            return ExtColumn.SORT_DESC;
        } else if (sortOrderIdentifier.equals(ExtColumn.SORT_DESC)) {
            return SORT_DOWNLOADORDER;
        } else {
            return ExtColumn.SORT_ASC;
        }
    }

    public Icon getSortIcon(String sortOrderIdentifier) {
        if (SORT_DOWNLOADORDER.equals(sortOrderIdentifier)) { return null; }
        return super.getSortIcon(sortOrderIdentifier);
    }

    /*
     * we override sort to have a better sorting of packages/files, to keep
     * their structure alive,data is only used to specify the size of the new
     * ArrayList
     */
    @Override
    public ArrayList<PackageLinkNode> sort(final ArrayList<PackageLinkNode> data, ExtColumn<PackageLinkNode> column) {
        if (column == null || column.getSortOrderIdentifier() == SORT_DOWNLOADORDER) {
            this.sortColumn = null;
            try {
                getStorage().put(ExtTableModel.SORT_ORDER_ID_KEY, (String) null);
                getStorage().put(ExtTableModel.SORTCOLUMN_KEY, (String) null);
            } catch (final Exception e) {
                Log.exception(e);
            }
            synchronized (DownloadController.ACCESSLOCK) {
                /* get all packages from controller */
                ArrayList<PackageLinkNode> packages = new ArrayList<PackageLinkNode>(DownloadController.getInstance().size());
                packages.addAll(DownloadController.getInstance().getPackages());
                ArrayList<PackageLinkNode> newData = new ArrayList<PackageLinkNode>(Math.max(data.size(), packages.size()));
                for (PackageLinkNode node : packages) {
                    newData.add(node);
                    if (!((FilePackage) node).isExpanded()) continue;
                    ArrayList<PackageLinkNode> files = null;
                    synchronized (node) {
                        files = new ArrayList<PackageLinkNode>(((FilePackage) node).getControlledDownloadLinks());
                    }
                    newData.addAll(files);
                }
                return newData;
            }
        } else {
            return super.sort(data, column);
        }
    }

}
