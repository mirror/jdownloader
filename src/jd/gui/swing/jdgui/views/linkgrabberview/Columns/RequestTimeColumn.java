package jd.gui.swing.jdgui.views.linkgrabberview.Columns;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JTable;

import jd.controlling.LinkGrabberController;
import jd.gui.swing.components.JDTable.JDTableColumn;
import jd.gui.swing.components.JDTable.JDTableModel;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.LinkGrabberFilePackage;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class RequestTimeColumn extends JDTableColumn {

    /**
     * 
     */
    private static final long serialVersionUID = 2228210790952050305L;
    private Component co;
    private DownloadLink dLink;

    public RequestTimeColumn(String name, JDTableModel table) {
        super(name, table);
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
        if (value instanceof LinkGrabberFilePackage) {
            value = "";
        } else if (value instanceof DownloadLink) {
            dLink = (DownloadLink) value;
            value = Formatter.formatMilliseconds(dLink.getRequestTime());
        }
        co = getDefaultTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        ((JRendererLabel) co).setBorder(null);
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
         * LinkGrabber hat nur null(Header) oder ne
         * ArrayList(LinkGrabberFilePackage)
         */
        if (obj == null && LinkGrabberController.getInstance().size() == 1) return true;
        if (obj instanceof ArrayList<?>) return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void sort(Object obj, final boolean sortingToggle) {
        ArrayList<LinkGrabberFilePackage> packages = null;
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (LinkGrabberController.getInstance().getPackages()) {
                packages = LinkGrabberController.getInstance().getPackages();
                if (obj != null && packages.size() > 1) packages = (ArrayList<LinkGrabberFilePackage>) obj;
                for (LinkGrabberFilePackage fp : packages) {
                    Collections.sort(fp.getDownloadLinks(), new Comparator<DownloadLink>() {
                        public int compare(DownloadLink a, DownloadLink b) {
                            DownloadLink aa = b;
                            DownloadLink bb = a;
                            if (sortingToggle) {
                                aa = a;
                                bb = b;
                            }
                            if (aa.getRequestTime() == bb.getRequestTime()) return 0;
                            return aa.getRequestTime() < bb.getRequestTime() ? -1 : 1;
                        }
                    });
                }
            }
        }
        /* inform LinkGrabberController that structure changed */
        LinkGrabberController.getInstance().throwRefresh();
    }

    @Override
    public boolean isEnabled(Object obj) {
        if (obj == null) return false;
        if (obj instanceof DownloadLink) return ((DownloadLink) obj).isEnabled();
        return true;
    }

}
