package jd.gui.swing.jdgui.views.linkgrabberview.Columns;

import java.awt.Component;

import javax.swing.JTable;

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

}
