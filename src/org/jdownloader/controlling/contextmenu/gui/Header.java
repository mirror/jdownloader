package org.jdownloader.controlling.contextmenu.gui;

import java.awt.Graphics;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.table.JTableHeader;

import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.SwingUtils;

public class Header extends MigPanel {

    private JTableHeader tableHeader;

    @Override
    public void paint(Graphics g) {
        tableHeader.setSize(getSize());
        tableHeader.paint(g);
        ;
        super.paint(g);
    }

    public Header(String layoutManager, Icon icon) {
        super("ins 0 0 1 0", "[]2[][][grow,fill][]0", "[grow,fill]");
        tableHeader = new JTableHeader();
        // setBackground(Color.RED);
        // setOpaque(true);

        JLabel lbl = SwingUtils.toBold(new JLabel(layoutManager));

        add(new JLabel(icon), "gapleft 1");
        add(lbl, "height 17!");

        add(Box.createHorizontalGlue());
        setOpaque(false);
        SwingUtils.setOpaque(lbl, false);

    }
}
