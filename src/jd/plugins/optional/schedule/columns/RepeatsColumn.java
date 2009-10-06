package jd.plugins.optional.schedule.columns;

import java.awt.Component;

import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.plugins.optional.schedule.Actions;
import jd.utils.locale.JDL;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class RepeatsColumn extends JDTableColumn {

    private static final long serialVersionUID = 5634248167278175584L;
    private JRendererLabel jlr;

    public RepeatsColumn(String name, JDTableModel table) {
        super(name, table);
        jlr = new JRendererLabel();
        jlr.setBorder(null);
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
        switch (((Actions) value).getRepeat()) {
        case 0:
            jlr.setText(JDL.L("jd.plugins.optional.schedule.MainGui.MyTableModel.add.once", "Only once"));
            break;
        case 60:
            jlr.setText(JDL.L("jd.plugins.optional.schedule.MainGui.MyTableModel.add.hourly", "Hourly"));
            break;
        case 1440:
            jlr.setText(JDL.L("jd.plugins.optional.schedule.MainGui.MyTableModel.add.daily", "Daily"));
            break;
        case 10080:
            jlr.setText(JDL.L("jd.plugins.optional.schedule.MainGui.MyTableModel.add.weekly", "Weekly"));
            break;
        default:
            int hour = ((Actions) value).getRepeat() / 60;
            jlr.setText(JDL.LF("jd.plugins.optional.schedule.MainGui.MyTableModel.add.interval", "Interval: %sh %sm", hour, ((Actions) value).getRepeat() - (hour * 60)));
        }
        return jlr;
    }

    @Override
    public void setValue(Object value, Object object) {
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

}
