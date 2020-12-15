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
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;

import javax.swing.filechooser.FileFilter;

import org.appwork.loggingv3.LogV3;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.LimitedInputStream;
import org.appwork.utils.net.NullInputStream;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.FileChooserType;
import org.appwork.utils.zip.ZipIOException;
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
        setName(_GUI.T.BackupCreateAction_BackupCreateAction());
        setTooltipText(_GUI.T.BackupCreateAction_BackupCreateAction_tt());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new Thread("Create Backup") {
            @Override
            public void run() {
                ExtFileChooserDialog d = new ExtFileChooserDialog(0, _GUI.T.BackupCreateAction_actionPerformed_filechooser_title(), _GUI.T.lit_save(), null);
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
                    Dialog.getInstance().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.lit_restart(), _GUI.T.BackupCreateAction_run_restart_ask(), null, _GUI.T.lit_continue(), null);
                    Dialog.getInstance().showDialog(d);
                    File file = d.getSelectedFile();
                    if (file == null) {
                        return;
                    } else if (!file.getName().endsWith(".jd2backup")) {
                        file = new File(file.getAbsolutePath() + ".jd2backup");
                    }
                    if (file.exists()) {
                        Dialog.getInstance().showConfirmDialog(0, _GUI.T.lit_overwrite(), _GUI.T.file_exists_want_to_overwrite_question(file.getName()));
                        file.delete();
                    }
                    final File backupFile = file;
                    ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
                        {
                            setHookPriority(Integer.MIN_VALUE);
                        }

                        @Override
                        public String toString() {
                            return "ShutdownHook: Create Backup";
                        }

                        @Override
                        public long getMaxDuration() {
                            return 0;
                        }

                        @Override
                        public void onShutdown(ShutdownRequest shutdownRequest) {
                            try {
                                create(backupFile);
                            } catch (Throwable e) {
                                LogV3.defaultLogger().log(e);
                                backupFile.delete();
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
        boolean bad = true;
        try {
            if (!auto.getParentFile().exists()) {
                auto.getParentFile().mkdirs();
            }
            final StringBuilder incompleteList = new StringBuilder();
            zipper = new ZipIOWriter(auto) {
                @Override
                protected void addDirectoryInternal(File addDirectory, boolean compress, String path) throws ZipIOException, IOException {
                    super.addDirectoryInternal(addDirectory, compress, path);
                }

                @Override
                protected boolean throwExceptionOnFileGone(File file) {
                    return false;
                }

                private boolean isFiltered(final File file) {
                    final String name = file.getName();
                    if (StringUtils.startsWithCaseInsensitive(name, "RememberRelativeLocator-")) {
                        return true;
                    } else if (StringUtils.startsWithCaseInsensitive(name, "RememberLastDimensor-")) {
                        return true;
                    } else if (StringUtils.startsWithCaseInsensitive(name, "RememberAbsoluteLocator-")) {
                        return true;
                    } else if (StringUtils.startsWithCaseInsensitive(name, "CaptchaDialogDimensions_")) {
                        return true;
                    } else {
                        return false;
                    }
                }

                final byte[] buf = new byte[32 * 1024];

                @Override
                public synchronized void addFile(final File addFile, final boolean compress, final String fullPath) throws ZipIOException, IOException, FileNotFoundException {
                    if (addFile == null) {
                        throw new ZipIOException("addFile invalid:null");
                    }
                    if (isFiltered(addFile)) {
                        return;
                    }
                    try {
                        FileInputStream fis = new FileInputStream(addFile);
                        try {
                            InputStream is = fis;
                            long remaining = addFile.length();
                            ZipEntry entry = null;
                            try {
                                if (remaining == 0) {
                                    entry = new ZipEntry(fullPath);
                                    entry.setSize(remaining);
                                    entry.setMethod(ZipEntry.DEFLATED);
                                    this.zipStream.putNextEntry(entry);
                                } else {
                                    long written = 0;
                                    while (remaining > 0) {
                                        final int read;
                                        try {
                                            read = is.read(buf);
                                            // if (DebugMode.TRUE_IN_IDE_ELSE_FALSE && new Random().nextInt(100) > 20 && is instanceof
                                            // FileInputStream) {
                                            // throw new IOException("Random");
                                            // }
                                            if (read == -1) {
                                                throw new EOFException();
                                            }
                                        } catch (IOException e) {
                                            e = new IOException(written + "/" + remaining + "|" + addFile.getAbsolutePath(), e);
                                            LogV3.defaultLogger().log(e);
                                            if (entry == null) {
                                                // nothing written yet, ignore
                                                return;
                                            } else if (is instanceof FileInputStream) {
                                                incompleteList.append(addFile.getAbsolutePath()).append("\r\n");
                                                // continue with NullInputstream to keep zip file intact
                                                try {
                                                    fis.close();
                                                } catch (IOException ignore) {
                                                    LogV3.defaultLogger().log(ignore);
                                                } finally {
                                                    fis = null;
                                                }
                                                is = new LimitedInputStream(new NullInputStream(), remaining);
                                                continue;
                                            } else {
                                                throw e;
                                            }
                                        }
                                        if (read > 0) {
                                            if (entry == null) {
                                                entry = new ZipEntry(fullPath);
                                                entry.setSize(remaining);
                                                entry.setMethod(ZipEntry.DEFLATED);
                                                this.zipStream.putNextEntry(entry);
                                            }
                                            this.zipStream.write(buf, 0, read);
                                            written += read;
                                            remaining -= read;
                                            notify(entry, read, written);
                                        }
                                    }
                                }
                            } finally {
                                if (entry != null) {
                                    this.zipStream.closeEntry();
                                }
                            }
                        } finally {
                            try {
                                if (fis != null) {
                                    fis.close();
                                }
                            } catch (IOException ignore) {
                                LogV3.defaultLogger().log(ignore);
                            }
                        }
                    } catch (FileNotFoundException e) {
                        incompleteList.append(addFile.getAbsolutePath()).append("\r\n");
                        LogV3.defaultLogger().log(e);
                        if (addFile.exists() == false) {
                            if (throwExceptionOnFileGone(addFile)) {
                                throw e;
                            } else {
                                return;
                            }
                        } else {
                            throw e;
                        }
                    } catch (IOException e) {
                        LogV3.defaultLogger().log(e);
                        throw e;
                    }
                }
            };
            zipper.addDirectory(Application.getResource("cfg"), false, null);
            if (incompleteList.length() > 0) {
                zipper.addByteArry(incompleteList.toString().getBytes("UTF-8"), true, "cfg", "IncompleteBackupFiles.txt");
            }
            bad = false;
        } finally {
            try {
                zipper.close();
            } catch (Throwable e) {
            }
            if (bad) {
                auto.delete();
            }
        }
    }
}
