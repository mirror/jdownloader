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

import jd.gui.swing.dialog.AboutDialog;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.actions.CachableInterface;
import org.jdownloader.gui.translate._GUI;

public class AboutAction extends AppAction implements CachableInterface {

    private static final long serialVersionUID = -353145605693194634L;

    public AboutAction() {
        super();
        setName(_GUI._.action_aboutaction());
        setIconKey("about");
        setTooltipText(_GUI._.action_aboutaction_tooltip());

    }

    public void actionPerformed(ActionEvent e) {
        try {
            Dialog.getInstance().showDialog(new AboutDialog());
        } catch (DialogNoAnswerException e1) {
        }
    }

    @Override
    public void setData(String data) {
    }

}
