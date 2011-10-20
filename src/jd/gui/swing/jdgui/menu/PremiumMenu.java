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

package jd.gui.swing.jdgui.menu;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;

import jd.Main;
import jd.gui.swing.jdgui.actions.ActionController;

import org.jdownloader.gui.translate._GUI;

public class PremiumMenu extends JMenu {

    private static final long  serialVersionUID = 5075413754334671773L;

    private static PremiumMenu INSTANCE;

    private PremiumMenu() {
        super(_GUI._.gui_menu_premium());
        Main.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                add(new JCheckBoxMenuItem(ActionController.getToolBarAction("premiumMenu.toggle")));
                add(ActionController.getToolBarAction("premiumMenu.configuration"));
            }

        });
    }

    public static PremiumMenu getInstance() {
        if (INSTANCE == null) INSTANCE = new PremiumMenu();
        return INSTANCE;
    }

}