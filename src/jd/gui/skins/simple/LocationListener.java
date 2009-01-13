//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.gui.skins.simple;

import java.awt.Component;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class LocationListener implements WindowListener {

    public LocationListener() {
    }

    public void saveAll(Component src) {
        if (src != null) {
            SimpleGUI.saveLastLocation(src, null);
            SimpleGUI.saveLastDimension(src, null);
        }
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
        saveAll(e.getComponent());
    }

    public void windowClosing(WindowEvent e) {
        saveAll(e.getComponent());
    }

    public void windowDeactivated(WindowEvent e) {
        saveAll(e.getComponent());
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

}
