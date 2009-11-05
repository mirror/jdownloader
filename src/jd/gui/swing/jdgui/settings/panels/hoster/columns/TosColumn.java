package jd.gui.swing.jdgui.settings.panels.hoster.columns;

import java.awt.Component;
import java.net.URL;

import javax.swing.SwingConstants;

import jd.HostPluginWrapper;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class TosColumn extends JDTableColumn {

    private static final long serialVersionUID = 4600633634774184026L;
    private JRendererLabel jlr;

    public TosColumn(String name, JDTableModel table) {
        super(name, table);
        this.setClickstoEdit(1);
        jlr = new JRendererLabel();
        jlr.setBorder(null);
        jlr.setHorizontalTextPosition(SwingConstants.CENTER);
        jlr.setText("Read TOS");
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isEditable(Object obj) {
        return true;
    }

    @Override
    public boolean isEnabled(Object obj) {
        return true;
    }

    @Override
    public boolean isSortable(Object obj) {
        return false;
    }

    @Override
    public Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
        try {
            JLink.openURL((new URL(((HostPluginWrapper) value).getPlugin().getAGBLink())));
        } catch (Exception e) {
        }
        return jlr;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return jlr;
    }

    @Override
    public void setValue(Object value, Object object) {
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

}