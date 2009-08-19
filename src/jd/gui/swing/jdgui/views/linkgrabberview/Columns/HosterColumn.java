package jd.gui.swing.jdgui.views.linkgrabberview.Columns;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JTable;

import jd.controlling.LinkGrabberController;
import jd.gui.swing.components.JDTable.JDTableColumn;
import jd.gui.swing.components.JDTable.JDTableModel;
import jd.plugins.DownloadLink;
import jd.plugins.LinkGrabberFilePackage;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class HosterColumn extends JDTableColumn {

    /**
     * 
     */
    private static final long serialVersionUID = 2228210790952050305L;
    private Component co;
    private DownloadLink dLink;
    private LinkGrabberFilePackage fp;

    public HosterColumn(String name, JDTableModel table) {
        super(name, table);
    }

    @Override
    public boolean isEditable(Object obj) {
        return false;
    }

    @Override
    public Component myTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        return null;
    }

    @Override
    public Component myTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof LinkGrabberFilePackage) {
            fp = (LinkGrabberFilePackage) value;
            value = fp.getHoster();
            co = getDefaultTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JRendererLabel) co).setBorder(null);
        } else if (value instanceof DownloadLink) {
            dLink = (DownloadLink) value;
            if (dLink.getPlugin().hasHosterIcon()) {
                co = getDefaultTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JRendererLabel) co).setText(dLink.getPlugin().getSessionInfo());
                ((JRendererLabel) co).setIcon(dLink.getPlugin().getHosterIcon());
                ((JRendererLabel) co).setBorder(null);
                return co;
            } else {
                value = dLink.getPlugin().getHost();
                co = getDefaultTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                ((JRendererLabel) co).setBorder(null);
            }
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
         * LinkGrabber hat nur null(Header) oder ne
         * ArrayList(LinkGrabberFilePackage)
         */
        if (obj == null || obj instanceof ArrayList<?>) return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void sort(Object obj, final boolean sortingToggle) {
        ArrayList<LinkGrabberFilePackage> packages = null;
        synchronized (LinkGrabberController.ControllerLock) {
            synchronized (LinkGrabberController.getInstance().getPackages()) {
                packages = LinkGrabberController.getInstance().getPackages();
                if (obj == null && packages.size() > 1) {
                    /* header, sortiere die packages nach namen */
                    Collections.sort(packages, new Comparator<LinkGrabberFilePackage>() {
                        public int compare(LinkGrabberFilePackage a, LinkGrabberFilePackage b) {
                            LinkGrabberFilePackage aa = a;
                            LinkGrabberFilePackage bb = b;
                            if (sortingToggle) {
                                aa = b;
                                bb = a;
                            }
                            return aa.getHoster().compareToIgnoreCase(bb.getHoster());
                        }
                    });
                } else {
                    /*
                     * in obj stecken alle selektierten packages, sortiere die
                     * links nach namen
                     */
                    if (obj != null) packages = (ArrayList<LinkGrabberFilePackage>) obj;
                    for (LinkGrabberFilePackage fp : packages) {
                        Collections.sort(fp.getDownloadLinks(), new Comparator<DownloadLink>() {
                            public int compare(DownloadLink a, DownloadLink b) {
                                DownloadLink aa = b;
                                DownloadLink bb = a;
                                if (sortingToggle) {
                                    aa = a;
                                    bb = b;
                                }
                                return aa.getHost().compareToIgnoreCase(bb.getHost());
                            }
                        });
                    }
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
