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

package jd.captcha.pixelobject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.logging.Logger;

import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.utils.Utilities;
import jd.nutils.Colors;

/**
 * Diese Klasse ist wie die Letterklasse ein PixelContainer. Allerdings werden
 * nur Pixel mit INhalt aufgenommen und intern in einem vector abgelegt. Es
 * müssen nicht so viele Pixel verarbeitet werden. Dies Klasse eignet sich also
 * um Pbjekte abzulegen um diese dann zu drehen doer zu transformieren. Für die
 * Objektsuche eignet sich diese Klasse wegen des internen vectors besser
 * 
 * @author JD-Team
 * 
 */
public class PixelObject implements Comparable<PixelObject> {
    /**
     * Farbdurschnitt des Onkekts
     */
    private int avg = 0;

    /**
     * Anzahl der gleichbleibenden add Aufrufe bis ein durchschnitt as icher
     * angesehen wird
     */
    private int avgIsSaveNum = 10;
    private boolean bordered = true;
    private HashMap<Integer, HashMap<Integer, int[]>> grid;
    /**
     * key=color value=quantity
     */
    public ArrayList<PixelObjectColor> colors = new ArrayList<PixelObjectColor>();
    /**
     * Kontrastwert für die durchschnisserkennung
     */
    private double contrast;

    public LetterComperator detected = null;

    /**
     * Logger
     */
    public Logger logger = Utilities.getLogger();

    /**
     * Interne prüfvariable die hochgezählt wird wenn dieneuen Pixel keine
     * Durchschnissänderung hervorrufen
     */
    private int noAvgChanges = 0;

    /**
     * Interner Vector
     */
    private ArrayList<int[]> object;

    /**
     * captcha als owner. Über owner kann auf den Parameter Dump zugegriffen
     * werden
     */
    public PixelGrid owner;

    /**
     * Als sicher angenommener Farb durchschnitt
     */
    private int saveAvg = 0;

    private double whiteContrast = 1;

    /**
     * Maximaler X Wert
     */
    private int xMax = Integer.MIN_VALUE;

    /**
     * Minimaler x Wert
     */
    private int xMin = Integer.MAX_VALUE;

    /**
     * Maximaler Y Wert
     */
    private int yMax = Integer.MIN_VALUE;

    /**
     * Minimaler y Wert
     */
    private int yMin = Integer.MAX_VALUE;

    public int getMostcolor() {
        Collections.sort(colors);
        if (colors.size() > 0)
            return colors.get(0).color;
        else
            return getAverage();

    }

    /**
     * @param grid
     */
    public PixelObject(PixelGrid grid) {
        this.owner = grid;
        this.grid = new HashMap<Integer, HashMap<Integer, int[]>>();
        object = new ArrayList<int[]>();

    }

    /**
     * adds a color to the colorlist
     * 
     * @param color
     * @return false if the color exists in the list
     */
    public boolean addColor(int color) {
        PixelObjectColor poc = new PixelObjectColor(color);
        int io = colors.indexOf(poc);
        if (io == -1) {
            colors.add(poc);
            return true;
        } else {
            colors.get(io).count++;
        }
        return false;
    }

    /**
     * Fügt einen neuen Pixel bei x,y hinzu. mit Color wird die originalfarbe
     * des pixels übergeben.
     * 
     * @param x
     * @param y
     * @param color
     */
    public void add(int x, int y, int color) {
        int[] tmp = { x, y, color };
        int tmpAvg = avg;

        if (color >= 0) {
            avg = Colors.mixColors(avg, color, getSize(), 1);
            addColor(color);
        }
        HashMap<Integer, int[]> row = grid.get(x);
        if (row == null) grid.put(x, row = new HashMap<Integer, int[]>());
        row.put(y, new int[] { x, y, color });
        // if(JAntiCaptcha.isLoggerActive())logger.info(" AVG "+avg+"
        // ("+color+")");
        if (Math.abs(avg - tmpAvg) < owner.getMaxPixelValue() * contrast) {
            noAvgChanges++;
            if (avgIsSaveNum <= noAvgChanges && saveAvg == 0) {
                saveAvg = avg;
                // if(JAntiCaptcha.isLoggerActive())logger.info("saveAvg "+avg);

            }
        } else {
            noAvgChanges = 0;
        }

        object.add(tmp);
        xMin = Math.min(x, xMin);
        xMax = Math.max(x, xMax);
        yMin = Math.min(y, yMin);
        yMax = Math.max(y, yMax);

    }

