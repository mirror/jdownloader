package jd.plugins;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.jdownloader.DomainInfo;

public class FavitIcon implements Icon {

    private int        width;
    private int        height;
    private DomainInfo domainInfo;
    private ImageIcon  icon;

    public FavitIcon(ImageIcon icon, DomainInfo domainInfo) {
        width = icon.getIconWidth();
        height = icon.getIconHeight();
        this.domainInfo = domainInfo;
        this.icon = icon;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        icon.paintIcon(c, g, x - 2, y - 2);
        Graphics2D g2d = (Graphics2D) g;
        g.setColor(Color.WHITE);

        int size = 12;
        int xx = width - size + 1;
        int yy = height - size + 1;
        Composite comp = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
        g.fillOval(xx - 1, yy - 1, size + 2, size + 2);
        // g.fillRect(xx, yy, size, size);
        g2d.setComposite(comp);
        domainInfo.getIcon(size).paintIcon(c, g, xx, yy);

    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }

}
