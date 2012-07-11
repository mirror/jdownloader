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

package org.jdownloader.extensions.improveddock;

import javax.swing.JMenuItem;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.plugins.AddonPanel;

import org.appwork.controlling.StateEvent;
import org.appwork.controlling.StateEventListener;
import org.appwork.txtresource.TranslateInterface;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;

public class ImprovedMacOSXDockExtension extends AbstractExtension<ImprovedMacOSXDockConfig, TranslateInterface> implements StateEventListener {

    @Override
    public boolean isLinuxRunnable() {
        return false;
    }

    @Override
    public boolean isWindowsRunnable() {
        return false;
    }

    @Override
    public boolean isMacRunnable() {
        return super.isMacRunnable();
    }

    private MacDockIconChanger updateThread;

    public ExtensionConfigPanel<ImprovedMacOSXDockExtension> getConfigPanel() {
        return null;
    }

    public boolean hasConfigPanel() {
        return false;
    }

    public ImprovedMacOSXDockExtension() throws StartException {
        super();
        setTitle("ImprovedDock");

    }

    @Override
    protected void stop() throws StopException {
        DownloadWatchDog.getInstance().getStateMachine().removeListener(this);
        if (updateThread != null) {
            updateThread.stopUpdating();
            updateThread = null;
        }
    }

    @Override
    protected void start() throws StartException {
        DownloadWatchDog.getInstance().getStateMachine().addListener(this);
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public AddonPanel<ImprovedMacOSXDockExtension> getGUI() {
        return null;
    }

    @Override
    public java.util.ArrayList<JMenuItem> getMenuAction() {
        return null;
    }

    @Override
    protected void initExtension() throws StartException {
    }

    public void onStateChange(StateEvent event) {
        if (DownloadWatchDog.IDLE_STATE == event.getNewState() || DownloadWatchDog.STOPPED_STATE == event.getNewState()) {
            if (updateThread != null) {
                updateThread.stopUpdating();
                updateThread = null;
            }
        } else if (DownloadWatchDog.RUNNING_STATE == event.getNewState()) {
            if (updateThread == null) {
                updateThread = new MacDockIconChanger();
                updateThread.start();
            }
        }
    }

    public void onStateUpdate(StateEvent event) {
    }

}
