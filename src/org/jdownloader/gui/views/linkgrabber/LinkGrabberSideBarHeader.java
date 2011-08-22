package org.jdownloader.gui.views.linkgrabber;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;

import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.app.gui.MigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class LinkGrabberSideBarHeader extends MigPanel {

    public LinkGrabberSideBarHeader(LinkGrabberTable table) {
        super("ins 2 0 0 0,wrap ", "[][grow,fill][]", "[grow,fill]");

        // setBackground(Color.RED);
        // setOpaque(true);
        JLabel lbl = new JLabel(_GUI._.LinkGrabberSideBarHeader_LinkGrabberSideBarHeader());
        add(lbl, "height 18!");
        add(Box.createHorizontalGlue());
        JButton bt = new JButton(NewTheme.I().getIcon("exttable/columnButton", -1));
        bt.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderLineColor())));
        // bt.setBorderPainted(false);
        bt.setContentAreaFilled(false);
        add(bt, "width 20!,gaptop 2");
    }

    @Override
    public Insets getInsets() {
        Insets ret = super.getInsets();
        return new Insets(0, 0, 0, 0);
    }

    @Override
    public Insets getInsets(Insets insets) {
        return super.getInsets(insets);
    }

    @Override
    public Rectangle getBounds(Rectangle rv) {
        return super.getBounds(rv);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
    }

}
