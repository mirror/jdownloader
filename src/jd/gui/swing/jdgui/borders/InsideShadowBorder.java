package jd.gui.swing.jdgui.borders;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

import javax.swing.border.AbstractBorder;
/**
 * Paint a Shadowborder
 * @author Coalado
 *
 */
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
            g.drawLine(x + Math.min(insets.left, i), y + i, x + w - Math.min(insets.left, i) - Math.min(insets.right, i), y + i);

            alphaValue -= (alphaValue / 2);
        }
        g2D.setColor(new Color(0, 16, 0));
        alphaValue = 0.4f;
        for (int i = 0; i < insets.bottom; i++) {
            AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaValue);
            g2D.setComposite(alpha);
            g.drawLine(x + Math.min(insets.left, i), y + h - i - 1, x + w - Math.min(insets.left, i) - Math.min(insets.right, i), y + h - i - 1);

            alphaValue -= (alphaValue / 2);
        }

        g2D.setColor(new Color(0, 16, 0));
        alphaValue = 0.4f;
        for (int i = 0; i < insets.right; i++) {
            AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaValue);
            g2D.setComposite(alpha);
            g.drawLine(w - i - 1, y + Math.min(insets.top, i), w - i - 1, y + h - Math.min(insets.top, i) - Math.min(insets.bottom, i));

            alphaValue -= (alphaValue / 2);
        }

        g2D.setColor(new Color(0, 16, 0));
        alphaValue = 0.4f;
        for (int i = 0; i < insets.left; i++) {
            AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaValue);
            g2D.setComposite(alpha);
            g.drawLine(x + i, y + Math.min(insets.top, i), x + i, y + h - Math.min(insets.top, i) - Math.min(insets.bottom, i));

            alphaValue -= (alphaValue / 2);
        }

        g2D.setComposite(composite);

    }

    public Insets getBorderInsets(Component c) {
        return insets;
    }
}
