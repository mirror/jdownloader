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

package jd.captcha.pixelgrid;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.captcha.JAntiCaptcha;
import jd.captcha.gui.ScrollPaneWindow;
import jd.captcha.pixelobject.PixelObject;
import jd.captcha.utils.Utilities;
import jd.config.Property;
import jd.controlling.JDLogger;
import jd.nutils.Colors;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

/**
 * Diese Klasse behinhaltet alle wichtigen Methoden um das Image-Pixelgrid zu
 * bearbeiten
 * 
 * @author JD-Team
 */

public class PixelGrid extends Property {

    private static final long serialVersionUID = 1L;

    public void autoBottomTopAlign() {
        int avg = getAverage();
        double bestOL = Double.MAX_VALUE;
        int xOL = 0;
        int yOL = 0;
        double bestOR = Double.MAX_VALUE;
        int xOR = 0;
        int yOR = 0;
        double bestUL = Double.MAX_VALUE;
        int xUL = 0;
        int yUL = 0;
        double bestUR = Double.MAX_VALUE;
        int xUR = 0;
        int yUR = 0;
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                if (isElement(getPixelValue(x, y), avg)) {
                    int yy = y * y;
                    int xx = x * x;
                    int xd = getWidth() - x;
                    int xxd = xd * xd;
                    int yd = getHeight() - y;
                    int yyd = yd * yd;
                    double diff = Math.sqrt(xx + yy);

                    if (diff < bestOL) {
                        xOL = x;
                        yOL = y;
                        bestOL = diff;
                    }

                    diff = Math.sqrt(xxd + yy);
                    if (diff < bestOR) {
                        xOR = x;
                        yOR = y;
                        bestOR = diff;
                    }
                    diff = Math.sqrt(xx + yyd);
                    if (diff < bestUL) {
                        xUL = x;
                        yUL = y;
                        bestUL = diff;
                    }
                    diff = Math.sqrt(xxd + yyd);
                    if (diff < bestUR) {
                        xUR = x;
                        yUR = y;
                        bestUR = diff;
                    }
                }
            }
        }
        grid[xOL][yOL] = 0xff0000;
        grid[xOR][yOR] = 0x00FFCC;
        grid[xUR][yUR] = 0x3366FF;
        grid[xUL][yUL] = 0xFFCC33;

        int g = 0;
        double distBest = getM(xOL, xOR, yOL, yOR);
        if (distBest == 0) distBest = 0.0001;
        double dist = getM(xUL, xUR, yUL, yUR);
        double distWBest = distBest / ((xOR - xOL + 1) / 4);
        double distW;
        boolean skipw = (xOR - xOL) < (getWidth() / 3) || yOL > getHeight() / 3 || yOR > getHeight() / 3;
        if (dist == 0)
            distW = distBest / ((xUR - xUL + 1) / 4);
        else
            distW = dist / ((xUR - xUL + 1) / 4);
        if (skipw || Math.abs(distW) < Math.abs(distWBest)) {
            distWBest = distW;
            distBest = dist;
            g = 1;
        }

        skipw = (xUR - xUL) < (getHeight() / 3) || yUL < (getHeight() * 2 / 3) || yUR < (getHeight() * 2 / 3);

        System.out.println(distBest);

        int turn = 60;
        // if(Math.abs( Math.round(distBest * turn))>6)
        if (g > 1)
            this.grid = turn((distBest * turn)).grid;
        else
            this.grid = turn((-distBest * turn)).grid;
        // if(Math.abs( Math.round(distBest * turn))>6)

        // BasicWindow.showImage(getImage().getScaledInstance(getWidth() * 10,
        // getHeight() * 10, 1), "Turned:" + Math.round(distBest * turn) + " G:"
        // + g);

    }

    public void autoAlign() {
        int avg = getAverage();
        double bestOL = Double.MAX_VALUE;
        int xOL = 0;
        int yOL = 0;
        double bestOR = Double.MAX_VALUE;
        int xOR = 0;
        int yOR = 0;
        double bestUL = Double.MAX_VALUE;
        int xUL = 0;
        int yUL = 0;
        double bestUR = Double.MAX_VALUE;
        int xUR = 0;
        int yUR = 0;
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                if (isElement(getPixelValue(x, y), avg)) {
                    int yy = y * y;
                    int xx = x * x;
                    int xd = getWidth() - x;
                    int xxd = xd * xd;
                    int yd = getHeight() - y;
                    int yyd = yd * yd;
                    double diff = Math.sqrt(xx + yy);

                    if (diff < bestOL) {
                        xOL = x;
                        yOL = y;
                        bestOL = diff;
                    }

                    diff = Math.sqrt(xxd + yy);
                    if (diff < bestOR) {
                        xOR = x;
                        yOR = y;
                        bestOR = diff;
                    }
                    diff = Math.sqrt(xx + yyd);
                    if (diff < bestUL) {
                        xUL = x;
                        yUL = y;
                        bestUL = diff;
                    }
                    diff = Math.sqrt(xxd + yyd);
                    if (diff < bestUR) {
                        xUR = x;
                        yUR = y;
                        bestUR = diff;
                    }
                }
            }
        }
        grid[xOL][yOL] = 0xff0000;
        grid[xOR][yOR] = 0x00FFCC;
        grid[xUR][yUR] = 0x3366FF;
        grid[xUL][yUL] = 0xFFCC33;

        int g = 0;
        double distBest = getM(xOL, xOR, yOL, yOR);
        if (distBest == 0) distBest = 0.0001;
        double dist = getM(xUL, xUR, yUL, yUR);
        double distWBest = distBest / ((xOR - xOL + 1) / 4);
        double distW;
        boolean skipw = (xOR - xOL) < (getWidth() / 3) || yOL > getHeight() / 3 || yOR > getHeight() / 3;
        if (dist == 0)
            distW = distBest / ((xUR - xUL + 1) / 4);
        else
            distW = dist / ((xUR - xUL + 1) / 4);
        if (skipw || Math.abs(distW) < Math.abs(distWBest)) {
            distWBest = distW;
            distBest = dist;
            g = 1;
        }

        skipw = (xUR - xUL) < (getHeight() / 3) || yUL < (getHeight() * 2 / 3) || yUR < (getHeight() * 2 / 3);

        dist = getM(yOL, yUL, xOL, xUL);
        if (dist == 0)
            distW = distBest / ((yUL - yOL + 1) / 4);
        else
            distW = dist / ((yUL - yOL + 1) / 4);
        if (skipw || Math.abs(distW) < Math.abs(distWBest)) {
            distWBest = distW;
            distBest = dist;
            g = 2;
        }
        skipw = (yUL - yOL) < (getHeight() / 4) || xUL > (getWidth() / 3) || xOL > (getWidth() / 3);

        dist = getM(yOR, yUR, xOR, xUR);
        if (dist == 0)
            distW = distBest / ((yUR - yOR + 1) / 4);
        else
            distW = dist / ((yUR - yOR + 1) / 4);

        if (skipw || Math.abs(distW) < Math.abs(distWBest)) {
            skipw = (yUR - yOR) < (getHeight() / 4) || xUR > (getHeight() * 2 / 3) || xOR > (getWidth() * 2 / 3);
            if (!skipw) {
                distWBest = distW;
                distBest = dist;
                g = 3;
            }
        }

        // System.out.println(distWBest);

        int turn = 60;
        // if(Math.abs( Math.round(distBest * turn))>6)
        if (g > 1)
            this.grid = turn((distBest * turn)).grid;
        else
            this.grid = turn((-distBest * turn)).grid;
        // if(Math.abs( Math.round(distBest * turn))>6)

        // BasicWindow.showImage(getImage().getScaledInstance(getWidth() * 10,
        // getHeight() * 10, 1), "Turned:" + Math.round(distBest * turn) + " G:"
        // + g);

    }

    /**
     * Dreht das PixelGrid um angle. Dabei wird breite und höhe angepasst. Das
     * drehen dauert länger als über PixelObject, leidet dafür deutlich weniger
     * unter Pixelfehlern
     * 
     * @param angle
     * @return new letter
     */
    public PixelGrid turn(double angle) {
        if (angle == 0.0) return this;
        while (angle < 0) {
            angle += 360;
        }
        angle /= 180;

        int newWidth = (int) (Math.abs(Math.cos(angle * Math.PI) * getWidth()) + Math.abs(Math.sin(angle * Math.PI) * getHeight()));
        int newHeight = (int) (Math.abs(Math.sin(angle * Math.PI) * getWidth()) + Math.abs(Math.cos(angle * Math.PI) * getHeight()));
        PixelGrid l = new PixelGrid(newWidth, newHeight);
        int left = (newWidth - getWidth()) / 2;
        int top = (newHeight - getHeight()) / 2;
        int[][] newGrid = new int[newWidth][newHeight];
        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                int[] n = Utilities.turnCoordinates(x - left, y - top, getWidth() / 2, getHeight() / 2, -(angle * 180));
                if (n[0] < 0 || n[0] >= getWidth() || n[1] < 0 || n[1] >= getHeight()) {
                    newGrid[x][y] = owner.getJas().getColorFaktor() - 1;
                    continue;
                }

                newGrid[x][y] = grid[n[0]][n[1]];

            }
        }
        l.setGrid(newGrid);
        return l;

    }

    private double getM(int x0, int x1, int y0, int y1) {
        return ((double) (y0 - y1)) / (x0 - x1);
    }

    public static void fillLetter(Letter l) {

        int limit = 200;
        int[][] tmp = new int[l.getWidth()][l.getHeight()];

        for (int x = 0; x < l.getWidth(); x++) {
            for (int y = 0; y < l.getHeight(); y++) {
                if (l.grid[x][y] > limit && tmp[x][y] != 1) {
                    PixelObject p = new PixelObject(l);
                    PixelGrid.recFill(p, l, x, y, tmp, 0);
                    if (p.isBordered() && p.getSize() < 60) {
                        l.fillWithObject(p, 0);
                    }
                    // BasicWindow.showImage(l.getImage(2), x+" - "+y);

                }
            }
        }

    }

    public static int[] getDimension(int[][] grid) {

        int topLines = 0;
        int bottomLines = 0;
        int leftLines = 0;
        int rightLines = 0;

        int width = grid.length;
        int height = grid[0].length;
        row: for (int x = 0; x < width; x++) {

            for (int y = 0; y < height; y++) {
                // JDUtilities.getLogger().info(grid[x][y]+"");
                if (grid[x][y] == 0) {
                    // grid[x][y] = 0xff0000;
                    break row;
                }
            }

            leftLines++;
        }
        // JDUtilities.getLogger().info("left "+leftLines);
        row: for (int x = width - 1; x >= 0; x--) {

            for (int y = 0; y < height; y++) {

                if (grid[x][y] == 0) {
                    // grid[x][y] = 0xff0000;
                    break row;
                }
            }

            rightLines++;
        }
        // JDUtilities.getLogger().info("right "+rightLines);
        if (leftLines >= width || width - rightLines > width) { return new int[] { 0, 0 }; }

        line: for (int y = 0; y < height; y++) {

            for (int x = leftLines; x < width - rightLines; x++) {
                if (grid[x][y] == 0) {
                    // grid[x][y] = 0xff0000;

                    break line;
                }
            }

            topLines++;
        }
        line: for (int y = height - 1; y >= 0; y--) {

            for (int x = leftLines; x < width - rightLines; x++) {
                if (grid[x][y] == 0) {
                    // grid[x][y] = 0xff0000;

                    break line;
                }
            }

            bottomLines++;
        }
        // JDUtilities.getLogger().info("top "+topLines);
        // JDUtilities.getLogger().info("bottom "+bottomLines);
        if (width - leftLines - rightLines < 0 || height - topLines - bottomLines < 0) { return new int[] { 0, 0 }; }
        return new int[] { width - leftLines - rightLines, height - topLines - bottomLines };

    }

    /** Why not simple return grid.clone(); ? */
    public static int[][] getGridCopy(int[][] grid) {
        if (grid.length == 0) return null;
        int[][] ret = new int[grid.length][grid[0].length];
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[0].length; y++) {
                ret[x][y] = grid[x][y];
            }
        }

        return ret;
    }

    public static int getGridHeight(int[][] grid) {
        if (grid.length == 0) return 0;
        return grid[0].length;
    }

    public static int getGridWidth(int[][] grid) {
        return grid.length;
    }

    /**
     * @param owner
     * @return Maximaler PixelWert
     */
    public static int getMaxPixelValue(JAntiCaptcha owner) {
        return owner.getJas().getColorFaktor() - 1;
    }

    /**
     * @param x
     * @param y
     * @param grid
     * @param owner
     * @return Pixelwert bei x,y
     */
    public static int getPixelValue(int x, int y, int[][] grid) {
        if (x < 0 || x >= grid.length) return -1;
        if (y < 0 || grid.length == 0 || y >= grid[0].length) return -1;
        return grid[x][y];
    }

    private static void recFill(PixelObject p, Letter l, int x, int y, int[][] tmp, int i) {
        i++;
        if (x >= 0 && y >= 0 && x < l.getWidth() && y < l.getHeight() && l.grid[x][y] > 200 && tmp[x][y] != 1) {
            if (x == 0 || y == 0 || x == l.getWidth() - 1 || y == l.getHeight() - 1) {
                p.setBordered(false);
            }
            p.add(x, y, 0xff0000);
            tmp[x][y] = 1;

            PixelGrid.recFill(p, l, x - 1, y, tmp, i);
            // getObject(x - 1, y - 1, tmpGrid, object);
            PixelGrid.recFill(p, l, x, y - 1, tmp, i);
            // getObject(x + 1, y - 1, tmpGrid, object);
            PixelGrid.recFill(p, l, x + 1, y, tmp, i);
            // getObject(x + 1, y + 1, tmpGrid, object);
            PixelGrid.recFill(p, l, x, y + 1, tmp, i);
            // getObject(x - 1, y + 1, tmpGrid, object);

        }

    }

    /**
     * Static setPixelValue Funktion
     * 
     * @param x
     * @param y
     * @param localGrid
     *            Grid inn der richtigen größe für x,y
     * @param value
     *            Pixelwert
     */
    public static void setPixelValue(int x, int y, int[][] localGrid, int value) {
        try {
            localGrid[x][y] = value;
        } catch (Exception e) {
        }
    }

    /**
     * Internes grid
     */
    public int[][]      grid;

    private int[]       location = new int[] { 0, 0 };

    /**
     * Logger
     */
    public Logger       logger   = Utilities.getLogger();

    /**
     * ParameterDump
     */
    public JAntiCaptcha owner;

    /**
     * Pixel Array
     */
    public int[]        pixel;

    protected int[][]   tmpGrid;

    /**
     * Konstruktor
     * 
     * @param width
     *            Breite des Bildes in pixel
     * @param height
     *            Höhe des Bildes in Pixel
     */
    public PixelGrid(int width, int height) {
        grid = new int[width][height];

    }

    /**
     * Lässt das Bild verschwimmen
     * 
     * @param faktor
     *            Stärke des Effekts
     */
    public void blurIt(int faktor) {

        int[][] newGrid = new int[getWidth()][getHeight()];

        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                PixelGrid.setPixelValue(x, y, newGrid, getAverage(x, y, faktor, faktor));

                // getAverage(x, y, faktor, faktor)
            }
        }

        grid = newGrid;

    }

    /**
     * Entfernt von allen 4 Seiten die Zeilen und Reihen bis nur noch der
     * content übrig ist
     * 
     * @return true/False
     */
    public boolean clean() {

        int topLines = 0;
        int bottomLines = 0;
        int leftLines = 0;
        int rightLines = 0;
        int avg = getAverage();

        for (int x = 0; x < getWidth(); x++) {
            boolean rowIsClear = true;
            for (int y = 0; y < getHeight(); y++) {

                if (isElement(getPixelValue(x, y), avg)) {
                    rowIsClear = false;
                    break;
                }
            }
            if (!rowIsClear) {
                break;
            }
            leftLines++;
        }

        for (int x = getWidth() - 1; x >= 0; x--) {
            boolean rowIsClear = true;
            for (int y = 0; y < getHeight(); y++) {

                if (isElement(getPixelValue(x, y), avg)) {
                    rowIsClear = false;
                    break;
                }
            }
            if (!rowIsClear) {
                break;
            }

            rightLines++;
        }

        if (leftLines >= getWidth() || getWidth() - rightLines > getWidth()) {
            if (Utilities.isLoggerActive()) {
                logger.severe("cleaning failed. nothing left1");
            }

            grid = new int[0][0];
            return false;

        }
        for (int y = 0; y < getHeight(); y++) {
            boolean lineIsClear = true;
            for (int x = leftLines; x < getWidth() - rightLines; x++) {
                if (isElement(getPixelValue(x, y), avg)) {
                    lineIsClear = false;
                    break;
                }
            }
            if (!lineIsClear) {
                break;
            }
            topLines++;
        }

        for (int y = getHeight() - 1; y >= 0; y--) {
            boolean lineIsClear = true;
            for (int x = leftLines; x < getWidth() - rightLines; x++) {
                if (isElement(getPixelValue(x, y), avg)) {
                    lineIsClear = false;
                    break;
                }
            }
            if (!lineIsClear) {
                break;
            }
            bottomLines++;
        }

        if (getWidth() - leftLines - rightLines < 0 || getHeight() - topLines - bottomLines < 0) {
            if (Utilities.isLoggerActive()) {
                logger.severe("cleaning failed. nothing left");
            }
            grid = new int[0][0];
            return false;
        }
        int[][] ret = new int[getWidth() - leftLines - rightLines][getHeight() - topLines - bottomLines];
        location[0] += leftLines;
        location[1] += topLines;
        for (int y = 0; y < getHeight() - topLines - bottomLines; y++) {
            for (int x = 0; x < getWidth() - leftLines - rightLines; x++) {
                ret[x][y] = getPixelValue(x + leftLines, y + topLines);
            }

        }
        grid = ret;

        return true;

    }

    /**
     * enbtfernt eine farbe... die tolleranz gibt die farbdistanz an die njoch
     * entfernt wird
     * 
     * @param i
     *            vergleichsfarbe
     * @param d
     *            tolleranz
     */
    public void cleanByColor(int i, double d) {
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                if (Colors.getColorDifference(i, this.getPixelValue(x, y)) < d) {
                    this.setPixelValue(x, y, getMaxPixelValue());
                }

            }
        }
        // grid = newgrid;
    }

    /**
     * Entfernt Alle Pixel die über getBackgroundSampleCleanContrast an avg
     * liegen
     * 
     * @param avg
     */
    public void cleanBackgroundByColor(int avg) {
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                int dif = Math.abs(avg - getPixelValue(x, y));
                if (dif < (int) (getMaxPixelValue() * owner.getJas().getDouble("BackgroundSampleCleanContrast"))) {
                    this.setPixelValue(x, y, getMaxPixelValue());
                }
            }
        }
    }

    /**
     * Nimmt an der angegebenen Positiond en farbwert auf und entfernt desen aus
     * dem ganzen Bild
     * 
     * @param px
     * @param py
     * @param width
     * @param height
     */
    public void cleanBackgroundBySample(int px, int py, int width, int height) {
        int avg = getAverage(px + width / 2, py + height / 2, width, height);
        cleanBackgroundByColor(avg);
    }

    /**
     * Färbt ein objekt im zugehörigem Captcha ein
     * 
     * @param object
     * @param color
     *            Farbe
     */
    public void colorObject(PixelObject object, int color) {
        for (int i = 0; i < object.getSize(); i++) {
            setPixelValue(object.elementAt(i)[0], object.elementAt(i)[1], color);
        }
    }

    /**
     * factory Funktion um einen Letter zu erstellen
     * 
     * @return Neuer letter
     */
    public Letter createLetter() {
        Letter ret = new Letter();
        ret.setOwner(owner);
        return ret;
    }

    /**
     * Schneidet das grid zurecht
     * 
     * @param leftPadding
     * @param topPadding
     * @param rightPadding
     * @param bottomPadding
     */
    public void crop(int leftPadding, int topPadding, int rightPadding, int bottomPadding) {
        int newWidth = getWidth() - (leftPadding + rightPadding);
        int newHeight = getHeight() - (topPadding + bottomPadding);

        int[][] newGrid = new int[newWidth][newHeight];
        location[0] += leftPadding;
        location[1] += topPadding;
        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                newGrid[x][y] = grid[x + leftPadding][y + topPadding];
            }
        }

        grid = newGrid;

    }

    public void desinx(double max, double omega, double phi) {
        omega = 2 * Math.PI / omega;

        int[][] tmp = new int[getWidth()][getHeight()];

        int shift;

        for (int y = 0; y < getHeight(); y++) {

            shift = (int) (max * Math.sin(omega * (y + phi)));

            for (int x = 0; x < getWidth(); x++) {

                tmp[x][y] = x + shift < getWidth() && x + shift >= 0 ? grid[x + shift][y] : 0xFF;
            }

        }

        setGrid(tmp);

    }

    /**
     * Die Wellenlänge omega kann aus dem captcha ausgemessen werden. Formel:
     * 2*PI/geschätzte Wellenlänge in Pixeln
     * 
     * @param max
     * @param omega
     * @param phi
     */
    public void desiny(double max, double omega, double phi) {
        int shift;
        omega = 2 * Math.PI / omega;

        int[][] tmp = new int[getWidth()][getHeight()];

        for (int x = 0; x < getWidth(); x++) {

            shift = (int) (max * Math.sin(omega * (x + phi)));

            for (int y = 0; y < getHeight(); y++) {

                tmp[x][y] = y + shift < getHeight() && y + shift >= 0 ? grid[x][y + shift] : 0xFF;
            }

        }

        setGrid(tmp);

    }

    /**
     * Gibt den Durchschnittlichen pixelwert des Bildes zurück
     * 
     * @return int
     */
    public int getAverage() {
        int[] avg = { 0, 0, 0 };
        int[] bv;
        int i = 0;

        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                // Nicht die Colormix Funktion verwenden!!! DIe gibt nur in
                // zurück, das ist nicht ausreichend
                bv = Colors.hexToRgb(getPixelValue(x, y));
                avg[0] += bv[0];
                avg[1] += bv[1];
                avg[2] += bv[2];
                i++;

            }
        }
        if (i == 0) { return 0; }
        avg[0] /= i;
        avg[1] /= i;
        avg[2] /= i;
        return Colors.rgbToHex(avg);

    }

    /**
     * Gibt den Durschnittlichen Pixelwert im angegebenen raum zurück
     * 
     * @param px
     *            Position x
     * @param py
     *            Position y
     * @param width
     *            Breite des Ausschnitts
     * @param height
     *            Höhe des Ausschnitts
     * @return int Durchschnittswert
     */
    public int getAverage(int px, int py, int width, int height) {
        int[] avg = { 0, 0, 0 };
        int[] bv;
        int i = 0;
        int halfW = width / 2;
        int halfH = height / 2;
        if (width == 1 && px == 0) {
            width = 2;
        }
        if (height == 1 && py == 0) {
            height = 2;
        }

        for (int x = Math.max(0, px - halfW); x < Math.min(px + width - halfW, getWidth()); x++) {
            for (int y = Math.max(0, py - halfH); y < Math.min(py + height - halfH, getHeight()); y++) {
                bv = Colors.hexToRgb(getPixelValue(x, y));
                avg[0] += bv[0];
                avg[1] += bv[1];
                avg[2] += bv[2];
                i++;

            }
        }

        avg[0] /= i;
        avg[1] /= i;
        avg[2] /= i;
        return Colors.rgbToHex(avg);
    }

    /**
     * Gibt den Durschnittlichen Pixelwert im angegebenen raum zurück.
     * Allerdings wird hier im Vergleich zu getAverage(int px,int py,int
     * width,int height) der Punkt slebet nicht mitberechnet
     * 
     * @param px
     *            Position x
     * @param py
     *            Position y
     * @param width
     *            Breite des Ausschnitts
     * @param height
     *            Höhe des Ausschnitts
     * @return int Durchschnittswert
     */
    public int getAverageWithoutPoint(int px, int py, int width, int height) {
        int[] avg = { 0, 0, 0 };
        int[] bv;
        int i = 0;
        int halfW = width / 2;
        int halfH = height / 2;
        if (width == 1 && px == 0) {
            width = 2;
        }
        if (height == 1 && py == 0) {
            height = 2;
        }

        for (int x = Math.max(0, px - halfW); x < Math.min(px + width - halfW, getWidth()); x++) {
            for (int y = Math.max(0, py - halfH); y < Math.min(py + height - halfH, getHeight()); y++) {
                if (x != px || y != py) {

                    bv = Colors.hexToRgb(getPixelValue(x, y));
                    avg[0] += bv[0];
                    avg[1] += bv[1];
                    avg[2] += bv[2];
                    i++;

                }

            }
        }
        if (i > 0) {
            avg[0] /= i;
            avg[1] /= i;
            avg[2] /= i;
        }
        return Colors.rgbToHex(avg);
    }

    protected Vector<PixelObject> getColorObjects(int letterNum) {

        // int percent =
        // owner.getJas().getInteger("colorObjectDetectionPercent");
        // int running =
        // owner.getJas().getInteger("colorObjectDetectionRunningAverage");
        logger.info("Max pixel value: " + this.getMaxPixelValue());
        // Erstelle Farbverteilungsmap
        HashMap<Integer[], PixelObject> map = new HashMap<Integer[], PixelObject>();
        logger.info("" + Colors.getColorDifference(new int[] { 0, 0, 204 }, new int[] { 0, 0, 184 }));
        logger.info("" + Colors.getColorDifference(new int[] { 0, 0, 204 }, new int[] { 60, 10, 240 }));

        logger.info("" + Colors.getColorDifference(new int[] { 255, 255, 255 }, new int[] { 0, 0, 0 }));
        final int avg = getAverage();
        int intensivity = 8;
        int h = getWidth() / letterNum / 4;
        Integer[] last = null;
        int d = 0;
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {

                Integer key = getPixelValue(x, y);
                int[] rgbA = Colors.hexToRgb(key);

                if (isElement(key, avg) || Colors.rgb2hsb(rgbA[0], rgbA[1], rgbA[2])[0] * 100 > 0) {
                    if (map.get(key) == null) {
                        if (d++ < getHeight() * 2) {
                            d = 0;
                            int[] bv = Colors.hexToRgb(key);
                            boolean found = false;
                            if (last != null && Colors.getHueColorDifference(bv, Colors.hexToRgb(map.get(last).getAverage())) < intensivity) {
                                map.get(last).add(x, y, key);
                                found = true;
                            } else {
                                Iterator<Integer[]> iterator = map.keySet().iterator();
                                Iterator<PixelObject> valsiter = map.values().iterator();
                                Integer[] bestKey = new Integer[] { -1, -1 };
                                double bestValue = 255;
                                double dif = 255;
                                while (iterator.hasNext() && valsiter.hasNext()) {
                                    Integer[] key2 = iterator.next();
                                    PixelObject object = valsiter.next();
                                    if (Math.abs((double) (x - key2[1] - object.getWidth())) < h) {
                                        dif = Colors.getHueColorDifference(bv, Colors.hexToRgb(object.getAverage()));

                                        if (dif < bestValue) {
                                            bestKey = key2;
                                            bestValue = dif;
                                            // map.get(key2).add(x, y,
                                            // getPixelValue(x,
                                            // y));

                                        }
                                    }

                                }
                                if (bestValue < intensivity) {
                                    map.get(bestKey).add(x, y, key);
                                    found = true;
                                }
                            }
                            if (!found) {
                                PixelObject object = new PixelObject(this);
                                object.add(x, y, key);
                                last = new Integer[] { key, x };
                                map.put(last, object);
                            }
                        } else {
                            PixelObject object = new PixelObject(this);
                            object.add(x, y, key);
                            last = new Integer[] { key, x };
                            map.put(last, object);
                            d = 0;
                        }

                    } else {
                        map.get(key).add(x, y, key);
                    }
                } else {
                    d++;
                }
            }
        }

        // int total = getWidth() * getHeight();
        ArrayList<Object[]> els = new ArrayList<Object[]>();

        Iterator<PixelObject> vals = map.values().iterator();
        Iterator<Integer[]> keys = map.keySet().iterator();
        while (keys.hasNext() && vals.hasNext()) {
            PixelObject ob = vals.next();
            els.add(new Object[] { keys.next(), ob });
        }
        Collections.sort(els, new Comparator<Object[]>() {
            public int compare(Object[] o1, Object[] o2) {
                Letter letter1 = ((PixelObject) o1[1]).toLetter();
                Letter letter2 = ((PixelObject) o2[1]).toLetter();
                if (letter1.getElementPixel() > letter2.getElementPixel()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        int c = map.size();
        if (c > letterNum) {
            Iterator<Object[]> iter = els.iterator();

            double addd = intensivity / 2;
            while (c > letterNum) {
                if (!iter.hasNext()) {
                    iter = els.iterator();
                    addd++;
                }
                Object[] thisel = iter.next();
                Integer[] integers = (Integer[]) thisel[0];
                PixelObject object = (PixelObject) thisel[1];
                Iterator<Object[]> iterator = els.iterator();
                Integer[] bestKey = null;
                PixelObject bestobj = null;
                double bestValue = Double.MAX_VALUE;
                double dif = Double.MAX_VALUE;
                double dif2 = Double.MAX_VALUE;
                while (iterator.hasNext()) {
                    Object[] it = iterator.next();
                    PixelObject obj = (PixelObject) it[1];
                    Integer[] key2 = (Integer[]) it[0];
                    if (key2 != integers) {
                        dif = key2[1] - integers[1];
                        dif2 = Math.abs((double) (key2[1] + obj.getWidth() - (integers[1] + object.getWidth())));

                        if (dif == 0 || dif2 == 0 || dif < 0 && dif + obj.getWidth() > 0) {
                            map.get(key2).add(object);
                            map.remove(integers);
                            iter.remove();
                            c--;
                            bestKey = null;
                            break;
                        }
                        if (Math.abs(dif) < bestValue) {
                            bestKey = key2;
                            bestobj = obj;
                            bestValue = Math.abs(dif);
                            // map.get(key2).add(x, y, getPixelValue(x, y));
                        }
                        if (dif2 < bestValue) {
                            bestKey = key2;
                            bestobj = obj;
                            bestValue = dif2;
                        }
                    }

                }
                if (bestKey != null) {
                    dif = Colors.getHueColorDifference(Colors.hexToRgb(bestobj.getAverage()), Colors.hexToRgb(object.getAverage()));
                    if (dif < addd) {
                        map.get(bestKey).add(object);
                        map.remove(integers);
                        iter.remove();
                        c--;
                    }
                }
            }
        }

        ArrayList<Integer[]> ar = new ArrayList<Integer[]>();
        ar.addAll(map.keySet());
        Collections.sort(ar, new Comparator<Integer[]>() {
            public int compare(Integer[] o1, Integer[] o2) {
                return o1[1].compareTo(o2[1]);
            }
        });
        Iterator<Integer[]> iterator2 = ar.iterator();
        Vector<PixelObject> ret = new Vector<PixelObject>();
        while (iterator2.hasNext()) {
            PixelObject it = map.get(iterator2.next());
            ret.add(it);
        }
        return ret;
    }

    /**
     * @return Dimmensionsstring
     */
    public String getDim() {
        return "(" + getWidth() + "/" + getHeight() + ")";
    }

    public BufferedImage getFullImage() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            if (Utilities.isLoggerActive()) {
                logger.severe("Dimensionen falsch: " + getDim());
            }
            return null;
        }
        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                graphics.setColor(new Color(getPixelValue(x, y) == 0 ? 0 : 0xffffff));
                graphics.fillRect(x, y, 1, 1);
            }
        }
        return image;

    }

    public int[][] getGrid() {
        return grid;
    }

    /**
     * 
     * @return Kopie des Internen Grids
     */
    public int[][] getGridCopy() {
        int[][] ret = new int[getWidth()][getHeight()];
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                ret[x][y] = grid[x][y];
            }
        }

        return ret;
    }

    /**
     * Gibt die Höhe des internen captchagrids zurück
     * 
     * @return Höhe
     */
    public int getHeight() {
        if (grid.length == 0) return 0;
        return grid[0].length;
    }

    /**
     * Gibt das Pixelgrid als Image zurück
     * 
     * @return Image
     */
    public BufferedImage getImage() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            if (Utilities.isLoggerActive()) {
                logger.severe("Dimensionen falsch: " + getDim());
            }
            return null;
        }
        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                graphics.setColor(new Color(getPixelValue(x, y)));
                graphics.fillRect(x, y, 1, 1);
            }
        }
        return image;

    }

    /**
     * Gibt das Pixelgrid als vergrößertes Image zurück
     * 
     * @param faktor
     *            Vergrößerung
     * @return Neues Bild
     */
    public BufferedImage getImage(int faktor) {
        if (getWidth() * faktor <= 0 || getHeight() * faktor <= 0) {
            // if(Utilities.isLoggerActive())logger.severe("Bild zu Klein.
            // Fehler!!. Buhcstbaen nicht richtig erkannt?");
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);

            return image;

        }
        BufferedImage image = new BufferedImage(getWidth() * faktor, getHeight() * faktor, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        for (int y = 0; y < getHeight() * faktor; y += faktor) {
            for (int x = 0; x < getWidth() * faktor; x += faktor) {
                graphics.setColor(new Color(getPixelValue(x / faktor, y / faktor)));
                graphics.fillRect(x, y, faktor, faktor);
            }
        }
        return image;

    }

    public int[] getLocation() {
        return location;
    }

    /**
     * Sollte je nach farbmodell den Höchsten pixelwert zurückgeben. RGB:
     * 0xffffff
     * 
     * @return Pixelwert je nach Farbbereich
     */
    public int getMaxPixelValue() {
        return PixelGrid.getMaxPixelValue(owner);
    }

    /**
     * Gibt den maxpixelvalue mit faktor gewichtet zurück
     * 
     * @param faktor
     * @return maxpixelvalue
     */
    public int getMaxPixelValue(double faktor) {
        return (int) (PixelGrid.getMaxPixelValue(owner) * faktor);
    }

    /**
     * Erstellt das Objekt, ausgehend von einem Pixel. rekursive Funktion! Diese
     * rekusrive Funktion kann bei zu großen Objekten zu einem Stackoverflow
     * führen. Man sollte sie mal umschreiben!
     * 
     * @param x
     * @param y
     * @param tmpGrid
     * @param object
     */
    private void getObject(int x, int y, int[][] tmpGrid, PixelObject object) {

        if (x < 0 || y < 0 || tmpGrid.length <= x || tmpGrid[0].length <= y || tmpGrid[x][y] < 0) { return; }
        int localValue = PixelGrid.getPixelValue(x, y, tmpGrid);
        // Utilities.trace(x+"/"+y);
        try {
            if (object.doesColorAverageFit(localValue)) {

                object.add(x, y, localValue);
                tmpGrid[x][y] = -1;

                // Achtung!! Algos funktionieren nur auf sw basis richtig
                // grid[x][y] = 254;
                getObject(x - 1, y, tmpGrid, object);
                if (owner.getJas().getBoolean("followXLines")) {
                    getObject(x - 1, y - 1, tmpGrid, object);
                }
                getObject(x, y - 1, tmpGrid, object);
                if (owner.getJas().getBoolean("followXLines")) {
                    getObject(x + 1, y - 1, tmpGrid, object);
                }
                getObject(x + 1, y, tmpGrid, object);
                if (owner.getJas().getBoolean("followXLines")) {
                    getObject(x + 1, y + 1, tmpGrid, object);
                }
                getObject(x, y + 1, tmpGrid, object);
                if (owner.getJas().getBoolean("followXLines")) {
                    getObject(x - 1, y + 1, tmpGrid, object);
                }
            }
        } catch (Exception e) {
            JDLogger.exception(e);
        }

        return;

    }

    /**
     * Ermittelt alle Objekte im Captcha
     * 
     * @param contrast
     * @param objectContrast
     * @return Gibt alle Gefundenen Elemente zurück
     */
    public Vector<PixelObject> getObjects(double contrast, double objectContrast) {
        int[][] tmpGrid = getGridCopy();
        int dist;

        Vector<PixelObject> ret = new Vector<PixelObject>();
        PixelObject lastObject = null;
        PixelObject object;
        boolean showdebug = false;
        ScrollPaneWindow w = null;
        if (showdebug) {
            w = new ScrollPaneWindow();
        }
        if (showdebug) {
            w.setTitle("getObjects2");
        }
        if (showdebug) {
            w.setImage(0, 0, this.getImage());
        }
        int line = 1;

        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {

                if (tmpGrid[x][y] < 0) {
                    continue;
                }

                if (getPixelValue(x, y) <= objectContrast * getMaxPixelValue()) {

                    // Füge 2 Objekte zusammen die scheinbar zusammen gehören

                    dist = 100;
                    if (lastObject != null) {
                        int xd = x - lastObject.getXMin() + lastObject.getWidth() / 2;
                        int yd = y - lastObject.getYMin() + lastObject.getHeight() / 2;
                        dist = xd * xd + yd * yd;
                    }
                    int d;
                    if (lastObject != null && lastObject.getArea() < owner.getJas().getInteger("minimumObjectArea") && dist < (d = owner.getJas().getInteger("minimumLetterWidth") / 2 + 1) * d) {

                        object = lastObject;
                        for (int i = 0; i < ret.size(); i++) {
                            if (ret.elementAt(i) == object) {
                                ret.remove(i);
                                break;
                            }
                        }
                        if (Utilities.isLoggerActive()) {
                            logger.finer("Verfolge weiter Letztes Object: area:" + lastObject.getArea() + " dist: " + dist);
                        }

                    } else {
                        object = new PixelObject(this);
                        object.setContrast(contrast);
                        // if(Utilities.isLoggerActive())logger.info("Kontrast:
                        // "+contrast+" : "+objectContrast);
                        object.setWhiteContrast(objectContrast);
                    }
                    if (showdebug) {
                        if (object.getArea() > 20) {
                            w.setImage(0, line, getImage());
                        }
                    }
                    int tmp = object.getSize();
                    getObject(x, y, tmpGrid, object);
                    if (tmp == object.getSize()) {
                        object = new PixelObject(this);
                        object.setContrast(contrast);
                        // if(Utilities.isLoggerActive())logger.info("Kontrast:
                        // "+contrast+" : "+objectContrast);
                        object.setWhiteContrast(objectContrast);
                        getObject(x, y, tmpGrid, object);
                    }
                    // if(Utilities.isLoggerActive())logger.info(object.getSize
                    // ()+"
                    // avg "+object.getAverage()+" area: "+object.getArea());
                    if (object.getArea() > 20) {
                        if (showdebug) {
                            w.setImage(1, line, getImage());
                        }
                        if (showdebug) {
                            w.setText(2, line, "Size: " + object.getSize());
                        }
                        if (showdebug) {
                            w.setText(3, line, "AVG: " + object.getAverage());
                        }
                        if (showdebug) {
                            w.setText(4, line, "Area: " + object.getArea());
                        }
                        if (showdebug) {
                            w.setImage(5, line, object.toLetter().getImage());
                        }
                        if (showdebug) {
                            w.setText(6, line, object.toLetter().getDim());
                        }
                    }
                    line++;
                    lastObject = object;
                    for (int i = 0; i < ret.size(); i++) {
                        if (object.getArea() > ret.elementAt(i).getArea()) {

                            ret.add(i, object);
                            // if(Utilities.isLoggerActive())logger.finer(
                            // "Found
                            // Object size:"+object.getSize()+"
                            // "+object.getWidth()+" - "+object.getArea());

                            // BasicWindow.showImage(this.getImage());

                            object = null;
                            break;
                        }
                    }
                    if (object != null) {
                        ret.add(object);
                        // if(Utilities.isLoggerActive())logger.finer("Found
                        // Object size:"+object.getSize()+"
                        // "+object.getWidth()+" - "+object.getArea());

                    }

                } else {

                    // logger.finer("fdsf");

                }
            }
        }
        if (showdebug) {
            w.refreshUI();
        }
        return ret;

    }

    /**
     * Gibt ein Pixelarray des internen Grids zurück
     * 
     * @return Pixelarray
     */
    public int[] getPixel() {
        int[] pix = new int[getWidth() * getHeight()];
        int pixel = 0;
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                pix[pixel] = getPixelValue(x, y);
                pixel++;
            }
        }
        return pix;
    }

    /**
     * Gibt den Pixelwert an der stelle x,y zurück.
     * 
     * @param x
     * @param y
     * @return Pixelwert bei x,y
     */
    public int getPixelValue(int x, int y) {
        return PixelGrid.getPixelValue(x, y, grid);
    }

    /**
     * 
     * @return Gibt einen ASCII String des Bildes zurück
     */
    public String getString() {
        int avg = getAverage();
        StringBuilder ret = new StringBuilder();

        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                if (isElement(getPixelValue(x, y), avg))
                    ret.append('*');
                else
                    ret.append((int) Math.floor(9 * (getPixelValue(x, y) / getMaxPixelValue())));

            }
            ret.append(new char[] { '\r', '\n' });
        }

        return ret.toString();

    }

    /**
     * Gibt die Breite des internen captchagrids zurück
     * 
     * @return breite
     */
    public int getWidth() {
        return grid.length;
    }

    /**
     * Erstellt das negativ
     */
    public void invert() {
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                int[] a = Colors.hexToRgb(getMaxPixelValue());
                int[] b = Colors.hexToRgb(getPixelValue(x, y));
                a[0] = a[0] - b[0];
                a[1] = a[1] - b[1];
                a[2] = a[2] - b[2];
                setPixelValue(x, y, Colors.rgbToHex(a));
            }
        }
    }

    /**
     * Kontrasterkennung. Prüft ob der wert über einer Schwelle ist
     * 
     * @param value
     * @param avg
     *            vergleichswet (meistens durchschnitsswert)
     * @return true, falls Pixel Etwas zum Bild beiträgt, sonst false
     */
    public boolean isElement(int value, int avg) {
        return value < avg * owner.getJas().getDouble("RelativeContrast");
    }

    /**
     * Normalisiert die Pixel und sorgt so für einen höheren Kontrast
     */
    public void normalize() {
        normalize(1);

    }

    /**
     * Normalisiert Pixel und Multipliziert deren wert mit multi. Der Kontrast
     * wird dabei künstlich erhöht bzw erniedrigt.
     * 
     * @param multi
     */
    public void normalize(double multi) {
        normalize(multi, 0, 0);
    }

    /**
     * Normalisiert den Bereich zwischen cutMin und CutMax
     * 
     * @param multi
     * @param cutMax
     * @param cutMin
     */
    public void normalize(double multi, double cutMax, double cutMin) {
        int max = 0;
        int min = Integer.MAX_VALUE;
        int akt;
        cutMin *= getMaxPixelValue();
        cutMax *= getMaxPixelValue();
        cutMax = getMaxPixelValue() - cutMax;
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                akt = getPixelValue(x, y);
                if (akt < min && akt > cutMin) {
                    min = akt;
                }
                if (akt > max && akt < cutMax) {
                    max = akt;
                }
            }
        }

        Double faktor = (double) (max - min) / (double) getMaxPixelValue();
        if (Utilities.isLoggerActive()) {
            logger.fine(min + " <> " + max + " : " + faktor);
        }
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                akt = getPixelValue(x, y);
                if (akt <= cutMin) {
                    setPixelValue(x, y, 0);
                    continue;
                }
                if (akt >= cutMax) {
                    setPixelValue(x, y, getMaxPixelValue());
                    continue;
                }

                akt -= min;
                akt /= faktor;
                akt *= multi;
                // if(Utilities.isLoggerActive())logger.fine(getPixelValue(x,y
                // )+"
                // = "+akt);
                akt = Math.min(akt, getMaxPixelValue());
                akt = Math.max(akt, 0);
                setPixelValue(x, y, akt);

            }
        }

    }

    /**
     * Gibt ein ACSI bild des Captchas aus
     */
    public void printGrid() {
        if (Utilities.isLoggerActive()) {
            logger.info("\r\n" + getString());
        }
    }

    /**
     * Entfernt Schwarze Störungen
     * 
     * @param faktor
     *            Stärke
     */
    public void reduceBlackNoise(int faktor) {
        reduceBlackNoise(faktor, 1.0);
    }

    /**
     * Entfernt schwarze Störungen
     * 
     * @param faktor
     *            prüfradius
     * @param contrast
     *            Kontrasteinstellungen
     */
    public void reduceBlackNoise(int faktor, double contrast) {
        int avg = getAverage();
        int[][] newGrid = new int[getWidth()][getHeight()];
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {

                if (x == 0 && y == 0 && faktor < 3) {
                    newGrid[0][0] = grid[0][0];
                } else {
                    int localAVG = getAverageWithoutPoint(x, y, faktor, faktor);
                    if (isElement(getPixelValue(x, y), (int) (avg * contrast)) && localAVG >= contrast * getMaxPixelValue()) {

                        PixelGrid.setPixelValue(x, y, newGrid, localAVG);
                    } else {
                        PixelGrid.setPixelValue(x, y, newGrid, getPixelValue(x, y));
                    }
                }
            }
        }
        grid = newGrid;
    }

    /**
     * Entfernt weißes Rauschen
     * 
     * @param faktor
     *            Stärke des Effekts
     */
    public void reduceWhiteNoise(int faktor) {
        reduceWhiteNoise(faktor, 1.0);
    }

    /**
     * Entfernt weißes Rauschen
     * 
     * @param faktor
     *            Prüfradius
     * @param contrast
     *            Kontrasteinstellungen.je kleiner, desto mehr Pixel werden als
     *            störung erkannt, hat bei sw bildern kaum auswirkungen
     */
    public void reduceWhiteNoise(int faktor, double contrast) {
        int avg = getAverage();
        int[][] newGrid = new int[getWidth()][getHeight()];
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                // Korrektur weil sonst das linke obere PUxel schwarz wird.
                if (x == 0 && y == 0 && faktor < 3) {
                    newGrid[0][0] = grid[0][0];
                } else {

                    if (!isElement(getPixelValue(x, y), (int) (avg * contrast))) {
                        PixelGrid.setPixelValue(x, y, newGrid, getAverageWithoutPoint(x, y, faktor, faktor));
                    }
                }
            }
        }
        grid = newGrid;
    }

    /**
     * entfernt moegliche bruecken die zwischen zwei buchstaben sind
     * 
     * @param pixels
     *            Wieviele Pixel Um das Objekt liegen dürfen
     * @param middel
     *            ambesten zwischen 2.1-3
     */
    public void removeBridges(int pixels, double middel) {
        int avg = getAverage();
        int[][] newGrid = new int[getWidth()][getHeight()];
        int ignorh2 = (int) (getHeight() / middel);
        int ignorh1 = getHeight() - ignorh2;

        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                if (x < pixels || y < pixels || y > ignorh1 || y < ignorh2) {
                    newGrid[x][y] = grid[x][y];
                } else {
                    if (isElement(getPixelValue(x, y), avg)) {
                        int c = 0;
                        int i = 0;
                        while (c <= pixels && y + i < getHeight()) {
                            if (isElement(getPixelValue(x, y + i), avg) || isElement(getPixelValue(x + 1, y + i), avg) || isElement(getPixelValue(x - 1, y + i), avg) || isElement(getPixelValue(x + 1, y + i - 1), avg) || isElement(getPixelValue(x - 1, y + i - 1), avg)) {
                                c++;
                            } else {
                                break;
                            }
                            i++;
                        }
                        i = 0;
                        while (c <= pixels && y - i > 0) {
                            if (isElement(getPixelValue(x, y - i), avg) || isElement(getPixelValue(x + 1, y - i), avg) || isElement(getPixelValue(x - 1, y - i), avg) || isElement(getPixelValue(x + 1, y - i - 1), avg) || isElement(getPixelValue(x - 1, y - i - 1), avg)) {
                                c++;
                            } else {
                                break;
                            }
                            i++;
                        }
                        if (c <= pixels) {
                            PixelGrid.setPixelValue(x, y, newGrid, getMaxPixelValue());
                        } else {
                            newGrid[x][y] = grid[x][y];
                        }

                    } else {
                        newGrid[x][y] = grid[x][y];
                    }
                }
            }
        }
        grid = newGrid;
    }

    /**
     * Entfernt ein Objekt aus dem Captcha (färbt es weiß ein)
     * 
     * @param object
     */
    public void removeObjectFromGrid(PixelObject object) {
        colorObject(object, getMaxPixelValue());

    }

    /**
     * Entfernt kleine Objekte aus dem Bild
     * 
     * @param contrast
     * @param objectContrast
     */
    public void removeSmallObjects(double contrast, double objectContrast) {
        int tmp = owner.getJas().getInteger("minimumObjectArea");

        owner.getJas().set("minimumObjectArea", 0);
        Vector<PixelObject> ret = getObjects(contrast, objectContrast);
        owner.getJas().set("minimumObjectArea", tmp);
        for (int i = 1; i < ret.size(); i++) {

            removeObjectFromGrid(ret.elementAt(i));

        }
    }

    /**
     * @param contrast
     * @param objectContrast
     * @param maxSize
     */
    public void removeSmallObjects(double contrast, double objectContrast, int maxSize) {
        int tmp = owner.getJas().getInteger("minimumObjectArea");

        owner.getJas().set("minimumObjectArea", 0);
        Vector<PixelObject> ret = getObjects(contrast, objectContrast);
        owner.getJas().set("minimumObjectArea", tmp);

        for (int i = 1; i < ret.size(); i++) {
            // BasicWindow.showImage(ret.elementAt(i).toLetter().getImage(),"LL "
            // +ret.elementAt(i).getSize());
            if (ret.elementAt(i).getSize() < maxSize) {
                removeObjectFromGrid(ret.elementAt(i));
            }

        }
    }

    /**
     * @param contrast
     * @param objectContrast
     * @param maxSize
     */
    public void removeSmallObjects(double contrast, double objectContrast, int maxSize, int mindistx, int mindisty) {
        int tmp = owner.getJas().getInteger("minimumObjectArea");

        owner.getJas().set("minimumObjectArea", 0);
        Vector<PixelObject> ret = getObjects(contrast, objectContrast);
        owner.getJas().set("minimumObjectArea", tmp);

        outer: for (int i = 0; i < ret.size(); i++) {
            // BasicWindow.showImage(ret.elementAt(i).toLetter().getImage(),"LL "
            // +ret.elementAt(i).getSize());
            PixelObject el = ret.elementAt(i);
            if (el.getSize() < maxSize) {
                for (PixelObject o : ret) {
                    if (el.getSize() >= maxSize && o.isTouching(el, true, mindistx, mindisty)) {
                        continue outer;
                    }
                }
                removeObjectFromGrid(el);
            }

        }
    }

    /**
     * Macht das Bild gröber
     * 
     * @param faktor
     *            Grobheit
     */
    public void sampleDown(int faktor) {
        int newWidth = (int) Math.ceil(getWidth() / (double) faktor);
        int newHeight = (int) Math.ceil(getHeight() / (double) faktor);

        int[][] newGrid = new int[getWidth()][getHeight()];

        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                int localAVG = 0;
                int values = 0;
                for (int gx = 0; gx < faktor; gx++) {
                    for (int gy = 0; gy < faktor; gy++) {
                        int newX = x * faktor + gx;
                        int newY = y * faktor + gy;
                        if (newX > getWidth() || newY > getHeight()) {
                            continue;
                        }
                        localAVG = Colors.mixColors(localAVG, getPixelValue(newX, newY), values, 1);
                        values++;

                    }
                }

                for (int gx = 0; gx < faktor; gx++) {
                    for (int gy = 0; gy < faktor; gy++) {
                        int newX = x * faktor + gx;
                        int newY = y * faktor + gy;
                        PixelGrid.setPixelValue(newX, newY, newGrid, localAVG);

                    }
                }

            }
        }

        grid = newGrid;

    }

    /**
     * Speichert das Bild asl JPG ab
     * 
     * @param file
     *            Zielpfad
     */
    public void saveImageasJpg(File file) {
        BufferedImage bimg = null;

        bimg = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        bimg.setRGB(0, 0, getWidth(), getHeight(), getPixel(), 0, getWidth());

        try {
            FileOutputStream fos = new FileOutputStream(file);

            JPEGImageEncoder jpeg = JPEGCodec.createJPEGEncoder(fos);
            jpeg.encode(bimg);
            fos.close();
        } catch (Exception e) {
            JDLogger.exception(e);
        }
    }

    /**
     * Setzt das interne Pixelgrid
     * 
     * @param letterGrid
     *            int[][]
     */
    public void setGrid(int[][] letterGrid) {
        grid = letterGrid;
    }

    public void setGridCopy(int[][] grid, int leftPadding, int topPadding, int rightPadding, int bottomPadding) {
        int newWidth = PixelGrid.getGridWidth(grid) - (leftPadding + rightPadding);
        int newHeight = PixelGrid.getGridHeight(grid) - (topPadding + bottomPadding);

        int[][] newGrid = new int[newWidth][newHeight];
        location[0] = 0;
        location[1] = 0;
        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                newGrid[x][y] = grid[x + leftPadding][y + topPadding];
            }
        }

        this.grid = newGrid;
    }

    public void setLocation(int[] loc) {
        location = loc;
    }

    public void setOrgGrid(int[][] grid) {
        tmpGrid = grid;
    }

    /**
     * @param owner
     *            the owner to set
     */
    public void setOwner(JAntiCaptcha owner) {
        this.owner = owner;
    }

    /**
     * Nimmt ein int-array auf und wandelt es in das interne Grid um
     * 
     * @param pixel
     *            Pixel Array
     */
    public void setPixel(int[] pixel) {
        this.pixel = pixel;
        int i = 0;
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                grid[x][y] = pixel[i++];
            }
        }
    }

    /**
     * Setzt den pixel value bei x,y. Umrechnungen werden dabei gemacht. deshalb
     * kann nicht auf grid direkt zugegriffen werden. Grid beinhaltet roh daten
     * 
     * @param x
     * @param y
     * @param value
     */
    public void setPixelValue(int x, int y, int value) {
        PixelGrid.setPixelValue(x, y, grid, value);
    }

    /**
     * TestFUnktion um farbräume zu testen. Das Bild sollte keine ändeungen
     * haben wenn alles stimmt
     * 
     */
    public void testColor() {
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                // Einmal um die Farbe und wieder zurück
                setPixelValue(x, y, getPixelValue(x, y));
            }
        }
    }

    /**
     * Verwendet die SampleDown Methode um ein reines Schwarzweißbild zu
     * erzeugen
     */
    public void toBlackAndWhite() {
        toBlackAndWhite(1);
    }

    /**
     * Erzeugt ein schwarzweiß bild
     * 
     * @param contrast
     *            Schwellwert für die Kontrasterkennung
     */
    public void toBlackAndWhite(double contrast) {
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                setPixelValue(x, y, isElement(getPixelValue(x, y), (int) (getMaxPixelValue() * contrast)) ? 0 : getMaxPixelValue());
            }
        }
    }

    public String toHsbColorString() {
        StringBuilder ret = new StringBuilder();
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                int[] rgb = Colors.hexToRgb(getPixelValue(x, y));
                float[] hsb = Colors.rgb2hsb(rgb[0], rgb[1], rgb[2]);
                ret.append("y(");
                ret.append(y);
                ret.append(")x(");
                ret.append(x);
                ret.append(")=");
                ret.append(hsb[0] * 100);
                ret.append('\n');
            }
        }
        return ret.toString();
    }

}