    public void add(PixelObject current) {
        avg = Colors.mixColors(avg, current.getAverage(), getSize(), current.getSize());
        xMin = Math.min(current.xMin, xMin);
        xMax = Math.max(current.xMax, xMax);
        yMin = Math.min(current.yMin, yMin);
        yMax = Math.max(current.yMax, yMax);
        for (Entry<Integer, HashMap<Integer, int[]>> set : current.grid.entrySet()) {
            HashMap<Integer, int[]> row = grid.get(set.getKey());
            if (row == null)
                grid.put(set.getKey(), set.getValue());
            else
                row.putAll(set.getValue());
        }
        for (int i = 0; i < current.object.size(); i++) {
            int[] ob = current.object.get(i);
            object.add(new int[] { ob[0], ob[1], -1 });
        }

    }

    /**
     * delete a PixelObject from this pixelobject
     * 
     * @param PixelObject
     *            current
     */

    public void del(PixelObject current) {
        for (int i = 0; i < current.object.size(); i++) {
            int x = current.object.get(i)[0];
            int y = current.object.get(i)[1];
            for (Iterator<int[]> iterator = object.iterator(); iterator.hasNext();) {
                int[] o = iterator.next();
                if (o[0] == x && o[1] == y) {
                    iterator.remove();
                    break;
                }
            }
        }
        xMin = Integer.MAX_VALUE;
        xMax = Integer.MIN_VALUE;
        yMin = Integer.MAX_VALUE;
        yMax = Integer.MIN_VALUE;
        colors = new ArrayList<PixelObjectColor>();
        for (int[] o : object) {
            addColor(owner.getPixelValue(o[0], o[1]));
            xMin = Math.min(o[0], xMin);
            xMax = Math.max(o[0], xMax);
            yMin = Math.min(o[1], yMin);
            yMax = Math.max(o[1], yMax);
        }
    }

    public int getYMax() {

        return yMax;
    }

    public int getXMax() {

        return xMax;
    }

    public boolean isTouching(PixelObject b, boolean followX, int radiusX, int radiusY) {

        if (b.getXMin() > getXMax() + radiusX) return false;
        if (b.getXMax() < getXMin() - radiusY) return false;
        if (b.getYMin() > getYMax() + radiusX) return false;
        if (b.getYMax() < getYMin() - radiusY) return false;
        HashMap<Integer, int[]> row;

        for (int[] px : b.object) {

            for (int sx = -radiusX; sx <= radiusX; sx++) {
                row = grid.get(px[0] + sx);
                if (row == null) continue;
                for (int sy = -radiusY; sy <= radiusY; sy++) {
                    if (Math.abs(sx) == Math.abs(sy) && !followX) continue;
                    if (row.get(px[1] + sy) != null) return true;
                }

            }

        }
        return false;

    }

    /**
     * Schnelle aber ungenauere align Methode
     * 
     * @return Versucht das Objekt automatisch auszurichten
     */
    public PixelObject align() {
        int accuracy = 1;
        PixelObject r = turn(-accuracy);
        PixelObject l = turn(accuracy);

        int angle;
        // Utilities.trace(getWidthToHeight()+" : right:"+r.getWidthToHeight()+"
        // left:"+l.getWidthToHeight());
        if (r.getWidthToHeight() >= getWidthToHeight() && l.getWidthToHeight() >= getWidthToHeight()) { return this; }
        int steps = r.getWidthToHeight() < l.getWidthToHeight() ? -accuracy : accuracy;
        angle = steps * 2;
        PixelObject ret = r.getWidthToHeight() < l.getWidthToHeight() ? r : l;
        PixelObject next;
        while ((next = turn(angle)).getWidthToHeight() < ret.getWidthToHeight()) {
            // Utilities.trace("akt angle: "+angle+" wh:
            // "+next.getWidthToHeight());
            ret = next;

            angle += steps;
        }
        return ret;

    }

