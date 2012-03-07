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

import jd.gui.UserIO;
import jd.nutils.JDFlags;

import org.appwork.update.inapp.RestartController;
import org.appwork.update.standalone.gui.UpdateFoundDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.update.JDUpdater;

public class RestartAction extends ActionAdapter {

    private static final long serialVersionUID = 1333126351380171619L;

    public RestartAction() {
        super(_GUI._.action_restart(), "action.restart", "restart");
    }

    @Override
    public void onAction(ActionEvent e) {
        if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(0, _GUI._.sys_ask_rlyrestart()), UserIO.RETURN_OK, UserIO.RETURN_DONT_SHOW_AGAIN)) {

            if (JDUpdater.getInstance().hasWaitingUpdates()) {
                UpdateFoundDialog dialog = new UpdateFoundDialog(null, new Runnable() {

                    public void run() {
                        RestartController.getInstance().restartViaUpdater(true);
                    }

                }, JDUpdater.getInstance());
                try {
                    Dialog.getInstance().showDialog(dialog);
                    return;
                } catch (DialogNoAnswerException e1) {

                }
            }
            RestartController.getInstance().directRestart(true);
        }
    }

    @Override
    public void initDefaults() {
    }

    @Override
    public String createMnemonic() {
        return _GUI._.action_restart_mnemonic();
    }

    @Override
    public String createAccelerator() {
        return _GUI._.action_restart_accelerator();
    }

    @Override
    public String createTooltip() {
        return _GUI._.action_restart_tooltip();
    }

}