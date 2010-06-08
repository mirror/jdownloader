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
import javax.swing.JOptionPane;

import jd.controlling.JDController;
import jd.gui.swing.dialog.AboutDialog;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

public class MacOSApplicationAdapter extends ApplicationAdapter {

    public static void enableMacSpecial() {
        Application macApplication = Application.getApplication();
        macApplication.addApplicationListener(new MacOSApplicationAdapter());

        // need to enable the preferences option manually
        macApplication.setEnabledPreferencesMenu(true);
    }

    private MacOSApplicationAdapter() {
    }

    @Override
    public void handleQuit(final ApplicationEvent e) {
        JDController.getInstance().exit();
    }

    public void handlePreferences(ApplicationEvent e) {
        e.setHandled(true);
        JOptionPane.showMessageDialog(null, "Show Preferences dialog here");
    }

    @Override
    public void handleAbout(final ApplicationEvent e) {
        e.setHandled(true);
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                new AboutDialog();
                return null;
            }

        }.start();
    }

    @Override
    public void handleReOpenApplication(final ApplicationEvent e) {
        final SwingGui swingGui = SwingGui.getInstance();
        if (swingGui == null || swingGui.getMainFrame() == null) return;
        final JFrame mainFrame = swingGui.getMainFrame();
        if (!mainFrame.isVisible()) {
            mainFrame.setVisible(true);
        }
    }

}
