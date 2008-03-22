//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.


package jd.gui.skins.simple.components;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import jd.utils.JDUtilities;

public class ContextMenu extends JPopupMenu {

    /**
	 * 
	 */
	private static final long serialVersionUID = 8890840310244673205L;

	protected Logger          logger = JDUtilities.getLogger();

    // public void mousePressed(MouseEvent e) {
    // logger.info("PRESSED");
    // if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
    // Point point = e.getPoint();
    // int x = e.getX();
    // int y = e.getY();
    // new InternalPopup(this, x, y);
    // }
    //
    // }

    private Vector<JMenuItem> menus;

  //  private int[]             indeces;

    private Point point;

    public ContextMenu(Component parent, Point point, String[] options, ActionListener list) {
        super();
        this.menus= new Vector<JMenuItem>();
        int x = (int) point.getX();
        int y = (int) point.getY();
        this.point=point;
        for (int i = 0; i < options.length; i++) {
            JMenuItem menu;
            this.menus.add(menu = new JMenuItem(options[i]));
            menu.addActionListener(list);
            this.add(menu);
        }
        this.add(new JSeparator());

        this.show(parent, x, y);
    }

    public Point getPoint() {
        return point;
    }

    public void setPoint(Point point) {
        this.point = point;
    }

}
