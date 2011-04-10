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

import org.jdownloader.gui.translate.T;

public class DownloadInfoPanel extends InfoPanel {

    private static final long    serialVersionUID = 6127915881119236559L;
    private DownloadInformations ds;
    private long                 speed;

    public DownloadInfoPanel() {
        super("gui.images.taskpanes.download");

        addInfoEntry(T._.jd_gui_swing_jdgui_views_info_DownloadInfoPanel_packages(), "0", 0, 0);
        addInfoEntry(T._.jd_gui_swing_jdgui_views_info_DownloadInfoPanel_links(), "0", 0, 1);
        addInfoEntry(T._.jd_gui_swing_jdgui_views_info_DownloadInfoPanel_size(), "0", 1, 0);
        addInfoEntry(T._.jd_gui_swing_jdgui_views_info_DownloadInfoPanel_speed(), "0", 2, 0);
        addInfoEntry(T._.jd_gui_swing_jdgui_views_info_DownloadInfoPanel_eta(), "0", 2, 1);
        addInfoEntry(T._.jd_gui_swing_jdgui_views_info_DownloadInfoPanel_progress(), "0", 3, 0);
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
                updateInfo(T._.jd_gui_swing_jdgui_views_info_DownloadInfoPanel_speed(), Formatter.formatReadable(speed) + "/s");
                updateInfo(T._.jd_gui_swing_jdgui_views_info_DownloadInfoPanel_eta(), Formatter.formatSeconds(speed == 0 ? -1 : ds.getETA()));
                updateInfo(T._.jd_gui_swing_jdgui_views_info_DownloadInfoPanel_packages(), ds.getPackagesCount());
                updateInfo(T._.jd_gui_swing_jdgui_views_info_DownloadInfoPanel_links(), ds.getDownloadCount());
                updateInfo(T._.jd_gui_swing_jdgui_views_info_DownloadInfoPanel_size(), Formatter.formatReadable(ds.getTotalDownloadSize()));
                updateInfo(T._.jd_gui_swing_jdgui_views_info_DownloadInfoPanel_progress(), ds.getPercent() + "%");
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