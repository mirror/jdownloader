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

package tests.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import jd.controlling.JDLogger;
import jd.utils.JDUtilities;

import com.apple.eawt.Application;

public class MacDockIconChanger {

    private BufferedImage dockImage = null;

    private final Color backgroundColor = Color.WHITE;

    private final Color foregroundColor = Color.RED;

    private final Color fontColor = Color.BLACK;

    private static final Logger LOG = JDLogger.getLogger();

    public MacDockIconChanger() {
        loadDockImage();
    }

    private void loadDockImage() {
        try {
            File dockImageFile = JDUtilities.getResourceFile("jd/img/logo/jd_logo_128_128.png");
            dockImage = ImageIO.read(dockImageFile);
        } catch (Exception e) {
            LOG.info("Can't Load Dock Image!");
        }
    }

    public void changeToProcent(int procent) {
        Graphics g = dockImage.getGraphics();

        // Draw background
        g.setColor(this.backgroundColor);
        g.fillRect(5, 44, 118, 40);

        // Draw foreground
        g.setColor(this.foregroundColor);
        int width = generateWidth(procent);
        g.fillRect(10, 49, width, 30);

        // Draw string
        g.setColor(this.fontColor);
        Font font = new Font("Arial", Font.BOLD, 15);
        g.setFont(font);
        g.drawString(procent + " %", 52, 68);

        g.dispose();

        Application.getApplication().setDockIconImage(dockImage);
    }

    public void setCompleteDownloadcount(int count) {
        Application.getApplication().setDockIconBadge(count + "");
    }

    private int generateWidth(int procent) {
        return (int) (108 * (procent / 100.0));
    }

}
