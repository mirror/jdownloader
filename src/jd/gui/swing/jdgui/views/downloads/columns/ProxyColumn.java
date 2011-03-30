package jd.gui.swing.jdgui.views.downloads.columns;

import java.awt.Component;

import jd.controlling.SingleDownloadController;
import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class ProxyColumn extends JDTableColumn {

    private static final long serialVersionUID = 2228210790952050305L;
    private DownloadLink      dLink;
    private FilePackage       fp;
    private JRendererLabel    jlr;

    public ProxyColumn(String name, JDTableModel table) {
        super(name, table);
        jlr = new JRendererLabel();
        jlr.setBorder(null);
    }

    @Override
    public boolean isEditable(Object obj) {
        return false;
    }

    @Override
    public boolean defaultVisible() {
        return false;
    }

    @Override
    public Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
        return null;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof DownloadLink) {
            dLink = (DownloadLink) value;
            SingleDownloadController dlc = dLink.getDownloadLinkController();
            if (dlc != null) {
                jlr.setText("" + dlc.getCurrentProxy());
                jlr.setToolTipText("" + dlc.getCurrentProxy());
            }
        }
        jlr.setText(null);
        jlr.setToolTipText(null);
        return jlr;
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
    public boolean isEnabled(Object obj) {
        if (obj == null) return false;
        if (obj instanceof DownloadLink) return ((DownloadLink) obj).isEnabled();
        return true;
    }

}
