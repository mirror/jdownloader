package jd.plugins.optional.langfileeditor.columns;

import java.awt.Component;

import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.plugins.optional.langfileeditor.KeyInfo;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class TypeColumn extends JDTableColumn {

    private static final long serialVersionUID = 4030301646643222509L;
    private JRendererLabel jlr;

    public TypeColumn(String name, JDTableModel table) {
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

    private static String getType(KeyInfo keyInfo) {
        if (keyInfo.isMissing()) return "M";
        if (keyInfo.getSource() == null || keyInfo.getSource().equals("")) return "O";
        if (keyInfo.getLanguage() != null && keyInfo.getLanguage().trim().length() > 0) return "D";
        return " ";
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        jlr.setText(getType(((KeyInfo) value)));
        return jlr;
    }

    @Override
    public void setValue(Object value, Object object) {
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

}
