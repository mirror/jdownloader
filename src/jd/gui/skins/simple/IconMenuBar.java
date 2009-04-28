package jd.gui.skins.simple;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JMenuBar;

import jd.nutils.JDImage;
import jd.utils.JDTheme;

public class IconMenuBar extends JMenuBar {

    private static final long serialVersionUID = -5708222882378406099L;
    private Image appIcon;
    private int height = 32;
    private int width = 32;

    public IconMenuBar() {
        super();
        appIcon = JDTheme.I("gui.images.logomenu", 32, 32);
        setFocusable(false);
        setBorderPainted(true);
        setOpaque(false);
        height = appIcon.getHeight(null);
        width = appIcon.getWidth(null);
        this.setMinimumSize(new Dimension(width, height));
    }

    //@Override
    public void paint(Graphics g) {
        // super.paint(g);
        g.drawImage(appIcon, 0, 0, null);
    }

}
