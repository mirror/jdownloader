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


package jd.gui.skins.simple;

import java.awt.Component;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import jd.utils.JDUtilities;

public class LocationListener implements ComponentListener, WindowListener {

    // private SubConfiguration guiConfig;
   // private Logger    logger;

    private Component src;

    public LocationListener() {

        // this.guiConfig=JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);
  //      this.logger = JDUtilities.getLogger();
    }

    public void componentHidden(ComponentEvent e) {

        src = e.getComponent();
    }

    public void componentMoved(ComponentEvent e) {

        src = e.getComponent();

    }

    public void componentResized(ComponentEvent e) {

        src = e.getComponent();
    }

    public void componentShown(ComponentEvent e) {

        src = e.getComponent();

    }

    public void windowActivated(WindowEvent e) {

        src = e.getComponent();

    }

    public void windowClosed(WindowEvent e) {

        src = e.getComponent();
        saveAll();

    }

    public void saveAll() {
        if (src != null) {
            JDUtilities.getLogger().info("Loc listener");
            SimpleGUI.saveLastLocation(src, null);
            SimpleGUI.saveLastDimension(src, null);
            JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).save();
        }
    }

    public void windowClosing(WindowEvent e) {

        src = e.getComponent();
        saveAll();
    }

    public void windowDeactivated(WindowEvent e) {

        src = e.getComponent();
        saveAll();
    }

    public void windowDeiconified(WindowEvent e) {

        src = e.getComponent();

    }

    public void windowIconified(WindowEvent e) {

        src = e.getComponent();

    }

    public void windowOpened(WindowEvent e) {

        src = e.getComponent();

    }

}
