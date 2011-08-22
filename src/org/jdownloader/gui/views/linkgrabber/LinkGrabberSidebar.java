package org.jdownloader.gui.views.linkgrabber;

import java.awt.Color;
import java.awt.Graphics;

import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.app.gui.MigPanel;

public class LinkGrabberSidebar extends MigPanel {

    private LinkGrabberTable table;

    public LinkGrabberSidebar(LinkGrabberTable table) {
        super("ins 0,wrap 1", "[grow,fill]", "[][grow,fill]");
        this.table = table;
        int c = LookAndFeelController.getInstance().getLAFOptions().getPanelBackgroundColor();
        setOpaque(false);
        setBackground(null);
        if (c >= 0) {
            setBackground(new Color(c));
            setOpaque(true);
        }

        // TableHeaderUI ds = table.getTableHeader().getUI();
        // JTableHeader th = table.getTableHeader();

        // setBorder(new JTextField().getBorder());
        // header = new LinkGrabberSideBarHeader();
        // scrollPane = new JScrollPane();
        // scrollPane.setBorder(null);
        // add(header, " height 20!");

        // add(scrollPane);

    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

    public void paint(Graphics g) {
        super.paint(g);
    }

    protected void paintBorder(Graphics g) {
        super.paintBorder(g);
    }
}
