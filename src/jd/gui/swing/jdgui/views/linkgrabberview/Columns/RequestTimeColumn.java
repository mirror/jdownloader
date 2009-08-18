package jd.gui.swing.jdgui.views.linkgrabberview.Columns;

import java.awt.Component;

import javax.swing.JTable;

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

}
