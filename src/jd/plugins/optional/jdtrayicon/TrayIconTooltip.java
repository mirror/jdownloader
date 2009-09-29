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

package jd.plugins.optional.jdtrayicon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.TrayIcon;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import jd.controlling.DownloadController;
import jd.controlling.DownloadInformations;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.components.JDProgressBar;
import jd.nutils.Formatter;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class TrayIconTooltip extends JWindow {

    private static final long serialVersionUID = -400023413449818691L;

    private TrayInfo trayInfo;

    private JLabel lblSpeed;
    private JLabel lblDlRunning;
    private JLabel lblDlFinished;
    private JLabel lblDlTotal;
    private JDProgressBar prgTotal;
    private JLabel lblETA;
    private JLabel lblProgress;

    private Point estimatedTopLeft;
    private TrayIcon trayIcon;

    private DownloadInformations ds = new DownloadInformations();

    public TrayIconTooltip() {

        JPanel toolPanel = new JPanel(new MigLayout("wrap 2", "[fill, grow][fill, grow]"));
        toolPanel.setOpaque(true);
        toolPanel.setBackground(new Color(0xb9cee9));
        toolPanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, toolPanel.getBackground().darker()));

        toolPanel.add(new JLabel(JDL.L("plugins.optional.trayIcon.downloads", "Downloads:")), "spanx 2");
        toolPanel.add(new JLabel(JDL.L("plugins.optional.trayIcon.dl.running", "Running:")), "gapleft 10");
        toolPanel.add(lblDlRunning = new JLabel(""));
        toolPanel.add(new JLabel(JDL.L("plugins.optional.trayIcon.dl.finished", "Finished:")), "gapleft 10");
        toolPanel.add(lblDlFinished = new JLabel(""));
        toolPanel.add(new JLabel(JDL.L("plugins.optional.trayIcon.dl.total", "Total:")), "gapleft 10");
        toolPanel.add(lblDlTotal = new JLabel(""));
        toolPanel.add(new JLabel(JDL.L("plugins.optional.trayIcon.speed", "Speed:")));
        toolPanel.add(lblSpeed = new JLabel(""));
        toolPanel.add(new JLabel(JDL.L("plugins.optional.trayIcon.progress", "Progress: ")));
        toolPanel.add(lblProgress = new JLabel(""));
        toolPanel.add(prgTotal = new JDProgressBar(), "spanx 2");
        toolPanel.add(new JLabel(JDL.L("plugins.optional.trayIcon.eta", "ETA:")));
        toolPanel.add(lblETA = new JLabel(""));
        this.setVisible(false);
        this.setAlwaysOnTop(true);
        this.add(toolPanel);
        this.pack();

    }

    public void show(Point point, TrayIcon trayIcon) {
        this.trayIcon = trayIcon;
        this.estimatedTopLeft = point;
        if (trayInfo != null) trayInfo.interrupt();
        trayInfo = new TrayInfo();
        trayInfo.start();
    }

    public void hideWindow() {
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                if (TrayIconTooltip.this.isVisible()) TrayIconTooltip.this.setVisible(false);
                return null;
            }

        }.start();
        // counter = 0;
        // inside = false;
    }

    private void setLocation() {
        new GuiRunnable<Object>() {
            // @Override
            public Object runSave() {
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int limitX = (int) screenSize.getWidth() / 2;
                int limitY = (int) screenSize.getHeight() / 2;
                Point pp = estimatedTopLeft;
                if (pp.x <= limitX) {
                    if (pp.y <= limitY) {
                        setLocation(pp.x, pp.y + trayIcon.getSize().height);
                    } else {
                        setLocation(pp.x, pp.y - getHeight());
                    }
                } else {
                    if (pp.y <= limitY) {
                        setLocation(pp.x - getWidth(), pp.y + trayIcon.getSize().height);
                    } else {
                        setLocation(pp.x - getWidth(), pp.y - getHeight());
                    }
                }
                return null;
            }
        }.waitForEDT();
    }

    private class TrayInfo extends Thread implements Runnable {

        // @Override
        public void run() {

            pack();
            setLocation();
            setVisible(true);
            toFront();

            final DownloadController dlc = JDUtilities.getDownloadController();

            while (TrayIconTooltip.this.isVisible()) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        dlc.getDownloadStatus(ds);

                        lblDlRunning.setText(String.valueOf(ds.getRunningDownloads()));
                        lblDlFinished.setText(String.valueOf(ds.getFinishedDownloads()));
                        lblDlTotal.setText(String.valueOf(ds.getDownloadCount()));
                        lblSpeed.setText(Formatter.formatReadable(JDUtilities.getController().getSpeedMeter()) + "/s");

                        lblProgress.setText(Formatter.formatFilesize(ds.getCurrentDownloadSize(), 0) + " / " + Formatter.formatFilesize(ds.getTotalDownloadSize(), 0));
                        prgTotal.setMaximum(ds.getTotalDownloadSize());
                        prgTotal.setValue(ds.getCurrentDownloadSize());

                        long etanum = 0;
                        if (JDUtilities.getController().getSpeedMeter() > 1024) etanum = (ds.getTotalDownloadSize() - ds.getCurrentDownloadSize()) / JDUtilities.getController().getSpeedMeter();

                        lblETA.setText(Formatter.formatSeconds(etanum));

                        pack();
                    }

                });

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    interrupt();
                }
            }

            hideWindow();
        }
    }

}
