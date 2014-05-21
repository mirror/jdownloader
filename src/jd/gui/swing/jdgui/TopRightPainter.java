package jd.gui.swing.jdgui;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

public interface TopRightPainter {

    Rectangle paint(Graphics2D g);

    boolean isVisible();

    void onMouseOver(MouseEvent e);

    void onMouseOut(MouseEvent e);

    void onClicked(MouseEvent e);

}
