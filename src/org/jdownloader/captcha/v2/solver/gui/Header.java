package org.jdownloader.captcha.v2.solver.gui;

import java.awt.Graphics;

import javax.swing.table.JTableHeader;

import org.appwork.swing.MigPanel;

public class Header extends MigPanel {

    public Header(String glob, String col, String row) {
        super(glob, col, row);
        tableHeader = new JTableHeader();
    }

    private JTableHeader tableHeader;

    @Override
    public void paint(Graphics g) {
        tableHeader.setSize(getSize());
        tableHeader.paint(g);
        ;
        super.paint(g);
    }

}
