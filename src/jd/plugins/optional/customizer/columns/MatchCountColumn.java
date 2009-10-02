package jd.plugins.optional.customizer.columns;

import java.awt.Color;
import java.awt.Component;

import javax.swing.SwingConstants;

import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.plugins.optional.customizer.CustomizeSetting;
import jd.plugins.optional.customizer.CustomizerTableModel;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class MatchCountColumn extends JDTableColumn {

    private static final long serialVersionUID = 4030301646643222509L;
    private JRendererLabel jlr;

    public MatchCountColumn(String name, JDTableModel table) {
        super(name, table);
        jlr = new JRendererLabel();
        jlr.setBorder(null);
        jlr.setHorizontalAlignment(SwingConstants.RIGHT);
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
        return ((CustomizeSetting) obj).isEnabled();
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
        jlr.setText(((CustomizeSetting) value).getMatchCount());
        return jlr;
    }

    @Override
    public void setValue(Object value, Object object) {
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

    @Override
    public void postprocessCell(Component c, JDTableModel table, Object value, boolean isSelected, int row, int column) {
        if (((CustomizeSetting) value).matches(((CustomizerTableModel) table).getJDTable().getGui().getTestText())) {
            c.setBackground(new Color(204, 255, 170));
        }
    }

}
