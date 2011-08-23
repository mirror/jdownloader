package org.jdownloader.gui.views.linkgrabber;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import org.appwork.app.gui.MigPanel;

public abstract class Checkbox extends MigPanel implements ActionListener {

    public void actionPerformed(ActionEvent e) {
        setSelected(checkbox.isSelected());
    }

    protected abstract void setSelected(boolean selected);

    private JCheckBox checkbox;
    private JLabel    lbl;

    public Checkbox(String label, String tt) {
        super("ins 0", "[grow,fill][]8[]4", "[]");
        checkbox = new JCheckBox();
        lbl = new JLabel(label);
        this.setToolTipText(tt);
        add(Box.createHorizontalGlue());
        add(lbl);
        add(checkbox);
        checkbox.setSelected(isSelected());
        checkbox.addActionListener(this);

    }

    protected abstract boolean isSelected();

}
