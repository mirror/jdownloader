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
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.gui.BasicWindow;
import jd.captcha.pixelobject.PixelObject;
import jd.captcha.utils.Utilities;
import jd.controlling.JDLogger;
import jd.nutils.Colors;

import com.jhlabs.image.BoxBlurFilter;
import com.jhlabs.image.ContrastFilter;
import com.jhlabs.image.PosterizeFilter;
import com.jhlabs.image.QuantizeFilter;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

/**
 * Diese Klasse behinhaltet Methoden zum verarbeiten von Captchas. Also Grafiken
 * die Mehr als ein Element enthalten
 * 
 * @author JD-Team
 */
public class Captcha extends PixelGrid {

    private static Logger logger = Utilities.getLogger();

    private static final long serialVersionUID = 1L;

    /**
     * Factory Funktion gibt einen captcha von File zurück
     * 
     * @param file
     *            Pfad zum bild
     * @param owner
     *            Parameter Dump JAntiCaptcha
     * @return Captcha neuer captcha
     */
    public static Captcha getCaptcha(final File file, final JAntiCaptcha owner) {
        final Image img = Utilities.loadImage(file);
        final Captcha ret = Captcha.getCaptcha(img, owner);
        return ret;
    }

    /**
     * factory Methode für eine captchainstanz
     * 
     * @param image
     * @param owner
     * @return neuer Captcha
     */
    public static Captcha getCaptcha(final Image image, final JAntiCaptcha owner) {
        final int width = image.getWidth(null);
        final int height = image.getHeight(null);
        if (width <= 0 || height <= 0) return null;

        final PixelGrabber pg = new PixelGrabber(image, 0, 0, width, height, false);

        try {
            pg.grabPixels();
        } catch (Exception e) {
            return null;
        }

        final Captcha ret = new Captcha(width, height);
        ret.setOwner(owner);
        if (Utilities.isLoggerActive()) {
            logger.fine(width + "/" + height);
        }

        ret.setColorModel(pg.getColorModel());
        final ColorModel cm = pg.getColorModel();

        if (!(cm instanceof IndexColorModel)) {
            // not an indexed file (ie: not a gif file)
            ret.setPixel((int[]) pg.getPixels());
        } else {

            // ; // Utilities.trace("COLORS: "+numColors);
            // pg.getPixels();
            ret.setPixel((byte[]) pg.getPixels());
        }

        ret.setOrgGrid(PixelGrid.getGridCopy(ret.getGrid()));
        return ret;
    }

