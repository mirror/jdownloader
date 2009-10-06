package jd.plugins.optional.schedule.columns;

import java.awt.Component;
import java.text.SimpleDateFormat;

import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.plugins.optional.schedule.Actions;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class DateColumn extends JDTableColumn {

    private static final long serialVersionUID = -2442203664468475171L;
    private JRendererLabel jlr;
    private SimpleDateFormat date;

    public DateColumn(String name, JDTableModel table) {
        super(name, table);
        jlr = new JRendererLabel();
        jlr.setBorder(null);
        date = new SimpleDateFormat("dd.MM.yyyy");
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
        jlr.setText(date.format(((Actions) value).getDate()));
        return jlr;
    }

    @Override
    public void setValue(Object value, Object object) {
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

}
