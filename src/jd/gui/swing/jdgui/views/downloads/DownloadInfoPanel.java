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

package jd.gui.swing.jdgui.views.downloads;

import jd.controlling.DownloadInformations;
import jd.controlling.DownloadWatchDog;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.views.InfoPanel;
import jd.nutils.Formatter;
import jd.utils.locale.JDL;

public class DownloadInfoPanel extends InfoPanel {

    private static final long    serialVersionUID = 6127915881119236559L;
    private static final String  JDL_PREFIX       = "jd.gui.swing.jdgui.views.info.DownloadInfoPanel.";
    private DownloadInformations ds;
    private long                 speed;

    public DownloadInfoPanel() {
        super("gui.images.taskpanes.download");

        addInfoEntry(JDL.L(JDL_PREFIX + "packages", "Package(s)"), "0", 0, 0);
        addInfoEntry(JDL.L(JDL_PREFIX + "links", "Links(s)"), "0", 0, 1);
        addInfoEntry(JDL.L(JDL_PREFIX + "size", "Total size"), "0", 1, 0);
        addInfoEntry(JDL.L(JDL_PREFIX + "speed", "Downloadspeed"), "0", 2, 0);
        addInfoEntry(JDL.L(JDL_PREFIX + "eta", "Download complete in"), "0", 2, 1);
        addInfoEntry(JDL.L(JDL_PREFIX + "progress", "Progress"), "0", 3, 0);
        ds = DownloadInformations.getInstance();
        Thread updateTimer = new Thread() {
            public void run() {
                this.setName("DownloadView: infoupdate");
                while (true) {
                    update();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        };
        updateTimer.start();
    }

    private void update() {
        if (!isShown()) return;
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                ds.updateInformations();
                if (DownloadWatchDog.getInstance().getActiveDownloads() > 0) {
                    speed = DownloadWatchDog.getInstance().getConnectionManager().getIncommingBandwidthUsage();
                } else {
                    speed = 0;
                }
                updateInfo(JDL.L(JDL_PREFIX + "speed", "Downloadspeed"), Formatter.formatReadable(speed) + "/s");
                updateInfo(JDL.L(JDL_PREFIX + "eta", "Download complete in"), Formatter.formatSeconds(speed == 0 ? -1 : ds.getETA()));
                updateInfo(JDL.L(JDL_PREFIX + "packages", "Package(s)"), ds.getPackagesCount());
                updateInfo(JDL.L(JDL_PREFIX + "links", "Links(s)"), ds.getDownloadCount());
                updateInfo(JDL.L(JDL_PREFIX + "size", "Total size"), Formatter.formatReadable(ds.getTotalDownloadSize()));
                updateInfo(JDL.L(JDL_PREFIX + "progress", "Progress"), ds.getPercent() + "%");
                return null;
            }
        }.start();
    }

    @Override
    public void onHide() {
    }

    @Override
    public void onShow() {
    }

}
