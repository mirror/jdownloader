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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;

import jd.parser.Regex;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Application;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.appwork.utils.zip.ZipIOException;
import org.appwork.utils.zip.ZipIOWriter;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class LogAction extends AppAction {

    public LogAction() {
        super();
        setName(_GUI._.LogAction());
        setIconKey("log");
        setTooltipText(_GUI._.LogAction_tooltip());

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        File[] logs = Application.getResource("logs").listFiles();
        ArrayList<LogFolder> folders = new ArrayList<LogFolder>();
        LogFolder latest = null;

        if (logs != null) {
            for (File f : logs) {
                String timestampString = new Regex(f.getName(), "(\\d+)_\\d\\d\\.\\d\\d").getMatch(0);
                if (timestampString != null) {
                    long timestamp = Long.parseLong(timestampString);
                    LogFolder lf;
                    folders.add(lf = new LogFolder(f, timestamp));
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

            final ArrayList<LogFolder> selection = d.getSelectedFolders();
            final File zip = Application.getResource("tmp/logs/" + System.currentTimeMillis() + "/bugreport_" + System.currentTimeMillis() + ".zip");
            zip.getParentFile().mkdirs();

            final ProgressDialog p = new ProgressDialog(new ProgressGetter() {

                @Override
                public int getProgress() {

                    return -1;
                }

                @Override
                public String getString() {
                    return "";
                }

                @Override
                public void run() throws Exception {
                    ZipIOWriter writer = new ZipIOWriter(zip) {
                        public void addFile(final File addFile, final boolean compress, final String fullPath) throws FileNotFoundException, ZipIOException, IOException {
                            if (addFile.getName().endsWith(".lck") || (addFile.isFile() && addFile.length() == 0)) return;
                            super.addFile(addFile, compress, fullPath);
                        }

                    };

                    for (LogFolder lf : selection) {
                        writer.addDirectory(lf.getFolder(), true, null);
                    }
                    writer.close();

                    CrossSystem.openFile(zip.getParentFile());

                }

                @Override
                public String getLabelString() {
                    return null;
                }
            }, Dialog.BUTTONS_HIDE_OK, _GUI._.LogAction_actionPerformed_zip_title_(), _GUI._.LogAction_actionPerformed_wait_(), null, null, null);

            try {
                Dialog.getInstance().showDialog(p);

            } catch (final DialogClosedException e1) {
                Log.exception(Level.WARNING, e1);

            } catch (final DialogCanceledException e1) {
                Log.exception(Level.WARNING, e1);

            }
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }
}
