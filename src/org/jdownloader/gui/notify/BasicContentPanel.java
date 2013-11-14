package org.jdownloader.gui.notify;

import java.awt.Component;

import javax.swing.ImageIcon;

import net.miginfocom.swing.MigLayout;

import org.appwork.swing.components.ExtTextArea;
import org.appwork.utils.swing.SwingUtils;

public class BasicContentPanel extends AbstractBubbleContentPanel {

    public BasicContentPanel(String text, ImageIcon icon) {
        super(icon);
        setLayout(new MigLayout("ins 0,wrap 2", "[][grow,fill]", "[]"));

        add(getMessage(text), "aligny center");
        SwingUtils.setOpaque(this, false);
    }

    private Component getMessage(String text) {
        ExtTextArea ret = new ExtTextArea();
        SwingUtils.setOpaque(ret, false);
        ret.setText(text);
        ret.setLabelMode(true);

        return ret;
    }

}
