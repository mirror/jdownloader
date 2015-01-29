package jd.gui.swing.jdgui.menu;

import java.awt.Dimension;
import java.awt.Graphics;

import org.jdownloader.actions.AppAction;
import org.jdownloader.images.AbstractIcon;

public class ExtDonateMenuButton extends ExtMenuButton {

    private AbstractIcon icon;

    public ExtDonateMenuButton(AppAction action) {
        super(action);
        icon = new AbstractIcon("heart", 20);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension ret = super.getPreferredSize();
        ret.width += 22;
        return ret;
    }

    protected void paintComponent(Graphics g) {

        // g.translate(22, 0);
        super.paintComponent(g);
        icon.paintIcon(this, g, getWidth() - icon.getIconWidth() - 4, 0);
    }
}
