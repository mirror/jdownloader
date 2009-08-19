package jd.gui.swing.jdgui.views.linkgrabberview.Columns;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.border.Border;

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
        return false;
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
        // TODO Auto-generated method stub

    }

}
