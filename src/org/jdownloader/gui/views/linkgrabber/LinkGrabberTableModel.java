package org.jdownloader.gui.views.linkgrabber;

import java.util.Iterator;
import java.util.List;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelData;
import org.jdownloader.gui.views.components.packagetable.columns.ChecksumColumn;
import org.jdownloader.gui.views.components.packagetable.columns.CommentColumn;
import org.jdownloader.gui.views.components.packagetable.columns.DownloadPasswordColumn;
import org.jdownloader.gui.views.components.packagetable.columns.FileTypeColumn;
import org.jdownloader.gui.views.components.packagetable.columns.HasCaptchaColumn;
import org.jdownloader.gui.views.components.packagetable.columns.LinkIDColumn;
import org.jdownloader.gui.views.downloads.columns.AddedDateColumn;
import org.jdownloader.gui.views.downloads.columns.AvailabilityColumn;
import org.jdownloader.gui.views.downloads.columns.EnabledDisabledColumn;
import org.jdownloader.gui.views.downloads.columns.FileColumn;
import org.jdownloader.gui.views.downloads.columns.HosterColumn;
import org.jdownloader.gui.views.downloads.columns.PriorityColumn;
import org.jdownloader.gui.views.downloads.columns.SizeColumn;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings;
import org.jdownloader.gui.views.linkgrabber.columns.DownloadFolderColumn;
import org.jdownloader.gui.views.linkgrabber.columns.PartColumn;
import org.jdownloader.gui.views.linkgrabber.columns.UrlColumn;
import org.jdownloader.gui.views.linkgrabber.columns.VariantColumn;
import org.jdownloader.myjdownloader.client.json.AvailableLinkState;
import org.jdownloader.settings.staticreferences.CFG_GUI;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

public class LinkGrabberTableModel extends PackageControllerTableModel<CrawledPackage, CrawledLink> {

    private static final long                  serialVersionUID = -198189279671615981L;
    private static final LinkGrabberTableModel INSTANCE         = new LinkGrabberTableModel();

    public static LinkGrabberTableModel getInstance() {
        return INSTANCE;
    }

    private boolean        autoConfirm;
    protected FileColumn   expandCollapse;
    private PriorityColumn priorityColumn;
    private VariantColumn  variantColumn;

    private LinkGrabberTableModel() {
        super(LinkCollector.getInstance(), "LinkGrabberTableModel");
    }

    public List<AbstractNode> refreshSort(final List<AbstractNode> data) {
        try {
            return super.refreshSort(data);
        } finally {
            if (!isTristateSorterEnabled()) {
                sortColumn = null;
            }
        }
    }

    @Override
    protected int[] getScrollPositionFromConfig() {
        return CFG_GUI.CFG.getLinkgrabberListScrollPosition();

    }

    public java.util.List<AbstractNode> sort(final java.util.List<AbstractNode> data, ExtColumn<AbstractNode> column) {
        final PackageControllerTableModelData<CrawledPackage, CrawledLink> ret = (PackageControllerTableModelData<CrawledPackage, CrawledLink>) super.sort(data, column);
        boolean autoConfirm = ret.size() > 0 && org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.LINKGRABBER_AUTO_CONFIRM_ENABLED.isEnabled();
        if (!autoConfirm) {
            final Iterator<CrawledLink> it = ret.getVisibleChildrenIterator();
            while (it.hasNext()) {
                final CrawledLink next = it.next();
                if (next.isAutoConfirmEnabled() && next.getLinkState() != AvailableLinkState.OFFLINE) {
                    autoConfirm = true;
                    break;
                }
            }
        }
        this.autoConfirm = autoConfirm;
        return ret;
    }

    public boolean isAutoConfirm() {
        return autoConfirm;
    }

    @Override
    protected void initColumns() {
        this.addColumn(expandCollapse = new FileColumn());
        this.addColumn(variantColumn = new VariantColumn(JsonConfig.create(LinkgrabberSettings.class).isVariantsColumnAlwaysVisible()));
        addColumn(new PartColumn());
        this.addColumn(new UrlColumn());
        this.addColumn(new DownloadFolderColumn());
        this.addColumn(new DownloadPasswordColumn());
        this.addColumn(new EnabledDisabledColumn());
        this.addColumn(new SizeColumn());
        this.addColumn(new HosterColumn());
        this.addColumn(new AvailabilityColumn() {

            @Override
            public boolean isDefaultVisible() {
                return true;
            }

        });
        // this.addColumn(new AddedDateColumn());
        this.addColumn(priorityColumn = new PriorityColumn());
        this.addColumn(new CommentColumn() {

            @Override
            public boolean isDefaultVisible() {
                return false;
            }
        });
        this.addColumn(new LinkIDColumn());
        this.addColumn(new AddedDateColumn());
        this.addColumn(new ChecksumColumn());
        this.addColumn(new FileTypeColumn());
        this.addColumn(new HasCaptchaColumn());
    }

    protected void setVariantsSupport(final boolean vs) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setVariantsColumnVisible(vs);
            }
        };
    }

    public void setColumnVisible(final ExtColumn<AbstractNode> column, final boolean visible) {
        try {
            this.getTable().getStorage().put(this.getTable().getColumnStoreKey("VISABLE_COL_", column.getID()), visible);
        } catch (final Exception e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        }
        this.getTable().updateColumns();

    }

    public void setVariantsColumnVisible(boolean b) {
        if (variantColumn != null) {
            if (variantColumn.setAutoVisible(b)) {
                this.getTable().updateColumns();
            }
        }
    }

    public void setPriorityColumnVisible(boolean b) {
        if (priorityColumn != null) {
            this.setColumnVisible(priorityColumn, b);
        }
    }

}
