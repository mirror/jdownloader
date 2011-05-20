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

import jd.gui.swing.jdgui.menu.actions.BackupAction;
import jd.gui.swing.jdgui.menu.actions.BackupLinkListAction;

import org.jdownloader.gui.translate._GUI;

public class SaveMenu extends JStartMenu {

    private static final long serialVersionUID = -153884445300435027L;

    public SaveMenu() {
        super(_GUI._.gui_menu_save(), "save");
        this.add(new BackupAction());
        this.add(new BackupLinkListAction());
    }

}