//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import java.util.HashMap;

import javax.swing.JComponent;

import net.java.balloontip.BalloonTip;

public class Balloons {

    private static HashMap<String, JComponent> attachableBallons = new HashMap<String, JComponent>();

    private Balloons() {
    }

    public static void attachComponent(JComponent comp, String name) {
        attachableBallons.put(name, comp);
    }

    public static void removeComponent(String name) {
        attachableBallons.remove(name);
    }

    public static void showBalloon(String name, String message) {
        JComponent com = attachableBallons.get(name);
        if (com != null && com.isShowing() && message != null && message.length() > 0) {
            @SuppressWarnings("unused")
            BalloonTip myBalloonTip = new BalloonTip(com, message);
        }
    }
}
