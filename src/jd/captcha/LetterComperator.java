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

package jd.captcha;

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;

import jd.captcha.configuration.JACScript;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.utils.Utilities;
import jd.controlling.JDLogger;

/**
 * Diese Klasse berechnet die Unterschiede zwischen zwei letter instanzen. Über
 * die getter kann nach dem ausführen von run(). Abgerufen werden welche
 * UNterschiede sich ergeben haben.
 * 
 * @author JD-Team
 */
public class LetterComperator {
    private static final int        ANBCOLOR                    = 0xff0000;
    private static final int        ANBFILTEREDCOLOR            = 0xffcccc;
    private static final int        BNACOLOR                    = 0x0000ff;
    private static final int        BNAFILTEREDCOLOR            = 0xccccff;
    // Farbkonstanten für die überlagerungsbilder
    private static final int        BOTHCOLOR                   = 0x660099;
    /**
     * Gibt an ob ein Intersectionletter( schnittbilder) erstellt werden soll.
     * Achtung langsam!
     */
    public static boolean           CREATEINTERSECTIONLETTER    = false;
    /**
     * Matchtable kann als Stringarray gesetzt werden... z.b.
     * {"123","456","789"} lässt auf id platz 1 nur 1,2 oder 3 zu usw.
     */
    public static String[]          MATCH_TABLE                 = null;
    // Ids die Auskunft über die Art der ERkennung geben
    /**
     * Detection IDS Erkennung durch einen Perfectmatch schwellwert
     */
    public static final int         PERFECTMATCH                = 1;
    /**
     * Detection IDS Erkennung durch den quickscan
     */
    public static final int         QUICKSCANMATCH              = 2;
    /**
     * Detection IDS Perfect Match durch den Quickscan
     */
    public static final int         QUICKSCANPERFECTMATCH       = 3;
    // Buchstaben a und b. a ist das captachbild und b das datenbankbild
    private Letter                  a                           = null;

    private Letter                  b                           = null;
    private int[]                   bc                          = new int[2];
    private Vector<Vector<Integer>> bothElements                = new Vector<Vector<Integer>>();
    private int                     bothElementsNum;
    private double                  cleftFaktor;
    private double                  coverageFaktorA             = 0;
    private double                  coverageFaktorAWeight;
    private double                  coverageFaktorB             = 0;
    private double                  coverageFaktorBWeight;
    private int                     detectionType;
    private double                  divider;
    private Vector<Integer>         element;
    private int[][]                 elementGrid;
    private double                  errorAWeight;
    private double                  errorbWeight;
    private Object[]                extensionCodeArguments      = new Object[] { null, 0.0 };
    private Method                  extensionCodeMethod         = null;
    private Class<?>[]              extensionCodeParameterTypes = new Class[] { LetterComperator.class, Double.class };
    private double                  extensionError              = 0.0;
    private double                  heightFaktor                = 0;
    private int[]                   imgOffset;
    private double                  intersectionAHeightFaktor;
    private double                  intersectionAHeightWeight;
    private double                  intersectionAWidthFaktor;
    private double                  intersectionAWidthWeight;
    private int[]                   intersectionDimension;
    private double                  intersectionDimensionWeight;
    private int[][]                 intersectionGrid            = new int[0][0];
    private int                     intersectionHeight          = 0;
    private Letter                  intersectionLetter          = new Letter();
    private int                     intersectionStartX          = 0;
    private int                     intersectionStartY          = 0;
    private int                     intersectionWidth           = 0;
    private JACScript               jas;
    private double                  localHeightPercent;
    private double                  localWidthPercent;
    private Logger                  logger                      = Utilities.getLogger();
    private int                     minCleftSize;
    private int[]                   offset;
    private int                     overlayNoiseSize;
    private JAntiCaptcha            owner;
    private int                     pixelAll;
    private int                     pixelANotB                  = 0;
    private int                     pixelBNotA                  = 0;
    private int                     pixelBoth                   = 0;
    private double                  pixelErrorA                 = 0;
    private double                  pixelErrorB                 = 0;
    private double                  prescanDivider;
    private int                     preScanFaktor;
    private int                     preScanFilter;
    private double                  preValityPercent;

    private double                  reliability;
    private int                     scanStepX;

    private int                     scanStepY;
    private int                     scanTime                    = -1;
    private int                     scanVarianceX               = -1;
    private int                     scanVarianceY               = -1;
    private double                  tmpCoverageFaktorA;
    private double                  tmpCoverageFaktorB;
    private double                  tmpErrorA;
    private double                  tmpErrorB;
    private double                  tmpErrorTotal;
    private double                  tmpExtensionError           = 0.0;
    private double                  tmpHeightAFaktor;
    private double                  tmpHeightFaktor;
    private int                     tmpPixelAButNotB;
    private int                     tmpPixelBButNotA;
    private int                     tmpPixelBoth                = 0;
    private double                  tmpPreScanValue;

    private double                  tmpWidthAFaktor;
    private double                  tmpWidthFaktor;
    private double                  totalPixelError             = 0;
    private double                  valityPercent               = 10000.0;
    private double                  widthFaktor                 = 0;
    private int[]                   position;

    /**
     * @param a
     * @param b
     */
    public LetterComperator(Letter a, Letter b) {
        super();
        this.a = a;
        this.b = b;

    }

    /**
     * TRansformiert Koordinaten von letter a nach b
     * 
     * @param x
     * @param y
     * @param xx
     * @param yy
     * @return neue Koordinaten
     */
    private int[] coordinatesFromAToB(int x, int y, int xx, int yy, int[] con) {
        con[0] = x - xx;
        con[1] = y - yy;
        return con;
    }

