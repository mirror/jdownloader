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

package jd.gui.swing.jdgui.menu.actions;

import java.awt.event.ActionEvent;
import java.util.Vector;

import jd.controlling.DownloadController;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;

public class CleanupPackages extends ToolBarAction {

    private static final long serialVersionUID = -7185006215784212976L;

    public CleanupPackages() {
        super("action.remove.packages", "gui.images.remove_ok");
    }

    @Override
    public void onAction(ActionEvent e) {
        DownloadController dlc = DownloadController.getInstance();
        Vector<FilePackage> packagestodelete = new Vector<FilePackage>();
        synchronized (dlc.getPackages()) {
            for (FilePackage fp : dlc.getPackages()) {
                if (fp.getLinksListbyStatus(LinkStatus.FINISHED | LinkStatus.ERROR_ALREADYEXISTS).size() == fp.size()) packagestodelete.add(fp);
            }
        }
        for (FilePackage fp : packagestodelete) {
            dlc.removePackage(fp);
        }
    }

    @Override
    public void init() {
    }

    @Override
    public void initDefaults() {
    }

}
