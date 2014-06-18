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

package org.jdownloader.gui.mainmenu;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.filechooser.FileFilter;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.utils.Application;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.FileChooserType;
import org.appwork.utils.zip.ZipIOReader;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.updatev2.ForcedRestartRequest;
import org.jdownloader.updatev2.RestartController;

public class BackupRestoreAction extends CustomizableAppAction {

    public BackupRestoreAction() {

        setIconKey(IconKey.ICON_LOAD);
        setName(_GUI._.BackupRestoreAction_BackupRestoreAction());
        setTooltipText(_GUI._.BackupRestoreAction_BackupRestoreAction_tt());

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new Thread("Restore Backup") {
            @Override
            public void run() {

                ExtFileChooserDialog d = new ExtFileChooserDialog(0, _GUI._.BackupCreateAction_actionPerformed_filechooser_title(), _GUI._.lit_open(), null);
                d.setFileFilter(new FileFilter() {

                    @Override
                    public String getDescription() {
                        return "*.jd2backup";
                    }

                    @Override
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().endsWith(".jd2backup");
                    }
                });

                d.setFileSelectionMode(FileChooserSelectionMode.FILES_ONLY);
                d.setMultiSelection(false);
                d.setType(FileChooserType.OPEN_DIALOG);

                try {
                    int i = 1;
                    File auto = Application.getResource("autobackup/backup_" + i + ".jd2backup");
                    while (auto.exists()) {
                        i++;
                        auto = Application.getResource("autobackup/backup_" + i + ".jd2backup");
                    }
                    final File fauto = auto;
                    Dialog.getInstance().showConfirmDialog(0, _GUI._.lit_restart(), _GUI._.BackupRestoreAction_run_restart_ask(auto.getAbsolutePath()), null, _GUI._.lit_continue(), null);
                    Dialog.getInstance().showDialog(d);

                    final File file = d.getSelectedFile();

                    if (file == null || !file.exists()) {
                        return;
                    }

                    ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
                        {
                            setHookPriority(Integer.MIN_VALUE);

                        }

                        @Override
                        public String toString() {
                            return "ShutdownHook: Restore Backup";
                        }

                        @Override
                        public void onShutdown(ShutdownRequest shutdownRequest) {
                            ZipIOReader zip = null;
                            try {
                                BackupCreateAction.create(fauto);

                                zip = new ZipIOReader(file);
                                File tmp = Application.getTempResource("restorebackup_" + System.currentTimeMillis());
                                while (tmp.exists()) {
                                    tmp = Application.getTempResource("restorebackup_" + System.currentTimeMillis());
                                }

                                File backup = Application.getResource("cfg_backup_" + System.currentTimeMillis());
                                while (backup.exists()) {
                                    backup = Application.getResource("cfg_backup_" + System.currentTimeMillis());
                                }
                                zip.extractTo(tmp);

                                Application.getResource("cfg").renameTo(backup);
                                if (Application.getResource("cfg").exists()) {
                                    throw new Exception("Could not delete " + Application.getResource("cfg"));
                                }
                                new File(tmp, "cfg").renameTo(Application.getResource("cfg"));

                            } catch (Exception e) {
                                e.printStackTrace();
                                Dialog.getInstance().showExceptionDialog(_GUI._.lit_error_occured(), e.getMessage() + "\r\nPlease try to close JDownloader, and extract the file\r\n" + file.getAbsolutePath() + "\r\nto " + Application.getResource("cfg").getParent() + "\r\nusing an application like WinZip, 7Zip or Winrar.\r\nIf this does not work, feel free to contact our support.", e);
                            } finally {
                                try {
                                    zip.close();
                                } catch (Throwable e) {
                                }
                            }
                        }
                    });
                    RestartController.getInstance().directRestart(new ForcedRestartRequest());
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }
            }
        }.start();
    }

}
