package org.jdownloader.gui.views.components;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;

import org.appwork.utils.ImageProvider.ImageProvider;

public final class CheckBoxIcon implements Icon {
    private JCheckBox cb;
    private ImageIcon internalIcon;

    public CheckBoxIcon(boolean selected) {
        cb = new JCheckBox() {
            {
                setSelected(true);
            }

            @Override
            public int getWidth() {
                return getPreferredSize().width;
            }

            @Override
            public int getHeight() {
                return getPreferredSize().height;
            }

            @Override
            public boolean isVisible() {
                return true;
            }
        };
        ;
        cb.setSelected(selected);
        // we need this workaround.
        // if we would use cb.paint(g); for every paintIcon call, this might habe sideeffects on the LAF painter.
        internalIcon = ImageProvider.toImageIcon(this);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (internalIcon != null) {
            g.drawImage(internalIcon.getImage(), x, y, null);
            return;
        }
        // g.setColor(Color.RED);
        // g.drawRect(0, 0, 14, 14);
        g = g.create(x, y, getIconWidth(), getIconHeight());
        // g.translate(x, y);
        g.translate(-4, -4);
        cb.paint(g);
        g.dispose();
        // g.translate(4, 4);
        // g.translate(-x, -y);

        // g.dispose();
        // g.translate(0, -10);

    }

    @Override
    public int getIconWidth() {
        return 14;
    }

    @Override
    public int getIconHeight() {
        return 14;
    }
}