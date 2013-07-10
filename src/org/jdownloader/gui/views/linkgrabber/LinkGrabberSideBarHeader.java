package org.jdownloader.gui.views.linkgrabber;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;

import jd.gui.swing.laf.LAFOptions;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.translate._GUI;

public class LinkGrabberSideBarHeader extends MigPanel {

    public LinkGrabberSideBarHeader(LinkGrabberTable table) {
        super("ins 0 0 1 0", "[][grow,fill]", "[grow,fill]");

        // setBackground(Color.RED);
        // setOpaque(true);
        JLabel lbl = new JLabel(_GUI._.LinkGrabberSideBarHeader_LinkGrabberSideBarHeader());

        add(lbl, "height 17!,gapleft 10");
        add(Box.createHorizontalGlue());
        setOpaque(true);
        SwingUtils.setOpaque(lbl, false);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LAFOptions.createColor(LookAndFeelController.getInstance().getLAFOptions().getColorForPanelHeaderLine())));

        setBackground(LAFOptions.createColor(LookAndFeelController.getInstance().getLAFOptions().getColorForPanelHeaderBackground()));

    }

}
