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

package jd.plugins.optional;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import jd.PluginWrapper;
import jd.controlling.DownloadWatchDog;
import jd.controlling.SingleDownloadController;
import jd.event.ControlEvent;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.Executer;
import jd.nutils.OSDetector;
import jd.nutils.io.JDIO;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginOptional;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision$", id = "growl", interfaceversion = 5, windows = false, linux = false)
public class JDGrowlNotification extends PluginOptional {

    private static final String JDL_PREFIX = "jd.plugins.optional.JDGrowlNotification.";

    public JDGrowlNotification(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public boolean initAddon() {
        JDUtilities.getController().addControlListener(this);
        /*
         * because something blocks file after first usage, we copy it to temp
         * one, so updater can update it if needed
         */
        if (!JDIO.copyFile(JDUtilities.getResourceFile("jd/osx/growlNotification.scpt"), JDUtilities.getResourceFile("tmp/growlNotification.scpt", true))) {
            logger.info("Growl Failed");
            return false;
        } else {
            logger.info("Growl OK");
            return true;
        }
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        return null;
    }

    @Override
    public void controlEvent(ControlEvent event) {
        switch (event.getID()) {
        case ControlEvent.CONTROL_INIT_COMPLETE:
            growlNotification(JDL.L(JDL_PREFIX + "started", "jDownloader started..."), getDateAndTime(), "Programstart");
            break;
        case ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED:
            if (DownloadWatchDog.getInstance().getDownloadssincelastStart() > 0) growlNotification(JDL.L(JDL_PREFIX + "allfinished", "All downloads stopped"), "", "All downloads finished");
            break;
        case ControlEvent.CONTROL_PLUGIN_INACTIVE:
            if (!(event.getSource() instanceof PluginForHost)) return;
            DownloadLink lastLink = ((SingleDownloadController) event.getParameter()).getDownloadLink();
            if (lastLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                growlNotification(JDL.L(JDL_PREFIX + "finished", "Download stopped"), lastLink.getFinalFileName(), "Download complete");
            }
            break; 
        default:
            break;
        }

        super.controlEvent(event);
    }

    private void growlNotification(String headline, String message, String title) {
        if (OSDetector.isMac()) {
            Executer exec = new Executer("/usr/bin/osascript");
            exec.addParameter(JDUtilities.getResourceFile("tmp/growlNotification.scpt").getAbsolutePath());
            exec.addParameter(headline);
            exec.addParameter(message);
            exec.addParameter(title);
            exec.setWaitTimeout(0);
            exec.start();
        }
    }

    @Override
    public void onExit() {
        JDUtilities.getController().removeControlListener(this);
    }

    private String getDateAndTime() {
        DateFormat dfmt = new SimpleDateFormat("EEEE dd.MM.yy hh:mm:ss");
        return dfmt.format(new Date());
    }

}
