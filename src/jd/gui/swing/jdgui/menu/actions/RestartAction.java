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
import java.awt.event.KeyEvent;

import org.jdownloader.actions.AppAction;
import org.jdownloader.actions.CachableInterface;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyRestartRequest;

public class RestartAction extends AppAction implements CachableInterface {

    private static final long serialVersionUID = 1333126351380171619L;

    public RestartAction() {
        setTooltipText(_GUI._.action_restart_tooltip());
        setName(_GUI._.action_restart());
        setIconKey("restart");
        setAccelerator(KeyEvent.VK_R);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        RestartController.getInstance().asyncRestart(new SmartRlyRestartRequest(false));
    }

    @Override
    public void setData(String data) {
    }

}