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
 * 
 * @author Coalado
 * 
 */
public class InsideShadowBorder extends AbstractBorder {

    private static final long serialVersionUID = -2197110609454639482L;
    private Insets insets;
    private Insets borderInsets;

    public InsideShadowBorder(int top, int left, int bottom, int right) {
        this.insets = new Insets(top, left, bottom, right);
        borderInsets = new Insets(0, 0, 0, 0);
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
            g.drawLine(x + Math.min(insets.left, i) + borderInsets.left, y + i+borderInsets.top, x + w - Math.min(insets.left, i) - Math.min(insets.right, i) - borderInsets.right, y + i+borderInsets.top);

            alphaValue -= (alphaValue / 2);
        }
        g2D.setColor(new Color(0, 16, 0));
        alphaValue = 0.4f;
        for (int i = 0; i < insets.bottom; i++) {
            AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaValue);
            g2D.setComposite(alpha);
            g.drawLine(x + Math.min(insets.left, i)+borderInsets.left, y + h - i - 1-borderInsets.bottom, x + w - Math.min(insets.left, i)-borderInsets.right - Math.min(insets.right, i), y + h - i - 1-borderInsets.bottom);

            alphaValue -= (alphaValue / 2);
        }

        g2D.setColor(new Color(0, 16, 0));
        alphaValue = 0.4f;
        for (int i = 0; i < insets.right; i++) {
            AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaValue);
            g2D.setComposite(alpha);
            g.drawLine(w - i - 1-borderInsets.right, y + Math.min(insets.top, i)+borderInsets.top, w - i - 1-borderInsets.right, y + h - Math.min(insets.top, i) - Math.min(insets.bottom, i)-borderInsets.bottom);

            alphaValue -= (alphaValue / 2);
        }

        g2D.setColor(new Color(0, 16, 0));
        alphaValue = 0.4f;
        for (int i = 0; i < insets.left; i++) {
            AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaValue);
            g2D.setComposite(alpha);
            g.drawLine(x + i+borderInsets.left, y + Math.min(insets.top, i)+borderInsets.top, x + i+borderInsets.left, y + h - Math.min(insets.top, i) - Math.min(insets.bottom, i)-borderInsets.bottom);

            alphaValue -= (alphaValue / 2);
        }

        g2D.setComposite(composite);

    }

    public Insets getBorderInsets(Component c) {
        return insets;
    }

    /**
     * Sets insets for the border itself
     * 
     * @param i
     * @param j
     * @param k
     * @param l
     */
    public void setBorderInsets(int i, int j, int k, int l) {
        this.borderInsets = new Insets(i, j, k, l);

    }
}
