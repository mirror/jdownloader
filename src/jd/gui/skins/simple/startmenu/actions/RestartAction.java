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

package jd.gui.skins.simple.startmenu.actions;

import java.awt.event.ActionEvent;

import jd.gui.UserIO;
import jd.gui.skins.SwingGui;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.gui.skins.simple.SimpleGuiUtils;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class RestartAction extends StartAction {

    private static final long serialVersionUID = 1333126351380171619L;

    public RestartAction() {
        super("action.restart", "gui.images.restart");
    }

    public void actionPerformed(ActionEvent e) {
        if (UserIO.RETURN_OK==UserIO.getInstance().requestConfirmDialog(0,JDL.L("sys.ask.rlyrestart", "Wollen Sie jDownloader wirklich neustarten?"))) {
            //TODO
            if(SimpleGUI.CURRENTGUI!=null){
                SimpleGUI.CURRENTGUI.getContentPane().getRightPanel().onHide();
            }
            SimpleGuiUtils.saveLastLocation(SwingGui.getInstance(), null);
            SimpleGuiUtils.saveLastDimension(SwingGui.getInstance(), null);
            SimpleGuiConstants.GUI_CONFIG.save();
            JDUtilities.restartJD();
        }
    }

}
