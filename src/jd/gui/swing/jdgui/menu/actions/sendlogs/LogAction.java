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

package jd.gui.swing.jdgui.menu.actions.sendlogs;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;

import jd.Launcher;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.appwork.utils.zip.ZipIOException;
import org.appwork.utils.zip.ZipIOWriter;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.jdserv.JD_SERV_CONSTANTS;
import org.jdownloader.jdserv.UploadInterface;
import org.jdownloader.logging.LogController;

public class LogAction extends AppAction {

    protected String id;

    public LogAction() {
        super();
        setName(_GUI._.LogAction());
        setIconKey("log");
        setTooltipText(_GUI._.LogAction_tooltip());

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        final ProgressDialog p = new ProgressDialog(new ProgressGetter() {
            private int total;
            private int current;

            @Override
            public int getProgress() {
                return -1;

            }

            @Override
            public String getString() {
                return _GUI._.LogAction_getString_uploading_();
            }

            @Override
            public void run() throws Exception {
                create();
            }

            @Override
            public String getLabelString() {
                return null;
            }
        }, Dialog.BUTTONS_HIDE_OK, _GUI._.LogAction_actionPerformed_zip_title_(), _GUI._.LogAction_actionPerformed_wait_(), null, null, null);

        try {
            Dialog.getInstance().showDialog(p);
        } catch (Throwable e1) {

        }

    }

    protected void create() {

        File[] logs = Application.getResource("logs").listFiles();
        final java.util.List<LogFolder> folders = new ArrayList<LogFolder>();

        LogFolder latest = null;

        if (logs != null) {
            for (File f : logs) {
                String timestampString = new Regex(f.getName(), "(\\d+)_\\d\\d\\.\\d\\d").getMatch(0);
                if (timestampString != null) {
                    long timestamp = Long.parseLong(timestampString);
                    LogFolder lf;
                    lf = new LogFolder(f, timestamp);
                    if (Launcher.startup == timestamp) {
                        /* this is our current logfolder, flush it before we can upload it */
                        lf.setNeedsFlush(true);
                    }
                    if (Files.getFiles(new FileFilter() {
                        @Override
                        public boolean accept(File pathname) {
                            return pathname.isFile() && pathname.length() > 0;
                        }
                    }, f).size() == 0) continue;

                    folders.add(lf);
                    if (latest == null || lf.getCreated() > latest.getCreated()) {
                        latest = lf;
                    }
                }
            }
        }
        if (latest != null) latest.setSelected(true);

        if (folders.size() == 0) {
            Dialog.getInstance().showExceptionDialog("WTF!", "At Least the current Log should be available", new WTFException());
            return;
        }
        SendLogDialog d = new SendLogDialog(folders);
        try {
            Dialog.getInstance().showDialog(d);

            final java.util.List<LogFolder> selection = d.getSelectedFolders();

            final ProgressDialog p = new ProgressDialog(new ProgressGetter() {
                private int total;
                private int current;

                {
                    total = selection.size();
                    current = 0;
                }

                @Override
                public int getProgress() {
                    if (current == 0) return -1;
                    return current * 100 / total;
                }

                @Override
                public String getString() {
                    return _GUI._.LogAction_getString_uploading_();
                }

                @Override
                public void run() throws Exception {
                    id = null;
                    try {
                        for (LogFolder lf : selection) {
                            final File zip = Application.getResource("tmp/logs/logPackage.zip");
                            zip.delete();
                            zip.getParentFile().mkdirs();
                            ZipIOWriter writer = null;
                            try {
                                if (lf.isNeedsFlush()) {
                                    LogController.getInstance().flushSinks(true);
                                }
                                writer = new ZipIOWriter(zip) {
                                    public void addFile(final File addFile, final boolean compress, final String fullPath) throws FileNotFoundException, ZipIOException, IOException {
                                        if (addFile.getName().endsWith(".lck") || (addFile.isFile() && addFile.length() == 0)) return;
                                        if (Thread.currentThread().isInterrupted()) throw new WTFException("INterrupted");
                                        super.addFile(addFile, compress, fullPath);
                                    }
                                };

                                writer.addDirectory(lf.getFolder(), true, null);
                            } finally {
                                try {
                                    writer.close();
                                } catch (final Throwable e) {
                                }
                            }
                            if (Thread.currentThread().isInterrupted()) throw new WTFException("INterrupted");
                            id = JD_SERV_CONSTANTS.CLIENT.create(UploadInterface.class).upload(IO.readFile(zip), "", id);
                            if (Thread.currentThread().isInterrupted()) throw new WTFException("INterrupted");
                            current++;
                        }
                    } catch (WTFException e) {
                        throw new InterruptedException();
                    }

                }

                @Override
                public String getLabelString() {
                    return null;
                }
            }, Dialog.BUTTONS_HIDE_OK, _GUI._.LogAction_actionPerformed_zip_title_(), _GUI._.LogAction_actionPerformed_wait_(), null, null, null);

            try {
                Dialog.getInstance().showDialog(p);

                Dialog.getInstance().showInputDialog(0, _GUI._.LogAction_actionPerformed_givelogid_(), "jdlog://" + id);
            } catch (final DialogNoAnswerException e1) {
                Log.exception(Level.WARNING, e1);
            }
        } catch (DialogNoAnswerException e1) {
            e1.printStackTrace();
        }
    }
}
