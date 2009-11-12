package jd.gui.swing.components.table;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTextField;

public abstract class JDTextEditorTableColumn extends JDTextTableColumn implements ActionListener {

    private static final long serialVersionUID = 1859257712113327417L;
    private JTextField text;

    public JDTextEditorTableColumn(String name, JDTableModel table) {
        super(name, table);

        text = new JTextField();
        prepareTableCellEditorComponent(text);

        setClickstoEdit(2);
    }

    /**
     * Should be overwritten to prepare the componente for the TableCellEditor
     * (e.g. setting tooltips)
     */
    protected void prepareTableCellEditorComponent(JTextField text) {
    }

    protected abstract void setStringValue(String value, Object object);

    @Override
    public final Object getCellEditorValue() {
        return text.getText();
    }

    @Override
    public boolean isEditable(Object obj) {
        return true;
    }

    @Override
    public final Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
        text.removeActionListener(this);
        text.setText(getStringValue(value));
        text.addActionListener(this);
        return text;
    }

    @Override
    public final void setValue(Object value, Object object) {
        setStringValue(value.toString(), object);
    }

    public void actionPerformed(ActionEvent e) {
        text.removeActionListener(this);
        this.fireEditingStopped();
    }

}
