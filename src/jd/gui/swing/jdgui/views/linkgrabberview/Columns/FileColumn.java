package jd.gui.swing.jdgui.views.linkgrabberview.Columns;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.border.Border;

import jd.controlling.LinkGrabberController;
import jd.gui.swing.components.JDTable.JDTableColumn;
import jd.gui.swing.components.JDTable.JDTableModel;
import jd.gui.swing.jdgui.views.linkgrabberview.LinkGrabberTable;
import jd.plugins.DownloadLink;
import jd.plugins.LinkGrabberFilePackage;
import jd.utils.JDTheme;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class FileColumn extends JDTableColumn {

    /**
     * 
     */
    private static final long serialVersionUID = 2228210790952050305L;
    private Component co;
    private DownloadLink dLink;
    private Border leftGap;
    private ImageIcon icon_fp_open;
    private ImageIcon icon_fp_open_error;
    private ImageIcon icon_fp_closed;
    private ImageIcon icon_fp_closed_error;
    private ImageIcon imgFileFailed;
    private LinkGrabberFilePackage fp;

    public FileColumn(String name, JDTableModel table) {
        super(name, table);
        leftGap = BorderFactory.createEmptyBorder(0, 30, 0, 0);
        icon_fp_open = JDTheme.II("gui.images.package_opened_tree", 16, 16);
        icon_fp_open_error = JDTheme.II("gui.images.package_open_error_tree", 16, 16);
        icon_fp_closed = JDTheme.II("gui.images.package_closed_tree", 16, 16);
        icon_fp_closed_error = JDTheme.II("gui.images.package_closed_error_tree", 16, 16);
        imgFileFailed = JDTheme.II("gui.images.offlinefile", 16, 16);
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
            co = getDefaultTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JRendererLabel) co).setText(fp.getName() + " [" + fp.size() + "]");
            if (fp.countFailedLinks(false) > 0) {
                ((JRendererLabel) co).setIcon(!fp.getBooleanProperty(LinkGrabberTable.PROPERTY_EXPANDED, false) ? icon_fp_closed_error : icon_fp_open_error);
            } else {
                ((JRendererLabel) co).setIcon(!fp.getBooleanProperty(LinkGrabberTable.PROPERTY_EXPANDED, false) ? icon_fp_closed : icon_fp_open);
            }
            ((JRendererLabel) co).setBorder(null);
        } else if (value instanceof DownloadLink) {
            dLink = (DownloadLink) value;
            co = getDefaultTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (dLink.isAvailabilityStatusChecked() && !dLink.isAvailable()) {
                ((JRendererLabel) co).setIcon(this.imgFileFailed);
            } else {
                ((JRendererLabel) co).setIcon(dLink.getIcon());
            }
            ((JRendererLabel) co).setText(dLink.getName());
            ((JRendererLabel) co).setBorder(leftGap);
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
                            return aa.getName().compareToIgnoreCase(bb.getName());
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
                                return aa.getName().compareToIgnoreCase(bb.getName());
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
