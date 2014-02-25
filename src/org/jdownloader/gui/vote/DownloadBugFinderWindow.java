package org.jdownloader.gui.vote;

import java.awt.Component;
import java.awt.Point;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import jd.gui.swing.jdgui.AbstractBugFinderWindow;
import jd.gui.swing.jdgui.DirectFeedback;
import jd.gui.swing.jdgui.DirectFeedbackInterface;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.MigPanel;
import org.jdownloader.gui.translate._GUI;

public class DownloadBugFinderWindow extends AbstractBugFinderWindow {

    @Override
    public String getTitle() {
        return _GUI._.DownloadBugFinderWindow_getTitle();
    }

    @Override
    public DirectFeedback handleComponent(Component c, Point mouse, MigPanel actualContent) {
        if (c instanceof DirectFeedbackInterface) {
            Point p = new Point(mouse.x, mouse.y);
            SwingUtilities.convertPointFromScreen(p, c);
            return ((DirectFeedbackInterface) c).layoutDirectFeedback(p, actualContent, this);

        }
        return null;
    }

    @Override
    public void layoutDefaultPanel(MigPanel actualContent) {
        actualContent.removeAll();
        setIconVisible(true);
        actualContent.setLayout(new MigLayout("ins 0", "[]", "[]"));
        // if (positive) {
        // actualContent.add(new JLabel(_GUI._.VoteFinderWindow_VoteFinderWindow_msg_positive()));
        // } else {
        // actualContent.add(new JLabel(_GUI._.VoteFinderWindow_VoteFinderWindow_msg_negative()));
        // }
        actualContent.add(new JLabel(_GUI._.DownloadBugFinderWindow_default()), "");
    }

}
