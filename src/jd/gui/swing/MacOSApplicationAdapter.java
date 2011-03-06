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
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.swing.dialog.AboutDialog;
import jd.gui.swing.jdgui.actions.ActionController;

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

public class MacOSApplicationAdapter implements QuitHandler, AboutHandler, PreferencesHandler, AppReOpenedListener, ControlListener, OpenFilesHandler, OpenURIHandler {

    public static void enableMacSpecial() {
        Application macApplication = Application.getApplication();
        MacOSApplicationAdapter adapter = new MacOSApplicationAdapter();
        macApplication.setAboutHandler(adapter);
        macApplication.setPreferencesHandler(adapter);
        macApplication.setQuitHandler(adapter);
        macApplication.addAppEventListener(adapter);
        macApplication.setOpenFileHandler(adapter);
        macApplication.setOpenURIHandler(adapter);
    }

    private QuitResponse        quitResponse;
    private String              openURIlinks;
    private static final Logger LOG = JDLogger.getLogger();

    private MacOSApplicationAdapter() {
    }

    public void handleQuitRequestWith(QuitEvent e, QuitResponse response) {
        JDController.getInstance().addControlListener(this);
        quitResponse = response; // we will respond in controlEvent
        JDController.getInstance().exit();
    }

    public void handlePreferences(PreferencesEvent e) {
        ActionController.getToolBarAction("action.settings").onAction(null);
        appReOpened(null);
    }

    public void handleAbout(AboutEvent e) {
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                new AboutDialog();
                return null;
            }

        }.start();
    }

    public void appReOpened(AppReOpenedEvent e) {
        final SwingGui swingGui = SwingGui.getInstance();
        if (swingGui == null || swingGui.getMainFrame() == null) return;
        final JFrame mainFrame = swingGui.getMainFrame();
        if (!mainFrame.isVisible()) {
            mainFrame.setVisible(true);
        }
    }

    public void controlEvent(ControlEvent event) {
        if (event.getEventID() == ControlEvent.CONTROL_SYSTEM_SHUTDOWN_PREPARED) {
            JDController.getInstance().removeControlListener(this);
            quitResponse.performQuit();
        } else if (event.getEventID() == ControlEvent.CONTROL_INIT_COMPLETE && openURIlinks != null) {
            JDController.getInstance().removeControlListener(this);
            LOG.info("Distribute links: " + openURIlinks);
            JDController.distributeLinks(openURIlinks);
            openURIlinks = null;
        }
    }

    public void openFiles(OpenFilesEvent e) {
        appReOpened(null);
        LOG.info("Handle open files from Dock " + e.getFiles().toString());
        for (File file : e.getFiles()) {
            if (JDController.isContainerFile(file)) {
                LOG.info("Processing Container file: " + file.getAbsolutePath());
                JDController.loadContainerFile(file);
            }
        }
    }

    public void openURI(AppEvent.OpenURIEvent e) {
        appReOpened(null);
        LOG.info("Handle open uri from Dock " + e.getURI().toString());
        String links = e.getURI().toString();
        if (Main.isInitComplete()) {
            LOG.info("Distribute links: " + links);
            JDController.distributeLinks(links);
        } else {
            openURIlinks = links;
            JDController.getInstance().addControlListener(this);
        }
    }
}
