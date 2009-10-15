package jd.plugins.optional.langfileeditor.columns;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTextField;

import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.plugins.optional.langfileeditor.KeyInfo;
import jd.plugins.optional.langfileeditor.LFETableModel;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class EnglishColumn extends JDTableColumn implements ActionListener {

    private static final long serialVersionUID = -2305836770033923728L;
    private JRendererLabel jlr;
    private JTextField text;

    public EnglishColumn(String name, JDTableModel table) {
        super(name, table);
        jlr = new JRendererLabel();
        jlr.setBorder(null);
        text = new JTextField();
    }

    @Override
    public Object getCellEditorValue() {
        return text.getText();
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
        return true;
    }

    @Override
    public Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
        text.removeActionListener(this);
        text.setText(((KeyInfo) value).getEnglish());
        text.addActionListener(this);
        return text;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        jlr.setText(((KeyInfo) value).getEnglish());
        return jlr;
    }

    @Override
    public void setValue(Object value, Object object) {
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
        ((LFETableModel) getJDTableModel()).setSorting(LFETableModel.SORT_ENGLISH, sortingToggle);
    }

    public void actionPerformed(ActionEvent e) {
        text.removeActionListener(this);
        this.fireEditingStopped();
    }

}
