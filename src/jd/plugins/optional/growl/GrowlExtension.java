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

package jd.plugins.optional.growl;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import jd.config.ConfigContainer;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDController;
import jd.controlling.SingleDownloadController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.Executer;
import jd.nutils.OSDetector;
import jd.plugins.AddonPanel;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.optional.ExtensionConfigPanel;
import jd.plugins.optional.PluginOptional;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;

public class GrowlExtension extends PluginOptional implements ControlListener {

    private static final String TMP_GROWL_NOTIFICATION_SCPT = "tmp/growlNotification.scpt";

    public void onShutdown() {
    }

    public boolean onShutdownRequest() throws ShutdownVetoException {
        return false;
    }

    public void onShutdownVeto(ArrayList<ShutdownVetoException> vetos) {
    }

    @Override
    protected void stop() throws StopException {
        JDController.getInstance().removeControlListener(this);
    }

    @Override
    protected void start() throws StartException {
        File tmp = Application.getResource(TMP_GROWL_NOTIFICATION_SCPT);
        tmp.delete();
        tmp.deleteOnExit();
        try {
            IO.writeToFile(tmp, IO.readURL(getClass().getResource("osxnopasswordforshutdown.scpt")));
        } catch (IOException e) {
            e.printStackTrace();
            throw new StartException(e);
        }

        JDController.getInstance().addControlListener(this);
    }

    @Override
    protected void initSettings(ConfigContainer config) {
    }

    @Override
    public String getConfigID() {
        return null;
    }

    @Override
    public String getAuthor() {
        return null;
    }

    @Override
    public String getDescription() {
        return JDL.L("jd.plugins.optional.jdgrowlnotification.description", null);
    }

    @Override
    public boolean isLinuxRunnable() {
        return false;
    }

    @Override
    public boolean isWindowsRunnable() {
        return false;
    }

    @Override
    public AddonPanel getGUI() {
        return null;
    }

    @Override
    public ArrayList<MenuAction> getMenuAction() {
        return null;
    }

    private static final String JDL_PREFIX = "jd.plugins.optional.JDGrowlNotification.";

    public ExtensionConfigPanel getConfigPanel() {
        return null;
    }

    public boolean hasConfigPanel() {
        return false;
    }

    public GrowlExtension() throws StartException {
        super(JDL.L("jd.plugins.optional.jdgrowlnotification", null));
    }

    public void onControlEvent(ControlEvent event) {
        switch (event.getEventID()) {
        case ControlEvent.CONTROL_INIT_COMPLETE:
            growlNotification(JDL.L(JDL_PREFIX + "started", "jDownloader started..."), getDateAndTime(), "Programstart");
            break;
        case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
            if (DownloadWatchDog.getInstance().getDownloadssincelastStart() > 0) growlNotification(JDL.L(JDL_PREFIX + "allfinished", "All downloads stopped"), "", "All downloads finished");
            break;
        case ControlEvent.CONTROL_PLUGIN_INACTIVE:
            if (!(event.getCaller() instanceof PluginForHost)) return;
            DownloadLink lastLink = ((SingleDownloadController) event.getParameter()).getDownloadLink();
            if (lastLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                growlNotification(JDL.L(JDL_PREFIX + "finished", "Download stopped"), lastLink.getName(), "Download complete");
            }
            break;
        }
    }

    private void growlNotification(String headline, String message, String title) {
        if (OSDetector.isMac()) {
            Executer exec = new Executer("/usr/bin/osascript");
            exec.addParameter(JDUtilities.getResourceFile(GrowlExtension.TMP_GROWL_NOTIFICATION_SCPT).getAbsolutePath());
            exec.addParameter(headline);
            exec.addParameter(message);
            exec.addParameter(title);
            exec.setWaitTimeout(0);
            exec.start();
        }
    }

    private String getDateAndTime() {
        DateFormat dfmt = new SimpleDateFormat("EEEE dd.MM.yy hh:mm:ss");
        return dfmt.format(new Date());
    }

    public void controlEvent(ControlEvent event) {
    }

    @Override
    protected void initExtension() throws StartException {
    }

}
