package jd.plugins.optional.customizer.columns;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Pattern;

import javax.swing.JTextField;

import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.plugins.optional.customizer.CustomizeSetting;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class RegexColumn extends JDTableColumn implements ActionListener {

    private static final long serialVersionUID = -2305836770033923728L;
    private JRendererLabel jlr;
    private JTextField text;

    public RegexColumn(String name, JDTableModel table) {
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
        return isEnabled(obj);
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
        text.removeActionListener(this);
        text.setText(((CustomizeSetting) value).getRegex());
        text.addActionListener(this);
        return text;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        jlr.setText(((CustomizeSetting) value).getRegex());
        return jlr;
    }

    @Override
    public void setValue(Object value, Object object) {
        ((CustomizeSetting) object).setRegex((String) value);
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == text) {
            text.removeActionListener(this);
            this.fireEditingStopped();
        }
    }

    @Override
    public void postprocessCell(Component c, JDTableModel table, Object value, boolean isSelected, int row, int column) {
        try {
            Pattern.compile(((CustomizeSetting) value).getRegex());
        } catch (Exception e) {
            c.setBackground(new Color(221, 34, 34));
        }
    }

}
