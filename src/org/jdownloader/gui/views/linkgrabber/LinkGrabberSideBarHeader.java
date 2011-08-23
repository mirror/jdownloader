package org.jdownloader.gui.views.linkgrabber;

import javax.swing.Box;
import javax.swing.JLabel;

import org.appwork.app.gui.MigPanel;
import org.jdownloader.gui.translate._GUI;

public class LinkGrabberSideBarHeader extends MigPanel {

    public LinkGrabberSideBarHeader(LinkGrabberTable table) {
        super("ins 0 0 1 0", "[][grow,fill]", "[grow,fill]");

        // setBackground(Color.RED);
        // setOpaque(true);
        JLabel lbl = new JLabel(_GUI._.LinkGrabberSideBarHeader_LinkGrabberSideBarHeader());

        add(lbl, "height 18!");
        add(Box.createHorizontalGlue());
        // add(bt, "width 20!");
    }

}
