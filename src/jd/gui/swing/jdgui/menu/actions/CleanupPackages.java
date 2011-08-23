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
import java.util.LinkedList;

import jd.controlling.DownloadController;
import jd.controlling.IOEQ;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;

import org.jdownloader.gui.translate._GUI;

public class CleanupPackages extends ToolBarAction {

    private static final long serialVersionUID = -7185006215784212976L;

    public CleanupPackages() {
        super(_GUI._.action_remove_packages(), "action.remove.packages", "remove_packages");
    }

    @Override
    public void onAction(ActionEvent e) {
        IOEQ.add(new Runnable() {

            public void run() {
                if (!UserIO.isOK(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.jd_gui_swing_jdgui_menu_actions_CleanupPackages_message()))) return;

                DownloadController dlc = DownloadController.getInstance();
                LinkedList<FilePackage> packagestodelete = new LinkedList<FilePackage>();
                final boolean readL = dlc.readLock();
                try {
                    for (FilePackage fp : dlc.getPackages()) {
                        if (dlc.getDownloadLinksbyStatus(fp, LinkStatus.FINISHED | LinkStatus.ERROR_ALREADYEXISTS).size() == fp.size()) packagestodelete.add(fp);
                    }
                } finally {
                    dlc.readUnlock(readL);
                }
                for (FilePackage fp : packagestodelete) {
                    dlc.removePackage(fp);
                }
            }
        });

    }

    @Override
    public void initDefaults() {
    }

    @Override
    protected String createMnemonic() {
        return _GUI._.action_remove_packages_mnemonic();
    }

    @Override
    protected String createAccelerator() {
        return _GUI._.action_remove_packages_accelerator();
    }

    @Override
    protected String createTooltip() {
        return _GUI._.action_remove_packages_tooltip();
    }

}