//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

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
 */
public class InsideShadowBorder extends AbstractBorder {

    private static final long serialVersionUID = -2197110609454639482L;

    private final Insets insets;
    private Insets borderInsets;

    public InsideShadowBorder(int top, int left, int bottom, int right) {
        this.insets = new Insets(top, left, bottom, right);
        this.borderInsets = new Insets(0, 0, 0, 0);
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return insets;
    }

    /**
     * Sets insets for the border itself
     */
    public void setBorderInsets(int top, int left, int bottom, int right) {
        this.borderInsets = new Insets(top, left, bottom, right);
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
        Graphics2D g2D = (Graphics2D) g;
        Composite composite = g2D.getComposite();

        // paint the shadow

        g2D.setColor(new Color(0, 16, 0));
        float alphaValue = 0.4f;
        for (int i = 0; i < insets.top; i++) {
            AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaValue);
            g2D.setComposite(alpha);
            g.drawLine(x + Math.min(insets.left, i) + borderInsets.left, y + i + borderInsets.top, x + w - Math.min(insets.left, i) - Math.min(insets.right, i) - borderInsets.right, y + i + borderInsets.top);

            alphaValue -= (alphaValue / 2);
        }
        g2D.setColor(new Color(0, 16, 0));
        alphaValue = 0.4f;
        for (int i = 0; i < insets.bottom; i++) {
            AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaValue);
            g2D.setComposite(alpha);
            g.drawLine(x + Math.min(insets.left, i) + borderInsets.left, y + h - i - 1 - borderInsets.bottom, x + w - Math.min(insets.left, i) - borderInsets.right - Math.min(insets.right, i), y + h - i - 1 - borderInsets.bottom);

            alphaValue -= (alphaValue / 2);
        }

        g2D.setColor(new Color(0, 16, 0));
        alphaValue = 0.4f;
        for (int i = 0; i < insets.right; i++) {
            AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaValue);
            g2D.setComposite(alpha);
            g.drawLine(w - i - 1 - borderInsets.right, y + Math.min(insets.top, i) + borderInsets.top, w - i - 1 - borderInsets.right, y + h - Math.min(insets.top, i) - Math.min(insets.bottom, i) - borderInsets.bottom);

            alphaValue -= (alphaValue / 2);
        }

        g2D.setColor(new Color(0, 16, 0));
        alphaValue = 0.4f;
        for (int i = 0; i < insets.left; i++) {
            AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaValue);
            g2D.setComposite(alpha);
            g.drawLine(x + i + borderInsets.left, y + Math.min(insets.top, i) + borderInsets.top, x + i + borderInsets.left, y + h - Math.min(insets.top, i) - Math.min(insets.bottom, i) - borderInsets.bottom);

            alphaValue -= (alphaValue / 2);
        }

        g2D.setComposite(composite);
    }

}
