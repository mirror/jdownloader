package jd.gui.swing.jdgui.views.linkgrabberview;

import jd.controlling.LinkGrabberController;
import jd.gui.swing.components.JDTable.JDTableModel;
import jd.gui.swing.jdgui.views.linkgrabberview.Columns.FileColumn;
import jd.gui.swing.jdgui.views.linkgrabberview.Columns.HosterColumn;
import jd.gui.swing.jdgui.views.linkgrabberview.Columns.PartColumn;
import jd.gui.swing.jdgui.views.linkgrabberview.Columns.RequestTimeColumn;
import jd.gui.swing.jdgui.views.linkgrabberview.Columns.SizeColumn;
import jd.gui.swing.jdgui.views.linkgrabberview.Columns.StatusColumn;
import jd.plugins.DownloadLink;
import jd.plugins.LinkGrabberFilePackage;
import jd.utils.locale.JDL;

public class LinkGrabberJTableModel extends JDTableModel {

    private static final long serialVersionUID = 896882146491584908L;

    public LinkGrabberJTableModel(String configname) {
        super(configname);
        this.addColumn(new FileColumn(JDL.L("gui.linkgrabber.header.packagesfiles", "Pakete/Dateien"), this));
        this.addColumn(new PartColumn(JDL.L("gui.treetable.header.part", "Part"), this));
        this.addColumn(new SizeColumn(JDL.L("gui.treetable.header.size", "Größe"), this));
        this.addColumn(new HosterColumn(JDL.L("gui.treetable.hoster", "Anbieter"), this));
        this.addColumn(new StatusColumn(JDL.L("gui.treetable.status", "Status"), this));
        this.addColumn(new RequestTimeColumn(JDL.L("gui.treetable.requesttime", "RequestTime"), this));
    }

    public void refreshModel() {
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (LinkGrabberController.getInstance().getPackages()) {
                synchronized (list) {
                    list.clear();
                    for (LinkGrabberFilePackage fp : LinkGrabberController.getInstance().getPackages()) {
                        list.add(fp);
                        if (fp.getBooleanProperty(LinkGrabberTable.PROPERTY_EXPANDED, false)) {
                            for (DownloadLink dl : fp.getDownloadLinks()) {
                                list.add(dl);
                            }
                        }
                    }
                }
            }
        }
    }

}