    /**
     * @return the a
     */
    public Letter getA() {
        return a;
    }

    /**
     * @return the b
     */
    public Letter getB() {
        return b;
    }

    /**
     * 
     * @return Anzahl der Elemente aus denen die Schnitttmenge besteht. je näher
     *         an eins desto besser
     */
    public int getBothElementsNum() {
        return bothElementsNum;
    }

    /**
     * @return the coverageFaktorA
     */
    public double getCoverageFaktorA() {
        return coverageFaktorA;
    }

    /**
     * 
     * @return the coverageFaktorB
     */
    public double getCoverageFaktorB() {
        return coverageFaktorB;
    }

    /**
     * @return Gibt den decoed value von b zurück
     */
    public String getDecodedValue() {
        if (b == null || b.getDecodedValue() == null || b.getDecodedValue().length() != 1) { return "-"; }
        return b.getDecodedValue();
    }

    /**
     * 
     * @return Detection ID
     */
    public int getDetectionType() {
        return detectionType;
    }

    public Letter getDifference() {
        try {
            int xx = imgOffset[0];
            int yy = imgOffset[1];
            int left = offset[0];
            int top = offset[1];
            int tmpIntersectionWidth = intersectionDimension[0];
            int tmpIntersectionHeight = intersectionDimension[1];

            // long starter=Utilities.getTimer();

            int[][] g = new int[tmpIntersectionWidth][tmpIntersectionHeight];

            for (int x = 0; x < tmpIntersectionWidth; x++) {
                for (int y = 0; y < tmpIntersectionHeight; y++) {
                    g[x][y] = getA().getMaxPixelValue();
                    int pixelType = getPixelType(x, y, xx, yy, left, top);

                    switch (pixelType) {
                    case 0:

                        // g[x][y] = 0xcccccc;
                        break;
                    case 1:
                        if (hasNeighbour(x, y, xx, yy, left, top, pixelType) > overlayNoiseSize) {

                            // g[x][y] = 0xff0000;
                        } else {
                            // g[x][y] = 0xff0000;

                        }
                        g[x][y] = 0;
                        break;
                    case 2:
                        if (hasNeighbour(x, y, xx, yy, left, top, pixelType) > overlayNoiseSize) {
                            g[x][y] = 0;
                        }
                        // else {
                        // g[x][y] = 0;
                        // g[x][y] = 0x00ff00;
                        // }
                        break;
                    default:

                    }
                }
            }

            Letter ret = getA().createLetter();
            int[] l = getA().getLocation();
            ret.setLocation(new int[] { l[0] + left, l[1] + top });
            ret.setGrid(g);
            ret.clean();
            return ret;
        } catch (Exception e) {
            return getA();
        }

    }

    /**
     * Rekursive Funktion, Die die große eines Elements zurückgibt. in den grids
     * werden schon vergebene pixel abgelegt
     * 
     * @param x
     * @param y
     * @param xx
     * @param yy
     * @param left
     * @param top
     * @param pixelType
     * @param elementGrid
     * @param counter
     * @return größe des Elements
     */
    private Integer getElement(int x, int y, int xx, int yy, int left, int top, int pixelType, int[][] elementGrid, Vector<Integer> counter) {
        if (x < 0 || y < 0 || x >= elementGrid.length || elementGrid.length == 0 || y >= elementGrid[0].length) { return null; }
        if (elementGrid[x][y] != 0) { return null; }
        int pt = getPixelType(x, y, xx, yy, left, top);

        if (pt == pixelType) {
            counter.add(pixelType);
            elementGrid[x][y] = pixelType + 100;
            getElement(x - 1, y, xx, yy, left, top, pt, elementGrid, counter);
            getElement(x - 1, y - 1, xx, yy, left, top, pt, elementGrid, counter);
            getElement(x, y - 1, xx, yy, left, top, pt, elementGrid, counter);
            getElement(x + 1, y - 1, xx, yy, left, top, pt, elementGrid, counter);
            getElement(x + 1, y, xx, yy, left, top, pt, elementGrid, counter);
            getElement(x + 1, y + 1, xx, yy, left, top, pt, elementGrid, counter);
            getElement(x, y + 1, xx, yy, left, top, pt, elementGrid, counter);
            getElement(x - 1, y + 1, xx, yy, left, top, pt, elementGrid, counter);
        }
        return null;
    }

    public double getExtensionError() {
        return extensionError;
    }

    /**
     * @return the heightPercent
     */
    public double getHeightFaktor() {
        return heightFaktor;
    }

    /**
     * 
     * @return Kombinierter Wert aus Valityvalue und Reliability. Kann zur
     *         berechnung der erkennungssicherheit verwendet werden
     */
    public double getIdentificationReliability() {
        return getValityPercent() - getReliability();

    }

    public int[] getImgOffset() {
        return imgOffset;
    }

    public Letter getIntersection() {
        try {
            // int xx = this.imgOffset[0];
            // int yy = this.imgOffset[1];
            int left = offset[0];
            int top = offset[1];
            int tmpIntersectionWidth = intersectionDimension[0];
            int tmpIntersectionHeight = intersectionDimension[1];

            // long starter=Utilities.getTimer();

            int[][] g = new int[tmpIntersectionWidth][tmpIntersectionHeight];

            for (int x = 0; x < tmpIntersectionWidth; x++) {
                for (int y = 0; y < tmpIntersectionHeight; y++) {
                    g[x][y] = a.getPixelValue(x + left, y + top);

                }
            }

            Letter ret = getA().createLetter();
            int[] l = getA().getLocation();
            ret.setLocation(new int[] { l[0] + left, l[1] + top });
            ret.setGrid(g);
            ret.clean();
            return ret;
        } catch (Exception e) {
            JDLogger.exception(e);
            return getA();
        }

    }

