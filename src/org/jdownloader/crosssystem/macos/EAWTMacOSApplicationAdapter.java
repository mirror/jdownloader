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
package org.jdownloader.crosssystem.macos;

import java.io.File;

import javax.swing.JFrame;

import jd.SecondLevelLaunch;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkOrigin;
import jd.gui.swing.dialog.AboutDialog;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.ReflectionUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.CrossSystem.OperatingSystem;
import org.appwork.utils.swing.windowmanager.WindowManager;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyExitRequest;

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

public class EAWTMacOSApplicationAdapter implements QuitHandler, AboutHandler, PreferencesHandler, AppReOpenedListener, OpenFilesHandler, OpenURIHandler {
    public static void enableMacSpecial() throws Exception {
        final Application macApplication = Application.getApplication();
        final EAWTMacOSApplicationAdapter adapter = new EAWTMacOSApplicationAdapter();
        ReflectionUtils.invoke(macApplication.getClass(), "setAboutHandler", macApplication, void.class, adapter);
        ReflectionUtils.invoke(macApplication.getClass(), "setPreferencesHandler", macApplication, void.class, adapter);
        ReflectionUtils.invoke(macApplication.getClass(), "setQuitHandler", macApplication, void.class, adapter);
        ReflectionUtils.invoke(macApplication.getClass(), "addAppEventListener", macApplication, void.class, adapter);
        ReflectionUtils.invoke(macApplication.getClass(), "setOpenFileHandler", macApplication, void.class, adapter);
        ReflectionUtils.invoke(macApplication.getClass(), "setOpenURIHandler", macApplication, void.class, adapter);
        if (CrossSystem.getOS().isMinimum(OperatingSystem.MAC_SIERRA)) {
            SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {
                public void run() {
                    try {
                        com.apple.eawt.FullScreenUtilities.setWindowCanFullScreen(JDGui.getInstance().getMainFrame(), true);
                        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("MacOS FullScreen Support activated");
                    } catch (Throwable e) {
                        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                    }
                }
            });
        }
        MacOSDockAdapter.init();
    }

    private EAWTMacOSApplicationAdapter() {
    }

    public void handleQuitRequestWith(QuitEvent e, final QuitResponse response) {
        RestartController.getInstance().exitAsynch(new SmartRlyExitRequest() {
            @Override
            public void onShutdown() {
                new Thread("QuitResponse:performQuit") {
                    public void run() {
                        /*
                         * own thread because else it will block, performQuit calls exit again
                         */
                        response.performQuit();
                    };
                }.start();
            }

            @Override
            public void onShutdownVeto() {
                new Thread("cancelQuit") {
                    public void run() {
                        /*
                         * own thread because else it will block, performQuit calls exit again
                         */
                        response.cancelQuit();
                    };
                }.start();
            }
        });
    }

    public void handlePreferences(PreferencesEvent e) {
        appReOpened(null);
        JsonConfig.create(GraphicalUserInterfaceSettings.class).setConfigViewVisible(true);
        JDGui.getInstance().setContent(ConfigurationView.getInstance(), true);
    }

    public void handleAbout(AboutEvent e) {
        AboutDialog.showNonBlocking();
    }

    public void appReOpened(AppReOpenedEvent e) {
        final JDGui swingGui = JDGui.getInstance();
        if (swingGui == null || swingGui.getMainFrame() == null) {
            return;
        }
        final JFrame mainFrame = swingGui.getMainFrame();
        if (!mainFrame.isVisible()) {
            WindowManager.getInstance().setVisible(mainFrame, true, FrameState.OS_DEFAULT);
        }
    }

    public void openFiles(final OpenFilesEvent e) {
        appReOpened(null);
        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("Handle open files from Dock " + e.getFiles().toString());
        final StringBuilder sb = new StringBuilder();
        for (final File file : e.getFiles()) {
            if (sb.length() > 0) {
                sb.append("\r\n");
            }
            sb.append(file.toURI().toString());
        }
        final String links = sb.toString();
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {
            @Override
            public void run() {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("Distribute links: " + links);
                LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(LinkOrigin.MAC_DOCK.getLinkOriginDetails(), links));
            }
        });
    }

    public void openURI(final AppEvent.OpenURIEvent e) {
        appReOpened(null);
        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("Handle open uri from Dock " + e.getURI().toString());
        final String links = e.getURI().toString();
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {
            @Override
            public void run() {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("Distribute links: " + links);
                LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(LinkOrigin.MAC_DOCK.getLinkOriginDetails(), links));
            }
        });
    }
}
