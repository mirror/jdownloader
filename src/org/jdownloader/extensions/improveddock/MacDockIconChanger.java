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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import jd.controlling.DownloadInformations;
import jd.controlling.DownloadWatchDog;
import jd.nutils.JDImage;

import com.apple.eawt.Application;

public class MacDockIconChanger extends Thread implements Runnable {

    private final BufferedImage        dockImage       = JDImage.getImage("logo/jd_logo_128_128");

    private final Color                frameColor      = Color.BLACK;

    private final Color                backgroundColor = Color.WHITE;

    private final Color                foregroundColor = Color.RED;

    // private final Color fontColor = Color.BLACK;

    private final DownloadInformations downloadInfo;

    private boolean                    interrupt       = false;

    public MacDockIconChanger() {
        super("Improved Mac OSX Dock Updater");

        downloadInfo = DownloadInformations.getInstance();
    }

    @Override
    public void run() {
        while (DownloadWatchDog.getInstance().getStateMonitor().isState(DownloadWatchDog.RUNNING_STATE)) {
            if (interrupt) break;

            updateDockIcon();

            try {
                Thread.sleep(2000);
            } catch (Exception e) {
                break;
            }
        }
    }

    public void stopUpdating() {
        interrupt = true;
    }

    private void updateDockIcon() {
        downloadInfo.updateInformations();

        updateDockIconImage((int) downloadInfo.getPercent());
        updateDockIconBadge(DownloadWatchDog.getInstance().getDownloadssincelastStart());
    }

    public void updateDockIconImage(int percentCompleted) {
        Graphics g = dockImage.getGraphics();

        // Draw Border
        g.setColor(this.frameColor);
        g.fillRect(0, 96, 128, 15);

        // Draw background
        g.setColor(this.backgroundColor);
        g.fillRect(2, 98, 124, 11);

        // Draw foreground
        g.setColor(this.foregroundColor);
        int width = generateWidth(percentCompleted);
        g.fillRect(2, 98, width, 11);

        // Draw string
        // g.setColor(this.fontColor);
        // Font font = new Font("Arial", Font.BOLD, 15);
        // g.setFont(font);
        // g.drawString(percentCompleted + " %", 52, 68);

        g.dispose();

        Application.getApplication().setDockIconImage(dockImage);
    }

    private void updateDockIconBadge(int completedDownloadCount) {
        Application.getApplication().setDockIconBadge(completedDownloadCount + "");
    }

    private int generateWidth(int percent) {
        return (int) (1.24 * percent);
    }

}
