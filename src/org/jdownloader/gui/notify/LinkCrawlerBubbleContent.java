package org.jdownloader.gui.notify;

import java.awt.Component;

import jd.gui.swing.jdgui.components.IconedProcessIndicator;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtTextArea;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class LinkCrawlerBubbleContent extends MigPanel {

    private IconedProcessIndicator linkGrabberIndicator;
    private ExtTextArea            ret;

    public LinkCrawlerBubbleContent() {

        super("ins 0,wrap 2", "[][grow,fill]", "[grow,fill]");

        linkGrabberIndicator = new IconedProcessIndicator(NewTheme.I().getIcon("linkgrabber", 20));

        linkGrabberIndicator.setTitle(_GUI._.StatusBarImpl_initGUI_linkgrabber());

        linkGrabberIndicator.setIndeterminate(true);
        linkGrabberIndicator.setEnabled(false);
        add(linkGrabberIndicator, "width 32!,height 32!,pushx,growx,pushy,growy");
        add(getMessage(""));
        SwingUtils.setOpaque(this, false);
    }

    public void setText(String text) {
        ret.setText(text);
    }

    private Component getMessage(String text) {
        ret = new ExtTextArea();
        SwingUtils.setOpaque(ret, false);
        ret.setText(text);
        ret.setLabelMode(true);
        return ret;
    }

    public void crawlerStopped() {
        linkGrabberIndicator.setIndeterminate(false);
        linkGrabberIndicator.setMaximum(100);
        linkGrabberIndicator.setValue(100);
    }

}
