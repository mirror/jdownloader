package org.jdownloader.gui.views.linkgrabber;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel;
import org.jdownloader.gui.views.downloads.columns.AddedDateColumn;
import org.jdownloader.gui.views.downloads.columns.AvailabilityColumn;
import org.jdownloader.gui.views.downloads.columns.CommentColumn;
import org.jdownloader.gui.views.downloads.columns.FileColumn;
import org.jdownloader.gui.views.downloads.columns.HosterColumn;
import org.jdownloader.gui.views.downloads.columns.PriorityColumn;
import org.jdownloader.gui.views.downloads.columns.SizeColumn;
import org.jdownloader.gui.views.linkgrabber.columns.DownloadFolderColumn;
import org.jdownloader.gui.views.linkgrabber.columns.UrlColumn;

public class LinkGrabberTableModel extends PackageControllerTableModel<CrawledPackage, CrawledLink> {

    private static final long                  serialVersionUID = -198189279671615981L;
    private static final LinkGrabberTableModel INSTANCE         = new LinkGrabberTableModel();

    public static LinkGrabberTableModel getInstance() {
        return INSTANCE;
    }

    private LinkGrabberTableModel() {
        super(LinkCollector.getInstance(), "LinkGrabberTableModel");
    }

    @Override
    protected void initColumns() {
        this.addColumn(new FileColumn());

        this.addColumn(new UrlColumn());
        this.addColumn(new DownloadFolderColumn());
        this.addColumn(new DownloadPasswordColumn());

        this.addColumn(new SizeColumn());
        this.addColumn(new HosterColumn());
        this.addColumn(new AvailabilityColumn());
        this.addColumn(new AddedDateColumn());
        this.addColumn(new PriorityColumn());
        this.addColumn(new CommentColumn());
    }

}