    public double getIntersectionAHeightFaktor() {
        return intersectionAHeightFaktor;
    }

    public double getIntersectionAWidthFaktor() {
        return intersectionAWidthFaktor;
    }

    public int[] getIntersectionDimension() {
        return intersectionDimension;
    }

    /**
     * @return the intersectionHeight
     */
    public int getIntersectionHeight() {
        return intersectionHeight;
    }

    /**
     * @return gibt einen Letterzurück, aus dem gut erkannt werden kann wie sich
     *         das ergebniss zusammensetzt. gleiche antile werden lila gefärbt,
     *         banteile rot und a anteile blau Um den Intersectionletter
     *         auszugene muss zuerst setCreateIntersectionLetter(true)
     *         ausgeführt werden
     */
    public Letter getIntersectionLetter() {

        return intersectionLetter;
    }

    /**
     * @return the intersectionStartX
     */
    public int getIntersectionStartX() {
        return intersectionStartX;
    }

    /**
     * @return the intersectionStartY
     */
    public int getIntersectionStartY() {
        return intersectionStartY;
    }

    /**
     * @return the intersectionWidth
     */
    public int getIntersectionWidth() {
        return intersectionWidth;
    }

    public double getLocalHeightPercent() {
        return localHeightPercent;
    }

    public int[] getOffset() {
        return offset;
    }

    public void setOffset(int[] offset) {
        this.offset = offset;
    }

    public JAntiCaptcha getOwner() {
        return owner;
    }

    /**
     * @return the pixelANotB
     */
    public int getPixelANotB() {
        return pixelANotB;
    }

    /**
     * @return the pixelBNotA
     */
    public int getPixelBNotA() {
        return pixelBNotA;
    }

    /**
     * @return the pixelBoth
     */
    public int getPixelBoth() {
        return pixelBoth;
    }

    /**
     * @return the pixelErrorA
     */
    public double getPixelErrorA() {
        return pixelErrorA;
    }

    /**
     * @return the pixelErrorB
     */
    public double getPixelErrorB() {
        return pixelErrorB;
    }

    /**
     * Gib zurück ob es sich um einen gemeinsammenpixel handekt oder nicht
     * 
     * @param x
     * @param y
     * @param xx
     * @param yy
     * @param left
     * @param top
     * @return -2(fehler)/ 0 gemeinsammer schwarzer Pixel /1 Pixel B aber nicht
     *         a / 2 pixel A aber nicht B/ -1 beide weiß
     */
    public int getPixelType(int x, int y, int xx, int yy, int left, int top) {

        int va = a.getPixelValue(x + left, y + top);
        bc = coordinatesFromAToB(x + left, y + top, xx, yy, bc);
        int vb = b.getPixelValue(bc[0], bc[1]);
        if (va < 0 || vb < 0) { return -2; }
        if (vb == 0 && va == 0) {
            return 0;
        } else if (vb == 0) {
            return 1;
        } else if (va == 0) {

        return 2; }
        return -1;
    }

    /**
     * 
     * @return Das Prescan Ergebniss
     */
    public double getPreValityPercent() {
        return preValityPercent;
    }

    public double getRealValityValue() {
        return valityPercent;
    }

    /**
     * 
     * @return ReliabilityValue. ABstand zum nächstbesten Buchstaben
     */
    public double getReliability() {
        return reliability;
    }

    /**
     * @return the scanTime
     */
    public int getScanTime() {
        return scanTime;
    }

    /**
     * @return the scanVarianceX
     */
    public int getScanVarianceX() {
        if (scanVarianceX >= 0) { return scanVarianceX; }
        return jas.getInteger("scanVarianceX");
    }

    /**
     * @return the scanVarianceY
     */
    public int getScanVarianceY() {
        if (scanVarianceX >= 0) { return scanVarianceY; }
        return jas.getInteger("scanVarianceY");
    }

    public double getTmpExtensionError() {
        return tmpExtensionError;
    }

    /**
     * @return the totalPixelError
     */
    public double getTotalPixelError() {
        return totalPixelError;
    }

    /**
     * @return Prozentwert 0(gut) bis 100 (schlecht) der Übereinstimmung
     */
    public double getValityPercent() {
        return Math.min(100.0, valityPercent);
    }

    /**
     * @return the widthPercent
     */
    public double getWidthFaktor() {
        return widthFaktor;
    }

    /**
     * Prüft ob de aktuellepixel nachbarn mit dem selben Pixeltype hat
     * 
     * @param x
     * @param y
     * @param xx
     * @param yy
     * @param left
     * @param top
     * @param pixelType
     * @return anzahl der Nachbarn
     */
    public int hasNeighbour(int x, int y, int xx, int yy, int left, int top, int pixelType) {

        int ret = 0;
        int faktor = 1;
        for (int xt = -faktor; xt <= faktor; xt++) {
            for (int yt = -faktor; yt <= faktor; yt++) {
                if (xt == 0 && yt == 0) {
                    continue;
                }
                if (getPixelType(x + xt, y + yt, xx, yy, left, top) == pixelType) {
                    ret++;
                }
            }
        }
        return ret;
    }

    /**
     * @return the createIntersectionLetter
     */
    public boolean isCreateIntersectionLetter() {
        return CREATEINTERSECTIONLETTER;
    }

    /**
     * Führt den Vergleichsvorgang aus
     */
    public void run() {

        if (MATCH_TABLE != null && this.getA().getId() > -1 && MATCH_TABLE.length > this.getA().getId()) {
            String matches = MATCH_TABLE[this.getA().getId()];

            if (!matches.contains(getB().getDecodedValue())) {

            return; }
        }

        scan();

    }

