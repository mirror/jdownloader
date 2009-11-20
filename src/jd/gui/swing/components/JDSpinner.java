package jd.gui.swing.components;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;

import net.miginfocom.swing.MigLayout;

public class JDSpinner extends JPanel {
    private static final long serialVersionUID = 8892482065686899916L;

    private JLabel lbl;

    private JSpinner spn;

    public JDSpinner(String label) {
        this(label, "w 70!, h 20!");
    }

    public JDSpinner(String label, String layout) {
        super(new MigLayout("ins 0", "[][grow,fill]"));

        lbl = new JLabel(label) {
            private static final long serialVersionUID = 8794670984465489135L;

            @Override
            public Point getToolTipLocation(MouseEvent e) {
                return new Point(0, -25);
            }
        };
        spn = new JSpinner();

        add(lbl);
        add(spn, layout);
    }

    public JSpinner getSpinner() {
        return spn;
    }

    public JLabel getLabel() {
        return lbl;
    }

    public void setText(String s) {
        lbl.setText(s);
    }

    public void setValue(Integer i) {
        spn.setValue(i);
    }

    public Integer getValue() {
        return (Integer) spn.getValue();
    }

    public void setColor(Color c) {
        lbl.setForeground(c);
        ((DefaultEditor) spn.getEditor()).getTextField().setForeground(c);
    }

    @Override
    public void setToolTipText(String s) {
        lbl.setToolTipText(s);
    }

}