    /**
     * Gibt sucht von winkael A bis Winkel B die Beste DRehposition und gibt
     * diese zurück Langsammer, aber genauer
     * 
     * @param angleA
     * @param angleB
     * @return Ausgerichtetes PixelObjekt
     */
    public PixelObject align(int angleA, int angleB) {
        if (angleB < angleA) {
            int tmp = angleB;
            angleB = angleA;
            angleA = tmp;
        }
        int accuracy = owner.owner.getJas().getInteger("AlignAngleSteps");
        double bestValue = Double.MAX_VALUE;
        PixelObject res = null;
        PixelObject tmp;
        // Utilities.trace("ALIGN "+this.getWidthToHeight());
        for (int angle = angleA; angle < angleB; angle += accuracy) {

            tmp = turn(angle < 0 ? 360 + angle : angle);

            // Utilities.trace((angle<0?360+angle:angle)+" test
            // "+this.getWidthToHeight());
            if (tmp.getWidthToHeight() < bestValue) {
                bestValue = tmp.getWidthToHeight();
                res = tmp;
            }
        }

        return res;
    }

    public int compareTo(PixelObject po) {
        if (po.getLocation()[0] < getLocation()[0]) { return 1; }
        if (po.getLocation()[0] > getLocation()[0]) { return -1; }
        return 0;
    }

    public PixelObject[] cut(int x1, int x2, int overlap) {
        PixelObject pre = new PixelObject(owner);
        PixelObject post = new PixelObject(owner);
        PixelObject cutter = new PixelObject(owner);
        if (x1 < owner.owner.getJas().getInteger("minimumLetterWidth")) {
            pre = null;
        }
        if (xMax - xMin - x2 < owner.owner.getJas().getInteger("minimumLetterWidth")) {
            post = null;
        }
        for (int i = 0; i < getSize(); i++) {
            int[] akt = elementAt(i);
            if (akt[0] >= xMin + x1 - overlap && akt[0] <= xMin + x2 + overlap) {
                cutter.add(akt[0], akt[1], saveAvg);
            }
            // if (pre!=null&&akt[0] < xMin + x1 + overlap) {
            // pre.add(akt[0], akt[1], this.saveAvg);
            // }else if(pre==null&&akt[0] < xMin + x1 + overlap){
            // cutter.add(akt[0], akt[1], this.saveAvg);
            // }
            // if (post!=null&&akt[0] > (xMin + x2) - overlap) {
            // post.add(akt[0], akt[1], this.saveAvg);
            // }else if (post==null&&akt[0] > (xMin + x2) - overlap){
            // cutter.add(akt[0], akt[1], this.saveAvg);
            // }
            if (pre != null && akt[0] < xMin + x1) {
                pre.add(akt[0], akt[1], saveAvg);
            } else if (pre == null && akt[0] < xMin + x1) {
                cutter.add(akt[0], akt[1], saveAvg);
            }
            if (post != null && akt[0] > xMin + x2) {
                post.add(akt[0], akt[1], saveAvg);
            } else if (post == null && akt[0] > xMin + x2) {
                cutter.add(akt[0], akt[1], saveAvg);
            }

        }

        return new PixelObject[] { pre, cutter, post };
    }