    /**
     * Scan ist die eigentliche vergleichsfunktion. a und b werden dabei
     * gegeneinander verschoben.
     */
    private void scan() {
        long startTime = System.currentTimeMillis();
        double bestValue = 20000.0;
        preValityPercent = 20000.0;
        tmpPreScanValue = 20000.0;
        // logger.info(b.getDecodedValue()+"----");

        // scanvarianzen geben an wieviel beim verschieben über die grenzen
        // geschoben wird. große werte brauchen CPU
        int vx = getScanVarianceX();
        int vy = getScanVarianceY();
        vx = Math.min(vx, b.getWidth());
        vy = Math.min(vy, b.getHeight());
        int scanXFrom = -vx;
        int scanXTo = a.getWidth() - b.getWidth() + vx;
        int scanYFrom = -vy;
        int scanYTo = a.getHeight() - b.getHeight() + vy;
        int tmp;
        if (scanXTo < scanXFrom) {
            tmp = scanXTo;
            scanXTo = scanXFrom;
            scanXFrom = tmp;
        }
        if (scanYTo < scanYFrom) {
            tmp = scanYTo;
            scanYTo = scanYFrom;
            scanYFrom = tmp;
        }

        double value;
        Letter tmpIntersection = null;
        if (isCreateIntersectionLetter()) {
            tmpIntersection = new Letter();
        }
        int left;
        int right;
        int top;
        int bottom;
        int tmpIntersectionWidth;
        int tmpIntersectionHeight;

        // if(b.id==1371) logger.info(" Scan from " + scanXFrom + "/" + scanXTo
        // + " - " + scanYFrom + "/" + scanYTo + " Var: " + vx + "/" + vy);
        // schleife verschieb a und b gegeneinander. Dabei wird um den
        // jeweiligen Mittelpunkt herumgesprungen. Die Warscheinlichsten Fälle
        // in der Nullage werden zuerst geprüft
        // if (this.getDecodedValue().equals("1"))
        // logger.info(this.getDecodedValue() + " :start");

        for (int xx = Utilities.getJumperStart(scanXFrom, scanXTo); Utilities.checkJumper(xx, scanXFrom, scanXTo); xx = Utilities.nextJump(xx, scanXFrom, scanXTo, 1)) {
            for (int yy = Utilities.getJumperStart(scanYFrom, scanYTo); Utilities.checkJumper(yy, scanYFrom, scanYTo); yy = Utilities.nextJump(yy, scanYFrom, scanYTo, 1)) {
                // Offsets
                left = Math.max(0, xx);
                right = Math.min(xx + b.getWidth(), a.getWidth());
                top = Math.max(0, yy);
                bottom = Math.min(yy + b.getHeight(), a.getHeight());
                // intersection ^=ausschnitt
                tmpIntersectionWidth = right - left;
                tmpIntersectionHeight = bottom - top;
                if (tmpIntersectionWidth <= 0 || tmpIntersectionHeight <= 0) {
                    // logger.warning("Scannvarianzen zu groß: " +
                    // tmpIntersectionWidth + "/" + tmpIntersectionHeight);
                    continue;
                }
                if (isCreateIntersectionLetter()) {
                    tmpIntersection = new Letter();
                    tmpIntersection.setOwner(owner);
                    intersectionGrid = new int[tmpIntersectionWidth][tmpIntersectionHeight];
                    tmpIntersection.setGrid(intersectionGrid);
                }
                //
                if (preScanFilter > 0) {
                    tmpPreScanValue = scanPreIntersection(xx, yy, left, top, tmpIntersectionWidth, tmpIntersectionHeight);
                    // logger.info("_"+preScan);

                    if ((int) tmpPreScanValue > preScanFilter) {
                        if (preValityPercent > tmpPreScanValue) {
                            setPreValityPercent(tmpPreScanValue);
                        }
                        continue;
                    }

                }
                // logger.info("Scan
                // "+tmpIntersectionWidth+"/"+tmpIntersectionHeight+" -
                // "+a.getElementPixel());

                value = scanIntersection(xx, yy, left, top, tmpIntersectionWidth, tmpIntersectionHeight);
                // logger.info(tmpIntersectionWidth+"/"+tmpIntersectionHeight+"
                // : "+" scanIntersection: ");
                //
                // if(getDecodedValue().equalsIgnoreCase("v")&&getBothElementsNum()==3){
                // logger.info("JJJ");
                // }
                if (value < bestValue) {
                    bestValue = value;
                    setExtensionError(tmpExtensionError);
                    setValityPercent(value);
                    setHeightFaktor(tmpHeightFaktor);
                    setWidthFaktor(tmpWidthFaktor);
                    setIntersectionHeight(tmpIntersectionHeight);
                    setIntersectionWidth(tmpIntersectionWidth);
                    setIntersectionStartX(xx);
                    setIntersectionStartY(yy);
                    setPosition(left, top);
                    setBothElementNum(bothElements.size());
                    setCoverageFaktorA(tmpCoverageFaktorA);
                    setCoverageFaktorB(tmpCoverageFaktorB);
                    setPixelErrorA(tmpErrorA);
                    setPreValityPercent(tmpPreScanValue);
                    setPixelErrorB(tmpErrorB);
                    setPixelANotB(tmpPixelAButNotB);
                    setPixelBNotA(tmpPixelBButNotA);
                    setPixelBoth(tmpPixelBoth);
                    intersectionAHeightFaktor = 0.0;
                    intersectionAWidthFaktor = 0.0;
                    setIntersectionAHeightFaktor(tmpHeightAFaktor);
                    setIntersectionAWidthFaktor(tmpWidthAFaktor);

                    setTotalPixelError(tmpErrorTotal);
                    if (isCreateIntersectionLetter()) {
                        setIntersectionLetter(tmpIntersection);

                    }

                }
            }
        }
        // }

        scanTime = (int) (System.currentTimeMillis() - startTime);

    }

