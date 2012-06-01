package org.jdownloader.gui.views.linkgrabber.addlinksdialog;

import java.awt.Color;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.UIManager;

import org.appwork.app.gui.MigPanel;

public abstract class Header extends MigPanel implements ActionListener {

    private JCheckBox checkbox;

    public Header(ImageIcon fileIcon, String label) {

        super("ins 0", "[][][][grow,fill][]", "[]");
        Color helpColor = (Color) UIManager.get("TextField.disabledForeground");
        if (helpColor == null) {
            helpColor = Color.LIGHT_GRAY;
        }
        add(new JLabel(fileIcon), "width 32!,height 32!");
        add(new JSeparator(), "width 5!");
        JLabel lbl = new JLabel(label);
        add(lbl);
        lbl.setForeground(helpColor);
        // SwingUtils.toBold(lbl);
        add(new JSeparator());
        checkbox = new JCheckBox();
        checkbox.addActionListener(this);
        add(checkbox);
    }

    public void setSelected(boolean b) {
        checkbox.setSelected(b);
    }

    public boolean isSelected() {
        return checkbox.isSelected();
    }

}
