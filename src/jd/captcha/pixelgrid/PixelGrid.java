//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.captcha.JAntiCaptcha;
import jd.captcha.configuration.Property;
import jd.captcha.gui.BasicWindow;
import jd.captcha.gui.ScrollPaneWindow;
import jd.captcha.pixelobject.PixelObject;
import jd.captcha.utils.UTILITIES;

import com.sun.image.codec.jpeg.ImageFormatException;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

/**
 * Diese Klasse behinhaltet alle wichtigen Methoden um das Image-Pixelgrid zu
 * bearbeiten
 * 
 * @author JD-Team
 */

public class PixelGrid extends Property {
    /**
     * Logger
     */
    public Logger logger = UTILITIES.getLogger();

    /**
     * ParameterDump
     */
    public JAntiCaptcha owner;

    /**
     * Internes grid
     */
    public int[][] grid;

    /**
     * Pixel Array
     */
    public int[] pixel;
    private int[] location = new int[] { 0, 0 };

    protected int[][] tmpGrid;

    public int[] getLocation() {
        return location;
    }

    public void setLocation(int[] loc) {
        this.location = loc;
    }

    /**
     * Gibt eine Prozentzahl aus. 0 = super 100= ganz schlimm
     * 
     * @param value
     * @param owner
     * @return Prozent der Erkennungssicherheit
     */
    public static int getValityPercent(int value, JAntiCaptcha owner) {
        if (value < 0) { return 100; }
        return (int) ((100.0 * (double) value) / (double) getMaxPixelValue(owner));
    }

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
                if (akt < min && akt > cutMin) min = akt;
                if (akt > max && akt < cutMax) max = akt;
            }
        }

        Double faktor = (double) (max - min) / (double) getMaxPixelValue();
        if (JAntiCaptcha.isLoggerActive()) logger.fine(min + " <> " + max + " : " + faktor);
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
                // if(JAntiCaptcha.isLoggerActive())logger.fine(getPixelValue(x,y)+"
                // = "+akt);
                akt = Math.min(akt, getMaxPixelValue());
                akt = Math.max(akt, 0);
                setPixelValue(x, y, (int) akt);

            }
        }

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
     * Gibt die Höhe des internen captchagrids zurück
     * 
     * @return Höhe
     */
    public int getHeight() {
        if (grid.length == 0) return 0;
        return grid[0].length;
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
     * Sollte je nach farbmodell den Höchsten pixelwert zurückgeben. RGB:
     * 0xffffff
     * 
     * @return Pixelwert je nach Farbbereich
     */
    public int getMaxPixelValue() {
        return getMaxPixelValue(owner);
    }

    /**
     * Gibt den maxpixelvalue mit faktor gewichtet zurück
     * 
     * @param faktor
     * @return maxpixelvalue
     */
    public int getMaxPixelValue(double faktor) {
        return (int) ((double) getMaxPixelValue(owner) * faktor);
    }

    /**
     * @param owner
     * @return Maximaler PixelWert
     */
    public static int getMaxPixelValue(JAntiCaptcha owner) {
        return owner.getJas().getColorFaktor() - 1;
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
        PixelGrid.setPixelValue(x, y, grid, value, owner);
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
     * @param owner
     *            JAntiCaptcha Instanz als Parameterdump
     */
    // public static void setPixelValue(int x, int y, int[][] localGrid,
    // int value, JAntiCaptcha owner) {
    // try {
    // float[] hsb=new float[3];
    // hsb[0]=0;
    // hsb[1]=0;
    // hsb[2]=0;
    // hsb[owner.getHSBType()]=(float) ((double) value /
    // owner.getColorValueFaktor());
    //			
    // localGrid[x][y] = Color.HSBtoRGB(hsb[0],hsb[1],hsb[2]);
    // } catch (ArrayIndexOutOfBoundsException e) {
    // UTILITIES.trace("ERROR: Nicht im grid; [" + x + "][" + y
    // + "] grid " + localGrid.length);
    // e.printStackTrace();
    //
    // }
    // }
    public static void setPixelValue(int x, int y, int[][] localGrid, int value, JAntiCaptcha owner) {
        try {
            localGrid[x][y] = value;
        } catch (Exception e) {
            // TODO: handle exception
        }

        // try {
        //
        // //
        // value*=(float)((float)owner.getColorFaktor()/(float)owner.getColorFaktor());
        // // String
        // // str=UTILITIES.fillInteger(Integer.toHexString(value),6,"0");
        // int[] v = UTILITIES.hexToRgb(value);
        // UTILITIES.trace(value+" - "+v[0]+"/"+v[1]+"/"+v[2]);
        // if (owner.getColorFormat() == 0) {
        // float[] hsb = { 0.0f, 0.0f, 0.0f };
        // if (owner.getColorComponent(0) <= 2)
        // hsb[owner.getColorComponent(0)] = (float) ((double) v[0] / 255.0);
        // if (owner.getColorComponent(1) <= 2)
        // hsb[owner.getColorComponent(1)] = (float) ((double) v[1] / 255.0);
        // if (owner.getColorComponent(2) <= 2)
        // hsb[owner.getColorComponent(2)] = (float) ((double) v[2] / 255.0);
        // UTILITIES.trace(value+" - "+hsb[0]+"/"+hsb[1]+"/"+hsb[2]);
        // localGrid[x][y] = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
        // while( localGrid[x][y]<0) localGrid[x][y]+=16777216;
        // UTILITIES.trace(localGrid[x][y]);
        // } else if (owner.getColorFormat() == 1) {
        // int[] rgb = { 0, 0, 0 };
        // if (owner.getColorComponent(0) <= 2)
        // rgb[owner.getColorComponent(0)] = v[0];
        // if (owner.getColorComponent(1) <= 2)
        // rgb[owner.getColorComponent(1)] = v[1];
        // if (owner.getColorComponent(2) <= 2)
        // rgb[owner.getColorComponent(2)] = v[2];
        //             
        // localGrid[x][y] = rgb[0] * 65536 + rgb[1] * 256 + rgb[2];
        // }
        //
        // } catch (ArrayIndexOutOfBoundsException e) {
        // UTILITIES.trace("ERROR: Nicht im grid; [" + x + "][" + y + "] grid "
        // + localGrid.length);
        // e.printStackTrace();
        //
        // }
    }

    /**
     * Gibt den Pixelwert an der stelle x,y zurück.
     * 
     * @param x
     * @param y
     * @return Pixelwert bei x,y
     */
    public int getPixelValue(int x, int y) {
        return PixelGrid.getPixelValue(x, y, grid, owner);
    }

    /**
     * @param x
     * @param y
     * @param grid
     * @param owner
     * @return Pixelwert bei x,y
     */
    public static int getPixelValue(int x, int y, int[][] grid, JAntiCaptcha owner) {
        if (x < 0 || x >= grid.length) return -1;
        if (y < 0 || grid.length == 0 || y >= grid[0].length) return -1;
        return grid[x][y];

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
                bv = UTILITIES.hexToRgb(getPixelValue(x, y));
                avg[0] += bv[0];
                avg[1] += bv[1];
                avg[2] += bv[2];
                i++;

            }
        }
        if (i == 0) return 0;
        avg[0] /= i;
        avg[1] /= i;
        avg[2] /= i;
        return UTILITIES.rgbToHex(avg);

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
        if (width == 1 && px == 0) width = 2;
        if (height == 1 && py == 0) height = 2;

        for (int x = Math.max(0, px - halfW); x < Math.min(px + width - halfW, getWidth()); x++) {
            for (int y = Math.max(0, py - halfH); y < Math.min(py + height - halfH, getHeight()); y++) {
                bv = UTILITIES.hexToRgb(getPixelValue(x, y));
                avg[0] += bv[0];
                avg[1] += bv[1];
                avg[2] += bv[2];
                i++;

            }
        }

        avg[0] /= i;
        avg[1] /= i;
        avg[2] /= i;
        return UTILITIES.rgbToHex(avg);
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
        if (width == 1 && px == 0) width = 2;
        if (height == 1 && py == 0) height = 2;

        for (int x = Math.max(0, px - halfW); x < Math.min(px + width - halfW, getWidth()); x++) {
            for (int y = Math.max(0, py - halfH); y < Math.min(py + height - halfH, getHeight()); y++) {
                if (x != px || y != py) {

                    bv = UTILITIES.hexToRgb(getPixelValue(x, y));
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
        return UTILITIES.rgbToHex(avg);
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

        this.grid = newGrid;

    }

    /**
     * Macht das Bild gröber
     * 
     * @param faktor
     *            Grobheit
     */
    public void sampleDown(int faktor) {
        int newWidth = (int) Math.ceil(getWidth() / faktor);
        int newHeight = (int) Math.ceil(getHeight() / faktor);

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
                        localAVG = UTILITIES.mixColors(localAVG, getPixelValue(newX, newY), values, 1);
                        values++;

                    }
                }

                for (int gx = 0; gx < faktor; gx++) {
                    for (int gy = 0; gy < faktor; gy++) {
                        int newX = x * faktor + gx;
                        int newY = y * faktor + gy;
                        setPixelValue(newX, newY, newGrid, localAVG, owner);

                    }
                }

            }
        }

        this.grid = newGrid;

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
                setPixelValue(x, y, newGrid, getAverage(x, y, faktor, faktor), owner);

                // getAverage(x, y, faktor, faktor)
            }
        }

        this.grid = newGrid;

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
     * Entfernt Alle Pixel die über getBackgroundSampleCleanContrast an avg
     * liegen
     * 
     * @param avg
     */
    public void cleanBackgroundByColor(int avg) {

        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                int dif = Math.abs(avg - getPixelValue(x, y));
                // if(JAntiCaptcha.isLoggerActive())logger.info(getPixelValue(x,
                // y)+"_");
                if (dif < (int) (getMaxPixelValue() * owner.getJas().getDouble("BackgroundSampleCleanContrast"))) {

                    this.setPixelValue(x, y, getMaxPixelValue());

                } else {

                }

            }
        }
        // grid = newgrid;

    }

    /**
     * Gibt das Pixelgrid als Image zurück
     * 
     * @return Image
     */
    public Image getImage() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            if (JAntiCaptcha.isLoggerActive()) logger.severe("Dimensionen falsch: " + this.getDim());
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

    public Image getFullImage() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            if (JAntiCaptcha.isLoggerActive()) logger.severe("Dimensionen falsch: " + this.getDim());
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

    /**
     * Gibt das Pixelgrid als vergrößertes Image zurück
     * 
     * @param faktor
     *            Vergrößerung
     * @return Neues Bild
     */
    public Image getImage(int faktor) {
        if ((getWidth() * faktor) <= 0 || (getHeight() * faktor) <= 0) {
            // if(JAntiCaptcha.isLoggerActive())logger.severe("Bild zu Klein.
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
                        setPixelValue(x, y, newGrid, getAverageWithoutPoint(x, y, faktor, faktor), this.owner);
                    }
                }
            }
        }
        grid = newGrid;
    }

    /**
     * Erstellt das negativ
     */
    public void invert() {
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                int[] a = UTILITIES.hexToRgb(getMaxPixelValue());
                int[] b = UTILITIES.hexToRgb(getPixelValue(x, y));
                a[0] = a[0] - b[0];
                a[1] = a[1] - b[1];
                a[2] = a[2] - b[2];
                setPixelValue(x, y, UTILITIES.rgbToHex(a));
            }
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
                    if (isElement(getPixelValue(x, y), (int) (avg * contrast)) && localAVG >= (contrast * getMaxPixelValue())) {

                        setPixelValue(x, y, newGrid, (int) (localAVG), this.owner);
                    } else {
                        setPixelValue(x, y, newGrid, getPixelValue(x, y), this.owner);
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
                            if (isElement(getPixelValue(x, y + i), avg) || isElement(getPixelValue(x + 1, y + i), avg) || isElement(getPixelValue(x - 1, y + i), avg) || isElement(getPixelValue(x + 1, y + i - 1), avg) || isElement(getPixelValue(x - 1, y + i - 1), avg))
                                c++;

                            else
                                break;
                            i++;
                        }
                        i = 0;
                        while (c <= pixels && y - i > 0) {
                            if (isElement(getPixelValue(x, y - i), avg) || isElement(getPixelValue(x + 1, y - i), avg) || isElement(getPixelValue(x - 1, y - i), avg) || isElement(getPixelValue(x + 1, y - i - 1), avg) || isElement(getPixelValue(x - 1, y - i - 1), avg))
                                c++;
                            else
                                break;
                            i++;
                        }
                        if (c <= pixels)
                            setPixelValue(x, y, newGrid, getMaxPixelValue(), this.owner);
                        else
                            newGrid[x][y] = grid[x][y];

                    } else
                        newGrid[x][y] = grid[x][y];
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

        // Encode as a JPEG
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file);

            JPEGImageEncoder jpeg = JPEGCodec.createJPEGEncoder(fos);
            jpeg.encode(bimg);
            fos.close();
        } catch (FileNotFoundException e) {

            e.printStackTrace();
        } catch (ImageFormatException e) {

            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        }
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
     * Gibt ein ACSI bild des Captchas aus
     */
    public void printGrid() {
        if (JAntiCaptcha.isLoggerActive()) logger.info("\r\n" + getString());
    }

    /**
     * @return Dimmensionsstring
     */
    public String getDim() {
        return "(" + getWidth() + "/" + getHeight() + ")";
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
        return value < (avg * this.owner.getJas().getDouble("RelativeContrast"));
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

    /**
     * Entfernt von allen 4 Seiten die Zeilen und Reihen bis nur noch der
     * content übrig ist
     * 
     * @return true/False
     */
    public boolean clean() {

        byte topLines = 0;
        byte bottomLines = 0;
        byte leftLines = 0;
        byte rightLines = 0;
        int avg = getAverage();

        for (int x = 0; x < getWidth(); x++) {
            boolean rowIsClear = true;
            for (int y = 0; y < getHeight(); y++) {

                if (isElement(getPixelValue(x, y), avg)) {
                    rowIsClear = false;
                    break;
                }
            }
            if (!rowIsClear) break;
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
            if (!rowIsClear) break;

            rightLines++;
        }

        if (leftLines >= getWidth() || (getWidth() - rightLines) > getWidth()) {
            if (JAntiCaptcha.isLoggerActive()) logger.severe("cleaning failed. nothing left1");

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
            if (!lineIsClear) break;
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
            if (!lineIsClear) break;
            bottomLines++;
        }

        if ((getWidth() - leftLines - rightLines) < 0 || (getHeight() - topLines - bottomLines) < 0) {
            if (JAntiCaptcha.isLoggerActive()) logger.severe("cleaning failed. nothing left");
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
     * 
     * @return Gibt einen ASCII String des Bildes zurück
     */
    public String getString() {
        int avg = getAverage();
        String ret = "";

        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {

                ret += isElement(getPixelValue(x, y), avg) ? "*" : (int) Math.floor(9 * (getPixelValue(x, y) / getMaxPixelValue()));

            }
            ret += "\r\n";
        }

        return ret;

    }

    /**
     * @param owner
     *            the owner to set
     */
    public void setOwner(JAntiCaptcha owner) {
        this.owner = owner;
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
     * Entfernt kleine Objekte aus dem Bild
     * 
     * @param contrast
     * @param objectContrast
     */
    public void removeSmallObjects(double contrast, double objectContrast) {
        double tmp = owner.getJas().getDouble("objectDetectionMergeSeperatedPartsDistance");
        owner.getJas().set("objectDetectionMergeSeperatedPartsDistance", -1.0);
        Vector<PixelObject> ret = getObjects(contrast, objectContrast);
        owner.getJas().set("objectDetectionMergeSeperatedPartsDistance", tmp);
        for (int i = 1; i < ret.size(); i++) {

            this.removeObjectFromGrid(ret.elementAt(i));

        }
    }

    /**
     * @param contrast
     * @param objectContrast
     * @param maxSize
     */
    public void removeSmallObjects(double contrast, double objectContrast, int maxSize) {
        double tmp = owner.getJas().getDouble("objectDetectionMergeSeperatedPartsDistance");
        owner.getJas().set("objectDetectionMergeSeperatedPartsDistance", -1.0);
        Vector<PixelObject> ret = getObjects(contrast, objectContrast);
        owner.getJas().set("objectDetectionMergeSeperatedPartsDistance", tmp);

        for (int i = 1; i < ret.size(); i++) {
            if (ret.elementAt(i).getSize() < maxSize) this.removeObjectFromGrid(ret.elementAt(i));

        }
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
        if (showdebug) w = new ScrollPaneWindow(this.owner);
        if (showdebug) w.setTitle("getObjects2");
        int line = 0;
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                if (tmpGrid[x][y] < 0) continue;

                if (getPixelValue(x, y) <= (objectContrast * getMaxPixelValue())) {

                    // Füge 2 Objekte zusammen die scheinbar zusammen gehören

                    dist = 100;
                    if (lastObject != null) {
                        dist = (int) (Math.pow(x - (lastObject.getXMin() + lastObject.getWidth() / 2), 2) + Math.pow(y - (lastObject.getYMin() + lastObject.getHeight() / 2), 2));
                    }

                    if (lastObject != null && lastObject.getArea() < owner.getJas().getInteger("minimumObjectArea") && dist < Math.pow(owner.getJas().getInteger("minimumLetterWidth") / 2 + 1, 2)) {

                        object = lastObject;
                        for (int i = 0; i < ret.size(); i++) {
                            if (ret.elementAt(i) == object) {
                                ret.remove(i);
                                break;
                            }
                        }
                        if (JAntiCaptcha.isLoggerActive()) logger.finer("Verfolge weiter Letztes Object: area:" + lastObject.getArea() + " dist: " + dist);

                    }
                    // else if
                    // (owner.getJas().getDouble("objectDetectionMergeSeperatedPartsDistance")
                    // > 0.0 && lastObject != null && x >= lastObject.getXMin()
                    // && x <= (lastObject.getXMin() + lastObject.getWidth())) {
                    // double xDist = Math.abs(x - (lastObject.getXMin() +
                    // lastObject.getWidth() / 2));
                    // double perc = 100.0 * (xDist / (double)
                    // (lastObject.getWidth() / 2));
                    //
                    // if (perc <
                    // owner.getJas().getDouble("objectDetectionMergeSeperatedPartsDistance"))
                    // {
                    //
                    // object = lastObject;
                    // for (int i = 0; i < ret.size(); i++) {
                    // if (ret.elementAt(i) == object) {
                    // ret.remove(i);
                    // break;
                    // }
                    // }
                    // if (JAntiCaptcha.isLoggerActive()) logger.finer(perc + "
                    // Neues Objekt liegt im alten.... weiter gehts");
                    // }
                    // else {
                    // object = new PixelObject(this);
                    // object.setContrast(contrast);
                    // //
                    // if(JAntiCaptcha.isLoggerActive())logger.info("Kontrast:
                    // // "+contrast+" : "+objectContrast);
                    // object.setWhiteContrast(objectContrast);
                    // }
                    // }
                    else {
                        object = new PixelObject(this);
                        object.setContrast(contrast);
                        // if(JAntiCaptcha.isLoggerActive())logger.info("Kontrast:
                        // "+contrast+" : "+objectContrast);
                        object.setWhiteContrast(objectContrast);
                    }
                    if (showdebug) {
                        if (object.getArea() > 20) w.setImage(0, line, getImage());
                    }
                    getObject(x, y, tmpGrid, object);
                    // if(JAntiCaptcha.isLoggerActive())logger.info(object.getSize()+"
                    // avg "+object.getAverage()+" area: "+object.getArea());
                    if (object.getArea() > 20) {
                        if (showdebug) w.setImage(1, line, getImage());
                        if (showdebug) w.setText(2, line, "Size: " + object.getSize());
                        if (showdebug) w.setText(3, line, "AVG: " + object.getAverage());
                        if (showdebug) w.setText(4, line, "Area: " + object.getArea());
                        if (showdebug) w.setImage(5, line, object.toLetter().getImage());
                        if (showdebug) w.setText(6, line, object.toLetter().getDim());
                    }
                    line++;
                    lastObject = object;
                    for (int i = 0; i < ret.size(); i++) {
                        if (object.getArea() > ret.elementAt(i).getArea()) {

                            ret.add(i, object);
                            // if(JAntiCaptcha.isLoggerActive())logger.finer("Found
                            // Object size:"+object.getSize()+"
                            // "+object.getWidth()+" - "+object.getArea());

                            // BasicWindow.showImage(this.getImage());

                            object = null;
                            break;
                        }
                    }
                    if (object != null) {
                        ret.add(object);
                        // if(JAntiCaptcha.isLoggerActive())logger.finer("Found
                        // Object size:"+object.getSize()+"
                        // "+object.getWidth()+" - "+object.getArea());

                    }

                }
            }
        }
        if (showdebug) w.refreshUI();
        return ret;

    }

    public String toHsbColorString() {
        String ret = "";
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                int[] rgb = UTILITIES.hexToRgb(getPixelValue(x, y));
                float[] hsb = UTILITIES.rgb2hsb(rgb[0], rgb[1], rgb[3]);
                ret += "y(" + y + ")x(" + x + ")=" + hsb[0] * 100 + "\n";
            }
        }
        return ret;
    }

    protected Vector<PixelObject> getColorObjects(int letterNum) {

        // int percent =
        // owner.getJas().getInteger("colorObjectDetectionPercent");
        // int running =
        // owner.getJas().getInteger("colorObjectDetectionRunningAverage");
        logger.info("Max pixel value: " + this.getMaxPixelValue());
        // Erstelle Farbverteilungsmap
        HashMap<Integer[], PixelObject> map = new HashMap<Integer[], PixelObject>();
        logger.info("" + UTILITIES.getColorDifference(new int[] { 0, 0, 204 }, new int[] { 0, 0, 184 }));
        logger.info("" + UTILITIES.getColorDifference(new int[] { 0, 0, 204 }, new int[] { 60, 10, 240 }));

        logger.info("" + UTILITIES.getColorDifference(new int[] { 255, 255, 255 }, new int[] { 0, 0, 0 }));
        final int avg = getAverage();
        int intensivity = 8;
        int h = getWidth() / letterNum / 4;
        Integer[] last = null;
        int d = 0;
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {

                Integer key = getPixelValue(x, y);
                int[] rgbA = UTILITIES.hexToRgb(key);

                if (isElement(key, avg) || UTILITIES.rgb2hsb(rgbA[0], rgbA[1], rgbA[2])[0] * 100 > 0) {
                    if (map.get(key) == null) {
                        if (d++ < getHeight() * 2) {
                            d = 0;
                            int[] bv = UTILITIES.hexToRgb(key);
                            boolean found = false;
                            if (last != null && UTILITIES.getHsbColorDifference(bv, UTILITIES.hexToRgb(map.get(last).getAverage())) < intensivity) {
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
                                        dif = UTILITIES.getHsbColorDifference(bv, UTILITIES

                                        .hexToRgb(object.getAverage()));

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
                                object.setColor(key);
                                object.add(x, y, key);
                                last = new Integer[] { key, x };
                                map.put(last, object);
                            }
                        } else {
                            PixelObject object = new PixelObject(this);
                            object.setColor(key);
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
            PixelObject ob = (PixelObject) vals.next();
            els.add(new Object[] { (Integer[]) keys.next(), ob });

        }
        Collections.sort(els, new Comparator<Object[]>() {

            public int compare(Object[] o1, Object[] o2) {
                Letter letter1 = ((PixelObject) o1[1]).toLetter();
                Letter letter2 = ((PixelObject) o2[1]).toLetter();
                if (letter1.getElementPixel() > letter2.getElementPixel())
                    return 1;
                else
                    return 0;
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
                Object[] thisel = (Object[]) iter.next();
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
                        dif = (double) (key2[1] - integers[1]);
                        dif2 = (Math.abs((double) ((key2[1] + obj.getWidth()) - (integers[1] + object.getWidth()))));

                        if (dif == 0 || dif2 == 0 || (dif < 0 && (dif + obj.getWidth()) > 0)) {
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
                    dif = UTILITIES.getHsbColorDifference(UTILITIES

                    .hexToRgb(bestobj.getAverage()), UTILITIES

                    .hexToRgb(object.getAverage()));
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
                // TODO Auto-generated method stub
                return o1[1].compareTo(o2[1]);
            }
        });
        Iterator<Integer[]> iterator2 = ar.iterator();
        Vector<PixelObject> ret = new Vector<PixelObject>();
        while (iterator2.hasNext()) {
            PixelObject it = map.get((Integer[]) iterator2.next());
            ret.add(it);
        }
        return ret;
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
        if (leftLines >= width || (width - rightLines) > width) { return new int[] { 0, 0 }; }
        ;

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
        if ((width - leftLines - rightLines) < 0 || (height - topLines - bottomLines) < 0) { return new int[] { 0, 0 }; }
        return new int[] { width - leftLines - rightLines, height - topLines - bottomLines };

    }

    public void desinx(double max, double omega, double phi) {
        omega = 2 * Math.PI / omega;

        int[][] tmp = new int[getWidth()][getHeight()];

        int shift;

        for (int y = 0; y < getHeight(); y++) {

            shift = (int) (max * Math.sin(omega * (y + phi)));

            for (int x = 0; x < getWidth(); x++) {

                tmp[x][y] = (x + shift < getWidth() && x + shift >= 0) ? grid[x + shift][y] : 0xFF;
            }

        }

        this.setGrid(tmp);

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

                tmp[x][y] = (y + shift < getHeight() && y + shift >= 0) ? grid[x][y + shift] : 0xFF;
            }

        }

        this.setGrid(tmp);

    }

    public void desin(double maxx, double omegax, double phix, double maxy, double omegay, double phiy) {
        int shift;
        omegax = 2 * Math.PI / omegax;
        omegay = 2 * Math.PI / omegay;
if(owner.getJas().getInteger("desinvariant")>=4){
    logger.warning("Entzerren fehlgeschlagen... versuche es ohne");
    return;
}
        int bestArea = 0;
        int[][] bestGrid = null;
        final HashMap<int[][], Integer> map = new HashMap<int[][], Integer>();
        Vector<int[][]> sorter = new  Vector<int[][]>();
        // for (int phix = (int)phixx*-1; phix <= phixx; phix++) {
        // for (int phiy = (int)phiyy*-1; phiy <= phiyy; phiy++) {
        all: for (int ax = 0; ax < 2; ax++) {
            for (int ay = 0; ay < 2; ay++) {

                int[][] tmp = new int[getWidth()][getHeight()];
                int[][] tmp2 = new int[getWidth()][getHeight()];

                for (int y = 0; y < getHeight(); y++) {

                    shift = (int) (maxy * Math.sin(omegay * (y + phiy))) * (ay == 0 ? -1 : 1);

                    for (int x = 0; x < getWidth(); x++) {

                        tmp[x][y] = (x + shift < getWidth() && x + shift >= 0) ? grid[x + shift][y] : 0xFF;
                    }
                    // grid[294 + shift][y] = 0xff0000 + 0x00ff00 * (ay == 0 ?
                    // -1 : 1);

                }
                for (int x = 0; x < getWidth(); x++) {

                    shift = (int) ((maxx * Math.sin(omegax * (x + phix))) * (ax == 0 ? -1.0 : 1.0));

                    for (int y = 0; y < getHeight(); y++) {

                        tmp2[x][y] = (y + shift < getHeight() && y + shift >= 0) ? tmp[x][y + shift] : 0xFF;
                    }
                    // grid[x][63 + shift] = 0xff0000 + 0x00ff00 * (ax == 0 ? -1
                    // : 1);

                }
                int[] a = getDimension(tmp2);
                Integer i = new Integer(a[0] + a[1]);
                sorter.add(tmp2);
                map.put( tmp2,i);
                //PixelGrid p = new PixelGrid(getWidth(),getHeight());
                //p.setGrid(tmp2);
               // BasicWindow.showImage(p.getImage(),i+"");


            }
        }
       
        // }
        // }
Collections.sort(sorter, new Comparator<Object>(){
    public int compare( Object a,  Object b){
       return map.get(a).compareTo(map.get(b));
        
    }
    
});
     logger.info("USE VARIANT: "+owner.getJas().getInteger("desinvariant"));
        this.setGrid(sorter.get(owner.getJas().getInteger("desinvariant")));
        
        
        
        //BasicWindow.showImage(this.getImage(), "USE VARIANT: "+owner.getJas().getInteger("desinvariant")+" : "+sorter.get(owner.getJas().getInteger("desinvariant")));

    }

    public void setOrgGrid(int[][] grid) {
      this.tmpGrid=grid;
        
    }


    /**
     * Erstellt das Objekt, ausgehend von einem Pixel. rekursive Funktion!
     * 
     * @param x
     * @param y
     * @param tmpGrid
     * @param object
     */
    private void getObject(int x, int y, int[][] tmpGrid, PixelObject object) {

        if (x < 0 || y < 0 || tmpGrid.length <= x || tmpGrid[0].length <= y || tmpGrid[x][y] < 0) return;
        int localValue = PixelGrid.getPixelValue(x, y, tmpGrid, owner);
        // UTILITIES.trace(x+"/"+y);
        if (object.doesColorAverageFit(localValue)) {
            object.add(x, y, localValue);
            tmpGrid[x][y] = -1;

            // Achtung!! Algos funktionieren nur auf sw basis richtig
            // grid[x][y] = 254;
            getObject(x - 1, y, tmpGrid, object);
            getObject(x - 1, y - 1, tmpGrid, object);
            getObject(x, y - 1, tmpGrid, object);
            getObject(x + 1, y - 1, tmpGrid, object);
            getObject(x + 1, y, tmpGrid, object);
            getObject(x + 1, y + 1, tmpGrid, object);
            getObject(x, y + 1, tmpGrid, object);
            getObject(x - 1, y + 1, tmpGrid, object);
        }

        return;

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

    public int[][] getGrid() {
        return grid;
    }

}