    private void setPosition(int left, int top) {
        this.position = new int[] { left, top };

    }

    public int[] getPosition() {
        return position;
    }

    public void setPosition(int[] position) {
        this.position = position;
    }

    private double scanIntersection(int xx, int yy, int left, int top, int tmpIntersectionWidth, int tmpIntersectionHeight) {
        offset = new int[] { left, top };
        imgOffset = new int[] { xx, yy };
        intersectionDimension = new int[] { tmpIntersectionWidth, tmpIntersectionHeight };

        double tmpError;
        pixelAll = 0;
        tmpPixelBButNotA = 0;
        tmpPixelAButNotB = 0;
        tmpPixelBoth = 0;
        tmpCoverageFaktorA = 0;
        tmpCoverageFaktorB = 0;
        // long starter=Utilities.getTimer();
        bothElements.removeAllElements();
        elementGrid = new int[tmpIntersectionWidth][tmpIntersectionHeight];

        for (int x = 0; x < tmpIntersectionWidth; x += scanStepX) {
            for (int y = 0; y < tmpIntersectionHeight; y += scanStepY) {

                int pixelType = getPixelType(x, y, xx, yy, left, top);
                pixelAll++;

                switch (pixelType) {
                case 0:
                    if (isCreateIntersectionLetter()) {
                        intersectionGrid[x][y] = BOTHCOLOR;
                    }
                    if (cleftFaktor > 0) {
                        getElement(x, y, xx, yy, left, top, pixelType, elementGrid, element = new Vector<Integer>());
                        if (element.size() > minCleftSize) {
                            bothElements.add(element);
                        }
                    }
                    tmpPixelBoth++;
                    break;
                case 1:
                    if (overlayNoiseSize <= 0 || hasNeighbour(x, y, xx, yy, left, top, pixelType) > overlayNoiseSize) {
                        tmpPixelBButNotA++;
                        if (isCreateIntersectionLetter()) {
                            intersectionGrid[x][y] = BNACOLOR;
                        }
                    } else {
                        if (isCreateIntersectionLetter()) {
                            intersectionGrid[x][y] = BNAFILTEREDCOLOR;
                        }
                    }
                    break;
                case 2:
                    if (overlayNoiseSize <= 0 || hasNeighbour(x, y, xx, yy, left, top, pixelType) > overlayNoiseSize) {
                        tmpPixelAButNotB++;
                        if (isCreateIntersectionLetter()) {
                            intersectionGrid[x][y] = ANBCOLOR;
                        }
                    } else {

                        if (isCreateIntersectionLetter()) {
                            intersectionGrid[x][y] = ANBFILTEREDCOLOR;
                        }
                    }
                    break;
                default:
                    if (isCreateIntersectionLetter()) {
                        intersectionGrid[x][y] = 0xffffff;
                    }
                }
            }
        }

        // logger.info("Scanner: "+Utilities.getTimer(starter));
        // if(getDecodedValue().equalsIgnoreCase("v")&&getBothElementsNum()==3){
        // logger.info("JJJ");
        // }
        if (pixelAll > 0 && (bothElements.size() > 0 || cleftFaktor == 0)) {
            tmpErrorA = (double) tmpPixelAButNotB / (double) (tmpPixelBoth + tmpPixelAButNotB);
            tmpErrorB = (double) tmpPixelBButNotA / (double) (tmpPixelBButNotA + tmpPixelBoth);
            tmpErrorTotal = tmpErrorA * errorAWeight + tmpErrorB * errorbWeight;

            tmpCoverageFaktorA = 1.0 - tmpPixelBoth / ((double) a.getElementPixel() / (scanStepX * scanStepY));
            tmpCoverageFaktorB = 1.0 - tmpPixelBoth / ((double) b.getElementPixel() / (scanStepX * scanStepY));
            setLocalHeightPercent((double) tmpIntersectionHeight / (double) b.getHeight());
            localWidthPercent = (double) tmpIntersectionWidth / (double) b.getWidth();
            double lhp = 1.0 - getLocalHeightPercent();
            double lwp = 1.0 - localWidthPercent;
            tmpHeightFaktor = lhp * lhp;
            tmpWidthFaktor = lwp * lwp;
            tmpHeightAFaktor = 1.0 - (double) tmpIntersectionHeight / (double) a.getHeight();
            tmpWidthAFaktor = 1.0 - (double) tmpIntersectionWidth / (double) a.getWidth();
            // logger.info(tmpIntersectionWidth+ "/"+a.getWidth()+" =
            // "+localWidthPercent+" --> "+tmpWidthFaktor);

            tmpError = tmpErrorTotal;
            tmpError += Math.min(1.0, tmpCoverageFaktorA) * coverageFaktorAWeight;
            tmpError += Math.min(1.0, tmpCoverageFaktorB) * coverageFaktorBWeight;
            tmpError += Math.min(1.0, tmpHeightFaktor) * intersectionDimensionWeight;
            tmpError += Math.min(1.0, tmpWidthFaktor) * intersectionDimensionWeight;
            tmpError += Math.min(1.0, tmpHeightAFaktor) * intersectionAHeightWeight;
            tmpError += Math.min(1.0, tmpWidthAFaktor) * intersectionAWidthWeight;
            if (bothElements.size() > 0) {
                tmpError += (bothElements.size() - 1) * cleftFaktor;
            }

            tmpExtensionError = 0.0;
            if (extensionCodeMethod != null) {
                try {
                    extensionCodeArguments[1] = tmpError / divider;
                    extensionCodeMethod.invoke(null, extensionCodeArguments);
                } catch (Exception e) {
                    JDLogger.exception(e);
                }
            }

            tmpError += tmpExtensionError;
            tmpError /= divider;
            // tmpError = Math.min(1.0, tmpError);
            // logger.info(pixelBoth+"_"+(tmpIntersectionHeight *
            // tmpIntersectionWidth));
            if (tmpPixelBoth * owner.getJas().getDouble("inverseFontWeight") < tmpIntersectionHeight * tmpIntersectionWidth) {
                tmpError = tmpErrorA = tmpErrorB = tmpErrorTotal = 10000.0 / 100.0;
            }

            return 100.0 * tmpError;
        } else {

            return 10000.0;
        }
    }

