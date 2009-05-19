//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

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