    /**
     * 
     * @param color
     * @return Prüft ob die farbe color zum Objekt passt
     */
    public boolean doesColorAverageFit(int color) {
        if (getSize() > 50000) {
            if (Utilities.isLoggerActive()) {
                logger.severe("Objekt scheint sehr groß zu werden. objectColorContrast zu hoch?");
            }
            return false;
        }

        int tavg = saveAvg == 0 ? avg : saveAvg;
        // if(JAntiCaptcha.isLoggerActive())logger.info(tavg+"-"+color+" :
        // "+(int)Math.abs(tavg -
        // color)+"<"+(int)(owner.getMaxPixelValue() * this.contrast)+" =
        // "+(((int)Math.abs(tavg - color) < (int)(owner.getMaxPixelValue() *
        // this.contrast))));

        // Filtere zu helle Pixel aus
        if (color > (int) (whiteContrast * owner.getMaxPixelValue())) { return false; }
        if (getSize() == 0) { return true; }

        return Math.abs(tavg - color) < (int) (owner.getMaxPixelValue() * contrast);

    }

    /**
     * 
     * @param i
     * @return Gibt den pixel bei i zurück [x,y,color}
     */
    public int[] elementAt(int i) {
        return object.get(i);
    }

    /**
     * 
     * @return Gibt die Fläche des Objekts zurück
     */
    public int getArea() {
        return getWidth() * getHeight();
    }

    /**
     * 
     * @return Gibt farbe des Objekts zurück
     */
    public int getAverage() {
        return avg;
    }

    /**
     * Diese Funktion erstellt einen Vector<[x,y,color]> der nur Randelemente
     * des Buchstabens enthält.
     * 
     * @param letter
     * @return
     */
    public Vector<int[]> getBorderVector(Letter letter) {
        Vector<int[]> ret = new Vector<int[]>();
        if (letter == null) {
            letter = toLetter();
        }
        int[][] grid = letter.getGrid();

        for (int i = 0; i < getSize(); i++) {
            int[] akt = elementAt(i);
            int x = akt[0];
            int y = akt[1];

            int[][] map = letter.getLocalMap(grid, x, y);
            boolean c = false;
            for (int xx = 0; xx < 3; xx++) {
                for (int yy = 0; yy < 3; yy++) {
                    if (map[xx][yy] != 0) {
                        c = true;
                        break;
                    }
                }
            }
            if (c) {
                ret.add(akt);
            }
        }

        return ret;

    }

    public int getXDistanceTo(int x) {
        int mindist = Integer.MAX_VALUE;
        for (int[] akt : object) {
            int xd = Math.abs(x - akt[0]);
            mindist = Math.min(mindist, xd);
        }
        return mindist;
    }

    public int getDistanceTo(int x, int y) {
        int mindist = Integer.MAX_VALUE;

        for (int[] akt : object) {
            int xd = Math.abs(x - akt[0]);
            int yd = Math.abs(y - akt[1]);
            int dis = (int) Math.sqrt(xd * xd + yd * yd);
            mindist = Math.min(mindist, dis);
        }
        return mindist;
    }

    /**
     * Gibt die resulztierende Bildhöhe zurück
     * 
     * @return Höhe
     */
    public int getHeight() {
        return yMax - yMin + 1;
    }

    /**
     * 
     * @return Gibt die Position (Links oben) des Objekts im gesamtCaptcha an
     */
    public int[] getLocation() {

        int[] ret = { xMin, yMin };
        return ret;
    }

    /**
     * Gibt den Pixelmassenwert in x und y zurück. Dieser Wert zeigt die
     * Pixelkonzentration an diesem Punkt an.
     * 
     * @param x
     * @param y
     * @return
     */
    public long getMassValue(int x, int y) {
        long ret = 0;
        for (int i = 0; i < getSize(); i++) {
            int[] akt = elementAt(i);
            if (x == akt[0] && y == akt[1]) {
                ret += owner.getMaxPixelValue() * 2;
            } else {
                int col = owner.getMaxPixelValue() - akt[2];
                double dist = Math.sqrt(Math.abs(akt[0] - x) * Math.abs(akt[0] - x) + Math.abs(akt[1] - y) * Math.abs(akt[1] - y)) / 0.5;

                ret += col / (dist + 1);
            }
        }
        return ret;
    }

