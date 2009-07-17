/**
 * BarBOrder based on jattoos Titleborder
 */

package jd.gui.skins.simple.components.borders;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;

import jd.utils.JDUtilities;
import sun.swing.SwingUtilities2;

import com.jtattoo.plaf.AbstractLookAndFeel;
import com.jtattoo.plaf.JTattooUtilities;

public class LineTitleBorder extends AbstractBorder {

    private static final long serialVersionUID = -5141737828416982838L;
    private Icon icon = null;
    private String title = null;

    private int innerSpace = 4;

    public LineTitleBorder(Icon aIcon, String aTitle, int aInnerSpace) {
        icon = aIcon;
        title = aTitle;

        innerSpace = aInnerSpace;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {

        Color loFrameColor = null;
        Color textColor = null;

        if (UIManager.getLookAndFeel() instanceof AbstractLookAndFeel) {
            loFrameColor = AbstractLookAndFeel.getControlDarkShadow();
            textColor = AbstractLookAndFeel.getForegroundColor();
        } else {
            loFrameColor = Color.gray;
            textColor = c.getForeground();
        }
        g.setFont(c.getFont());
        FontMetrics fm = g.getFontMetrics();
        int strWidth = fm.stringWidth(title);
        int titleHeight = getBorderInsets(c).top - 3 - innerSpace;

        g.setColor(loFrameColor);
        int hh = y + titleHeight / 2 + 3;
        int xs = 2 + strWidth + icon.getIconWidth() + 14;
        g.drawLine(x + xs, hh, w - getBorderInsets(c).right - innerSpace - 4, hh);

        paintText(c, g, x, y, w, h, textColor, null);
    }

    private void paintText(Component c, Graphics g, int x, int y, int w, int h, Color textColor, Color shadowColor) {
        boolean leftToRight = true;
        int sw = w - 8 - (2 * innerSpace);
        if (leftToRight) {
            x += 4 + innerSpace;
        } else {
            x = w - 4 - innerSpace;
        }
        int titleHeight = getBorderInsets(c).top - 3 - innerSpace;
        // paint the icon
        if (icon != null) {
            int yc = y + 2 + ((titleHeight - icon.getIconHeight()) / 2);
            if (leftToRight) {
                icon.paintIcon(c, g, x, yc);
                x += icon.getIconWidth() + 4;
            } else {
                icon.paintIcon(c, g, x - icon.getIconWidth(), yc);
                x -= icon.getIconWidth() + 4;
            }
            sw -= icon.getIconWidth();
        }
        // paint the text
        if ((title != null) && (title.trim().length() > 0)) {
            g.setFont(c.getFont());
            FontMetrics fm = g.getFontMetrics();
            String theTitle = JTattooUtilities.getClippedText(title, fm, sw);
            if (!leftToRight) {
                x -= fm.getStringBounds(theTitle, g).getWidth();
            }
            y += fm.getHeight();
            if (shadowColor != null) {
                g.setColor(shadowColor);
                g.drawString(theTitle, x + 1, y + 1);
            }
            g.setColor(textColor);
            if (c instanceof JComponent) {
                if (JDUtilities.getJavaVersion() >= 1.6) {
                    try {
                        SwingUtilities2.drawString((JComponent) c, g, theTitle, x, y);
                    } catch (Exception ex) {
                        g.drawString(theTitle, x, y);
                    }
                } else {
                    g.drawString(theTitle, x, y);
                }
            } else {
                g.drawString(theTitle, x, y);
            }
        }
    }

    public Insets getBorderInsets(Component c) {
        Graphics g = c.getGraphics();
        if (g != null) {
            FontMetrics fm = g.getFontMetrics(c.getFont());
            int frameWidth = 2 + innerSpace;
            int titleHeight = fm.getHeight() + (fm.getHeight() / 4);
            if (icon != null) {
                titleHeight = Math.max(titleHeight, icon.getIconHeight() + (icon.getIconHeight() / 4));
            }
            return new Insets(titleHeight + frameWidth, frameWidth, frameWidth, frameWidth);
        }
        return new Insets(0, 0, 0, 0);
    }
}
