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
import jd.controlling.LinkGrabberController;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.actions.ThreadedAction;
import jd.gui.swing.jdgui.views.linkgrabber.LinkGrabberPanel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkGrabberFilePackage;
import jd.plugins.LinkStatus;

import org.jdownloader.gui.translate._GUI;

public class RemoveDupesAction extends ThreadedAction {

    private static final long serialVersionUID = 3088399063634025074L;

    public RemoveDupesAction() {
        super(_GUI._.action_remove_dupe_links(), "action.remove_dupes", "remove_dupes");
    }

    @Override
    public void initDefaults() {
    }

    @Override
    public void threadedActionPerformed(ActionEvent e) {
        if (!UserIO.isOK(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.jd_gui_swing_jdgui_menu_actions_RemoveDupesAction_message()))) return;

        if (!LinkGrabberPanel.getLinkGrabber().isNotVisible()) {
            synchronized (LinkGrabberController.ControllerLock) {
                synchronized (LinkGrabberController.getInstance().getPackages()) {
                    ArrayList<LinkGrabberFilePackage> selected_packages = new ArrayList<LinkGrabberFilePackage>(LinkGrabberController.getInstance().getPackages());
                    selected_packages.add(LinkGrabberController.getInstance().getFilterPackage());
                    for (LinkGrabberFilePackage fp2 : selected_packages) {
                        ArrayList<DownloadLink> selected_links = fp2.getLinksListbyStatus(LinkStatus.ERROR_ALREADYEXISTS);
                        fp2.remove(selected_links);
                    }
                }
            }
        } else {
            DownloadController dlc = DownloadController.getInstance();
            ArrayList<DownloadLink> downloadstodelete = new ArrayList<DownloadLink>();
            synchronized (dlc.getPackages()) {
                for (FilePackage fp : dlc.getPackages()) {
                    downloadstodelete.addAll(fp.getLinksListbyStatus(LinkStatus.ERROR_ALREADYEXISTS));
                }
            }
            for (DownloadLink dl : downloadstodelete) {
                dl.getFilePackage().remove(dl);
            }
        }
    }

    @Override
    protected String createMnemonic() {
        return _GUI._.action_remove_dupe_links_mnemonic();
    }

    @Override
    protected String createAccelerator() {
        return _GUI._.action_remove_dupe_links_accelerator();
    }

    @Override
    protected String createTooltip() {
        return _GUI._.action_remove_dupe_links_tooltip();
    }
}