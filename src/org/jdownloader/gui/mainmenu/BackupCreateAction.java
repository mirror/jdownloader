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
import java.io.IOException;

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
import org.appwork.utils.zip.ZipIOWriter;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.updatev2.ForcedRestartRequest;
import org.jdownloader.updatev2.RestartController;

public class BackupCreateAction extends CustomizableAppAction {

    public static final String HIDE_ON_MAC = "HideOnMac";

    public BackupCreateAction() {

        setIconKey(IconKey.ICON_SAVE);
        setName(_GUI._.BackupCreateAction_BackupCreateAction());
        setTooltipText(_GUI._.BackupCreateAction_BackupCreateAction_tt());

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new Thread("Create Backup") {
            @Override
            public void run() {

                ExtFileChooserDialog d = new ExtFileChooserDialog(0, _GUI._.BackupCreateAction_actionPerformed_filechooser_title(), _GUI._.lit_save(), null);
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
                d.setType(FileChooserType.SAVE_DIALOG);

                try {

                    Dialog.getInstance().showConfirmDialog(0, _GUI._.lit_restart(), _GUI._.BackupCreateAction_run_restart_ask(), null, _GUI._.lit_continue(), null);
                    Dialog.getInstance().showDialog(d);

                    File file = d.getSelectedFile();
                    if (!file.getName().endsWith(".jd2backup")) {
                        file = new File(file.getAbsolutePath() + ".jd2backup");
                    }
                    if (file == null) {
                        return;
                    }
                    if (file.exists()) {
                        Dialog.getInstance().showConfirmDialog(0, _GUI._.lit_overwrite(), _GUI._.file_exists_want_to_overwrite_question(file.getName()));
                        file.delete();
                    }

                    final File ffile = file;
                    ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
                        {
                            setHookPriority(Integer.MIN_VALUE);

                        }

                        @Override
                        public String toString() {
                            return "ShutdownHook: Create Backup";
                        }

                        @Override
                        public void onShutdown(ShutdownRequest shutdownRequest) {
                            try {
                                create(ffile);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Dialog.getInstance().showExceptionDialog(_GUI._.lit_error_occured(), e.getMessage(), e);
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

    public static void create(File auto) throws IOException {
        ZipIOWriter zipper = null;
        try {
            auto.getParentFile().mkdirs();
            zipper = new ZipIOWriter(auto);
            final File root = Application.getResource("cfg").getParentFile();
            final ZipIOWriter fZipper = zipper;

            zipper.addDirectory(Application.getResource("cfg"), false, null);

        } finally {
            try {
                zipper.close();
            } catch (Throwable e) {
            }
        }
    }
}
