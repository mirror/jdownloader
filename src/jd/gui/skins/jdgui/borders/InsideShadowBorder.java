package jd.gui.skins.jdgui.borders;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

import javax.swing.border.AbstractBorder;

public class InsideShadowBorder extends AbstractBorder {

    private static final long serialVersionUID = -2197110609454639482L;
    private Insets insets;

    public InsideShadowBorder(int top, int left, int bottom, int right) {
        this.insets = new Insets(top, left, bottom, right);
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
        Graphics2D g2D = (Graphics2D) g;
        Composite composite = g2D.getComposite();

        // paint the shadow

        g2D.setColor(new Color(0, 16, 0));
        float alphaValue = 0.4f;
        for (int i = 0; i < insets.top; i++) {
            AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaValue);
            g2D.setComposite(alpha);
            g.drawLine(x + i, y + i, x + w - i * 2, y + i);

            alphaValue -= (alphaValue / 2);
        }

        g2D.setComposite(composite);

    }

    public Insets getBorderInsets(Component c) {
        return insets;
    }
}