    private double scanPreIntersection(int xx, int yy, int left, int top, int tmpIntersectionWidth, int tmpIntersectionHeight) {
        double tmpError;
        int yStep = Math.max(1, tmpIntersectionHeight / (preScanFaktor + 1));

        tmpPixelBoth = 0;
        tmpPixelBButNotA = 0;
        tmpPixelAButNotB = 0;
        pixelAll = 0;
        for (int y = yStep; y <= tmpIntersectionHeight - yStep; y += yStep) {
            for (int x = 0; x < tmpIntersectionWidth; x++) {
                pixelAll++;

                int pixelType = getPixelType(x, y, xx, yy, left, top);
                switch (pixelType) {
                case 0:
                    tmpPixelBoth++;
                    break;
                case 1:
                    tmpPixelBButNotA++;
                    break;
                case 2:
                    tmpPixelAButNotB++;
                    break;
                }
            }
        }
        // logger.info((Utilities.getTimer() - startTime)+ "intersection
        // scantime: "+(Utilities.getTimer()-starter2));
        if (pixelAll > 0) {
            tmpErrorA = (double) tmpPixelAButNotB / (double) (tmpPixelBoth + tmpPixelAButNotB);
            tmpErrorB = (double) tmpPixelBButNotA / (double) (tmpPixelBButNotA + tmpPixelBoth);
            tmpErrorTotal = tmpErrorA * errorAWeight + tmpErrorB * errorbWeight;
            setLocalHeightPercent((double) tmpIntersectionHeight / (double) b.getHeight());
            localWidthPercent = (double) tmpIntersectionWidth / (double) b.getWidth();
            double lhp = 1.0 - getLocalHeightPercent();
            double lwp = 1.0 - localWidthPercent;
            tmpHeightFaktor = lhp * lhp;
            tmpWidthFaktor = lwp * lwp;

            // tmpHeightAFaktor = Math.pow(1.0 - (double) tmpIntersectionHeight
            // / (double) a.getHeight(), 2);
            // tmpWidthAFaktor = Math.pow(1.0 - (double) tmpIntersectionWidth /
            // (double) a.getWidth(), 2);
            tmpError = tmpErrorTotal;
            tmpError += Math.min(1.0, tmpHeightFaktor) * intersectionDimensionWeight;
            tmpError += Math.min(1.0, tmpWidthFaktor) * intersectionDimensionWeight;
            tmpError += Math.min(1.0, tmpHeightAFaktor) * intersectionAHeightWeight;
            tmpError += Math.min(1.0, tmpWidthAFaktor) * intersectionAWidthWeight;
            tmpError /= prescanDivider;
            tmpError *= 1.2;

            return (int) (100 * tmpError);
        } else {
            return 100;
        }
    }

    /**
     * @param a
     *            the a to set
     */
    public void setA(Letter a) {
        this.a = a;
    }

    /**
     * @param b
     *            the b to set
     */
    public void setB(Letter b) {
        this.b = b;
    }

    private void setBothElementNum(int i) {
        bothElementsNum = i;

    }

    /**
     * @param coverageFaktor
     *            the coverageFaktor to set
     */
    private void setCoverageFaktorA(double coverageFaktor) {
        coverageFaktorA = coverageFaktor;
    }

    private void setCoverageFaktorB(double coverageFaktor) {
        coverageFaktorB = coverageFaktor;
    }

    /**
     * @param createIntersectionLetter
     *            the createIntersectionLetter to set
     */
    public void setCreateIntersectionLetter(boolean createIntersectionLetter) {
        CREATEINTERSECTIONLETTER = createIntersectionLetter;
    }

    /**
     * Setzt die art der ERkennung. Siehe Detection IDS
     * 
     * @param matchtype
     */
    public void setDetectionType(int matchtype) {
        detectionType = matchtype;
    }

    public void setExtensionError(double extensionError) {
        this.extensionError = extensionError;
    }

    /**
     * @param heightPercent
     *            the heightPercent to set
     */
    public void setHeightFaktor(double heightPercent) {
        heightFaktor = heightPercent;
    }

    public void setIntersectionAHeightFaktor(double intersectionAHeightFaktor) {
        this.intersectionAHeightFaktor = intersectionAHeightFaktor;
    }

    public void setIntersectionAWidthFaktor(double intersectionAWidthFaktor) {
        this.intersectionAWidthFaktor = intersectionAWidthFaktor;
    }

    /**
     * @param intersectionHeight
     *            the intersectionHeight to set
     */
    private void setIntersectionHeight(int intersectionHeight) {
        this.intersectionHeight = intersectionHeight;
    }

    /**
     * @param intersection
     */
    private void setIntersectionLetter(Letter intersection) {

        intersectionLetter = intersection;

    }

