package jd.gui.swing.components.table;

import java.awt.Component;

import org.jdesktop.swingx.renderer.JRendererLabel;

public abstract class JDTextTableColumn extends JDTableColumn {

    private static final long serialVersionUID = 2114805529462086691L;
    private JRendererLabel jlr;

    public JDTextTableColumn(String name, JDTableModel table) {
        super(name, table);

        jlr = new JRendererLabel();
        jlr.setBorder(null);
        prepareTableCellRendererComponent(jlr);
    }

    /**
     * Should be overwritten to prepare the componente for the TableCellRenderer
     * (e.g. setting tooltips)
     */
    protected void prepareTableCellRendererComponent(JRendererLabel jlr) {
    }

    protected abstract String getStringValue(Object value);

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isEditable(Object obj) {
        return false;
    }

    @Override
    public Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
        return null;
    }

    @Override
    public final Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        String text = getStringValue(value);
        jlr.setText(text);
        jlr.setToolTipText(text);
        return jlr;
    }

    @Override
    public void setValue(Object value, Object object) {
    }

}
