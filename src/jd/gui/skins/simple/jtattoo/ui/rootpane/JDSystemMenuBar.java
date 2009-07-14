package jd.gui.skins.simple.jtattoo.ui.rootpane;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.Icon;
import javax.swing.JMenuBar;
import javax.swing.UIManager;

import jd.nutils.JDImage;

/**
 * extended from JTattoo!
 *  Systemmenubar. top left icon menubar in mainframe
 * 
 * @author Coalado
 * 
 */
public class JDSystemMenuBar extends JMenuBar {

    /**
     * 
     */
    private static final long serialVersionUID = 188849083724159528L;
    private Frame frame;

    public JDSystemMenuBar(Frame frame) {
        this.frame = frame;
    }

    public void paint(Graphics g) {

        Image image = (frame != null) ? frame.getIconImage() : null;
        if (image != null) {
            double max = getHeight() - 3;
            double iw = image.getWidth(null);
            double ih = image.getHeight(null);
            double scale = (iw > ih ? max / iw : max / ih);
            iw *= scale;
            ih *= scale;
            int x = 2;
            int y = (getHeight() - (int) ih) / 2;

            g.drawImage(JDImage.getImage("logo/logo_" + (int) iw + "_" + (int) ih), x, y, (int) iw, (int) ih, null);
        } else {
            Icon icon = UIManager.getIcon("InternalFrame.icon");
            if (icon != null) {
                icon.paintIcon(this, g, 2, 2);
            }
        }
    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();

        Image image = (frame != null) ? frame.getIconImage() : null;
        if (image != null) {
            return new Dimension(Math.max(image.getWidth(null), size.width), Math.max(image.getHeight(null), size.height));
        } else {
            return size;
        }
    }
};