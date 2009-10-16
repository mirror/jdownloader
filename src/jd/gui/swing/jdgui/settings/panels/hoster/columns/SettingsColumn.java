package jd.gui.swing.jdgui.settings.panels.hoster.columns;

import java.awt.Component;

import javax.swing.JCheckBox;

import jd.HostPluginWrapper;
import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;

import org.jdesktop.swingx.renderer.JRendererCheckBox;

public class SettingsColumn extends JDTableColumn {

    private static final long serialVersionUID = 9164858843215840133L;
    private JRendererCheckBox boolrend;

    public SettingsColumn(String name, JDTableModel table) {
        super(name, table);
        boolrend = new JRendererCheckBox();
        boolrend.setHorizontalAlignment(JCheckBox.CENTER);
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isEditable(Object obj) {
        return false;
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
        return null;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        boolrend.setSelected(((HostPluginWrapper) value).hasConfig());
        return boolrend;
    }

    @Override
    public void setValue(Object value, Object object) {
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

}
