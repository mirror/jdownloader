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

package jd.captcha.utils;

import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import jd.controlling.JDLogger;
import jd.gui.swing.GuiRunnable;
import jd.utils.JDUtilities;

/**
 * Diese Klasse beinhaltet mehrere Hilfsfunktionen
 * 
 * @author JD-Team
 */
public final class Utilities {
    /**
     * Don't let anyone instantiate this class.
     */
    private Utilities() {
    }

    private static final Logger LOG = JDLogger.getLogger();

    public static Logger getLogger() {
        return LOG;
    }

    public static boolean isLoggerActive() {
        return JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL;
    }

    public static boolean checkJumper(final int x, final int from, final int to) {
        return x >= from && x <= to;
    }

    /**
     * Zeigt einen Directory Chooser an
     * 
     * @param path
     * @return User Input /null
     */
    public static File directoryChooser() {
        final JFileChooser fc = new JFileChooser();
        fc.setApproveButtonText("OK");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile();
        } else {
            return null;
        }
    }

    public static String getMethodDir() {
        return JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/" + JDUtilities.getJACMethodsDirectory();
    }

    /**
     * Gibt die default GridBagConstants zurück
     * 
     * @param x
     * @param y
     * @param width
     * @param height
     * @return Default GridBagConstraints
     */
    public static GridBagConstraints getGBC(final int x, final int y, final int width, final int height) {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = width;
        gbc.gridheight = height;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(1, 1, 1, 1);
        return gbc;
    }

    public static int getJumperStart(final int from, final int to) {
        return from + (to - from) / 2;
    }

    public static int getPercent(final int a, final int b) {
        return (b == 0) ? 100 : a * 100 / b;
    }

    /**
     * Lädt file als Bildatei und wartet bis file geladen wurde. gibt file als
     * Image zurück
     * 
     * @param file
     * @return Neues Bild
     */
    public static Image loadImage(final File file) {
        final GuiRunnable<Image> run = new GuiRunnable<Image>() {
            @Override
            public Image runSave() {
                final JFrame jf = new JFrame();
                final Image img = jf.getToolkit().getImage(file.getAbsolutePath());
                final MediaTracker mediaTracker = new MediaTracker(jf);
                mediaTracker.addImage(img, 0);
                try {
                    mediaTracker.waitForID(0);
                } catch (InterruptedException e) {
                    return null;
                }
                mediaTracker.removeImage(img);
                return img;
            }
        };
        return run.getReturnValue();
    }

    public static int nextJump(final int x, final int from, final int to, final int step) {
        final int start = Utilities.getJumperStart(from, to);
        int ret;
        if (x == start) {
            ret = start + step;
            if (ret > to) {
                ret = start - step;
            }
        } else if (x > start) {
            final int dif = x - start;
            ret = start - dif;
        } else {
            final int dif = start - x + step;
            ret = start + dif;
            if (ret > to) {
                ret = start - dif;
            }
        }
        return ret;
    }

    /**
     * Dreht die Koordinaten x und y um den Mittelpunkt nullX und nullY umd en
     * Winkel winkel
     * 
     * @param x
     * @param y
     * @param nullX
     * @param nullY
     * @param winkel
     * @return neue Koordinaten
     */
    public static int[] turnCoordinates(final int x, final int y, final int nullX, final int nullY, double winkel) {
        winkel /= 180.0;
        final int newX = x - nullX;
        final int newY = y - nullY;
        final double aktAngle = Math.atan2(newY, newX);

        final int[] ret = new int[2];
        final double radius = Math.sqrt(newX * newX + newY * newY);
        final int yTrans = (int) Math.round(radius * Math.sin((aktAngle + winkel * Math.PI)));
        final int xTrans = (int) Math.round(radius * Math.cos((aktAngle + winkel * Math.PI)));
        ret[0] = xTrans + nullX;
        ret[1] = yTrans + nullY;
        return ret;
    }

}