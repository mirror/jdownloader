

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import jd.gui.skins.simple.SimpleGUI;

public class JDCheckBoxListRenderer extends JCheckBox implements ActionListener, ListCellRenderer {

    private static final long serialVersionUID = 6771031916814464267L;

    public static void main(String[] args) {
        new JDCheckBoxListRenderer().showGUI();
    }

    private void showGUI() {
        JComboBox cmb = new JComboBox(new JDCheckBoxContainer[] { new JDCheckBoxContainer("test1", true), new JDCheckBoxContainer("test2", true), new JDCheckBoxContainer("test3", false) });
        cmb.setRenderer(new JDCheckBoxListRenderer());
        cmb.addActionListener(this);
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.getContentPane().add(cmb);
        f.pack();
        f.setVisible(true);
    }

    public JDCheckBoxListRenderer() {
        if (SimpleGUI.isSubstance()) {
            setOpaque(false);
        } else {
            setOpaque(true);
        }

        setHorizontalTextPosition(JCheckBox.RIGHT);
        addActionListener(this);
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (SimpleGUI.isSubstance()) {
            if (isSelected) {
                setOpaque(true);
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setOpaque(false);
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
        } else {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
        }

        setText(((JDCheckBoxContainer) value).getName());
        setSelected(((JDCheckBoxContainer) value).isSelected());
        setFont(list.getFont());

        return this;
    }

    public void actionPerformed(ActionEvent e) {
        JComboBox cmb = (JComboBox) e.getSource();
        JDCheckBoxContainer cont = (JDCheckBoxContainer) cmb.getSelectedItem();
        setSelected(cont.invertSelection());
    }

    public static class JDCheckBoxContainer {

        private String name;

        private boolean selected;

        public JDCheckBoxContainer(String name, boolean selected) {
            this.name = name;
            this.selected = selected;
        }

        public String getName() {
            return name;
        }

        public boolean isSelected() {
            return selected;
        }

        public boolean invertSelection() {
            return (selected = !selected);
        }

    }

}