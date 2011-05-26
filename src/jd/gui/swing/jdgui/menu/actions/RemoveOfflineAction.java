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
import java.util.LinkedList;

import jd.controlling.DownloadController;
import jd.controlling.IOEQ;
import jd.controlling.LinkGrabberController;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.views.linkgrabber.LinkGrabberPanel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkGrabberFilePackage;
import jd.plugins.LinkStatus;

import org.jdownloader.gui.translate._GUI;

public class RemoveOfflineAction extends ToolBarAction {

    private static final long serialVersionUID = 4181319974731148936L;

    public RemoveOfflineAction() {
        super(_GUI._.action_remove_offline_links(), "action.remove_offline", "remove_offline");
    }

    @Override
    public void initDefaults() {
    }

    @Override
    public void onAction(ActionEvent e) {
        IOEQ.add(new Runnable() {

            public void run() {
                if (!UserIO.isOK(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.jd_gui_swing_jdgui_menu_actions_RemoveOfflineAction_message()))) return;

                if (!LinkGrabberPanel.getLinkGrabber().isNotVisible()) {
                    synchronized (LinkGrabberController.ControllerLock) {
                        synchronized (LinkGrabberController.getInstance().getPackages()) {
                            ArrayList<LinkGrabberFilePackage> selected_packages = new ArrayList<LinkGrabberFilePackage>(LinkGrabberController.getInstance().getPackages());
                            selected_packages.add(LinkGrabberController.getInstance().getFilterPackage());
                            for (LinkGrabberFilePackage fp2 : selected_packages) {
                                fp2.removeOffline();
                            }
                        }
                    }
                } else {
                    DownloadController dlc = DownloadController.getInstance();
                    LinkedList<DownloadLink> downloadstodelete = new LinkedList<DownloadLink>();
                    synchronized (DownloadController.ACCESSLOCK) {
                        for (FilePackage fp : dlc.getPackages()) {
                            for (DownloadLink dl : fp.getDownloadLinkList()) {
                                if (dl.getLinkStatus().hasStatus(LinkStatus.ERROR_FILE_NOT_FOUND)) downloadstodelete.add(dl);
                            }
                        }
                    }
                    for (DownloadLink dl : downloadstodelete) {
                        dl.getFilePackage().remove(dl);
                    }
                }
            }
        });
    }

    @Override
    protected String createMnemonic() {
        return _GUI._.action_remove_offline_links_mnemonic();
    }

    @Override
    protected String createAccelerator() {
        return _GUI._.action_remove_offline_links_accelerator();
    }

    @Override
    protected String createTooltip() {
        return _GUI._.action_remove_offline_links_tooltip();
    }
}