package jd.gui.skins.simple.components.borders;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;

import com.jtattoo.plaf.AbstractLookAndFeel;
import com.jtattoo.plaf.ColorHelper;
import com.jtattoo.plaf.JTattooUtilities;

public  class TitleBorder extends AbstractBorder {

    private Icon icon = null;
    private String title = null;
    private int shadowSize = 3;
    private int innerSpace = 4;

    public TitleBorder(Icon aIcon, String aTitle, int aShadowSize, int aInnerSpace) {
        icon = aIcon;
        title = aTitle;
        shadowSize = aShadowSize;
        innerSpace = aInnerSpace;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
        Graphics2D g2D = (Graphics2D) g;
        Composite composite = g2D.getComposite();

        Color hiFrameColor = null;
        Color loFrameColor = null;
        Color hiBackColor = null;
        Color loBackColor = null;
        Color textColor = null;

        if (UIManager.getLookAndFeel() instanceof AbstractLookAndFeel) {
            hiFrameColor = AbstractLookAndFeel.getControlHighlight();
            loFrameColor = AbstractLookAndFeel.getControlDarkShadow();
            hiBackColor = ColorHelper.brighter(AbstractLookAndFeel.getBackgroundColor(), 20);
            loBackColor = ColorHelper.darker(AbstractLookAndFeel.getBackgroundColor(), 5);
            textColor = AbstractLookAndFeel.getForegroundColor();
        } else {
            hiFrameColor = Color.white;
            loFrameColor = Color.gray;
            hiBackColor = ColorHelper.brighter(c.getBackground(), 30.0f);
            loBackColor = ColorHelper.darker(c.getBackground(), 10.0f);
            textColor = c.getForeground();
        }

        int titleHeight = getBorderInsets(c).top - 3 - innerSpace;
        g.setColor(loFrameColor);
        g.drawRect(x, y, w - shadowSize - 1, h - shadowSize - 1);
        g.setColor(hiFrameColor);
        g.drawRect(x + 1, y + 1, w - shadowSize - 3, h - shadowSize - 3);

        g.setColor(loFrameColor);
        g.drawLine(x + 2, y + getBorderInsets(c).top - innerSpace - 1, x + w - shadowSize - 1, y + getBorderInsets(c).top - innerSpace - 1);

        // paint the shadow
        if (shadowSize > 0) {
            g2D.setColor(new Color(0, 16, 0));
            float alphaValue = 0.4f;
            for (int i = 0; i < shadowSize; i++) {
                AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaValue);
                g2D.setComposite(alpha);
                g.drawLine(x + w - shadowSize + i, y + shadowSize + 4, x + w - shadowSize + i, y + h - shadowSize - 1 + i);
                g.drawLine(x + shadowSize + 2, y + h - shadowSize + i, x + w - shadowSize + i, y + h - shadowSize + i);
                alphaValue -= (alphaValue / 2);
            }
        }

        g2D.setComposite(composite);
        Color[] colors = ColorHelper.createColorArr(hiBackColor, loBackColor, 48);
        JTattooUtilities.fillVerGradient(g, colors, x + 2, y + 2, w - shadowSize - 4, titleHeight);

        paintText(c, g, x, y, w, h, textColor, null);
    }

    private void paintText(Component c, Graphics g, int x, int y, int w, int h, Color textColor, Color shadowColor) {
        boolean leftToRight = JTattooUtilities.isLeftToRight(c);
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
                JTattooUtilities.drawString((JComponent) c, g, theTitle, x, y);
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
            return new Insets(titleHeight + frameWidth, frameWidth, frameWidth + shadowSize, frameWidth + shadowSize);
        }
        return new Insets(0, 0, 0, 0);
    }
}
