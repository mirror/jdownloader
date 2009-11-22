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

/*
Beispiel:
http://api.xrel.to/eif/1b7b7d29781ee788d285d6fd9e6d46bd/movie28662.xml
http://api.xrel.to/eif/KEY/movie28662.xml
 */

package jd.plugins.optional.xrelnotifier;

import java.util.ArrayList;
import jd.PluginWrapper;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.PluginOptional;
import jd.plugins.OptionalPlugin;

@OptionalPlugin(rev = "$Revision$", defaultEnabled = false, id = "jdxrelnotifier", hasGui = true, interfaceversion = 5, mac = true, linux = true)
public class JDXrelNotifier extends PluginOptional {

    private MenuAction menuAction;

    public JDXrelNotifier(PluginWrapper wrapper) {
        super(wrapper);
    }

    public boolean initAddon() {
        return false;
    }

    public void onExit() {
    }

    public ArrayList<MenuAction> createMenuitems() {
        return null;
    }
}
