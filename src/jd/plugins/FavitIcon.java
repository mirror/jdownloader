package jd.plugins;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.Icon;

import jd.controlling.faviconcontroller.FavIconRequestor;
import jd.controlling.faviconcontroller.FavIcons;

import org.appwork.utils.images.IconIO;
import org.jdownloader.DomainInfo;

public class FavitIcon implements Icon, FavIconRequestor {

    private int        width;
    private int        height;
    private final int  size  = 10;
    private final Icon icon;
    private Icon       badge = null;

    public FavitIcon(Icon icon, DomainInfo domainInfo) {
        width = icon.getIconWidth();
        height = icon.getIconHeight();
        this.badge = IconIO.getScaledInstance(FavIcons.getFavIcon(domainInfo.getTld(), this), size, size);
        this.icon = icon;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Icon back = icon;

        // badge = new ImageIcon(ImageProvider.getScaledInstance((BufferedImage) icon.getImage(), size, size,
        // RenderingHints.VALUE_INTERPOLATION_BILINEAR, true));
        // back = domainInfo.getIcon(icon.getIconHeight());
        back.paintIcon(c, g, x - 0, y - 0);
        Graphics2D g2d = (Graphics2D) g;
        g.setColor(Color.WHITE);

        Composite comp = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));

        int xx = x + width - badge.getIconWidth();
        int yy = y + height - badge.getIconHeight();
        g.fillOval(xx, yy, badge.getIconWidth(), badge.getIconHeight());
        // g.fillRect(xx, yy, size, size);

        badge.paintIcon(c, g, xx, yy);
        if (comp != null) g2d.setComposite(comp);

    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }

    @Override
    public Icon setFavIcon(Icon icon) {
        if (icon != null) badge = IconIO.getScaledInstance(icon, size, size);
        return badge;
    }
}
