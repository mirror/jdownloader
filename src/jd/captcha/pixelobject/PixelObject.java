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

package jd.captcha.pixelobject;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.utils.UTILITIES;

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
@SuppressWarnings("unchecked")
public class PixelObject implements Comparable {
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
    public int colorpixel = 0;
    private HashMap<Integer, HashMap<Integer, int[]>> grid;
    /**
     * Kontrastwert für die durchschnisserkennung
     */
    private double contrast;

    public LetterComperator detected = null;

    /**
     * Logger
     */
    public Logger logger = UTILITIES.getLogger();

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
    private PixelGrid owner;

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

    /**
     * @param grid
     */
    public PixelObject(PixelGrid grid) {
        this.owner = grid;
        this.grid = new HashMap<Integer, HashMap<Integer, int[]>>();
        object = new ArrayList<int[]>();
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

        avg = UTILITIES.mixColors(avg, color, getSize(), 1);
        HashMap<Integer, int[]> row = grid.get(x);
        if (row == null) grid.put(x, row = new HashMap<Integer, int[]>());
        row.put(y, new int[]{x,y,color});
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
        for (int i = 0; i < current.object.size(); i++) {
            add(current.object.get(i)[0], current.object.get(i)[1], current.object.get(i)[2]);
        }

    }

    private int getYMax() {
       
        return yMax;
    }

    private int getXMax() {
       
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
        // UTILITIES.trace(getWidthToHeight()+" : right:"+r.getWidthToHeight()+"
        // left:"+l.getWidthToHeight());
        if (r.getWidthToHeight() >= getWidthToHeight() && l.getWidthToHeight() >= getWidthToHeight()) { return this; }
        int steps = r.getWidthToHeight() < l.getWidthToHeight() ? -accuracy : accuracy;
        angle = steps * 2;
        PixelObject ret = r.getWidthToHeight() < l.getWidthToHeight() ? r : l;
        PixelObject next;
        while ((next = turn(angle)).getWidthToHeight() < ret.getWidthToHeight()) {
            // UTILITIES.trace("akt angle: "+angle+" wh:
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
        // UTILITIES.trace("ALIGN "+this.getWidthToHeight());
        for (int angle = angleA; angle < angleB; angle += accuracy) {

            tmp = turn(angle < 0 ? 360 + angle : angle);

            // UTILITIES.trace((angle<0?360+angle:angle)+" test
            // "+this.getWidthToHeight());
            if (tmp.getWidthToHeight() < bestValue) {
                bestValue = tmp.getWidthToHeight();
                res = tmp;
            }
        }

        return res;
    }

    public int compareTo(Object arg) {
        if (((PixelObject) arg).getLocation()[0] < getLocation()[0]) { return 1; }
        if (((PixelObject) arg).getLocation()[0] > getLocation()[0]) { return -1; }
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
            if (JAntiCaptcha.isLoggerActive()) {
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

    public void setColor(int pixelValue) {

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

    @Override
    public String toString() {
        return super.toString() + " " + getLocation()[0]+"-"+getLocation()[1];
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
            int[] n = UTILITIES.turnCoordinates(akt[0], akt[1], xMin + getWidth() / 2, yMin + getHeight() / 2, angle);
            po.add(n[0], n[1], avg);
        }
        return po;
    }

}