//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.captcha.pixelobject;

import java.util.Vector;
import java.util.logging.Logger;

import jd.captcha.JAntiCaptcha;
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
     * Logger
     */
    public Logger         logger        = UTILITIES.getLogger();

    /**
     * Interner Vector
     */
    private Vector<int[]> object;

    /**
     * Farbdurschnitt des Onkekts
     */
    private int           avg           = 0;

    /**
     * Interne prüfvariable die hochgezählt wird wenn dieneuen Pixel keine
     * Durchschnissänderung hervorrufen
     */
    private int           noAvgChanges  = 0;

    /**
     * Anzahl der gleichbleibenden add Aufrufe bis ein durchschnitt as icher
     * angesehen wird
     */
    private int           avgIsSaveNum  = 10;

    /**
     * Als sicher angenommener Farb durchschnitt
     */
    private int           saveAvg       = 0;

    /**
     * Minimaler x Wert
     */
    private int           xMin          = Integer.MAX_VALUE;

    /**
     * Maximaler X Wert
     */
    private int           xMax          = Integer.MIN_VALUE;

    /**
     * Minimaler y Wert
     */
    private int           yMin          = Integer.MAX_VALUE;

    /**
     * Maximaler Y Wert
     */
    private int           yMax          = Integer.MIN_VALUE;

    /**
     * captcha als owner. Über owner kann auf den Parameter Dump zugegriffen
     * werden
     */
    private PixelGrid     owner;

    /**
     * Kontrastwert für die durchschnisserkennung
     */
    private double        contrast;

    private double        whiteContrast = 1;

    private int           letterColor   = 0;
    public int colorpixel = 0;
    /**
     * @param owner
     */
    public PixelObject(PixelGrid owner) {
        this.owner = owner;
        object = new Vector<int[]>();
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
        // if(JAntiCaptcha.isLoggerActive())logger.info(" AVG "+avg+"
        // ("+color+")");
        if (Math.abs(avg - tmpAvg) < (owner.getMaxPixelValue() * this.contrast)) {
            noAvgChanges++;
            if (avgIsSaveNum <= noAvgChanges && saveAvg == 0) {
                saveAvg = avg;
                // if(JAntiCaptcha.isLoggerActive())logger.info("saveAvg "+avg);

            }
        }
        else {
            noAvgChanges = 0;
        }

        object.add(tmp);
        xMin = Math.min(x, xMin);
        xMax = Math.max(x, xMax);
        yMin = Math.min(y, yMin);
        yMax = Math.max(y, yMax);

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
     * 
     * @return Gibt die Fläche des Objekts zurück
     */
    public int getArea() {
        return getWidth() * getHeight();
    }

    /**
     * 
     * @param color
     * @return Prüft ob die farbe color zum Objekt passt
     */
    public boolean doesColorAverageFit(int color) {
        if (getSize() > 50000) {
            if (JAntiCaptcha.isLoggerActive()) logger.severe("Objekt scheint sehr groß zu werden. objectColorContrast zu hoch?");
            return false;
        }

        int tavg = saveAvg == 0 ? avg : saveAvg;
        // if(JAntiCaptcha.isLoggerActive())logger.info(tavg+"-"+color+" :
        // "+(int)Math.abs(tavg -
        // color)+"<"+(int)(owner.getMaxPixelValue() * this.contrast)+" =
        // "+(((int)Math.abs(tavg - color) < (int)(owner.getMaxPixelValue() *
        // this.contrast))));

        // Filtere zu helle Pixel aus
        if (color > (int) (whiteContrast * owner.getMaxPixelValue())) return false;
        if (getSize() == 0) return true;

        return ((int) Math.abs(tavg - color) < (int) (owner.getMaxPixelValue() * this.contrast));

    }

    /**
     * 
     * @return Anzahl der eingetragenen pixel
     */
    public int getSize() {
        return object.size();

    }

    /**
     * 
     * @return Gibt farbe des Objekts zurück
     */
    public int getAverage() {
        return avg;
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
     * 
     * @param i
     * @return Gibt den pixel bei i zurück [x,y,color}
     */
    public int[] elementAt(int i) {
        return object.elementAt(i);
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
                if (akt[0] >= xMin + x * part - overlap && akt[0] <= (xMin + (x + 1) * part) + overlap) {
                    ret.elementAt(x).add(akt[0], akt[1], this.saveAvg);

                }
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
        l.setGrid(ret);
        return l;

    }

    /**
     * 
     * @return Gibt das Breite/Höhe Verjältniss zurück
     */
    public double getWidthToHeight() {
        return (double) getWidth() / (double) getHeight();
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
        if (r.getWidthToHeight() >= getWidthToHeight() && l.getWidthToHeight() >= getWidthToHeight()) return this;
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

    public int compareTo(Object arg) {
        if (((PixelObject) arg).getLocation()[0] < this.getLocation()[0]) return 1;
        if (((PixelObject) arg).getLocation()[0] > this.getLocation()[0]) return -1;
        return 0;
    }

    /**
     * Setzte den WhiteKontrast
     * 
     * @param objectContrast
     */
    public void setWhiteContrast(double objectContrast) {
        this.whiteContrast = objectContrast;

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

    public String toString() {
        return super.toString() + " " + this.getLocation()[0];
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
            }
            else {
                int col = owner.getMaxPixelValue() - akt[2];
                double dist = Math.sqrt(Math.abs(akt[0] - x) * Math.abs(akt[0] - x) + Math.abs(akt[1] - y) * Math.abs(akt[1] - y)) / 0.5;

                ret += col / (dist + 1);
            }
        }
        return ret;
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
        if (letter == null) letter = this.toLetter();
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
            if (c) ret.add(akt);
        }

        return ret;

    }

    public void setColor(int pixelValue) {
        this.letterColor = pixelValue;

    }

    public void add(PixelObject current) {
        for (int i = 0; i < current.object.size(); i++) {
            add(current.object.get(i)[0], current.object.get(i)[1], current.object.get(i)[2]);
        }

    }
}