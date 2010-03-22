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
import java.util.ArrayList;

import jd.controlling.DownloadController;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.views.linkgrabberview.LinkGrabberPanel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

public class RemoveFailedAction extends ToolBarAction {

    private static final long serialVersionUID = -5425871515927494136L;

    public RemoveFailedAction() {
        super("action.remove_failed", "gui.images.remove_failed");
    }

    @Override
    public void onAction(ActionEvent e) {
        if (!LinkGrabberPanel.getLinkGrabber().isNotVisible()) return;
        DownloadController dlc = DownloadController.getInstance();
        ArrayList<DownloadLink> downloadstodelete = new ArrayList<DownloadLink>();
        synchronized (dlc.getPackages()) {
            for (FilePackage fp : dlc.getPackages()) {
                synchronized (fp.getDownloadLinkList()) {
                    for (DownloadLink dl : fp.getDownloadLinkList()) {
                        if (dl.getLinkStatus().isFailed()) downloadstodelete.add(dl);
                    }
                }
            }
        }
        for (DownloadLink dl : downloadstodelete) {
            dl.getFilePackage().remove(dl);
        }
    }

    @Override
    public void init() {
    }

    @Override
    public void initDefaults() {
    }

}
