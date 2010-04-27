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
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class BackupAction extends ThreadedAction {

    private static final long serialVersionUID = 823930266263085474L;

    public BackupAction() {
        super("action.backup", "gui.images.save");
    }

    @Override
    public void init() {
    }

    @Override
    public void initDefaults() {
    }

    @Override
    public void threadedActionPerformed(ActionEvent e) {
        JDController.getInstance().syncDatabase();
        File backupFile = JDUpdateUtils.backupDataBase();
        if (backupFile == null) {
            Balloon.show(JDL.L("gui.balloon.backup.title", "Backup"), JDTheme.II("gui.images.save", 32, 32), JDL.LF("gui.backup.finished.failed", "Linklist Backup failed! Check %s for rights!", JDUtilities.getResourceFile("backup/")));
        } else {
            Balloon.show(JDL.L("gui.balloon.backup.title", "Backup"), JDTheme.II("gui.images.save", 32, 32), JDL.LF("gui.backup.finished.success", "Linklist Backup successful! (%s)", backupFile.getAbsolutePath()));
        }
    }

}
