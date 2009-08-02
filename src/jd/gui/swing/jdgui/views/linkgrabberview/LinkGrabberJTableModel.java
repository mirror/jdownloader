package jd.gui.swing.jdgui.views.linkgrabberview;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import jd.config.SubConfiguration;
import jd.controlling.LinkGrabberController;
import jd.plugins.DownloadLink;
import jd.plugins.LinkGrabberFilePackage;
import jd.utils.locale.JDL;

public class LinkGrabberJTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 896882146491584908L;
    public static final byte COL_PACK_FILE = 0;
    public static final byte COL_SIZE = 1;
    public static final byte COL_HOSTER = 2;
    public static final byte COL_STATUS = 3;

    private static final String[] COLUMN_NAMES = { JDL.L("gui.linkgrabber.header.packagesfiles", "Pakete/Dateien"), JDL.L("gui.treetable.header.size", "Größe"), JDL.L("gui.treetable.header_3.hoster", "Anbieter"), JDL.L("gui.treetable.header_4.status", "Status") };
    private ArrayList<Object> addlist = new ArrayList<Object>();
    private SubConfiguration config;

    public LinkGrabberJTableModel() {
        super();
        config = SubConfiguration.getConfig("linkgrabber");
        refreshModel();
    }

    public int getRowCount() {
        return addlist.size();
    }

    public int getRowforObject(Object o) {
        synchronized (addlist) {
            return addlist.indexOf(o);
        }
    }

    public Object getObjectforRow(int row) {
        synchronized (addlist) {
            if (row < addlist.size()) return addlist.get(row);
            return null;
        }
    }

    public void refreshModel() {
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (LinkGrabberController.getInstance().getPackages()) {
                synchronized (addlist) {
                    addlist.clear();
                    for (LinkGrabberFilePackage fp : LinkGrabberController.getInstance().getPackages()) {
                        addlist.add(fp);
                        if (fp.getBooleanProperty(LinkGrabberTable.PROPERTY_EXPANDED, false)) {
                            for (DownloadLink dl : fp.getDownloadLinks()) {
                                addlist.add(dl);
                            }
                        }
                    }
                }
            }
        }
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        try {
            return addlist.get(rowIndex);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public int getRealColumnCount() {
        return COLUMN_NAMES.length;
    }

    public String getRealColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    public int getColumnCount() {
        int j = 0;
        for (int i = 0; i < COLUMN_NAMES.length; ++i) {
            if (isVisible(i)) ++j;
        }
        return j;
    }

    public boolean isVisible(int column) {
        return config.getBooleanProperty("VISABLE_COL_" + column, true);
    }

    public void setVisible(int column, boolean visible) {
        config.setProperty("VISABLE_COL_" + column, visible);
        config.save();
    }

    public int toModel(int column) {
        int i = 0;
        int k;
        for (k = 0; k < getRealColumnCount(); ++k) {
            if (isVisible(k)) {
                ++i;
            }
            if (i > column) break;
        }
        return k;
    }

    public int toVisible(int column) {
        int i = column;
        int k;
        for (k = column; k >= 0; --k) {
            if (!isVisible(k)) {
                --i;
            }
        }
        return i;
    }

    // @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[toModel(column)];
    }

    // @Override
    public Class<?> getColumnClass(int column) {
        return Object.class;
    }

}
