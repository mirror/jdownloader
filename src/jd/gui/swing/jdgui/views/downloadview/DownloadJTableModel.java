package jd.gui.swing.jdgui.views.downloadview;

import jd.controlling.DownloadController;
import jd.gui.swing.components.JDTable.JDTableModel;
import jd.gui.swing.jdgui.views.downloadview.Columns.DateAddedColumn;
import jd.gui.swing.jdgui.views.downloadview.Columns.DateFinishedColumn;
import jd.gui.swing.jdgui.views.downloadview.Columns.FileColumn;
import jd.gui.swing.jdgui.views.downloadview.Columns.HosterColumn;
import jd.gui.swing.jdgui.views.downloadview.Columns.PartColumn;
import jd.gui.swing.jdgui.views.downloadview.Columns.ProgressColumn;
import jd.gui.swing.jdgui.views.downloadview.Columns.SizeColumn;
import jd.gui.swing.jdgui.views.downloadview.Columns.StatusColumn;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.locale.JDL;

public class DownloadJTableModel extends JDTableModel {

    private static final long serialVersionUID = 1L;

    public DownloadJTableModel(String configname) {
        super(configname);
        this.addColumn(new FileColumn(JDL.L("gui.treetable.name", "F"), this));
        this.addColumn(new PartColumn(JDL.L("gui.treetable.part", "Part"), this));
        this.addColumn(new HosterColumn(JDL.L("gui.treetable.hoster", "Anbieter"), this));
        this.addColumn(new StatusColumn(JDL.L("gui.treetable.status", "Status"), this));
        this.addColumn(new ProgressColumn(JDL.L("gui.treetable.progress", "Fortschritt"), this));
        this.addColumn(new DateAddedColumn(JDL.L("gui.treetable.added", "Added date"), this));
        this.addColumn(new DateFinishedColumn(JDL.L("gui.treetable.finished", "Finished date"), this));
        this.addColumn(new SizeColumn(JDL.L("gui.treetable.size", "FileSize"), this));
    }

    public void refreshModel() {
        synchronized (DownloadController.ControllerLock) {
            synchronized (DownloadController.getInstance().getPackages()) {
                synchronized (list) {
                    list.clear();
                    for (FilePackage fp : DownloadController.getInstance().getPackages()) {
                        list.add(fp);
                        if (fp.getBooleanProperty(DownloadTable.PROPERTY_EXPANDED, false)) {
                            for (DownloadLink dl : fp.getDownloadLinkList()) {
                                list.add(dl);
                            }
                        }
                    }
                }
            }
        }
    }
}
