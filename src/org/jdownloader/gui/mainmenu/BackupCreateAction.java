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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;

import javax.swing.filechooser.FileFilter;

import org.appwork.loggingv3.LogV3;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
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
import org.seamless.util.io.IO;

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
                        private final AtomicBoolean runningFlag  = new AtomicBoolean(true);
                        private final AtomicLong    progress     = new AtomicLong(0);
                        private final AtomicBoolean activityFlag = new AtomicBoolean(false);
                        {
                            setHookPriority(Integer.MIN_VALUE);
                        }

                        @Override
                        public long getMaxDuration() {
                            return 60 * 1000l;
                        }

                        @Override
                        public String toString() {
                            return "ShutdownHook: Create Backup";
                        }

                        @Override
                        protected void waitFor() {
                            long last = progress.get();
                            int noProgressCheck = 30;
                            while (runningFlag.get() && noProgressCheck > 0) {
                                if (progress.get() == last && !activityFlag.get()) {
                                    noProgressCheck--;
                                } else {
                                    last = progress.get();
                                    noProgressCheck = 30;
                                }
                                synchronized (runningFlag) {
                                    if (runningFlag.get()) {
                                        try {
                                            runningFlag.wait(1000);
                                        } catch (InterruptedException e) {
                                        }
                                    }
                                }
                            }
                        }

                        @Override
                        public void onShutdown(ShutdownRequest shutdownRequest) {
                            try {
                                runningFlag.set(true);
                                create(backupFile, progress, activityFlag);
                            } catch (Throwable e) {
                                LogV3.defaultLogger().log(e);
                                Dialog.getInstance().showExceptionDialog(_GUI.T.lit_error_occured(), e.getMessage(), e);
                            } finally {
                                synchronized (runningFlag) {
                                    runningFlag.set(false);
                                    runningFlag.notifyAll();
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

    public static void create(File auto, final AtomicLong createAlive, final AtomicBoolean activityFlag) throws IOException {
        ZipIOWriter zipper = null;
        boolean bad = true;
        try {
            if (!auto.getParentFile().exists()) {
                auto.getParentFile().mkdirs();
            }
            zipper = new ZipIOWriter(auto) {
                @Override
                protected void notify(ZipEntry entry, long bytesWrite, long bytesProcessed) {
                    if (createAlive != null) {
                        if (entry.isDirectory()) {
                            createAlive.incrementAndGet();
                        } else {
                            createAlive.addAndGet(bytesWrite);
                        }
                    }
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

                @Override
                public synchronized void addFile(final File addFile, final boolean compress, final String fullPath) throws ZipIOException, IOException, FileNotFoundException {
                    if (addFile == null) {
                        throw new ZipIOException("addFile invalid:null");
                    }
                    if (isFiltered(addFile)) {
                        createAlive.incrementAndGet();
                        return;
                    }
                    boolean zipEntryAdded = false;
                    try {
                        if (activityFlag != null) {
                            activityFlag.set(true);
                        }
                        final byte[] bytes;
                        try {
                            bytes = IO.readBytes(addFile);
                        } catch (final FileNotFoundException e) {
                            throw e;
                        } catch (final IOException e) {
                            if (!addFile.exists()) {
                                throw new FileNotFoundException(addFile.getAbsolutePath());
                            } else {
                                throw e;
                            }
                        }
                        final ZipEntry zipAdd = new ZipEntry(fullPath);
                        final long size = bytes.length;
                        zipAdd.setSize(size);
                        if (compress) {
                            zipAdd.setMethod(ZipEntry.DEFLATED);
                        } else {
                            zipAdd.setMethod(ZipEntry.STORED);
                            zipAdd.setCompressedSize(size);
                            /* STORED must have a CRC32! */
                            zipAdd.setCrc(Hash.getCRC32(bytes));
                        }
                        this.zipStream.putNextEntry(zipAdd);
                        zipEntryAdded = true;
                        this.zipStream.write(bytes);
                        notify(zipAdd, size, size);
                    } catch (FileNotFoundException e) {
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
                    } finally {
                        if (activityFlag != null) {
                            activityFlag.set(false);
                        }
                        if (zipEntryAdded) {
                            this.zipStream.closeEntry();
                        }
                    }
                }
            };
            zipper.addDirectory(Application.getResource("cfg"), false, null);
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
