//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.gui.swing;

import java.io.File;
import java.util.logging.Logger;

import javax.swing.JFrame;

import jd.Main;
import jd.controlling.JDLogger;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.gui.swing.dialog.AboutDialog;
import jd.gui.swing.jdgui.components.toolbar.actions.ShowSettingsAction;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.update.inapp.RestartController;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;

import com.apple.eawt.AboutHandler;
import com.apple.eawt.AppEvent;
import com.apple.eawt.AppEvent.AboutEvent;
import com.apple.eawt.AppEvent.AppReOpenedEvent;
import com.apple.eawt.AppEvent.OpenFilesEvent;
import com.apple.eawt.AppEvent.PreferencesEvent;
import com.apple.eawt.AppEvent.QuitEvent;
import com.apple.eawt.AppReOpenedListener;
import com.apple.eawt.Application;
import com.apple.eawt.OpenFilesHandler;
import com.apple.eawt.OpenURIHandler;
import com.apple.eawt.PreferencesHandler;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;

public class MacOSApplicationAdapter implements QuitHandler, AboutHandler, PreferencesHandler, AppReOpenedListener, OpenFilesHandler, OpenURIHandler {

    public static void enableMacSpecial() {
        Application macApplication = Application.getApplication();
        final MacOSApplicationAdapter adapter = new MacOSApplicationAdapter();
        macApplication.setAboutHandler(adapter);
        macApplication.setPreferencesHandler(adapter);
        macApplication.setQuitHandler(adapter);
        macApplication.addAppEventListener(adapter);
        macApplication.setOpenFileHandler(adapter);
        macApplication.setOpenURIHandler(adapter);
        Main.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                if (adapter.openURIlinks != null) {
                    LOG.info("Distribute links: " + adapter.openURIlinks);
                    LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(adapter.openURIlinks));
                    adapter.openURIlinks = null;
                }
            }

        });
    }

    private QuitResponse        quitResponse = null;
    private String              openURIlinks;
    private static final Logger LOG          = JDLogger.getLogger();

    private MacOSApplicationAdapter() {
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void run() {
                if (quitResponse != null) quitResponse.performQuit();
            }
        });
    }

    public void handleQuitRequestWith(QuitEvent e, final QuitResponse response) {
        quitResponse = response;
        RestartController.getInstance().exit(true);
    }

    public void handlePreferences(PreferencesEvent e) {
        ShowSettingsAction.getInstance().actionPerformed(null);
        appReOpened(null);
    }

    public void handleAbout(AboutEvent e) {
        try {
            Dialog.getInstance().showDialog(new AboutDialog());
        } catch (DialogNoAnswerException e1) {
        }
    }

    public void appReOpened(AppReOpenedEvent e) {
        final SwingGui swingGui = SwingGui.getInstance();
        if (swingGui == null || swingGui.getMainFrame() == null) return;
        final JFrame mainFrame = swingGui.getMainFrame();
        if (!mainFrame.isVisible()) {
            mainFrame.setVisible(true);
        }
    }

    public void openFiles(OpenFilesEvent e) {
        appReOpened(null);
        LOG.info("Handle open files from Dock " + e.getFiles().toString());
        StringBuilder sb = new StringBuilder();
        for (final File file : e.getFiles()) {
            if (sb.length() > 0) {
                sb.append("\r\n");
            }
            sb.append("file://");
            sb.append(file.getPath());
        }
        LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(sb.toString()));
    }

    public void openURI(AppEvent.OpenURIEvent e) {
        appReOpened(null);
        LOG.info("Handle open uri from Dock " + e.getURI().toString());
        String links = e.getURI().toString();
        if (Main.GUI_COMPLETE.isReached()) {
            LOG.info("Distribute links: " + links);
            LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(links));
        } else {
            openURIlinks = links;
        }
    }
}
