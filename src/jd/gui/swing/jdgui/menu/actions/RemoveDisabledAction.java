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

import jd.config.MenuAction;
import jd.controlling.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

public class RemoveDisabledAction extends MenuAction {

    private static final long serialVersionUID = -5335194420202699757L;

    public RemoveDisabledAction() {
        super("action.remove_disabled", "gui.images.remove_disabled");
    }

    public void actionPerformed(ActionEvent e) {
        DownloadController dlc = DownloadController.getInstance();
        Vector<DownloadLink> downloadstodelete = new Vector<DownloadLink>();
        synchronized (dlc.getPackages()) {
            for (FilePackage fp : dlc.getPackages()) {
                synchronized (fp.getDownloadLinkList()) {
                    for (DownloadLink dl : fp.getDownloadLinkList()) {
                        if (!dl.isEnabled()) downloadstodelete.add(dl);
                    }
                }
            }
        }
        for (DownloadLink dl : downloadstodelete) {
            dl.getFilePackage().remove(dl);
        }
    }
}
