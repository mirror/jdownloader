package jd.gui.swing.laf.ext.jattoo.ui;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;

import com.jtattoo.plaf.AbstractLookAndFeel;
import com.jtattoo.plaf.BaseButtonUI;

public class BaseJDButtonUI extends BaseButtonUI {
    public static ComponentUI createUI(JComponent c) {
        return new BaseJDButtonUI();
    }
    
    public void paint(Graphics g, JComponent c) {
        Graphics2D g2D = (Graphics2D) g;

        AbstractButton b = (AbstractButton) c;
        Font f = c.getFont();
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        Insets insets = c.getInsets();

        viewRect.x = insets.left;
        viewRect.y = insets.top;
        viewRect.width = b.getWidth() - (insets.right + viewRect.x);
        viewRect.height = b.getHeight() - (insets.bottom + viewRect.y);

        textRect.x = textRect.y = textRect.width = textRect.height = 0;
        iconRect.x = iconRect.y = iconRect.width = iconRect.height = 0;

        String text = SwingUtilities.layoutCompoundLabel(c, fm, b.getText(), b.getIcon(), b.getVerticalAlignment(), b.getHorizontalAlignment(), b.getVerticalTextPosition(), b.getHorizontalTextPosition(), viewRect, iconRect, textRect, b.getText() == null ? 0 : defaultTextIconGap);
        
        paintBackground(g, b);

        if (b.getIcon() != null) {
            textRect.x+=b.getIconTextGap();
            if (!b.isEnabled()) {
                Composite composite = g2D.getComposite();
                AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
                g2D.setComposite(alpha);
                paintIcon(g, c, iconRect);
                g2D.setComposite(composite);
            } else {
                if (b.getModel().isPressed() && b.getModel().isRollover()) {
                    iconRect.x++;
                    iconRect.y++;
                }
                paintIcon(g, c, iconRect);
            }
        }

        if (text != null && !text.equals("")) {
            View v = (View) c.getClientProperty(BasicHTML.propertyKey);
            if (v != null) {
                Object savedRenderingHint = null;
                if (AbstractLookAndFeel.getTheme().isTextAntiAliasingOn()) {
                    savedRenderingHint = g2D.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
                    g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                }
                v.paint(g, textRect);
                if (AbstractLookAndFeel.getTheme().isTextAntiAliasingOn()) {
                    g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, savedRenderingHint);
                }
            } else {
                paintText(g, b, textRect);
            }
        }

        if (b.isFocusPainted() && b.hasFocus()) {
            paintFocus(g, b, viewRect, textRect, iconRect);
        }
    }
}
