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

import org.jdownloader.gui.shortcuts.ShortcutController;
import org.jdownloader.gui.translate._GUI;

public class RestartAction extends ActionAdapter {

    private static final long serialVersionUID = 1333126351380171619L;

    public RestartAction() {
        super(_GUI._.action_restart(), "action.restart", "restart");
    }

    @Override
    public void onAction(ActionEvent e) {
        // ShutdownController.getInstance().hasShutdownEvent(SilentUpdaterEvent)
        // if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(0,
        // _GUI._.sys_ask_rlyrestart()), UserIO.RETURN_OK,
        // UserIO.RETURN_DONT_SHOW_AGAIN)) {
        //
        // if (JDUpdater.getInstance().hasWaitingUpdates()) {
        // UpdateFoundDialog dialog = new UpdateFoundDialog(null, new Runnable()
        // {
        //
        // public void run() {
        // org.jdownloader.controlling.JDRestartController.getInstance().restartViaUpdater(true);
        // }
        //
        // }, JDUpdater.getInstance());
        // try {
        // Dialog.getInstance().showDialog(dialog);
        // return;
        // } catch (DialogNoAnswerException e1) {
        //
        // }
        // }
        org.jdownloader.controlling.JDRestartController.getInstance().directRestart();
        // }
    }

    @Override
    public void initDefaults() {
    }

    @Override
    public String createAccelerator() {
        return ShortcutController._.getRestartJDownloaderAction();
    }

    @Override
    public String createTooltip() {
        return _GUI._.action_restart_tooltip();
    }

}