    /**
     * 
     * @return Anzahl der eingetragenen pixel
     */
    public int getSize() {
        return object.size();

    }

    /**
     * Gibt die resulitierende Bildbreite zurück
     * 
     * @return Breite
     */
    public int getWidth() {
        return xMax - xMin + 1;
    }

    /**
     * 
     * @return Gibt das Breite/Höhe Verjältniss zurück
     */
    public double getWidthToHeight() {
        return (double) getWidth() / (double) getHeight();
    }

    /**
     * @return the xMin
     */
    public int getXMin() {
        return xMin;
    }

    /**
     * @return the yMin
     */
    public int getYMin() {
        return yMin;
    }

    public boolean isBordered() {
        return bordered;
    }

    public void setBordered(boolean b) {
        bordered = b;

    }

    /**
     * Setzt den Kontrast für die fitColor funktion
     * 
     * @param contrast
     */
    public void setContrast(double contrast) {
        this.contrast = contrast;

    }

    /**
     * Setzte den WhiteKontrast
     * 
     * @param objectContrast
     */
    public void setWhiteContrast(double objectContrast) {
        whiteContrast = objectContrast;

    }

    /**
     * Teilt das Objekt in splitNum gleich große Objekte auf
     * 
     * @param splitNum
     * @return splitNum gleich große Objekte
     */
    public Vector<PixelObject> split(int splitNum) {
        return split(splitNum, 0);
    }

    /**
     * teil ein Pixelobject in splitNumteile. mit dem Overlap b
     * 
     * @param splitNum
     * @param overlap
     * @return PixelObjectvector
     */
    public Vector<PixelObject> split(int splitNum, int overlap) {

        Vector<PixelObject> ret = new Vector<PixelObject>();
        for (int t = 0; t < splitNum; t++) {
            ret.add(new PixelObject(owner));
        }
        int part = getWidth() / splitNum;
        for (int i = 0; i < getSize(); i++) {
            int[] akt = elementAt(i);
            for (int x = 0; x < splitNum; x++) {
                if (akt[0] >= xMin + x * part - overlap && akt[0] <= xMin + (x + 1) * part + overlap) {
                    ret.elementAt(x).add(akt[0], akt[1], saveAvg);

                }
            }

        }
        return ret;
    }

