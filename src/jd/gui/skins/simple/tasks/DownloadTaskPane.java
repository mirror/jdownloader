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

package jd.gui.skins.simple.tasks;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import jd.controlling.DownloadController;
import jd.controlling.DownloadInformations;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.skins.simple.components.DownloadView.JDProgressBar;
import jd.nutils.Formatter;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class DownloadTaskPane extends TaskPanel {

    private static final long serialVersionUID = -9134449913836967453L;

    private JLabel packages;
    private JLabel downloadlinks;
    private JLabel totalsize;
    private JDProgressBar progress;
    private JLabel speed;
    private JLabel eta;
    private JLabel downloadlist;
    private JLabel progresslabel;
    private Thread fadeTimer;

    private DownloadInformations ds = new DownloadInformations();
    private long speedm = 0;
    private DownloadController dlc = JDUtilities.getDownloadController();

    public DownloadTaskPane(String string, ImageIcon ii) {
        super(string, ii, "downloadtask");
        initGUI();

        fadeTimer = new Thread() {
            public void run() {
                this.setName("DownloadTask: infoupdate");
                while (true) {
                    if (!isCollapsed()) update();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        };
        fadeTimer.start();
    }

    /**
     * TODO: soll mal über events aktuallisiert werden
     */
    private void update() {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                dlc.getDownloadStatus(ds);
                speedm = JDUtilities.getController().getSpeedMeter();
                packages.setText(JDL.LF("gui.taskpanes.download.downloadlist.packages", "%s Packages", ds.getPackagesCount()));
                downloadlinks.setText(JDL.LF("gui.taskpanes.download.downloadlist.downloadLinks", "%s Links", ds.getDownloadCount()));
                totalsize.setText(JDL.LF("gui.taskpanes.download.downloadlist.size", "Total size: %s", Formatter.formatReadable(ds.getTotalDownloadSize())));
                progress.setMaximum(ds.getTotalDownloadSize());
                progress.setValue(ds.getCurrentDownloadSize());
                progress.setToolTipText(Math.round((ds.getCurrentDownloadSize() * 10000.0) / ds.getTotalDownloadSize()) / 100.0 + "%");
                if (speedm > 1024) {
                    speed.setText(JDL.LF("gui.taskpanes.download.progress.speed", "Speed: %s", Formatter.formatReadable(speedm) + "/s"));
                    long etanum = speedm == 0 ? 0 : (ds.getTotalDownloadSize() - ds.getCurrentDownloadSize()) / speedm;
                    eta.setText(JDL.LF("gui.taskpanes.download.progress.eta", "ETA: %s", Formatter.formatSeconds(etanum)));
                } else {
                    eta.setText("");
                    speed.setText("");
                }
                return null;
            }
        }.start();
    }

    private void initGUI() {
        downloadlist = new JLabel(JDL.L("gui.taskpanes.download.downloadlist", "Downloadlist"));
        downloadlist.setIcon(JDTheme.II("gui.splash.dllist", 16, 16));
        packages = new JLabel(JDL.LF("gui.taskpanes.download.downloadlist.packages", "%s Package(s)", 0));
        downloadlinks = new JLabel(JDL.LF("gui.taskpanes.download.downloadlist.downloadLinks", "%s Link(s)", 0));
        totalsize = new JLabel(JDL.LF("gui.taskpanes.download.downloadlist.size", "Total size: %s", 0));
        progresslabel = new JLabel(JDL.L("gui.taskpanes.download.progress", "Total progress"));
        progresslabel.setIcon(JDTheme.II("gui.images.progress", 16, 16));
        progress = new JDProgressBar();
        progress.setStringPainted(false);
        speed = new JLabel(JDL.LF("gui.taskpanes.download.progress.speed", "Speed: %s", 0));
        eta = new JLabel(JDL.LF("gui.taskpanes.download.progress.eta", "ETA: %s", 0));

        add(downloadlist, D1_LABEL_ICON);
        add(packages, D2_LABEL);
        add(downloadlinks, D2_LABEL);
        add(totalsize, D2_LABEL);
        add(progresslabel, D1_LABEL_ICON);
        add(progress, D2_PROGRESSBAR);
        add(speed, D2_LABEL);
        add(eta, D2_LABEL);
    }

    public void actionPerformed(ActionEvent arg0) {
    }
}