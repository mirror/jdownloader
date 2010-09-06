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

/**
 * @author letdoch
 * 
 */

package jd.gui.swing.jdgui.views.linkgrabber;

import java.util.ArrayList;

import jd.controlling.DownloadWatchDog;
import jd.controlling.LinkGrabberController;
import jd.controlling.LinkGrabberControllerEvent;
import jd.controlling.LinkGrabberControllerListener;
import jd.controlling.ProgressController;
import jd.controlling.ProgressControllerEvent;
import jd.controlling.ProgressControllerListener;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.plugins.LinkGrabberFilePackage;
import jd.utils.locale.JDL;

public class DownloadAutostart implements LinkGrabberControllerListener {

    private final static DownloadAutostart instance = new DownloadAutostart();

    private ProgressController pc = null;

    public static DownloadAutostart getInstance() {
        return instance;
    }

    private DownloadAutostart() {
        LinkGrabberController.getInstance().addListener(this);
    }

    public synchronized void launchAutostart() {
        if (pc != null) abortAutostart();
        if (!GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_START_AFTER_ADDING_LINKS_AUTO, false)) return;
        if (LinkGrabberController.getInstance().size() == 0) return;
        pc = new ProgressController(JDL.L("controller.downloadautostart", "Autostart downloads in few seconds..."), null);
        pc.getBroadcaster().addListener(new ProgressControllerListener() {
            public void onProgressControllerEvent(ProgressControllerEvent event) {
                if (event.getID() == ProgressControllerEvent.CANCEL) {
                    pc.setStatusText("Autostart aborted!");
                } else if (event.getID() == ProgressControllerEvent.FINISHED) {
                    if (!pc.isAbort()) {
                        ArrayList<LinkGrabberFilePackage> fps = new ArrayList<LinkGrabberFilePackage>(LinkGrabberController.getInstance().getPackages());
                        for (int i = 0; i < fps.size(); i++) {
                            LinkGrabberPanel.getLinkGrabber().confirmPackage(fps.get(i), null, i);
                        }
                        DownloadWatchDog.getInstance().startDownloads();
                    }
                }
            }
        });
        pc.doFinalize(10 * 1000l);
    }

    public synchronized void abortAutostart() {
        if (pc != null) {
            if (!pc.isFinished()) {
                pc.fireCancelAction();
                pc.setFinished();
            }
            pc = null;
        }
    }

    public void onLinkGrabberControllerEvent(LinkGrabberControllerEvent event) {
        switch (event.getEventID()) {
        case LinkGrabberControllerEvent.FINISHED:
            launchAutostart();
            break;
        case LinkGrabberControllerEvent.ADDED:
        case LinkGrabberControllerEvent.REFRESH_STRUCTURE:
        case LinkGrabberControllerEvent.ADD_FILEPACKAGE:
        case LinkGrabberControllerEvent.FILTER_CHANGED:
        case LinkGrabberControllerEvent.EMPTY:
        case LinkGrabberControllerEvent.NEW_LINKS:
            abortAutostart();
            break;
        }

    }
}
