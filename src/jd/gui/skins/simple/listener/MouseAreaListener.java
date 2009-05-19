package jd.gui.skins.simple.listener;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import jd.gui.skins.simple.SimpleGUI;

public class MouseAreaListener implements MouseMotionListener, MouseListener {
    private boolean over = false;
    private int xStart;
    private int yStart;
    private int yEnd;
    private int xEnd;

    public MouseAreaListener(int i, int j, int k, int l) {
        this.xStart = i;
        this.yStart = j;
        this.xEnd = k;
        this.yEnd = l;
    }

    public boolean isOverIcon(Point p) {
        if (p.x > xStart && p.x < xEnd && p.y > yStart && p.y < yEnd) return true;
        return false;
    }

    public void mouseClicked(MouseEvent e) {
        if (isOverIcon(e.getPoint())) {
            SimpleGUI.CURRENTGUI.onMainMenuMouseClick(e);
        }
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
        if (isOverIcon(e.getPoint()) && !over) {
            over = true;
            SimpleGUI.CURRENTGUI.onMainMenuMouseEnter();
        } else if (!isOverIcon(e.getPoint()) && over) {
            over = false;
            SimpleGUI.CURRENTGUI.onMainMenuMouseExit();
        }

    }

    public void mouseEntered(MouseEvent e) {
        mouseMoved(e);
    }

    public void mouseExited(MouseEvent e) {
        mouseMoved(e);
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

}
