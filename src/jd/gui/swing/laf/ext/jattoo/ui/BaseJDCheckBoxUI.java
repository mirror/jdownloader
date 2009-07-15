package jd.gui.swing.laf.ext.jattoo.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ComponentUI;

import com.jtattoo.plaf.BaseCheckBoxUI;

public class BaseJDCheckBoxUI extends BaseCheckBoxUI {

    private static Dimension size = new Dimension();
    private static Rectangle viewRect = new Rectangle();
    private static Rectangle iconRect = new Rectangle();
    private static Rectangle textRect = new Rectangle();
    private static BaseJDCheckBoxUI checkBoxUI;

    public static ComponentUI createUI(JComponent b) {
        if (checkBoxUI == null) {
            checkBoxUI = new BaseJDCheckBoxUI();
        }
        return checkBoxUI;
    }

    public void paint(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        Font f = c.getFont();
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();

        Insets i = c.getInsets();
        size = b.getSize(size);
        viewRect.x = i.left;
        viewRect.y = i.top;
        viewRect.width = size.width - (i.right + viewRect.x);
        viewRect.height = size.height - (i.bottom + viewRect.y);
        iconRect.x = iconRect.y = iconRect.width = iconRect.height = 0;
        textRect.x = textRect.y = textRect.width = textRect.height = 0;

        Icon altIcon = b.getIcon();
        String text = SwingUtilities.layoutCompoundLabel(c, fm, b.getText(), altIcon != null ? altIcon : getDefaultIcon(), b.getVerticalAlignment(), b.getHorizontalAlignment(), b.getVerticalTextPosition(), b.getHorizontalTextPosition(), viewRect, iconRect, textRect, b.getIconTextGap());

        // fill background
        if (c.isOpaque()) {
            paintBackground(g, c);
        }

        paintIcon(g, c, iconRect);

        if (text != null) {
            paintText(g, c, text, textRect);
        }

        if (b.hasFocus() && b.isFocusPainted() && (textRect.width > 0) && (textRect.height > 0)) {
            paintFocus(g, textRect, size);
        }
    }
}
