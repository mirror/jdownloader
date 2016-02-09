package org.jdownloader.gui.views.linkgrabber;

import java.awt.Graphics;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.table.JTableHeader;

import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

public class LinkGrabberSideBarHeader extends MigPanel {

    private JTableHeader tableHeader;

    @Override
    public void paint(Graphics g) {
        tableHeader.setSize(getSize());
        tableHeader.paint(g);
        super.paint(g);
    }

    public LinkGrabberSideBarHeader(LinkGrabberTable table) {
        super("ins " + LAFOptions.getInstance().getExtension().customizePanelHeaderInsets(), "[][grow,fill]", "[grow,fill]");
        tableHeader = new JTableHeader();
        // setBackground(Color.RED);
        // setOpaque(true);
        JLabel lbl = new JLabel(_GUI.T.LinkGrabberSideBarHeader_LinkGrabberSideBarHeader());

        add(lbl, "height 17!,gapleft 10");
        add(Box.createHorizontalGlue());
        setOpaque(false);
        SwingUtils.setOpaque(lbl, false);

        LAFOptions.getInstance().getExtension().customizeLinkgrabberSidebarHeader(lbl, this);
    }

}