    public PixelObject[] horizintalSplitAt(int yposition) {
        PixelObject[] ret = new PixelObject[2];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = new PixelObject(owner);
        }
        for (int i = 0; i < getSize(); i++) {
            int[] akt = elementAt(i);
            boolean b = true;
            for (int y = 0; y < 2; y++) {
                if (akt[1] >= yMin + y * yposition && akt[1] <= yMin + (y + 1) * yposition) {
                    ret[y].add(akt[0], akt[1], saveAvg);
                    b = false;
                }
            }
            if (b) {
                ret[1].add(akt[0], akt[1], saveAvg);
            }

        }
        return ret;
    }

    public PixelObject[] splitAt(int position) {
        PixelObject[] ret = new PixelObject[2];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = new PixelObject(owner);
        }
        for (int i = 0; i < getSize(); i++) {
            int[] akt = elementAt(i);
            boolean b = true;
            for (int x = 0; x < 2; x++) {
                if (akt[0] >= xMin + x * position && akt[0] <= xMin + (x + 1) * position) {
                    ret[x].add(akt[0], akt[1], saveAvg);
                    b = false;
                }
            }
            if (b) {
                ret[1].add(akt[0], akt[1], saveAvg);
            }

        }
        return ret;
    }

    /**
     * Erstellt ein Letter mit den Farben vom Captcha
     */
    public Letter toColoredLetter() {
        return toColoredLetter(owner.getMaxPixelValue(), owner);
    }

    /**
     * Erstellt ein Letter mit den Farben des owners
     * 
     * @param backgroundcolor
     * @param owner
     * @return
     */
    public Letter toColoredLetter(int backgroundcolor, PixelGrid owner) {
        Letter l = new Letter(getWidth(), getHeight());
        l.setOwner(owner.owner);
        l.setGrid(getGrid(backgroundcolor, owner));
        l.setElementPixel(getSize());
        l.setLocation(new int[] { getXMin(), getYMin() });
        l.detected = detected;
        return l;
    }

    /**
     * Erstellt ein grid aus dem PixelObjekt mit den Farben vom Captcha
     * 
     * @param backgroundcolor
     * @return
     */
    public int[][] getGrid() {
        return getGrid(owner.getMaxPixelValue(), owner);
    }

    /**
     * Erstellt ein grid aus dem PixelObjekt mit den Farben des owners
     * 
     * @param backgroundcolor
     *            , owner
     * @return
     */
    public int[][] getGrid(int backgroundcolor, PixelGrid owner) {
        int[][] ret = new int[getWidth()][getHeight()];
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                ret[x][y] = backgroundcolor;
            }
        }
        for (int i = 0; i < getSize(); i++) {
            int[] akt = elementAt(i);
            ret[akt[0] - getXMin()][akt[1] - getYMin()] = owner.getPixelValue(akt[0], akt[1]);

        }
        return ret;
    }

    /**
     * 
     * @return Gibt einen Entsprechenden Sw-Letter zurück
     */
    public Letter toLetter() {
        int[][] ret = new int[getWidth()][getHeight()];
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                ret[x][y] = owner.getMaxPixelValue();

            }
        }
        for (int i = 0; i < getSize(); i++) {
            int[] akt = elementAt(i);
            ret[akt[0] - xMin][akt[1] - yMin] = getAverage();

        }
        Letter l = owner.createLetter();
        l.setElementPixel(getSize());
        l.setLocation(new int[] { getXMin(), getYMin() });
        l.setGrid(ret);
        l.detected = detected;
        return l;

    }

    // @Override
    @Override
    public String toString() {
        return super.toString() + " " + getLocation()[0] + "-" + getLocation()[1];
    }

    /**
     * Dreht das Objekt um angle grad
     * 
     * @param angle
     * @return gedrehtes Pixelobjekt
     */
    public PixelObject turn(double angle) {
        PixelObject po = new PixelObject(owner);
        for (int i = 0; i < getSize(); i++) {
            int[] akt = elementAt(i);
            int[] n = Utilities.turnCoordinates(akt[0], akt[1], xMin + getWidth() / 2, yMin + getHeight() / 2, angle);
            po.add(n[0], n[1], avg);
        }
        return po;
    }

    public boolean isTouching(int x, int y, boolean followX, int radiusX, int radiusY) {
        if (x > getXMax() + radiusX) return false;
        if (x < getXMin() - radiusY) return false;
        if (y > getYMax() + radiusX) return false;
        if (y < getYMin() - radiusY) return false;
        HashMap<Integer, int[]> row;

        for (int sx = -radiusX; sx <= radiusX; sx++) {
            row = grid.get(x + sx);
            if (row == null) continue;
            for (int sy = -radiusY; sy <= radiusY; sy++) {
                if (Math.abs(sx) == Math.abs(sy) && !followX) continue;
                if (row.get(y + sy) != null) return true;
            }

        }

        return false;
    }

    public int[] getNextPixel(int x, int y) {
        Double bestDist = Double.MAX_VALUE;
        int[] bestAKT = null;
        for (int i = 0; i < getSize(); i++) {
            int[] akt = elementAt(i);
            int xd = x - akt[0];
            int yd = y - akt[1];
            if (Math.max(Math.abs(xd), Math.abs(yd)) < bestDist) {
                double dist = Math.sqrt(xd * xd + yd * yd);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestAKT = akt;
                }
            }
        }
        return bestAKT;
    }

}
