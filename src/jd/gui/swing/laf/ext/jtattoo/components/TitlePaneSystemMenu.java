package jd.gui.swing.laf.ext.jtattoo.components;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JMenuBar;

import jd.nutils.JDImage;

/**
 * Systemmenu on top left.
 * 
 * @author Coalado
 * 
 */
public class TitlePaneSystemMenu extends JMenuBar {

    /**
     * 
     */
    private static final long serialVersionUID = 188849083724159528L;

    public TitlePaneSystemMenu() {

    }

    public void paint(Graphics g) {

        g.drawImage(JDImage.getImage("logo/logo_20_20"), 0, 0, (int) 20, (int) 20, null);

    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    public Dimension getPreferredSize() {
        // Dimension size = super.getPreferredSize();

        return new Dimension(20, 20);

    }
};