    /**
     * @param intersectionStartX
     *            the intersectionStartX to set
     */
    private void setIntersectionStartX(int intersectionStartX) {
        this.intersectionStartX = intersectionStartX;
    }

    /**
     * @param intersectionStartY
     *            the intersectionStartY to set
     */
    private void setIntersectionStartY(int intersectionStartY) {
        this.intersectionStartY = intersectionStartY;
    }

    /**
     * @param intersectionWidth
     *            the intersectionWidth to set
     */
    private void setIntersectionWidth(int intersectionWidth) {
        this.intersectionWidth = intersectionWidth;
    }

    public void setLocalHeightPercent(double localHeightPercent) {
        this.localHeightPercent = localHeightPercent;
    }

    /**
     * Setzt den Owner (Parameterdump jas wird gelesen
     * 
     * @param owner
     */
    public void setOwner(JAntiCaptcha owner) {
        this.owner = owner;
        jas = owner.getJas();
        errorAWeight = jas.getDouble("errorAWeight");
        errorbWeight = jas.getDouble("errorBWeight");
        coverageFaktorAWeight = jas.getDouble("coverageFaktorAWeight");
        coverageFaktorBWeight = jas.getDouble("coverageFaktorBWeight");
        intersectionDimensionWeight = jas.getDouble("intersectionDimensionWeight");
        intersectionAHeightWeight = jas.getDouble("intersectionAHeightWeight");
        intersectionAWidthWeight = jas.getDouble("intersectionAWidthWeight");
        cleftFaktor = jas.getDouble("cleftFaktor");
        minCleftSize = jas.getInteger("minCleftSize");
        preScanFilter = jas.getInteger("preScanFilter");
        preScanFaktor = jas.getInteger("preScanFaktor");
        overlayNoiseSize = jas.getInteger("overlayNoiseSize");
        scanStepX = jas.getInteger("scanstepx");
        scanStepY = jas.getInteger("scanstepy");
        prescanDivider = jas.getDouble("prescandivider");
        divider = jas.getDouble("divider");
        extensionCodeArguments[0] = this;
        if (jas.getString("comparatorExtension").length() > 0) {
            String[] ref = jas.getString("comparatorExtension").split("\\.");
            if (ref.length != 2) {
                if (Utilities.isLoggerActive()) {
                    logger.severe("comparatorExtension should have the format Class.Method");
                }

            }
            String cl = ref[0];
            String methodname = ref[1];
            Class<?> newClass;
            try {
                newClass = Class.forName("jd.captcha.specials." + cl);

                extensionCodeMethod = newClass.getMethod(methodname, extensionCodeParameterTypes);

            } catch (Exception e) {
                JDLogger.exception(e);
            }
        }

    }

    /**
     * @param pixelANotB
     *            the pixelANotB to set
     */
    private void setPixelANotB(int pixelANotB) {
        this.pixelANotB = pixelANotB;
    }

    /**
     * @param pixelBNotA
     *            the pixelBNotA to set
     */
    private void setPixelBNotA(int pixelBNotA) {
        this.pixelBNotA = pixelBNotA;
    }

    /**
     * @param pixelBoth
     *            the pixelBoth to set
     */
    private void setPixelBoth(int pixelBoth) {
        this.pixelBoth = pixelBoth;
    }

    /**
     * @param pixelErrorA
     *            the pixelErrorA to set
     */
    private void setPixelErrorA(double pixelErrorA) {
        this.pixelErrorA = pixelErrorA;
    }

    /**
     * @param pixelErrorB
     *            the pixelErrorB to set
     */
    private void setPixelErrorB(double pixelErrorB) {
        this.pixelErrorB = pixelErrorB;
    }

    private void setPreValityPercent(double value) {
        preValityPercent = value;
    }

    /**
     * TODO: Gleiche buchtaben als nächsten nachbarn ignoriere Setzte den
     * reliability wert. Er ist de Abstand zum nächstmöglichen Treffer. Und
     * sagtdeshalb etwas über die ERkennungswarscheinlichkeit aus
     * 
     * @param d
     */
    public void setReliability(double d) {
        reliability = d;

    }

    /**
     * Setzt die zu verwendene Scanvarianz. (Anzal der pixel die über den rand
     * gescannt werden)
     * 
     * @param x
     * @param y
     */
    public void setScanVariance(int x, int y) {
        scanVarianceX = x;
        scanVarianceY = y;

    }

    public void setTmpExtensionError(double tmpExtensionError) {
        this.tmpExtensionError = tmpExtensionError;
    }

    /**
     * @param totalPixelError
     *            the totalPixelError to set
     */
    private void setTotalPixelError(double totalPixelError) {
        this.totalPixelError = totalPixelError;
    }

    public void setValityPercent(double bestValue) {
        valityPercent = bestValue;

    }

    /**
     * @param widthPercent
     *            the widthPercent to set
     */
    public void setWidthFaktor(double widthPercent) {
        widthFaktor = widthPercent;
    }

