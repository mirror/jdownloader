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

import jd.gui.swing.jdgui.menu.actions.CleanupDownloads;
import jd.gui.swing.jdgui.menu.actions.CleanupPackages;
import jd.gui.swing.jdgui.menu.actions.RemoveDisabledAction;
import jd.gui.swing.jdgui.menu.actions.RemoveDupesAction;
import jd.gui.swing.jdgui.menu.actions.RemoveFailedAction;
import jd.gui.swing.jdgui.menu.actions.RemoveOfflineAction;
import jd.utils.locale.JDL;

public class CleanupMenu extends JStartMenu {

    private static final long serialVersionUID = 2238339685705371437L;

    public CleanupMenu() {
        super(JDL.L("gui.menu.remove", null), "gui.images.delete");

        this.add(new CleanupDownloads());
        this.add(new CleanupPackages());
        this.addSeparator();
        this.add(new RemoveDupesAction());
        this.add(new RemoveDisabledAction());
        this.add(new RemoveOfflineAction());
        this.add(new RemoveFailedAction());
    }

}
