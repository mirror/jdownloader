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

import javax.swing.JFrame;

import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.swing.dialog.AboutDialog;
import jd.gui.swing.jdgui.actions.ActionController;

import com.apple.eawt.AboutHandler;
import com.apple.eawt.AppEvent.AboutEvent;
import com.apple.eawt.AppEvent.AppReOpenedEvent;
import com.apple.eawt.AppEvent.PreferencesEvent;
import com.apple.eawt.AppEvent.QuitEvent;
import com.apple.eawt.AppReOpenedListener;
import com.apple.eawt.Application;
import com.apple.eawt.PreferencesHandler;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;

public class MacOSApplicationAdapter implements QuitHandler, AboutHandler, PreferencesHandler, AppReOpenedListener, ControlListener {

    public static void enableMacSpecial() {
        Application macApplication = Application.getApplication();
        MacOSApplicationAdapter adapter = new MacOSApplicationAdapter();
        macApplication.setAboutHandler(adapter);
        macApplication.setPreferencesHandler(adapter);
        macApplication.setQuitHandler(adapter);
        macApplication.addAppEventListener(adapter);

    }

    private QuitResponse quitResponse;

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
        }
    }
}
