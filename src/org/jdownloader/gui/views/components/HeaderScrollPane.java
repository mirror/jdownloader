package org.jdownloader.gui.views.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import jd.gui.swing.laf.LookAndFeelController;

public class HeaderScrollPane extends JScrollPane {
    private Color headerColor;
    private Color headerlineColor;

    public HeaderScrollPane(JComponent sidebar) {
        super(sidebar);
        int c = LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderColor();

        headerColor = new Color(c);

        headerlineColor = new Color(LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderLineColor());
    }

    protected void paintComponent(Graphics g) {

        super.paintComponent(g);
    }

    protected void paintBorder(Graphics g) {
        if (getColumnHeader() != null) {
            g.setColor(headerColor);
            Insets in = getBorder().getBorderInsets(this);
            g.fillRect(1, 1, getWidth() - 2, getColumnHeader().getHeight() + in.top);
            g.setColor(headerlineColor);
            g.drawLine(1, getColumnHeader().getHeight() + in.top + 1, getWidth() - 2, getColumnHeader().getHeight() + in.top + 1);
        }
        super.paintBorder(g);

    }

}
