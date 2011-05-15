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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import jd.config.SubConfiguration;
import jd.controlling.DownloadController;
import jd.crypt.JDCrypt;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.actions.ThreadedAction;
import jd.nutils.JDHash;
import jd.nutils.io.JDFileFilter;
import jd.nutils.io.JDIO;
import jd.plugins.FilePackage;
import jd.utils.JDHexUtils;

import org.jdownloader.gui.translate._GUI;

public class BackupLinkListAction extends ThreadedAction {

    private static final long serialVersionUID = 823930266263085474L;

    public BackupLinkListAction() {
        super("action.backuplinklist", "gui.images.save");
    }

    public static byte[] getPWByte(String requestInputDialog) {
        return JDHexUtils.getByteArray(JDHash.getMD5(requestInputDialog));
    }

    @Override
    public void initDefaults() {
    }

    @Override
    public void threadedActionPerformed(ActionEvent e) {
        ArrayList<FilePackage> packages = DownloadController.getInstance().getPackages();
        try {
            // Serialize to a byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            synchronized (DownloadController.ControllerLock) {
                out.writeObject(packages);
            }
            out.close();

            File[] files = UserIO.getInstance().requestFileChooser("_LOADSAVEDLC", null, null, new JDFileFilter(null, ".jdc", true), null, null, UserIO.SAVE_DIALOG);
            if (files == null) return;

            String defaultpw = SubConfiguration.getConfig("JDC_CONFIG").getStringProperty("password", "jddefault");
            String pw = UserIO.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN, _GUI._.jd_gui_swing_jdgui_menu_actions_BackupLinkListAction_password(), defaultpw);
            if (pw == null || pw.length() == 0) return;
            byte[] crypted = JDCrypt.encrypt(JDHexUtils.getHexString(bos.toByteArray()), getPWByte(pw));
            JDIO.saveToFile(files[0], crypted);
        } catch (Exception ew) {
        }
    }
}