    /**
     * Resize auf newHeight. die proportionen bleiben erhalten
     * 
     * @param newHeight
     */
    public void resizetoHeight(final int newHeight) {
        final double faktor = (double) newHeight / (double) getHeight();
        final int newWidth = (int) Math.ceil(getWidth() * faktor);
        final int[][] newGrid = new int[newWidth][newHeight];
        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                final int v = grid[(int) Math.floor(x / faktor)][(int) Math.floor(y / faktor)];
                newGrid[x][y] = v;
            }
        }
        setGrid(newGrid);
    }

    /**
     * Captcha.getCaptcha(Letter a, Letter b) Gibt einen captcha zurück der aus
     * a +6pxTrennlinie +b besteht. (Addiert 2 Letter)
     * 
     * @param a
     * @param b
     * @return Neuer captcha
     */
    public static Captcha getCaptcha(Letter a, Letter b) {

        int newWidth = a.getWidth() + b.getWidth() + 6;
        int newHeight = Math.max(a.getHeight(), b.getHeight());
        Captcha ret = new Captcha(newWidth, newHeight);
        if (a.owner != null) {
            ret.setOwner(a.owner);
        }
        if (ret.owner == null) {
            ret.setOwner(b.owner);
        }
        if (ret.owner == null) {
            if (Utilities.isLoggerActive()) {
                logger.warning("Owner konnte nicht bestimmt werden!Dieser captcha ist nur eingeschränkt verwendbar.");
            }
        }
        ret.grid = new int[newWidth][newHeight];
        for (int x = 0; x < a.getWidth(); x++) {
            for (int y = 0; y < newHeight; y++) {
                ret.grid[x][y] = y < a.getHeight() ? a.grid[x][y] : (int) a.getMaxPixelValue();

            }
        }
        for (int x = a.getWidth(); x < a.getWidth() + 6; x++) {
            for (int y = 0; y < newHeight; y++) {
                ret.grid[x][y] = x == a.getWidth() + 2 || x == a.getWidth() + 3 ? 0 : (int) a.getMaxPixelValue();

            }
        }

        for (int x = a.getWidth() + 6; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                ret.grid[x][y] = y < b.getHeight() ? b.grid[x - (a.getWidth() + 6)][y] : (int) b.getMaxPixelValue();

            }
        }
        return ret;

    }

    private File captchafile;

    private ColorModel colorModel;

    private String correctCaptchaCode;

    /**
     * Array der länge getWidth()+1. hier werden gefundene Gaps abgelegt.
     * Einträge mit true bedeuten eine Lücke
     */
    private boolean[] gaps;

    /**
     * Speichert die Positiond es letzten erkannten Letters
     */
    private int lastletterX = 0;

    private LetterComperator[] letterComperators;

    private boolean perfectObjectDetection;

    /**
     * Speichert die Information ob der captcha schon vorverarbeitet wurde
     */
    private boolean prepared = false;

    /**
     * Speichert das original RGB Pixelgrid
     */
    public int[][] rgbGrid;

    /**
     * Temp Array für die getrennten letters; *
     */
    private Letter[] seperatedLetters;

    private double valityPercent;

    /**
     * Diese Klasse beinhaltet ein 2D-Pixel-Grid. Sie stellt mehrere Methoden
     * zur verfügung dieses Grid zu bearbeiten Um Grunde sind hier alle Methoden
     * zu finden um ein captcha als ganzes zu bearbeiten
     * 
     * @author JD-Team
     * @param width
     * @param height
     */

    public Captcha(int width, int height) {
        super(width, height);
        rgbGrid = new int[width][height];

    }

    public void cleanBackgroundByHorizontalSampleLine(int x1, int x2, int y1, int y2) {
        int avg = getAverage(x1, y1, x2 - x1, y2 - y1);
        cleanBackgroundByColor(avg);

    }

    public void cleanWithDetailMask(Captcha mask, int dif, int tolerance) {

        int[][] newgrid = new int[getWidth()][getHeight()];
        int[][] test = new int[getWidth()][getHeight()];
        if (mask.getWidth() != getWidth() || mask.getHeight() != getHeight()) {
            if (Utilities.isLoggerActive()) {
                logger.info("ERROR Maske und Bild passen nicht zusammmen");
            }
            return;
        }
        if (Utilities.isLoggerActive()) {
            logger.info(dif + "_");
        }
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                int min2 = 1;
                test[x][y] = Math.abs(mask.getPixelValue(x, y) - getPixelValue(x, y));
                int del = 0;
                int del2 = 0;
                if (Colors.getColorDifference(mask.getPixelValue(x, y), getPixelValue(x, y)) < dif) {
                    del += tolerance;
                    del2 += tolerance;
                    for (int i = 1; i < tolerance; i++) {
                        if (y + i < getHeight()) {
                            if (Colors.getColorDifference(mask.getPixelValue(x, y + i), getPixelValue(x, y + i)) < dif) del += tolerance - i;
                            del2 += tolerance - i;
                        } else
                            min2++;
                    }
                    for (int i = 1; i < tolerance; i++) {
                        if (x + i < getWidth()) {
                            if (Colors.getColorDifference(mask.getPixelValue(x + i, y), getPixelValue(x + i, y)) < dif) del += tolerance - i;
                            del2 += tolerance - i;
                        } else
                            min2++;
                    }
                }
                del *= min2;
                if (del > (del2 / 1.5)) {
                    // if (Math.abs(mask.getPixelValue(x, y) - getPixelValue(x,
                    // y)) < dif) {

                    PixelGrid.setPixelValue(x, y, newgrid, getMaxPixelValue());

                } else {
                    newgrid[x][y] = grid[x][y];
                }
            }
        }
        grid = newgrid;

    }

    public void cleanWithDetailMask(Captcha mask, int dif) {

        int[][] newgrid = new int[getWidth()][getHeight()];
        // int[][] test = new int[getWidth()][getHeight()];
        if (mask.getWidth() != getWidth() || mask.getHeight() != getHeight()) {
            if (Utilities.isLoggerActive()) {
                logger.info("ERROR Maske und Bild passen nicht zusammmen");
            }
            return;
        }
        if (Utilities.isLoggerActive()) {
            logger.info(dif + "_");
        }
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                // test[x][y] = Math.abs(mask.getPixelValue(x, y) -
                // getPixelValue(x, y));

                if (Colors.getColorDifference(mask.getPixelValue(x, y), getPixelValue(x, y)) < dif) {
                    // if (Math.abs(mask.getPixelValue(x, y) - getPixelValue(x,
                    // y)) < dif) {

                    PixelGrid.setPixelValue(x, y, newgrid, getMaxPixelValue());

                } else {
                    newgrid[x][y] = grid[x][y];
                }
            }
        }
        grid = newgrid;

    }

    /**
     * Entfernt Störungen über eine Maske und ersetzt diese mit den umliegenden
     * pixeln
     * 
     * @param mask
     *            Maske
     * @param width
     *            breite des Ersatzfeldes
     * @param height
     *            Höhe des Ersatzfeldes
     */
    public void cleanWithMask(Captcha mask, int width, int height) {
        int[][] newgrid = new int[getWidth()][getHeight()];
        // logger.info(mask.getWidth()+"/"+mask.getHeight()+" - "+getWidth()+" -
        // "+getHeight());
        if (mask.getWidth() != getWidth() || mask.getHeight() != getHeight()) {
            if (Utilities.isLoggerActive()) {
                logger.info("ERROR Maske und Bild passen nicht zusammmen");
            }
            return;
        }

        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                if (mask.getPixelValue(x, y) < 100) {

                    PixelGrid.setPixelValue(x, y, newgrid, getAverage(x, y, width, height, mask));

                } else {
                    newgrid[x][y] = grid[x][y];
                }
            }
        }
        grid = newgrid;
        // BasicWindow.showImage(this.getImage());
    }

    /**
     * Fügt 2 Capchtas zusammen. Schwarze stellen werden dabei ignoriert.
     * 
     * @param tmp
     */
    public void concat(Captcha tmp) {
        if (getWidth() != tmp.getWidth() || getHeight() != tmp.getHeight()) {
            if (Utilities.isLoggerActive()) {
                logger.severe("Concat fehlgeschlagen Dimensions nicht gleich");
            }
            return;
        }

        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                if (PixelGrid.getPixelValue(x, y, tmp.grid) > owner.getJas().getDouble("getBlackPercent") * getMaxPixelValue()) {

                    int newPixelValue = Colors.mixColors(getPixelValue(x, y), PixelGrid.getPixelValue(x, y, tmp.grid));
                    setPixelValue(x, y, newPixelValue);
                }
            }
        }

    }

    /**
     * KOnvertiert den Captcha gemäß dem neuen newColorFormat (Mix aus RGB oder
     * hsb) Als Ausgangsgrid dienen die originalFarben
     * 
     * @param newColorFormat
     */
    public void convertOriginalPixel(String newColorFormat) {
        owner.getJas().setColorType(newColorFormat);
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                // grid[x][y] = pixel[i++];

                Color c = new Color(rgbGrid[x][y]);
                float[] col = new float[4];

                if (owner.getJas().getColorFormat() == 0) {

                    Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), col);
                    col[3] = 0;

                } else if (owner.getJas().getColorFormat() == 1) {

                    col[0] = (float) c.getRed() / 255;
                    col[1] = (float) c.getGreen() / 255;
                    col[2] = (float) c.getBlue() / 255;
                    col[3] = 0;
                }

                grid[x][y] = (int) (col[owner.getJas().getColorComponent(0)] * 255) * 65536 + (int) (col[owner.getJas().getColorComponent(1)] * 255) * 256 + (int) (col[owner.getJas().getColorComponent(2)] * 255);

            }
        }

    }

    /**
     * Konvertiert das Aktuelle Grid in einen Neuen Farbbereich.
     * 
     * @param newColorFormat
     */
    public void convertPixel(String newColorFormat) {
        owner.getJas().setColorType(newColorFormat);

        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                // grid[x][y] = pixel[i++];

                Color c = new Color(grid[x][y]);
                float[] col = new float[4];

                if (owner.getJas().getColorFormat() == 0) {

                    Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), col);
                    col[3] = 0;

                } else if (owner.getJas().getColorFormat() == 1) {

                    col[0] = (float) c.getRed() / 255;
                    col[1] = (float) c.getGreen() / 255;
                    col[2] = (float) c.getBlue() / 255;
                    col[3] = 0;
                }

                grid[x][y] = (int) (col[owner.getJas().getColorComponent(0)] * 255) * 65536 + (int) (col[owner.getJas().getColorComponent(1)] * 255) * 256 + (int) (col[owner.getJas().getColorComponent(2)] * 255);

            }
        }

    }

    /**
     * Diese Methode gibt alle internen Datenresourcen wie pixeldaten wieder
     * frei.
     * 
     */
    public void destroyInternalData() {

    }

    /**
     * Errechnet einen Durchschnissfarbwert im angegebenen Bereich. Elemente der
     * übergebenene Maske werden dabei vernachlässigt
     * 
     * @param px
     * @param py
     * @param width
     * @param height
     * @param mask
     * @return durchschnittswert
     */
    public int getAverage(int px, int py, int width, int height, Captcha mask) {
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
                if (mask.getPixelValue(x, y) > 100) {
                    bv = Colors.hexToRgb(getPixelValue(x, y));
                    avg[0] += bv[0];
                    avg[1] += bv[1];
                    avg[2] += bv[2];
                    i++;

                }

            }
        }
        avg[0] /= i;
        avg[1] /= i;
        avg[2] /= i;
        return Colors.rgbToHex(avg);
    }

    public Vector<PixelObject> getBiggestObjects(int letterNum, int minArea, double contrast, double objectContrast) {
        int splitter;
        int splitNum;
        int found = 0;
        int i = 0;
        int minWidth = Integer.MAX_VALUE;
        int maxWidth;
        // Alle Objekte aus dem captcha holen. Sie sind nach der Größe Sortiert
        Vector<PixelObject> objects = getObjects(contrast, objectContrast);

        if (owner.jas.getBoolean("directLetterDetection")) {
            logger.info("Get directLetterDetection");
            Vector<PixelObject> objectsret = new Vector<PixelObject>();

            // double iaww =
            // owner.getJas().getDouble("intersectionAWidthWeight");
            int bx = owner.getJas().getInteger("borderVarianceX");
            int by = owner.getJas().getInteger("borderVarianceY");

            for (int objectI = 0; objectI < objects.size(); objectI++) {
                PixelObject pixelObject = objects.get(objectI);

                if (pixelObject.getArea() > minArea * 2) {
                    // owner.getJas().set("coverageFaktorAWeight", cfaw * 2.3);
                    owner.getJas().set("borderVarianceX", bx * owner.getJas().getInteger("minimumLetterWidth") * 100);
                    owner.getJas().set("borderVarianceY", by * owner.getJas().getInteger("minimumLetterWidth") * 100);
                    PixelObject current = pixelObject;
                    Letter letter = current.toLetter();
                    LetterComperator resletter = owner.getLetter(letter);
                    resletter.setValityPercent(resletter.getValityPercent() * 0.75);
                    current.detected = resletter;
                    int b;

                    // if (owner.isShowDebugGui())
                    // BasicWindow.showImage(letter.getImage(), "Letter " +
                    // objectI + " -" + resletter.getDecodedValue());
                    if (resletter.getB() != null && !resletter.getDecodedValue().equals("-") && (b = pixelObject.getArea() - resletter.getB().getArea()) > minArea && b > resletter.getB().getArea() / 3 && resletter.getOffset() != null && resletter.getOffset().length > 0) {
                        // int spat = 0;
                        logger.info("dld: got letter: " + resletter.getDecodedValue());
                        // BasicWindow.showImage(resletter.getB().getImage(),
                        // resletter.getDecodedValue());
                        // if (resletter.getOffset()[0] <
                        // resletter.getB().getWidth() / 3)
                        // spat = resletter.getOffset()[0] +
                        // resletter.getB().getWidth();
                        // else
                        // spat = resletter.getOffset()[0];
                        PixelObject[] spobjects = current.cut(resletter.getOffset()[0], resletter.getOffset()[0] + resletter.getB().getWidth(), owner.jas.getInteger("splitPixelObjectsOverlap"));
                        PixelObject cutter = spobjects[1];
                        PixelObject pre = spobjects[0];
                        PixelObject post = spobjects[2];

                        if (owner.isShowDebugGui()) {
                            BasicWindow.showImage(cutter.toLetter().getImage(), "cutter " + resletter.getDecodedValue());
                        }

                        objectsret.add(cutter);
                        cutter.detected = resletter;

                        if (pre != null && pre.getArea() > minArea && pre.getSize() > owner.getJas().getInteger("minObjectSize")) {
                            objects.add(pre);
                            if (owner.isShowDebugGui()) {
                                BasicWindow.showImage(pre.toLetter().getImage(), "pre cut " + resletter.getDecodedValue());
                            }

                        }
                        if (post != null && post.getArea() > minArea && post.getSize() > owner.getJas().getInteger("minObjectSize")) {
                            objects.add(post);
                            if (owner.isShowDebugGui()) {
                                BasicWindow.showImage(post.toLetter().getImage(), "post cut " + resletter.getDecodedValue());
                            }

                        }
                    } else {
                        objectsret.add(current);
                        if (owner.isShowDebugGui()) {
                            BasicWindow.showImage(current.toLetter().getImage(), "normal ext " + resletter.getDecodedValue());
                        }

                    }

                    // owner.getJas().set("coverageFaktorAWeight", cfaw);
                    owner.getJas().set("borderVarianceX", bx);
                    owner.getJas().set("borderVarianceY", by);
                } else {
                    PixelObject current = pixelObject;
                    Letter letter = current.toLetter();
                    LetterComperator resletter = owner.getLetter(letter);
                    current.detected = resletter;

                    if (owner.isShowDebugGui()) {
                        BasicWindow.showImage(current.toLetter().getImage(), "normal " + resletter.getDecodedValue());
                    }

                    objectsret.add(current);

                }
            }
            objects = objectsret;

        }
        // if (owner.jas.getBoolean("rapidshareSpecial")) {
        // String methodsPath = Utilities.getFullPath(new String[] {
        // JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath(),
        // "jd",
        // "captcha", "methods" });
        // String hoster = "rscat.com";
        // JAntiCaptcha jac = new JAntiCaptcha(methodsPath, hoster);
        // ListIterator<PixelObject> iter =
        // objects.listIterator(objects.size());
        // while (iter.hasPrevious() && objects.size() > letterNum) {
        // PixelObject pixelObject = (PixelObject) iter.previous();
        // if (pixelObject.getArea() > minArea) {
        // if (rapidshareSpecial(pixelObject, jac)) iter.remove();
        // } else
        // iter.remove();
        //
        // }
        // }

        mergeMultiPartObjects(objects);
        if (owner.jas.getBoolean("autoLetterNum")) {
            Iterator<PixelObject> iterr = objects.iterator();
            int r = 0;
            while (iterr.hasNext()) {
                PixelObject pixelObject = iterr.next();
                if (pixelObject.getArea() > minArea) {
                    r++;

                }
            }
            owner.setLetterNum(r);
            letterNum = r;
        }
        boolean perfectObjectDetection = true;
        // Kleine Objekte ausfiltern
        /*
         * String removeObjectsContainingImage = "dog.png"; if
         * (removeObjectsContainingImage != null && objects.size() > letterNum)
         * { Captcha remImage =
         * owner.createCaptcha(Utilities.loadImage(owner.getResourceFile
         * (removeObjectsContainingImage))); ArrayList<Integer[]> blackPoints =
         * new ArrayList<Integer[]>(); int avg = getAverage(); for (int y = 0; y
         * < remImage.getHeight(); y++) { for (int x = 0; x <
         * remImage.getWidth(); x++) { if (isElement(remImage.getPixelValue(x,
         * y), avg)) { blackPoints.add(new Integer[] { x, y }); } } }
         * ListIterator<PixelObject> iter =
         * objects.listIterator(objects.size()); while (iter.hasPrevious() &&
         * objects.size() > letterNum) { PixelObject pixelObject = (PixelObject)
         * iter.previous(); if (objectContainCaptcha(pixelObject, remImage,
         * blackPoints)) {
         * 
         * iter.remove(); } } }
         */
        //
        // for(PixelObject oo:objects){
        // int hh = objects.elementAt(i++).getArea();
        // hh=hh;
        // }
        while (i < objects.size() && objects.elementAt(i++).getArea() > minArea && found < letterNum) {
            if (Utilities.isLoggerActive()) {
                logger.info(objects.elementAt(i - 1).getWidth() + " Element: " + found + " : " + objects.elementAt(i - 1).getArea());
            }
            found++;
        }
        if (!owner.jas.getBoolean("autoLetterNum")) {
            Vector<PixelObject> splitObjects;
            if (Utilities.isLoggerActive()) {
                logger.fine("found " + found + " minArea: " + minArea);
                // Teil die größten Objekte bis man die richtige anzahl an
                // lettern
                // hat
                //
                // for(Iterator<PixelObject> it =
                // objects.iterator();it.hasNext();){
                //
                // BasicWindow.showImage(it.next().toLetter().getImage(2));
                // }
            }

            while (objects.size() > 0 && found < letterNum) {
                PixelObject po = objects.remove(0);
                PixelObject next = null;
                if (objects.size() > 0) {
                    next = objects.elementAt(0);
                }
                found--;
                maxWidth = po.getWidth();

                minWidth = minArea / po.getHeight();
                if (owner.getJas().getInteger("minimumLetterWidth") > 0 && owner.getJas().getInteger("minimumLetterWidth") > minWidth) {
                    minWidth = owner.getJas().getInteger("minimumLetterWidth");
                }
                splitter = 1;

                if (Utilities.isLoggerActive()) {
                    logger.info(maxWidth + "/" + minWidth);
                }
                // ermittle die Vermutliche Buchstabenanzahl im Ersten captcha
                while ((splitNum = Math.min((int) Math.ceil(maxWidth / ((double) minWidth / (double) splitter)), letterNum - found)) < 2) {
                    splitter++;
                }
                if (Utilities.isLoggerActive()) {
                    logger.info("l " + splitNum);
                }
                while (found + splitNum > letterNum) {
                    splitNum--;
                }
                if (Utilities.isLoggerActive()) {
                    logger.info("l " + splitNum);
                }
                while (splitNum > 2 && next != null && maxWidth / splitNum < next.getWidth() * 0.55) {
                    splitNum--;
                }
                if (Utilities.isLoggerActive()) {
                    logger.finer("teile erstes element " + po.getWidth() + " : splitnum " + splitNum);
                }

                if (found + splitNum - 1 > letterNum || splitNum < 2) {
                    if (Utilities.isLoggerActive()) {
                        logger.severe("Richtige Letteranzahl 1 konnte nicht ermittelt werden");
                    }
                    return null;
                }

                // found += splitNum - 1;

                splitObjects = po.split(splitNum, owner.jas.getInteger("splitPixelObjectsOverlap"));
                if (Utilities.isLoggerActive()) {
                    logger.finer("Got splited: " + splitObjects.size());
                    // Füge die geteilen Objekte wieder dem Objektvektor hinzu.
                    // Eventl
                    // müssen sie nochmals geteil werden.
                }

                for (int t = 0; t < splitNum; t++) {

                    for (int s = 0; s < objects.size(); s++) {
                        if (splitObjects.elementAt(t).getArea() > objects.elementAt(s).getArea()) {
                            objects.add(s, splitObjects.elementAt(t));
                            splitObjects.setElementAt(null, t);
                            found++;
                            perfectObjectDetection = false;
                            if (Utilities.isLoggerActive()) {
                                logger.finer("add split " + found);
                            }

                            break;
                        }

                    }
                    if (splitObjects.elementAt(t) != null) {
                        objects.add(splitObjects.elementAt(t));
                        splitObjects.setElementAt(null, t);
                        found++;
                        perfectObjectDetection = false;
                        if (Utilities.isLoggerActive()) {
                            logger.finer("add split " + found);
                        }
                    }

                }
                if (Utilities.isLoggerActive()) {
                    logger.finer("splitted ... treffer: " + found);
                }

            }

            if (found != letterNum && !owner.jas.getBoolean("autoLetterNum")) {
                perfectObjectDetection = false;
                if (Utilities.isLoggerActive()) {
                    logger.severe("Richtige Letteranzahl 2 konnte nicht ermittelt werden");
                }
                return null;
            }
            // entfernt Überflüssige Objekte und
            for (int ii = objects.size() - 1; ii >= found; ii--) {
                objects.remove(ii);
            }
            setPerfectObjectDetection(perfectObjectDetection);
        }
        // Sortiert die Objekte nun endlich in der richtigen Reihenfolge (von
        // link nach rechts)
        Collections.sort(objects);
        if (Utilities.isLoggerActive()) {
            logger.finer("Found " + objects.size() + " Elements");
        }

        return objects;
    }

    public File getCaptchaFile() {
        return captchafile;
    }

    private Letter[] getColoredLetters(int letterNum) {
        Vector<PixelObject> objects = getColorObjects(letterNum);
        Letter[] letters = new Letter[objects.size()];
        Iterator<PixelObject> iter = objects.iterator();
        int i = 0;
        while (iter.hasNext()) {
            PixelObject pixelObject = iter.next();

            letters[i] = pixelObject.toLetter();
            letters[i].toBlackAndWhite();
            letters[i].removeSmallObjects(0.6, 0.5, 4);
            letters[i].clean();
            i++;
        }
        for (i = 0; i < letters.length; i++) {
            owner.getJas().executeLetterPrepareCommands(letters[i]);
            letters[i] = letters[i].getSimplified(owner.getJas().getDouble("simplifyFaktor"));

        }
        return letters;
    }

    public String getCorrectCaptchaCode() {
        return correctCaptchaCode;
    }

    /**
     * Gibt ein pixelgrid zurück. Inkl.trennstrichen für die Gaps
     * 
     * @return int[][] PixelGrid
     */
    public int[][] getGridWithGaps() {
        int[][] pix = new int[getWidth()][getHeight()];
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                pix[x][y] = grid[x][y];
                if (gaps != null && gaps.length + 1 > x) {
                    if (gaps[x] == true) {
                        pix[x][y] = 0;
                    }
                }

            }
        }
        return pix;
    }

    /**
     * Gibt die Höhe des internen captchagrids zurück
     * 
     * @return Höhe
     */
    @Override
    public int getHeight() {
        if (grid.length == 0) { return 0; }
        return grid[0].length;
    }

    /**
     * Gibt den Captcha als Bild mit Trennzeilen für die gaps zurück
     * 
     * @param faktor
     *            Vergrößerung
     * @return neues Image
     */

    public Image getImageWithGaps(int faktor) {

        Image image;

        int[][] tmp = grid;
        grid = getGridWithGaps();
        image = getImage(faktor);
        grid = tmp;
        return image;

    }

    /**
     * Gibt die Ermittelten lettercomperators zurück
     * 
     * @return letterComperators
     */
    public LetterComperator[] getLetterComperators() {
        return letterComperators;

    }

    /**
     * Versucht die Buchstaben aus dem captcha zu extrahieren und gibt ein
     * letter-array zuück
     * 
     * @param letterNum
     *            Anzahl der vermuteten Buchstaben
     * @return Array mit den gefundenen Lettern
     */
    public Letter[] getLetters(int letterNum) {
        if (seperatedLetters != null) { return seperatedLetters; }
        Letter[] ret = getLetters0(letterNum);
        if (ret == null) return null;
        if (owner.getJas().getString("useLetterFilter") != null && owner.getJas().getString("useLetterFilter").length() > 0) {
            String[] ref = owner.getJas().getString("useLetterFilter").split("\\.");
            if (ref.length != 2) {
                if (Utilities.isLoggerActive()) {
                    logger.severe("useLetterFilter should have the format Class.Method");
                }
                return null;
            }
            String cl = ref[0];
            String methodname = ref[1];

            Class<?> newClass;
            try {
                newClass = Class.forName("jd.captcha.specials." + cl);

                Class<?>[] parameterTypes = new Class[] { ret.getClass(), owner.getClass() };
                Method method = newClass.getMethod(methodname, parameterTypes);
                Object[] arguments = new Object[] { ret, owner };
                Object instance = null;
                Letter[] ret2 = (Letter[]) method.invoke(instance, arguments);
                if (ret2 != null) {
                    seperatedLetters = ret2;
                    return ret2;
                } else {
                    if (Utilities.isLoggerActive()) {
                        logger.severe("Special filter failed.");
                    }
                    seperatedLetters = ret;
                    return ret;
                }

            } catch (Exception e) {
                if (Utilities.isLoggerActive()) {
                    logger.severe("Fehler in useLetterFilter:" + e.getLocalizedMessage() + " / " + owner.getJas().getString("useSpecialGetLetters"));
                }
                JDLogger.exception(e);
            }
            seperatedLetters = ret;
            return ret;
        }
        seperatedLetters = ret;
        return ret;
    }

    /**
     * Benutzt die Objekterkennung um alle Buchstaben zu finden
     * 
     * @param letterNum
     *            Anzahl der zu suchenen Buchstaben
     * @param contrast
     *            Kontrast innerhalb der Elemente
     * @param objectContrast
     *            Kontrast der Elemente zum Hintergrund
     * @param minArea
     *            MindestFläche eines Elements
     * @return Erkannte Buchstaben
     */
    public Letter[] getLetters(int letterNum, double contrast, double objectContrast, int minArea) {
        Vector<PixelObject> letters = getBiggestObjects(letterNum, minArea, contrast, objectContrast);
        if (letters == null) { return null; }
        gaps = new boolean[getWidth() + 1];
        ArrayList<Letter> ret = new ArrayList<Letter>();
        for (int i = 0; i < letters.size(); i++) {

            PixelObject obj = letters.elementAt(i);

            Letter l = obj.toLetter();
            // l.removeSmallObjects(owner.getJas().getDouble("ObjectColorContrast"
            // ),
            // owner.getJas().getDouble("ObjectDetectionContrast"));
            owner.getJas().executeLetterPrepareCommands(l);
            // if(owner.getJas().getInteger("leftAngle")!=0 ||
            // owner.getJas().getInteger("rightAngle")!=0) l =
            // l.align(owner.getJas().getDouble("ObjectDetectionContrast"),owner.
            // getJas
            // ().getInteger("leftAngle"),owner.getJas().getInteger("rightAngle"
            // ));
            // l.reduceWhiteNoise(2);
            // l.toBlackAndWhite(0.6);

            ret.add(l.getSimplified(owner.getJas().getDouble("simplifyFaktor")));

            gaps[letters.elementAt(i).getLocation()[0] + letters.elementAt(i).getWidth()] = true;
        }
        return ret.toArray(new Letter[ret.size()]);

    }

    /**
     * Gibt die Buchstaben zurück. Trennkriterium ist das Array gaps in dem die
     * Trenn-X-Wert abgelegt sind
     * 
     * @param letterNum
     * @param gaps
     * @return Erkannte Buchstaben
     */
    public Letter[] getLetters(int letterNum, int[] gaps) {

        if (seperatedLetters != null) { return seperatedLetters; }

        Letter[] ret = new Letter[letterNum];
        lastletterX = 0;
        this.gaps = new boolean[getWidth() + 1];
        for (int letterId = 0; letterId < letterNum; letterId++) {
            ret[letterId] = getNextLetter(letterId, gaps);

            if (ret[letterId] == null) {

                return null;
                // ret[letterId]= ret[letterId].getSimplified(SIMPLIFYFAKTOR);
            } else {
                // if(owner.getJas().getInteger("leftAngle")!=0 ||
                // owner.getJas().getInteger("rightAngle")!=0) ret[letterId] =
                // ret[letterId].align(
                // owner.getJas().getDouble("ObjectDetectionContrast"),owner.
                // getJas().getInteger("leftAngle"),owner.getJas().getInteger(
                // "rightAngle"));
                owner.getJas().executeLetterPrepareCommands(ret[letterId]);

                ret[letterId] = ret[letterId].getSimplified(owner.getJas().getDouble("simplifyFaktor"));

            }

        }
        seperatedLetters = ret;
        return ret;
    }

    public Letter[] getLetters0(int letterNum, int[] gaps) {

        if (seperatedLetters != null) { return seperatedLetters; }

        Letter[] ret = new Letter[letterNum];
        lastletterX = 0;
        this.gaps = new boolean[getWidth() + 1];
        for (int letterId = 0; letterId < letterNum; letterId++) {
            ret[letterId] = getNextLetter(letterId, gaps);

            if (ret[letterId] != null) {

                owner.getJas().executeLetterPrepareCommands(ret[letterId]);

                ret[letterId] = ret[letterId].getSimplified(owner.getJas().getDouble("simplifyFaktor"));

            }

        }
        seperatedLetters = ret;
        return ret;
    }

    public Letter[] getLetters0(int letterNum) {

        if (letterNum == 1) {
            Letter ret = createLetter();
            ret.setGrid(getGrid());
            logger.info("Letternum=1 --> captch to Letter");
            return new Letter[] { ret };
        }
        if (owner.getJas().getString("useSpecialGetLetters") != null && owner.getJas().getString("useSpecialGetLetters").length() > 0) {
            String[] ref = owner.getJas().getString("useSpecialGetLetters").split("\\.");
            if (ref.length != 2) {
                if (Utilities.isLoggerActive()) {
                    logger.severe("useSpecialGetLetters should have the format Class.Method");
                }
                return null;
            }
            String cl = ref[0];
            String methodname = ref[1];

            Class<?> newClass;
            try {
                logger.severe("Special detection :" + "jd.captcha.specials." + cl);
                newClass = Class.forName("jd.captcha.specials." + cl);

                Class<?>[] parameterTypes = new Class[] { this.getClass() };
                Method method = newClass.getMethod(methodname, parameterTypes);
                Object[] arguments = new Object[] { this };
                Object instance = null;
                Letter[] ret = (Letter[]) method.invoke(instance, arguments);
                if (ret != null) {

                    return ret;
                } else {
                    if (Utilities.isLoggerActive()) {
                        logger.severe("Special detection failed.");
                    }
                    return null;
                }

            } catch (Exception e) {
                if (Utilities.isLoggerActive()) {
                    logger.severe("Fehler in useSpecialGetLetters:" + e.getLocalizedMessage() + " / " + owner.getJas().getString("useSpecialGetLetters"));
                }
                JDLogger.exception(e);
            }
            return null;
        }
        if (owner.getJas().getBoolean("useColorObjectDetection")) {
            if (Utilities.isLoggerActive()) {
                logger.finer("Use Color Object Detection");
            }
            Letter[] ret = getColoredLetters(letterNum);
            if (ret != null) {

                return ret;
            } else {
                if (Utilities.isLoggerActive()) {
                    logger.severe("Color Object detection failed. Try alternative Methods");
                }
            }
        }

        if (owner.getJas().getBoolean("useObjectDetection")) {
            if (Utilities.isLoggerActive()) {
                logger.finer("Use Object Detection");
            }
            Letter[] ret = this.getLetters(letterNum, owner.getJas().getDouble("ObjectColorContrast"), owner.getJas().getDouble("ObjectDetectionContrast"), owner.getJas().getInteger("MinimumObjectArea"));
            if (ret != null) {

                return ret;
            } else {
                if (Utilities.isLoggerActive()) {
                    logger.severe("Object detection failed. Try alternative Methods");
                }
            }
        }
        if (owner.getJas().getBoolean("cancelIfObjectDetectionFailed")) { return null; }
        if (!owner.getJas().getBoolean("UseAverageGapDetection") && !owner.getJas().getBoolean("UsePeakGapdetection") && owner.getJas().getGaps() != null) {

            if (Utilities.isLoggerActive()) {
                logger.finer("Use predefined Gaps");
            }
            return getLetters(letterNum, owner.getJas().getGaps());
        }
        if (Utilities.isLoggerActive()) {
            logger.finer("Use Line Detection");
        }
        gaps = new boolean[getWidth() + 1];
        Letter[] ret = new Letter[letterNum];
        lastletterX = 0;
        int letters = 0;
        for (int letterId = 0; letterId < letterNum; letterId++) {
            ret[letterId] = getNextLetter();

            if (ret[letterId] == null && !owner.jas.getBoolean("autoLetterNum")) {
                if (owner.getJas().getGaps() != null) {
                    return getLetters(letterNum, owner.getJas().getGaps());
                } else {
                    return null;
                }
                // ret[letterId]= ret[letterId].getSimplified(SIMPLIFYFAKTOR);
            } else if (ret[letterId] != null) {
                letters++;
                owner.getJas().executeLetterPrepareCommands(ret[letterId]);
                // if(owner.getJas().getInteger("leftAngle")!=0 ||
                // owner.getJas().getInteger("rightAngle")!=0) ret[letterId] =
                // ret[letterId].align(
                // owner.getJas().getDouble("ObjectDetectionContrast"),owner.
                // getJas().getInteger("leftAngle"),owner.getJas().getInteger(
                // "rightAngle"));

                ret[letterId] = ret[letterId].getSimplified(owner.getJas().getDouble("simplifyFaktor"));

            }

        }

        if (owner.jas.getBoolean("autoLetterNum") && letters < ret.length) {
            Letter[] tmp = ret;

            ret = new Letter[letters];
            System.arraycopy(tmp, 0, ret, 0, letters);
        }

        return ret;
    }

    public void setContrast(float contrast) {
        setContrast(contrast, 0);
    }

    public void setContrast(float contrast, float brightness) {
        BufferedImage image = getImage();
        ContrastFilter cf = new ContrastFilter();
        cf.setContrast(contrast);
        if (brightness != 0) cf.setBrightness(brightness);
        BufferedImage dest = cf.createCompatibleDestImage(image, null);
        cf.filter(image, dest);
        Captcha cap2 = owner.createCaptcha(dest);
        grid = cap2.grid;
    }

    public void blur(int hRadius, int vRadius, int iteration) {
        BufferedImage image = getImage();
        BoxBlurFilter blur = new BoxBlurFilter(hRadius, vRadius, iteration);
        BufferedImage dest = blur.createCompatibleDestImage(image, null);
        blur.filter(image, dest);
        Captcha cap2 = owner.createCaptcha(dest);
        grid = cap2.grid;
    }

    /**
     * reduziert ein bild auf eine gewisse farbanzahl
     * 
     * @param colorNums
     */
    public void reduceColors(int colorNums) {
        BufferedImage image = getImage();
        QuantizeFilter reduceFilter = new QuantizeFilter();
        BufferedImage dest = reduceFilter.createCompatibleDestImage(image, null);
        reduceFilter.setNumColors(colorNums);
        reduceFilter.filter(image, dest);
        Captcha cap2 = owner.createCaptcha(dest);
        grid = cap2.grid;
    }

    /**
     * reduziert ein bild auf eine gewisse farbanzahl
     * 
     * @param colorNums
     */
    public void reduceColorsPosterizeFilter(int numLevels) {
        BufferedImage image = getImage();
        PosterizeFilter reduceFilter = new PosterizeFilter();
        BufferedImage dest = reduceFilter.createCompatibleDestImage(image, null);
        reduceFilter.setNumLevels(numLevels);
        reduceFilter.filter(image, dest);
        Captcha cap2 = owner.createCaptcha(dest);
        grid = cap2.grid;
    }

    /**
     * Sucht angefangen bei der aktullen Positiond en ncähsten letter und gibt
     * ihn zurück
     * 
     * @param letterId
     *            Id des Letters (0-letterNum-1)
     * @return Letter gefundener Letter
     */
    private Letter getNextLetter() {
        Letter ret = createLetter();

        int[][] letterGrid = new int[getWidth()][getHeight()];
        int[] rowAverage = new int[getWidth()];
        int[] rowPeak = new int[getWidth()];
        for (int i = 0; i < rowAverage.length; i++) {
            rowAverage[i] = 0;
            rowPeak[i] = Integer.MAX_VALUE;
        }

        int average = getAverage();

        int x;
        int noGapCount = 0;

        boolean lastOverPeak = false;

        for (x = lastletterX; x < getWidth(); x++) {
            int count = 0;
            for (int y = 0; y < getHeight(); y++) {
                int pixelValue;

                for (int line = 0; line < owner.getJas().getInteger("GapWidthPeak"); line++) {
                    if (getWidth() > x + line) {
                        pixelValue = getPixelValue(x + line, y);
                        if (pixelValue < rowPeak[x]) {
                            rowPeak[x] = pixelValue;
                        }
                    }
                }

                for (int line = 0; line < owner.getJas().getInteger("GapWidthAverage"); line++) {
                    if (getWidth() > x + line) {
                        rowAverage[x] = Colors.mixColors(rowAverage[x], getPixelValue(x + line, y), count, 1);
                        count++;
                    }
                }

                letterGrid[x][y] = getPixelValue(x, y);
            }

            boolean isGap = false;
            boolean isAverageGap;
            boolean isOverPeak;
            boolean isPeakGap;
            if (owner.getJas().getBoolean("GapAndAverageLogic")) {
                isAverageGap = rowAverage[x] > average * owner.getJas().getDouble("GapDetectionAverageContrast") || !owner.getJas().getBoolean("UseAverageGapDetection");

                isOverPeak = rowPeak[x] < average * owner.getJas().getDouble("GapDetectionPeakContrast");
                isPeakGap = lastOverPeak && !isOverPeak || !owner.getJas().getBoolean("UsePeakGapdetection");

                isGap = isAverageGap && isPeakGap;

            } else {
                isAverageGap = rowAverage[x] > average * owner.getJas().getDouble("GapDetectionAverageContrast") && owner.getJas().getBoolean("UseAverageGapDetection");
                isOverPeak = rowPeak[x] < average * owner.getJas().getDouble("GapDetectionPeakContrast");
                isPeakGap = lastOverPeak && !isOverPeak || !owner.getJas().getBoolean("UsePeakGapdetection");
                isGap = isAverageGap || isPeakGap;
            }
            lastOverPeak = isOverPeak;

            if (isGap && noGapCount > owner.getJas().getInteger("MinimumLetterWidth")) {
                break;
            } else if (rowAverage[x] < average * owner.getJas().getDouble("GapDetectionAverageContrast")) {

                noGapCount++;
            }

        }

        ret.setGrid(letterGrid);
        ret.setLocation(new int[] { 0, 0 });
        if (!ret.trim(lastletterX, x)) { return null; }

        if (!ret.clean()) { return null; }

        lastletterX = x;

        gaps[Math.min(lastletterX, getWidth())] = true;
        return ret;
    }

    /**
     * Alternativ Methode über das gaps array. TODO: Nicht optimal. Das trim()
     * kann man sich sparen indem man gleich die rihtige Arraygröße wählt
     * 
     * @param letterId
     * @param gaps
     * @return Nächster Buchstabe in der Reihe
     */
    private Letter getNextLetter(int letterId, int[] gaps) {
        Letter ret = createLetter();
        int overlap = owner.jas.getInteger("splitGapsOverlap");
        int nextGap = -1;
        if (gaps != null && gaps.length > letterId) {
            nextGap = gaps[letterId];
        }

        if (gaps == null || gaps.length == 0) {
            if (Utilities.isLoggerActive()) {
                logger.severe("Das Gaps Array wurde nicht erstellt");
            }
        }
        if (gaps != null && letterId > gaps.length - 1) {
            if (Utilities.isLoggerActive()) {
                logger.severe("LetterNum und Gaps Array passen nicht zusammen. Siemüssen die selbe Länge haben!");
            }
        }
        if (letterId > 0 && nextGap <= gaps[letterId - 1]) {
            if (Utilities.isLoggerActive()) {
                logger.severe(letterId + " Das Userdefinierte gaps array ist falsch!. Die Gaps müssen aufsteigend sortiert sein!");
            }
        }
        int[][] letterGrid = new int[Math.min(getWidth() - 1, nextGap + overlap) - Math.max(0, lastletterX - overlap)][getHeight()];
        int x;
        if (Utilities.isLoggerActive()) {
            logger.info("Gap at " + nextGap + " last gap: " + lastletterX + " this: " + Math.max(0, lastletterX - overlap) + " - " + Math.min(getWidth() - 1, nextGap + overlap));
        }
        ret.setLocation(new int[] { Math.max(0, lastletterX - overlap), 0 });
        for (x = Math.max(0, lastletterX - overlap); x < Math.min(getWidth() - 1, nextGap + overlap); x++) {
            for (int y = 0; y < getHeight(); y++) {
                letterGrid[x - Math.max(0, lastletterX - overlap)][y] = getPixelValue(x, y);
            }

        }

        ret.setGrid(letterGrid);
        //
        // if (!ret.trim(Math.max(0,lastletterX-overlap), x)){
        // return null;
        // }

        if (!ret.clean()) { return null; }

        lastletterX = x - overlap;

        this.gaps[Math.min(lastletterX, getWidth())] = true;
        return ret;
    }

    /**
     * Gibt das Pixel Array zurück.
     * 
     * @return Pixel Array
     */
    @Override
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
     * Gibt ein pixelarray zurück. In das Pixelarray sind Trennstriche für die
     * Gaps eingerechnet
     * 
     * @return Pixelarray
     */
    public int[] getPixelWithGaps() {
        int[] pix = new int[getWidth() * getHeight()];
        int pixel = 0;

        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                pix[pixel] = getPixelValue(x, y);
                if (gaps[x] == true) {
                    pix[pixel] = 0;
                }
                pixel++;
            }
        }
        return pix;
    }

    /**
     * Gibt einen vereifgachten captcha zurück. /gröber
     * 
     * @param faktor
     *            der vereinfachung
     * @return neuer captcha
     */
    public Captcha getSimplified(double faktor) {
        int newWidth = (int) Math.ceil(getWidth() / faktor);
        int newHeight = (int) Math.ceil(getHeight() / faktor);
        Captcha ret = new Captcha(newWidth, newHeight);
        int[][] newGrid = new int[newWidth][newHeight];
        int avg = getAverage();

        if (faktor == 1.0 || faktor == 0.0) { return this; }

        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                PixelGrid.setPixelValue(x, y, newGrid, getMaxPixelValue());
            }
        }
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {

                if (isElement(getPixelValue(x, y), avg)) {
                    int newX = (int) Math.round(x / faktor);
                    int newY = (int) Math.round(y / faktor);
                    PixelGrid.setPixelValue(newX, newY, newGrid, 0);

                }

            }
        }

        ret.setGrid(newGrid);

        ret.printGrid();

        return ret;
    }

    /**
     * Gibt einen ASCII String des Bildes zurück
     * 
     * @return ASCII Bild
     */
    @Override
    public String getString() {
        int avg = getAverage();
        StringBuilder ret = new StringBuilder();
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                if (isElement(getPixelValue(x, y), avg))
                    ret.append("*");
                else
                    ret.append((int) Math.floor(9 * (getPixelValue(x, y) / getMaxPixelValue())));

            }
            ret.append(new char[] { '\r', '\n' });
        }

        return ret.toString();

    }

    /**
     * Gibt in prozent zurück wie sicher die erkennung war (0. top sicher 100
     * schlecht)
     * 
     * @return int validprozent
     */
    public double getValityPercent() {
        return valityPercent;
    }

    /**
     * Gibt die Breite des internen captchagrids zurück
     * 
     * @return breite
     */
    @Override
    public int getWidth() {
        return grid.length;
    }

    public boolean isPerfectObjectDetection() {
        return perfectObjectDetection;
    }

    /**
     * @return the prepared
     */
    public boolean isPrepared() {
        return prepared;
    }

    private void mergeMultiPartObjects(Vector<PixelObject> objects) {
        if (owner.getJas().getInteger("multiplePartMergeMinSize") <= 0) { return; }

        for (int i = objects.size() - 1; i >= 0; i--) {
            PixelObject current = objects.get(i);
            if (current.getSize() < owner.getJas().getInteger("multiplePartMergeMinSize")) {
                continue;
            }
            int xMin = current.getXMin();
            int xMax = current.getXMin() + current.getWidth();
            int yMin = current.getYMin();
            int yMax = current.getYMin() + current.getHeight();
            for (int ii = i - 1; ii >= 0; ii--) {
                PixelObject tmp = objects.get(ii);

                if (xMin >= tmp.getXMin() && xMax <= tmp.getXMin() + tmp.getWidth() && yMin >= tmp.getYMin() && yMax <= tmp.getYMin() + tmp.getHeight()) {
                    logger.info("current liegt mitten in tmp.. merge");

                    tmp.add(current);
                    objects.remove(i);
                    break;
                }
            }

        }

    }

    /**
     * Holt die größten Objekte aus dem captcha
     * 
     * @param letterNum
     *            anzahl der zu suchenden objekte
     * @param minArea
     *            mindestFläche der Objekte
     * @param contrast
     *            Kontrast zur erkennung der farbunterschiede (z.B. 0.3)
     * @param objectContrast
     *            Kontrast zur erkennung einens objektstartpunkte (z.B. 0.5 bei
     *            weißem Hintergrund)
     * @return Vector mit den gefundenen Objekten
     */
    public boolean objectContainCaptcha(PixelObject pixelObject, Captcha captcha, ArrayList<Integer[]> blackPoints) {

        // logger.info(mask.getWidth()+"/"+mask.getHeight()+" - "+getWidth()+" -
        // "+getHeight());
        if (captcha.getWidth() > pixelObject.getWidth() || captcha.getHeight() > pixelObject.getHeight()) { return true; }
        int avg = getAverage();
        int size = blackPoints.size();
        for (int y = 0; y < pixelObject.getHeight(); y++) {
            for (int x = 0; x < pixelObject.getWidth(); x++) {
                int right = 0;
                int bad = 0;
                Iterator<Integer[]> bpiter = blackPoints.iterator();
                while (bpiter.hasNext()) {
                    if (bad > size / 3 || right + size / 10 < bad) {
                        break;
                    }

                    try {

                        Integer[] integers = bpiter.next();
                        if (pixelObject.getWidth() > x + integers[0] && pixelObject.getHeight() > y + integers[1]) {
                            if (isElement(getPixelValue(x + integers[0], y + integers[1]), avg)) {
                                right++;
                            } else {
                                bad++;
                            }
                        }
                    } catch (Exception e) {
                    }
                }
                if (right > 200) return true;

            }
        }
        // BasicWindow.showImage(this.getImage());
        return false;
    }

    /**
     * Gibt ein ACSI bild des Captchas aus
     */
    public void printCaptcha() {
        if (Utilities.isLoggerActive()) {
            logger.info("\r\n" + getString());
        }
    }

    public int[][] getOrgGridCopy() {
        return PixelGrid.getGridCopy(tmpGrid);
    }

    /**
     * Setztd as interne Grid auf den ausgangszustand zurück. Funktioniert nur
     * wenn dieser gespeichert ist. Im fehlerfall wird fals zurückgegegen
     * 
     * @return
     */
    public boolean reset() {
        seperatedLetters = null;
        setPrepared(false);
        if (tmpGrid == null) return false;
        grid = PixelGrid.getGridCopy(tmpGrid);

        return true;
    }

    /**
     * Speichert den captcha als Bild mit Trennstrichen ab
     * 
     * @param file
     *            . ZielPfad
     */
    public void saveImageasJpgWithGaps(File file) {
        BufferedImage bimg = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        bimg.setRGB(0, 0, getWidth(), getHeight(), getPixelWithGaps(), 0, getWidth());

        try {
            FileOutputStream fos = new FileOutputStream(file);

            JPEGImageEncoder jpeg = JPEGCodec.createJPEGEncoder(fos);
            jpeg.encode(bimg);
            fos.close();
        } catch (Exception e) {
            JDLogger.exception(e);
        }
    }

    public void setCaptchaFile(File captchafile) {
        this.captchafile = captchafile;
    }

    /**
     * Setzt das verwendete Colormodel
     * 
     * @param colorModel
     */
    public void setColorModel(ColorModel colorModel) {
        this.colorModel = colorModel;
    }

    public void setCorrectcaptchaCode(String trim) {
        correctCaptchaCode = trim;
    }

    /**
     * Setzte die ermittelten LetterComperators
     * 
     * @param newLetters
     */
    public void setLetterComperators(LetterComperator[] newLetters) {
        letterComperators = newLetters;
    }

    public void setPerfectObjectDetection(boolean perfectObjectDetection) {
        this.perfectObjectDetection = perfectObjectDetection;
    }

    /**
     * Setzt Pixel als byte[] (z.B. aus einem Gif
     * 
     * @param bpixel
     */
    public void setPixel(byte[] bpixel) {
        pixel = new int[bpixel.length];
        int i = 0;
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                // grid[x][y] = pixel[i++];
                try {
                    int pb = ((IndexColorModel) colorModel).getPixelSize();
                    int maskbits = pb;
                    if (maskbits == 3) {
                        maskbits = 4;
                    } else if (maskbits > 4 && maskbits < 8) {
                        maskbits = 8;
                    }
                    int pixel_mask = (1 << maskbits) - 1;

                    pixel[i] = ((IndexColorModel) colorModel).getRGB(bpixel[i] & pixel_mask);
                } catch (Exception e) {
                    JDLogger.exception(e);
                    pixel[i] = 0;
                }

                Color c = new Color(pixel[i]);
                float[] col = new float[4];

                if (owner.getJas().getColorFormat() == 0) {

                    Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), col);
                    col[3] = 0;

                } else if (owner.getJas().getColorFormat() == 1) {

                    col[0] = (float) c.getRed() / 255;
                    col[1] = (float) c.getGreen() / 255;
                    col[2] = (float) c.getBlue() / 255;
                    col[3] = 0;
                }

                grid[x][y] = (int) (col[owner.getJas().getColorComponent(0)] * 255) * 65536 + (int) (col[owner.getJas().getColorComponent(1)] * 255) * 256 + (int) (col[owner.getJas().getColorComponent(2)] * 255);
                rgbGrid[x][y] = pixel[i];
                i++;
                // return dd;

            }
        }

    }

    /**
     * Nimmt ein int-array auf und wandelt es in das interne Grid um
     * 
     * @param pixel
     */
    @Override
    public void setPixel(int[] pixel) {
        this.pixel = pixel;
        int i = 0;
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                // grid[x][y] = pixel[i++];

                Color c = new Color(pixel[i]);
                float[] col = new float[4];

                if (owner.getJas().getColorFormat() == 0) {

                    Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), col);
                    col[3] = 0;

                } else if (owner.getJas().getColorFormat() == 1) {

                    col[0] = (float) c.getRed() / 255;
                    col[1] = (float) c.getGreen() / 255;
                    col[2] = (float) c.getBlue() / 255;
                    col[3] = 0;
                }

                grid[x][y] = (int) (col[owner.getJas().getColorComponent(0)] * 255) * 65536 + (int) (col[owner.getJas().getColorComponent(1)] * 255) * 256 + (int) (col[owner.getJas().getColorComponent(2)] * 255);
                rgbGrid[x][y] = pixel[i];
                i++;
                // return dd;

            }
        }

    }

    /**
     * @param prepared
     *            the prepared to set
     */
    public void setPrepared(boolean prepared) {
        this.prepared = prepared;
    }

    /**
     * Setztd en valityPercent Wert
     * 
     * @param d
     */
    public void setValityPercent(double d) {
        valityPercent = d;

    }

    /**
     * Entfernt bildbereiche deren Sättigung größe bzw kleiner der tollerance
     * ist
     * 
     * @param mode
     * @param tollerance
     */
    public void cleanBySaturation(int mode, int tollerance) {
        int[][] newgrid = new int[getWidth()][getHeight()];

        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {

                int p = getPixelValue(x, y);

                Color c = new Color(p);
                int min = Math.min(c.getRed(), Math.min(c.getGreen(), c.getBlue()));
                int max = Math.max(c.getRed(), Math.max(c.getGreen(), c.getBlue()));
                int v = 0;
                if (max > 0) {
                    v = (max - min) * 100 / max;
                }
                if (mode == 1) {
                    if (v > tollerance) {
                        PixelGrid.setPixelValue(x, y, newgrid, getMaxPixelValue());
                    } else {
                        newgrid[x][y] = grid[x][y];
                    }

                } else {
                    if (v < tollerance) {
                        PixelGrid.setPixelValue(x, y, newgrid, getMaxPixelValue());
                    } else {
                        newgrid[x][y] = grid[x][y];
                    }

                }

            }
        }
        grid = newgrid;

    }

    /**
     * Entfernt bildbereiche deren RGB distance größe bzw kleiner der tollerance
     * ist
     * 
     * @param mode
     * @param tollerance
     */
    public void cleanByRGBDistance(int mode, int tollerance) {
        int[][] newgrid = new int[getWidth()][getHeight()];

        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {

                int p = getPixelValue(x, y);

                Color c = new Color(p);
                int br = Math.abs(c.getBlue() - c.getRed());
                int bg = Math.abs(c.getBlue() - c.getGreen());
                int rg = Math.abs(c.getGreen() - c.getRed());

                int v = (br + bg + rg) / 3;

                if (mode == 1) {
                    if (v > tollerance) {
                        PixelGrid.setPixelValue(x, y, newgrid, getMaxPixelValue());
                    } else {
                        newgrid[x][y] = grid[x][y];
                    }
                } else {
                    if (v < tollerance) {
                        PixelGrid.setPixelValue(x, y, newgrid, getMaxPixelValue());
                    } else {
                        newgrid[x][y] = grid[x][y];
                    }
                }
            }
        }
        grid = newgrid;
    }

    public void addAt(int xx, int yy, Captcha tmp) {
        for (int x = 0; x < tmp.getWidth(); x++) {
            for (int y = 0; y < tmp.getHeight(); y++) {
                if (this.getWidth() > x + xx && this.getHeight() > y + yy) {
                    this.setPixelValue(x + xx, y + yy, tmp.getPixelValue(x, y));
                }
            }
        }
    }

}