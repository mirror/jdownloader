package jd.gui.skins.simple.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import jd.utils.JDUtilities;

public class JCancelButton extends JButton implements MouseListener {

    private static final long serialVersionUID = 1071048540203207992L;
    private final ImageIcon imgCloseMouseOver = new ImageIcon(JDUtilities.getResourceFile("/jd/img/button_close_mouseover.png").getAbsolutePath());
    private final ImageIcon imgClose = new ImageIcon(JDUtilities.getResourceFile("/jd/img/button_close.png").getAbsolutePath());

    public JCancelButton() {
        this.setIcon(imgClose);
        this.setMaximumSize(new Dimension(16, 16));
        this.setForeground(new Color(255, 0, 0));
        this.setVisible(true);
        this.addMouseListener(this);
    }

    public void mouseClicked(MouseEvent arg0) {
    }

    public void mouseEntered(MouseEvent arg0) {
        this.setIcon(imgCloseMouseOver);
    }

    public void mouseExited(MouseEvent arg0) {
        this.setIcon(imgClose);
    }

    public void mousePressed(MouseEvent arg0) {
    }

    public void mouseReleased(MouseEvent arg0) {
    }

}
