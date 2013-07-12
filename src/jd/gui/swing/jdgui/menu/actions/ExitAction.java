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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.jdownloader.actions.AppAction;
import org.jdownloader.actions.CachableInterface;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyExitRequest;

public class ExitAction extends AppAction implements CachableInterface {

    public static final String HIDE_ON_MAC      = "HideOnMac";
    private static final long  serialVersionUID = -1428029294638573437L;

    public ExitAction() {

        setIconKey("exit");
        setName(_GUI._.action_exit());
        setTooltipText(_GUI._.action_exit_tooltip());
        setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));

    }

    private boolean hideOnMac = false;

    @Customizer(name = "Hide Action on MAC OS")
    public boolean isHideOnMac() {

        return hideOnMac;
    }

    public void setHideOnMac(boolean hideOnMac) {
        this.hideOnMac = hideOnMac;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        RestartController.getInstance().exitAsynch(new SmartRlyExitRequest());
    }

    @Override
    public void setData(String data) {
    }

}
