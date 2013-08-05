package org.jdownloader.gui.jdtrayicon;

import java.awt.Component;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.RoundRectangle2D;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.appwork.swing.ExtJWindow;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextArea;
import org.appwork.utils.swing.SwingUtils;

import com.sun.awt.AWTUtilities;

public class Notify extends ExtJWindow implements ComponentListener {

    private MigPanel content;

    public Notify(String caption, String text, ImageIcon icon) {
        super();
        addComponentListener(this);
        content = new MigPanel("ins 5", "[][grow,fill]", "[][grow,fill]");
        setContentPane(content);

        content.add(getIconPanel(icon), "spany");
        content.add(getCaption(caption));
        content.add(getMessage(text));
        AWTUtilities.setWindowOpacity(this, 0.75f);

    }

    private Component getMessage(String text) {
        ExtTextArea ret = new ExtTextArea();
        ret.setText(text);
        ret.setLabelMode(true);
        return ret;
    }

    private Component getCaption(String caption) {

        return SwingUtils.toBold(new JLabel(caption));
    }

    private Component getIconPanel(ImageIcon icon) {

        return new JLabel(icon);
    }

    @Override
    public void componentResized(ComponentEvent e) {
        setShape(new RoundRectangle2D.Float(10, 10, getWidth() - 20, getHeight() - 20, 20, 20));
    }

    @Override
    public void componentMoved(ComponentEvent e) {
    }

    @Override
    public void componentShown(ComponentEvent e) {
    }

    @Override
    public void componentHidden(ComponentEvent e) {
    }

}
