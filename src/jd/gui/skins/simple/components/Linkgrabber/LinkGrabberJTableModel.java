package jd.gui.skins.simple.components.Linkgrabber;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import jd.controlling.LinkGrabberController;
import jd.plugins.DownloadLink;
import jd.utils.locale.JDL;

public class LinkGrabberJTableModel extends AbstractTableModel {

    /**
     * 
     */
    private static final long serialVersionUID = 896882146491584908L;
    public static final int COL_PACK_FILE = 0;
    public static final int COL_SIZE = 1;
    public static final int COL_HOSTER = 2;
    public static final int COL_STATUS = 3;

    /** table column names */
    static protected String[] COLUMN_NAMES = { JDL.L("gui.linkgrabber.header.packagesfiles", "Pakete/Dateien"), JDL.L("gui.treetable.header.size", "Größe"), JDL.L("gui.treetable.header_3.hoster", "Anbieter"), JDL.L("gui.treetable.header_4.status", "Status") };
    static ArrayList<Object> addlist = new ArrayList<Object>();

    public LinkGrabberJTableModel() {
        super();
        refreshmodel();
    }

    @Override
    public int getRowCount() {
        return addlist.size();
    }

    public int getRowforObject(Object o) {
        synchronized (addlist) {
            return addlist.indexOf(o);
        }
    }

    public void refreshmodel() {
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

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        try {
            return addlist.get(rowIndex);
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
