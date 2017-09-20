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

import java.io.File;
import java.io.IOException;
import java.util.List;

import jd.controlling.ClipboardMonitoring;

import org.appwork.exceptions.WTFException;
import org.appwork.uio.UIOManager;
import org.appwork.utils.logging2.LogSink.FLUSH;
import org.appwork.utils.logging2.LogSourceProvider;
import org.appwork.utils.logging2.sendlogs.AbstractLogAction;
import org.appwork.utils.logging2.sendlogs.LogFolder;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.jdserv.JDServUtils;
import org.jdownloader.logging.LogController;
import org.jdownloader.startup.commands.ThreadDump;

public class LogAction extends AbstractLogAction {
    protected String id;

    public LogAction() {
        super();
        setName(_GUI.T.LogAction());
        setSmallIcon(new AbstractIcon(IconKey.ICON_LOG, 22));
        setTooltipText(_GUI.T.LogAction_tooltip());
        id = null;
    }

    @Override
    protected void createPackage(List<LogFolder> selection) throws Exception {
        id = null;
        new ThreadDump().run(null, new String[0]);
        super.createPackage(selection);
        final String id = this.id;
        if (id != null) {
            final String name = format(selection.get(0).getCreated()) + " <--> " + format(selection.get(selection.size() - 1).getLastModified());
            final String jdLog = "jdlog://" + id + "/";
            ClipboardMonitoring.getINSTANCE().setCurrentContent(jdLog);
            Dialog.getInstance().showInputDialog(UIOManager.BUTTONS_HIDE_CANCEL, _GUI.T.LogAction_actionPerformed_givelogid_(), name + " " + jdLog);
        }
    }

    @Override
    protected void onNewPackage(File zip, String name) throws IOException {
        try {
            if (Thread.currentThread().isInterrupted()) {
                throw new WTFException("INterrupted");
            }
            id = JDServUtils.uploadLog(zip, id);
            if (Thread.currentThread().isInterrupted()) {
                throw new WTFException("INterrupted");
            }
        } catch (Exception e) {
            Dialog.getInstance().showExceptionDialog("Exception ocurred", e.getMessage(), e);
        }
    }

    @Override
    protected void flushLogs() {
        LogSourceProvider.flushAllSinks(FLUSH.FORCE);
    }

    @Override
    protected boolean isCurrentLogFolder(long timestamp) {
        final long startup = LogController.getInstance().getInitTime();
        return startup == timestamp;
    }
}
