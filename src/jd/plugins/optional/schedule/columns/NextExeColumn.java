package jd.plugins.optional.schedule.columns;

import java.awt.Component;

import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.nutils.Formatter;
import jd.plugins.optional.schedule.Actions;
import jd.utils.locale.JDL;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class NextExeColumn extends JDTableColumn {

    private static final long serialVersionUID = -2945101320574207493L;
    private JRendererLabel jlr;
    private long nexttime = 0;

    public NextExeColumn(String name, JDTableModel table) {
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
        if (!((Actions) value).isEnabled()) {
            jlr.setText(JDL.L("jd.plugins.optional.schedule.disabled", "disabled"));
        } else {
            nexttime = ((Actions) value).getDate().getTime() - System.currentTimeMillis();
            if (nexttime < 0) {
                if (((Actions) value).getRepeat() == 0) {
                    jlr.setText(JDL.L("jd.plugins.optional.schedule.expired", "expired"));
                } else {
                    jlr.setText(JDL.L("jd.plugins.optional.schedule.wait", "wait a moment"));
                }
            } else {
                jlr.setText(Formatter.formatSeconds(nexttime / 1000, false));
            }
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
