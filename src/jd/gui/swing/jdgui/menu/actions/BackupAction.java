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
import java.io.File;

import jd.controlling.IOEQ;
import jd.controlling.JDController;
import jd.update.JDUpdateUtils;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class BackupAction extends AppAction {

    private static final long serialVersionUID = 823930266263085474L;

    public BackupAction() {
        super();
        setName(_GUI._.action_backup());
        setIconKey("save");
        setMnemonic(_GUI._.action_backup_mnemonic());
        setAccelerator(_GUI._.action_backup_accelerator());
        setTooltipText(_GUI._.action_backup_tooltip());
    }

    public void actionPerformed(ActionEvent e) {

        IOEQ.add(new Runnable() {

            public void run() {
                JDController.getInstance().syncDatabase();
                File backupFile = JDUpdateUtils.backupDataBase();
            }

        });
    }

}