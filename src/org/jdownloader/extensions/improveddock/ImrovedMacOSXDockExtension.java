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

import java.util.ArrayList;

import jd.config.ConfigContainer;
import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.AddonPanel;

import org.appwork.shutdown.ShutdownVetoException;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;

public class ImrovedMacOSXDockExtension extends AbstractExtension implements ControlListener {

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

    public ExtensionConfigPanel getConfigPanel() {
        return null;
    }

    public boolean hasConfigPanel() {
        return false;
    }

    public ImrovedMacOSXDockExtension() throws StartException {
        super(null);
    }

    public void onControlEvent(ControlEvent event) {
        switch (event.getEventID()) {
        case ControlEvent.CONTROL_DOWNLOAD_START:
            if (updateThread == null) {
                updateThread = new MacDockIconChanger();
                updateThread.start();
            }
            break;
        case ControlEvent.CONTROL_DOWNLOAD_STOP:
            if (updateThread != null) {
                updateThread.stopUpdating();
                updateThread = null;
            }
            break;
        }
    }

    public void onShutdown() {
    }

    public void onShutdownRequest() throws ShutdownVetoException {

    }

    public void onShutdownVeto(ArrayList<ShutdownVetoException> vetos) {
    }

    @Override
    protected void stop() throws StopException {
        if (updateThread != null) {
            updateThread.stopUpdating();
            updateThread = null;
        }
    }

    @Override
    protected void start() throws StartException {
        JDController.getInstance().addControlListener(this);

    }

    @Override
    protected void initSettings(ConfigContainer config) {
        JDController.getInstance().removeControlListener(this);
    }

    @Override
    public String getConfigID() {
        return "improvedmacosxdock";
    }

    @Override
    public String getAuthor() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public AddonPanel getGUI() {
        return null;
    }

    @Override
    public ArrayList<MenuAction> getMenuAction() {
        return null;
    }

    public void controlEvent(ControlEvent event) {
    }

    @Override
    protected void initExtension() throws StartException {
    }

}
