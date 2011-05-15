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

import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.nutils.Executer;
import jd.nutils.JDFlags;
import jd.utils.JDUtilities;

import org.jdownloader.gui.translate._GUI;

public class RestoreAction extends ToolBarAction {

    private static final long serialVersionUID = -1428029294638573437L;

    public RestoreAction() {
        super("action.restore", "gui.images.edit");
    }

    @Override
    public void onAction(ActionEvent e) {
        if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(0, _GUI._.sys_ask_rlyrestore()), UserIO.RETURN_OK, UserIO.RETURN_DONT_SHOW_AGAIN)) {
            final Executer exec = new Executer("java");
            exec.addParameters(new String[] { "-jar", "jdupdate.jar", "-restore" });
            exec.setRunin(JDUtilities.getResourceFile(".").getAbsolutePath());
            exec.setWaitTimeout(0);
            JDController.getInstance().addControlListener(new ControlListener() {
                public void controlEvent(ControlEvent event) {
                    if (event.getEventID() == ControlEvent.CONTROL_SYSTEM_SHUTDOWN_PREPARED) {
                        exec.start();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            });
            JDController.getInstance().exit();
        }
    }

    @Override
    public void initDefaults() {
    }

}