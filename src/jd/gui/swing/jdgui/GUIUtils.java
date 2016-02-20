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

package jd.gui.swing.jdgui;

import org.jdownloader.settings.staticreferences.CFG_GUI;

public class GUIUtils {

    public static String getAccountName(final String username) {
        String output = username;
        if (CFG_GUI.CFG.isPresentationModeEnabled()) {
            if (username == null || "".equals(username)) {
                output = "";
            } else if (username.length() <= 1) {
                output = "*********";
            } else {
                output = username.substring(0, 2) + "*******";
            }
        }
        return output;
    }
}
