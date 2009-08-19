package jd.gui.swing.jdgui.views.downloadview.Columns;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import javax.swing.JTable;

import jd.controlling.DownloadController;
import jd.gui.swing.components.JDTable.JDTableColumn;
import jd.gui.swing.components.JDTable.JDTableModel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class DateAddedColumn extends JDTableColumn {

    /**
     * 
     */
    private static final long serialVersionUID = 2228210790952050305L;
    private Component co;
    private DownloadLink dLink;
    private Date date;

    public DateAddedColumn(String name, JDTableModel table) {
        super(name, table);
        date = new Date();
    }

    @Override
    public boolean isEditable(Object obj) {
        return false;
    }

    @Override
    public boolean defaultEnabled() {
        return false;
    }

    @Override
    public Component myTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        return null;
    }

    @Override
    public Component myTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof FilePackage) {
            co = getDefaultTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
        } else if (value instanceof DownloadLink) {
            dLink = (DownloadLink) value;
            co = getDefaultTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (dLink.getCreated() <= 0) {
                ((JRendererLabel) co).setText("");
            } else {
                date.setTime(dLink.getCreated());
                ((JRendererLabel) co).setText(date.toString());
            }
            ((JRendererLabel) co).setBorder(null);
        }
        return co;
    }

    @Override
    public void setValue(Object value, Object object) {
    }

    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isSortable(Object obj) {
        /*
         * DownloadView hat nur null(Header) oder ne ArrayList(FilePackage)
         */
        if (obj == null && DownloadController.getInstance().getPackages().size() == 1) return true;
        if (obj == null || obj instanceof ArrayList<?>) return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void sort(Object obj, final boolean sortingToggle) {
        ArrayList<FilePackage> packages = null;
        synchronized (DownloadController.ControllerLock) {
            synchronized (DownloadController.getInstance().getPackages()) {
                packages = DownloadController.getInstance().getPackages();
                /*
                 * in obj stecken alle selektierten packages, sortiere die links
                 * nach namen
                 */
                if (obj != null && packages.size() > 1) packages = (ArrayList<FilePackage>) obj;
                for (FilePackage fp : packages) {
                    Collections.sort(fp.getDownloadLinkList(), new Comparator<DownloadLink>() {
                        public int compare(DownloadLink a, DownloadLink b) {
                            DownloadLink aa = b;
                            DownloadLink bb = a;
                            if (sortingToggle) {
                                aa = a;
                                bb = b;
                            }
                            if (aa.getCreated() == bb.getCreated()) return 0;
                            return aa.getCreated() < bb.getCreated() ? -1 : 1;
                        }
                    });
                }
            }
        }
        /* inform DownloadController that structure changed */
        DownloadController.getInstance().fireStructureUpdate();
    }

    @Override
    public boolean isEnabled(Object obj) {
        if (obj == null) return false;
        if (obj instanceof DownloadLink) return ((DownloadLink) obj).isEnabled();
        if (obj instanceof FilePackage) return ((FilePackage) obj).isEnabled();
        return true;
    }

}
