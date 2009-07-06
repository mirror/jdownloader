package jd.gui.skins.simple.components.DownloadView;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import jd.controlling.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.locale.JDL;

public class DownloadJTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    public static final int COL_PART = 0;
    public static final int COL_HOSTER = 1;
    public static final int COL_STATUS = 2;
    public static final int COL_PROGRESS = 3;

    private static final String[] COLUMN_NAMES = { JDL.L("gui.treetable.header_1.tree", "F"), JDL.L("gui.treetable.header_3.hoster", "Anbieter"), JDL.L("gui.treetable.header_4.status", "Status"), JDL.L("gui.treetable.header_5.progress", "Fortschritt") };
    private ArrayList<Object> downloadlist = new ArrayList<Object>();

    public DownloadJTableModel() {
        super();
        refreshmodel();
    }

    public int getRowCount() {
        return downloadlist.size();
    }

    public int getRowforObject(Object o) {
        synchronized (downloadlist) {
            return downloadlist.indexOf(o);
        }
    }

    public void refreshmodel() {
        synchronized (DownloadController.ControllerLock) {
            synchronized (DownloadController.getInstance().getPackages()) {
                synchronized (downloadlist) {
                    downloadlist.clear();
                    for (FilePackage fp : DownloadController.getInstance().getPackages()) {
                        downloadlist.add(fp);
                        if (fp.getBooleanProperty(DownloadTable.PROPERTY_EXPANDED, false)) {
                            for (DownloadLink dl : fp.getDownloadLinkList()) {
                                downloadlist.add(dl);
                            }
                        }
                    }
                }
            }
        }
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        try {
            return downloadlist.get(rowIndex);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    // @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    // @Override
    public Class<?> getColumnClass(int column) {
        return Object.class;
    }

}
