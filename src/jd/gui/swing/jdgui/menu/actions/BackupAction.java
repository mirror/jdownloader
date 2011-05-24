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

import jd.controlling.JDController;
import jd.gui.swing.components.Balloon;
import jd.gui.swing.jdgui.actions.ThreadedAction;
import jd.update.JDUpdateUtils;
import jd.utils.JDUtilities;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class BackupAction extends ThreadedAction {

    private static final long serialVersionUID = 823930266263085474L;

    public BackupAction() {
        super(_GUI._.action_backup(), "action.backup", "save");
    }

    @Override
    public void initDefaults() {
    }

    @Override
    public void threadedActionPerformed(ActionEvent e) {
        JDController.getInstance().syncDatabase();
        File backupFile = JDUpdateUtils.backupDataBase();
        if (backupFile == null) {
            Balloon.show(_GUI._.gui_balloon_backup_title(), NewTheme.I().getIcon("save", 32), _GUI._.gui_backup_finished_failed(JDUtilities.getResourceFile("backup/")));
        } else {
            Balloon.show(_GUI._.gui_balloon_backup_title(), NewTheme.I().getIcon("save", 32), _GUI._.gui_backup_finished_success(backupFile.getAbsolutePath()));
        }
    }

    @Override
    protected String createMnemonic() {
        return _GUI._.action_backup_mnemonic();
    }

    @Override
    protected String createAccelerator() {
        return _GUI._.action_backup_accelerator();
    }

    @Override
    protected String createTooltip() {
        return _GUI._.action_backup_tooltip();
    }

}