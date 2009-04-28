package jd.gui.skins.simple;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class MouseAreaListener implements MouseMotionListener, MouseListener {
    private boolean over = false;
    private int xStart;
    private int yStart;
    private int yEnd;
    private int xEnd;

    public MouseAreaListener(int i, int j, int k, int l) {
       this.xStart=i;
       this.yStart=j;
       this.xEnd=k;
       this.yEnd=l;
    }

    public boolean isOverIcon(Point p) {
        if (p.x >= xStart && p.x <= xEnd) {
            if (p.y <= yEnd&&p.y>=yStart) { return true; }
        }
        return false;
    }

    public void mouseClicked(MouseEvent e) {
        if (isOverIcon(e.getPoint())) {
            SimpleGUI.CURRENTGUI.onMainMenuMouseClick(e);
        }

    }

    public void mouseDragged(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mouseMoved(MouseEvent e) {
        if (isOverIcon(e.getPoint()) && !over) {
            over = true;
           
            SimpleGUI.CURRENTGUI.onMainMenuMouseEnter(e);
        } else if (!isOverIcon(e.getPoint()) && over) {
            over = false;
      
            SimpleGUI.CURRENTGUI.onMainMenuMouseExit(e);
        }

    }

    public void mouseEntered(MouseEvent e) {
        mouseMoved(e);

    }

    public void mouseExited(MouseEvent e) {
        mouseMoved(e);

    }

    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub

    }

}