    /**
     * Gibt einen laaaangenstring mit den meisten entscheidungsparametern
     * aus.Kann zum verlich verwendet werden
     * 
     * @return parameterstring
     */
    // @Override
    @Override
    public String toString() {
        Hashtable<String, Object> hs = new Hashtable<String, Object>();
        // if(getDecodedValue().equalsIgnoreCase("v")&&getBothElementsNum()==3){
        // logger.info("V");
        // }
        hs.put("DecodedValue", getDecodedValue());
        hs.put("widthFaktor", getWidthFaktor() + "/" + getWidthFaktor() * intersectionDimensionWeight);
        hs.put("heightFaktor", getHeightFaktor() + "/" + getHeightFaktor() * intersectionDimensionWeight);
        hs.put("pixelErrorA", getPixelErrorA());
        hs.put("pixelErrorB", getPixelErrorB());
        hs.put("pixelErrorTotal", getTotalPixelError());
        hs.put("ValidtyPercent", getValityPercent());
        hs.put("pixelAButNotB", getPixelANotB());
        hs.put("pixelBButNotA", getPixelBNotA());
        hs.put("pixelBoth", getPixelBoth());
        hs.put("coverageA", getCoverageFaktorA() + "/" + getCoverageFaktorA() * coverageFaktorAWeight);
        hs.put("coverageB", getCoverageFaktorB() + "/" + getCoverageFaktorB() * coverageFaktorBWeight);
        hs.put("bothElements", getBothElementsNum());
        hs.put("preCompare", getPreValityPercent());
        hs.put("realValityValue", getRealValityValue());
        hs.put("widthAFaktor", getIntersectionAWidthFaktor() + "/" + getIntersectionAWidthFaktor() * intersectionAWidthWeight + "(" + intersectionAWidthWeight + ")");
        hs.put("heightAFaktor", getIntersectionAHeightFaktor() + "/" + getIntersectionAHeightFaktor() * intersectionAHeightWeight + "(" + intersectionAHeightWeight + ")");
        hs.put("extensionError", getExtensionError());
        double t = getRealValityValue() * 6.0 / 100.0;
        double tmpError = getTotalPixelError();
        StringBuilder calc = new StringBuilder("Error= ");
        calc.append(getTotalPixelError());
        calc.append(" (totalPixelError) ");
        calc.append(getTotalPixelError() * 100.0 / t);
        calc.append("\r\n");
        tmpError += Math.min(1.0, getCoverageFaktorA()) * coverageFaktorAWeight;
        calc.append(Math.round(Math.min(1.0, getCoverageFaktorA()) * coverageFaktorAWeight * 100.0 / t));
        calc.append("%          + ");
        calc.append(Math.min(1.0, getCoverageFaktorA()) * coverageFaktorAWeight);
        calc.append('=');
        calc.append(tmpError);
        calc.append(" (coverage A)\r\n");
        tmpError += Math.min(1.0, getCoverageFaktorB()) * coverageFaktorBWeight;
        calc.append(Math.round(Math.min(1.0, getCoverageFaktorB()) * coverageFaktorBWeight * 100.0 / t));
        calc.append("%          + ");
        calc.append(Math.min(1.0, getCoverageFaktorB()) * coverageFaktorBWeight);
        calc.append('=');
        calc.append(tmpError);
        calc.append(" (coverage B)\r\n");
        tmpError += Math.min(1.0, getHeightFaktor()) * intersectionDimensionWeight;
        calc.append(Math.round(Math.min(1.0, getHeightFaktor()) * intersectionDimensionWeight * 100.0 / t));
        calc.append("%          + ");
        calc.append(Math.min(1.0, getHeightFaktor()) * intersectionDimensionWeight);
        calc.append('=');
        calc.append(tmpError);
        calc.append(" (BHeightFaktor)\r\n");
        tmpError += Math.min(1.0, getWidthFaktor()) * intersectionDimensionWeight;
        calc.append(Math.round(Math.min(1.0, getWidthFaktor()) * intersectionDimensionWeight * 100.0 / t));
        calc.append("%          + ");
        calc.append(Math.min(1.0, getWidthFaktor()) * intersectionDimensionWeight);
        calc.append('=');
        calc.append(tmpError);
        calc.append(" (BWidthFaktor)\r\n");
        tmpError += Math.min(1.0, getIntersectionAHeightFaktor()) * intersectionAHeightWeight;
        calc.append(Math.round(Math.min(1.0, getIntersectionAHeightFaktor()) * intersectionAHeightWeight * 100.0 / t));
        calc.append("%          + ");
        calc.append(Math.min(1.0, getIntersectionAHeightFaktor()) * intersectionAHeightWeight);
        calc.append('=');
        calc.append(tmpError);
        calc.append(" (AHeightFaktor)\r\n");

        tmpError += Math.min(1.0, getIntersectionAWidthFaktor()) * intersectionAWidthWeight;
        calc.append(Math.round(Math.min(1.0, getIntersectionAWidthFaktor()) * intersectionAWidthWeight * 100.0 / t));
        calc.append("%          + ");
        calc.append(Math.min(1.0, getIntersectionAWidthFaktor()) * intersectionAWidthWeight);
        calc.append('=');
        calc.append(tmpError);
        calc.append(" (AWidthFaktor)\r\n");
        tmpError += (getBothElementsNum() - 1) * cleftFaktor;
        calc.append(Math.round((getBothElementsNum() - 1) * cleftFaktor * 100.0 / t));
        calc.append("%          + ");
        calc.append((getBothElementsNum() - 1) * cleftFaktor);
        calc.append('=');
        calc.append(tmpError);
        calc.append(" (CleftFaktor)\r\n");
        tmpError += getExtensionError();
        calc.append(Math.round(getExtensionError() * 100.0 / t));
        calc.append("%          + ");
        calc.append(getExtensionError());
        calc.append('=');
        calc.append(tmpError);
        calc.append(" (ExtensionError)\r\n");

        tmpError /= divider;
        calc.append('/');
        calc.append(divider);
        calc.append(new char[] { ' ', '=', ' ' });
        calc.append(tmpError);
        calc.append(" => ");
        calc.append(tmpError * 100);
        calc.append(new char[] { '%', ' ' });
        // tmpError = Math.min(1.0, tmpError);

        hs.put("totalFaktor", tmpError);
        return tmpError * 100 + "% " + hs.toString() + "\r\n" + calc.toString();

    }

}
