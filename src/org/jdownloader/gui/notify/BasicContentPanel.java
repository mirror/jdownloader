package org.jdownloader.gui.notify;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextArea;
import org.appwork.utils.swing.SwingUtils;

public class BasicContentPanel extends MigPanel {

    public BasicContentPanel(String text, ImageIcon icon) {

        super("ins 0,wrap 2", "[][grow,fill]", "[grow,fill]");

        add(getIconPanel(icon));
        add(getMessage(text));
        SwingUtils.setOpaque(this, false);
    }

    private Component getMessage(String text) {
        ExtTextArea ret = new ExtTextArea();
        SwingUtils.setOpaque(ret, false);
        ret.setText(text);
        ret.setLabelMode(true);
        return ret;
    }

    private Component getIconPanel(ImageIcon icon) {
        JLabel ret = new JLabel(icon);
        SwingUtils.setOpaque(ret, false);
        return ret;
    }

}
