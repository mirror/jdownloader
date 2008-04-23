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

package jd.captcha;

import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;

import jd.captcha.configuration.JACScript;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.utils.UTILITIES;

/**
 * Diese Klasse berechnet die Unterschiede zwischen zwei letter instanzen. Über
 * die getter kann nach dem ausführen von run(). Abgerufen werden welche
 * UNterschiede sich ergeben haben.
 * 
 * @author JD-Team
 */
public class LetterComperator {
    // Ids die Auskunft über die Art der ERkennung geben
    /**
     * Detection IDS Erkennung durch einen Perfectmatch schwellwert
     */
    public static final int PERFECTMATCH = 1;
    /**
     * Detection IDS Keine ERkennung. fehler!
     */
    public static final int ERROR = -1;
    /**
     * Detection IDS Erkennung durch den quickscan
     */
    public static final int QUICKSCANMATCH = 2;
    /**
     * Detection IDS Perfect Match durch den Quickscan
     */
    public static final int QUICKSCANPERFECTMATCH = 3;
    // Farbkonstanten für die überlagerungsbilder
    private static final int BOTHCOLOR = 0x660099;
    private static final int BNACOLOR = 0x0000ff;
    private static final int ANBCOLOR = 0xff0000;
    private static final int BNAFILTEREDCOLOR = 0xccccff;
    private static final int ANBFILTEREDCOLOR = 0xffcccc;
    // Buchstaben a und b. a ist das captachbild und b das datenbankbild
    private Letter a = null;
    private Letter b = null;

    private double valityPercent = 100.0;
    private JAntiCaptcha owner;
    private JACScript jas;
    /**
     * Gibt an ob ein Intersectionletter( schnittbilder) erstellt werden soll.
     * Achtung langsam!
     */
    public static boolean CREATEINTERSECTIONLETTER = false;
    private double pixelErrorA = 0;
    private double pixelErrorB = 0;
    private double tmpErrorA;
    private double tmpErrorB;
    private double errorAWeight;
    private double errorbWeight;
    private int tmpPixelBButNotA;
    private int tmpPixelAButNotB;
    private int pixelANotB = 0;
    private int pixelBNotA = 0;
    private double coverageFaktorA = 0;
    private double coverageFaktorB = 0;
    private double tmpCoverageFaktorA;
    private double tmpCoverageFaktorB;
    private double coverageFaktorAWeight;
    private double coverageFaktorBWeight;
    private int tmpPixelBoth = 0;
    private int pixelBoth = 0;
    private int pixelAll;
    private double totalPixelError = 0;
    private double widthFaktor = 0;
    private double heightFaktor = 0;
    private double tmpHeightFaktor;
    private double tmpWidthFaktor;
    private double localHeightPercent;
    private double localWidthPercent;
    private int intersectionStartX = 0;
    private int intersectionStartY = 0;
    private int intersectionWidth = 0;
    private int intersectionHeight = 0;
    private Letter intersectionLetter = new Letter();
    private int[][] intersectionGrid = new int[0][0];
    private double intersectionDimensionWeight;
    private int overlayNoiseSize;
    private double reliability;
    @SuppressWarnings("unused")
    private Logger logger = UTILITIES.getLogger();
    private int detectionType;
    private int bothElementsNum;
    private double cleftFaktor;
    Vector<Integer> element;
    Vector<Vector<Integer>> bothElements = new Vector<Vector<Integer>>();
    int minCleftSize;
    private int[][] elementGrid;
    private int scanVarianceX = -1;
    private int scanVarianceY = -1;
    private int scanTime = -1;
    private double tmpErrorTotal;

    private int preScanFaktor;
    private int preScanFilter;

    private double tmpPreScanValue;
    private double preValityPercent;
    private int[] offset;
    private int[] imgOffset;
    private int[] intersectionDimension;
    private int[] bc= new int[2];

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
     * Scan ist die eigentliche vergleichsfunktion. a und b werden dabei
     * gegeneinander verschoben.
     */
    private void scan() {
        long startTime = UTILITIES.getTimer();
        double bestValue = 101.0;
      
           // logger.info(b.getDecodedValue()+"----");
            
       
        // scanvarianzen geben an wieviel beim verschieben über die grenzen
        // geschoben wird. große werte brauchen CPU
        int vx = this.getScanVarianceX();
        int vy = this.getScanVarianceY();
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
        Letter tmpIntersection=null;
        if (isCreateIntersectionLetter()) {
            tmpIntersection= new Letter();
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
        for (int xx = UTILITIES.getJumperStart(scanXFrom, scanXTo); UTILITIES.checkJumper(xx, scanXFrom, scanXTo); xx = UTILITIES.nextJump(xx, scanXFrom, scanXTo, 1)) {
            for (int yy = UTILITIES.getJumperStart(scanYFrom, scanYTo); UTILITIES.checkJumper(yy, scanYFrom, scanYTo); yy = UTILITIES.nextJump(yy, scanYFrom, scanYTo, 1)) {
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
                if (this.preScanFilter < 100) {
                    tmpPreScanValue = this.scanPreIntersection(xx, yy, left, top, tmpIntersectionWidth, tmpIntersectionHeight);
                    // logger.info("_"+preScan);
                    if ((int) tmpPreScanValue > this.preScanFilter) {

                        continue;
                    }

                }
                // logger.info("Scan
                // "+tmpIntersectionWidth+"/"+tmpIntersectionHeight+" -
                // "+a.getElementPixel());

                value = this.scanIntersection(xx, yy, left, top, tmpIntersectionWidth, tmpIntersectionHeight);
                // logger.info(tmpIntersectionWidth+"/"+tmpIntersectionHeight+"
                // : "+" scanIntersection: ");
                // logger.info(" : "+value);
//                if(getDecodedValue().equalsIgnoreCase("v")&&getBothElementsNum()==3){
//                    logger.info("JJJ");
//                }
                if (value < bestValue) {
                   
                    bestValue = value;
                    this.offset = new int[] { left, top };
                    this.imgOffset = new int[] { xx, yy };
                    this.intersectionDimension = new int[] { tmpIntersectionWidth, tmpIntersectionHeight };
                    this.setValityPercent(bestValue);
                    this.setHeightFaktor(tmpHeightFaktor);
                    this.setWidthFaktor(tmpWidthFaktor);
                    this.setIntersectionHeight(tmpIntersectionHeight);
                    this.setIntersectionWidth(tmpIntersectionWidth);
                    this.setIntersectionStartX(xx);
                    this.setIntersectionStartY(yy);
                    this.setBothElementNum(bothElements.size());
                    this.setCoverageFaktorA(tmpCoverageFaktorA);
                    this.setCoverageFaktorB(tmpCoverageFaktorB);
                    this.setPixelErrorA(tmpErrorA);
                    this.setPreValityPercent(tmpPreScanValue);
                    this.setPixelErrorB(tmpErrorB);
                    this.setPixelANotB(tmpPixelAButNotB);
                    this.setPixelBNotA(tmpPixelBButNotA);
                    this.setPixelBoth(tmpPixelBoth);

                    this.setTotalPixelError(tmpErrorTotal);
                    if (isCreateIntersectionLetter()) {
                        this.setIntersectionLetter(tmpIntersection);

                    }

                }
            }
        }
        // }

        this.scanTime = (int) (UTILITIES.getTimer() - startTime);

    }

    private void setValityPercent(double bestValue) {
        this.valityPercent = bestValue;

    }

    private double scanPreIntersection(int xx, int yy, int left, int top, int tmpIntersectionWidth, int tmpIntersectionHeight) {
        double tmpError;
        int yStep = tmpIntersectionHeight / (preScanFaktor + 1);

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
        // logger.info((UTILITIES.getTimer() - startTime)+ "intersection
        // scantime: "+(UTILITIES.getTimer()-starter2));
        if (pixelAll > 0) {
            tmpErrorA = (double) tmpPixelAButNotB / (double) (tmpPixelBoth + tmpPixelAButNotB);
            tmpErrorB = (double) tmpPixelBButNotA / (double) (tmpPixelBButNotA + tmpPixelBoth);
            tmpErrorTotal = tmpErrorA * errorAWeight + tmpErrorB * errorbWeight;
            localHeightPercent = (double) tmpIntersectionHeight / (double) b.getHeight();
            localWidthPercent = (double) tmpIntersectionWidth / (double) b.getWidth();
            tmpHeightFaktor = Math.pow(1.0 - localHeightPercent, 2);
            tmpWidthFaktor = Math.pow(1.0 - localWidthPercent, 2);
            tmpError = tmpErrorTotal;
            tmpError += Math.min(1.0, tmpHeightFaktor) * intersectionDimensionWeight;
            tmpError += Math.min(1.0, tmpWidthFaktor) * intersectionDimensionWeight;
            tmpError /= 4.0;
            tmpError *= 1.2;
            tmpError = Math.min(1.0, tmpError);
            return (int) (100 * tmpError);
        } else {
            return 100;
        }
    }

    private double scanIntersection(int xx, int yy, int left, int top, int tmpIntersectionWidth, int tmpIntersectionHeight) {

        double tmpError;
        pixelAll = 0;
        tmpPixelBButNotA = 0;
        tmpPixelAButNotB = 0;
        tmpPixelBoth = 0;
        tmpCoverageFaktorA = 0;
        tmpCoverageFaktorB = 0;
        // long starter=UTILITIES.getTimer();
        bothElements.removeAllElements();
        elementGrid = new int[tmpIntersectionWidth][tmpIntersectionHeight];

        for (int x = 0; x < tmpIntersectionWidth; x++) {
            for (int y = 0; y < tmpIntersectionHeight; y++) {

                int pixelType = getPixelType(x, y, xx, yy, left, top);
                pixelAll++;

                switch (pixelType) {
                case 0:
                    if (isCreateIntersectionLetter()) intersectionGrid[x][y] = BOTHCOLOR;

                    getElement(x, y, xx, yy, left, top, pixelType, elementGrid, element = new Vector<Integer>());
                    if (element.size() > minCleftSize) bothElements.add(element);

                    tmpPixelBoth++;
                    break;
                case 1:
                    if (hasNeighbour(x, y, xx, yy, left, top, pixelType) > overlayNoiseSize) {
                        tmpPixelBButNotA++;
                        if (isCreateIntersectionLetter()) intersectionGrid[x][y] = BNACOLOR;
                    } else {
                        if (isCreateIntersectionLetter()) intersectionGrid[x][y] = BNAFILTEREDCOLOR;
                    }
                    break;
                case 2:
                    if (hasNeighbour(x, y, xx, yy, left, top, pixelType) > overlayNoiseSize) {
                        tmpPixelAButNotB++;
                        if (isCreateIntersectionLetter()) intersectionGrid[x][y] = ANBCOLOR;
                    } else {

                        if (isCreateIntersectionLetter()) intersectionGrid[x][y] = ANBFILTEREDCOLOR;
                    }
                    break;
                default:
                    if (isCreateIntersectionLetter()) intersectionGrid[x][y] = 0xffffff;
                }
            }
        }

        // logger.info("Scanner: "+UTILITIES.getTimer(starter));
//        if(getDecodedValue().equalsIgnoreCase("v")&&getBothElementsNum()==3){
//            logger.info("JJJ");
//        }
        if (pixelAll > 0 && bothElements.size() > 0) {
            tmpErrorA = (double) tmpPixelAButNotB / (double) (tmpPixelBoth + tmpPixelAButNotB);
            tmpErrorB = (double) tmpPixelBButNotA / (double) (tmpPixelBButNotA + tmpPixelBoth);
            tmpErrorTotal = tmpErrorA * errorAWeight + tmpErrorB * errorbWeight;
            tmpCoverageFaktorA = 1.0 - ((double) tmpPixelBoth / (double) a.getElementPixel());
            tmpCoverageFaktorB = 1.0 - ((double) tmpPixelBoth / (double) b.getElementPixel());
            localHeightPercent = (double) tmpIntersectionHeight / (double) b.getHeight();
            localWidthPercent = (double) tmpIntersectionWidth / (double) b.getWidth();

            tmpHeightFaktor = Math.pow(1.0 - localHeightPercent, 2);
            tmpWidthFaktor = Math.pow(1.0 - localWidthPercent, 2);
            // logger.info(tmpIntersectionWidth+ "/"+a.getWidth()+" =
            // "+localWidthPercent+" --> "+tmpWidthFaktor);

            tmpError = tmpErrorTotal;
            tmpError += Math.min(1.0, tmpCoverageFaktorA) * coverageFaktorAWeight;
            tmpError += Math.min(1.0, tmpCoverageFaktorB) * coverageFaktorBWeight;
            tmpError += Math.min(1.0, tmpHeightFaktor) * intersectionDimensionWeight;
            tmpError += Math.min(1.0, tmpWidthFaktor) * intersectionDimensionWeight;
            tmpError += (bothElements.size() - 1) * cleftFaktor;
            tmpError /= 6.0;
            tmpError = Math.min(1.0, tmpError);
            // logger.info(pixelBoth+"_"+(tmpIntersectionHeight *
            // tmpIntersectionWidth));
            if ((tmpPixelBoth * owner.getJas().getDouble("inverseFontWeight")) < (tmpIntersectionHeight * tmpIntersectionWidth)) {
                tmpError = tmpErrorA = tmpErrorB = tmpErrorTotal = 1.0;
            }

            return 100.0 * tmpError;
        } else {

            return 100.0;
        }
    }

    private void setBothElementNum(int i) {
        this.bothElementsNum = i;

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
        if (x < 0 || y < 0 || x >= elementGrid.length || elementGrid.length == 0 || y >= elementGrid[0].length) return null;
        if (elementGrid[x][y] != 0) return null;
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
    private int hasNeighbour(int x, int y, int xx, int yy, int left, int top, int pixelType) {

        int ret = 0;
        int faktor = 1;
        for (int xt = -faktor; xt <= faktor; xt++) {
            for (int yt = -faktor; yt <= faktor; yt++) {
                if (xt == 0 && yt == 0) continue;
                if (getPixelType(x + xt, y + yt, xx, yy, left, top) == pixelType) ret++;
                ;

            }
        }
        return ret;
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
    private int[] coordinatesFromAToB(int x, int y, int xx, int yy,int[] con) {
        con[0]= x - xx;
        con[1]=y - yy ;
        return con;
    }

    public Letter getDifference() {
        try{
        int xx = this.imgOffset[0];
        int yy = this.imgOffset[1];
        int left = this.offset[0];
        int top = this.offset[1];
        int tmpIntersectionWidth = this.intersectionDimension[0];
        int tmpIntersectionHeight = this.intersectionDimension[1];
       

        // long starter=UTILITIES.getTimer();

        int[][] g = new int[tmpIntersectionWidth][tmpIntersectionHeight];

        for (int x = 0; x < tmpIntersectionWidth; x++) {
            for (int y = 0; y < tmpIntersectionHeight; y++) {
                g[x][y] = getA().getMaxPixelValue();
                int pixelType = getPixelType(x, y, xx, yy, left, top);

                switch (pixelType) {
                // case 0:
                // if (isCreateIntersectionLetter()) intersectionGrid[x][y] =
                // BOTHCOLOR;
                //
                // getElement(x, y, xx, yy, left, top, pixelType, elementGrid,
                // element = new Vector<Integer>());
                // if (element.size() > minCleftSize) bothElements.add(element);
                //
                // tmpPixelBoth++;
                // break;
                case 1:
                    if (hasNeighbour(x, y, xx, yy, left, top, pixelType) > overlayNoiseSize) {

                      //  g[x][y] = 0xff0000;
                    } else {
                      //  g[x][y] = 0xff0000;
                    }
                    break;
                case 2:
                    if (hasNeighbour(x, y, xx, yy, left, top, pixelType) > overlayNoiseSize) {

                        g[x][y] = 0;
                    } else {

                       // g[x][y] = 0x00ff00;
                    }
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
        }catch(Exception e){
            return getA();
        }

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
    private int getPixelType(int x, int y, int xx, int yy, int left, int top) {

        int va = a.getPixelValue(x + left, y + top);
        bc = coordinatesFromAToB(x + left, y + top, xx, yy,this.bc);
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
     * @return Prozentwert 0(gut) bis 100 (schlecht) der Übereinstimmung
     */
    public double getValityPercent() {
        return this.valityPercent;
    }

    /**
     * 
     * @return Das Prescan Ergebniss
     */
    public double getPreValityPercent() {
        return preValityPercent;
    }

    private void setPreValityPercent(double value) {
        this.preValityPercent = value;
    }

    /**
     * @param intersection
     */
    private void setIntersectionLetter(Letter intersection) {

        this.intersectionLetter = intersection;

    }

    /**
     * @return gibt einen Letterzurück, aus dem gut erkannt werden kann wie sich
     *         das ergebniss zusammensetzt. gleiche antile werden lila gefärbt,
     *         banteile rot und a anteile blau Um den Intersectionletter
     *         auszugene muss zuerst setCreateIntersectionLetter(true)
     *         ausgeführt werden
     */
    public Letter getIntersectionLetter() {

        return this.intersectionLetter;
    }

    /**
     * @return Gibt den decoed value von b zurück
     */
    public String getDecodedValue() {
        if (b == null || b.getDecodedValue().length() != 1) return "-";
        return this.b.getDecodedValue();
    }

    /**
     * @return the a
     */
    public Letter getA() {
        return a;
    }

    /**
     * @param a
     *            the a to set
     */
    public void setA(Letter a) {
        this.a = a;
    }

    /**
     * @return the b
     */
    public Letter getB() {
        return b;
    }

    /**
     * @param b
     *            the b to set
     */
    public void setB(Letter b) {
        this.b = b;
    }

    /**
     * Führt den Vergleichsvorgang aus
     */
    public void run() {
        scan();

    }

    /**
     * Setzt den Owner (Parameterdump jas wird gelesen
     * 
     * @param owner
     */
    public void setOwner(JAntiCaptcha owner) {
        this.owner = owner;
        this.jas = owner.getJas();
        errorAWeight = jas.getDouble("errorAWeight");
        errorbWeight = jas.getDouble("errorBWeight");
        coverageFaktorAWeight = jas.getDouble("coverageFaktorAWeight");
        coverageFaktorBWeight = jas.getDouble("coverageFaktorBWeight");
        intersectionDimensionWeight = jas.getDouble("intersectionDimensionWeight");
        cleftFaktor = jas.getDouble("cleftFaktor");
        minCleftSize = jas.getInteger("minCleftSize");
        preScanFilter = jas.getInteger("preScanFilter");
        preScanFaktor = jas.getInteger("preScanFaktor");
        overlayNoiseSize = jas.getInteger("overlayNoiseSize");

    }

    /**
     * @return the createIntersectionLetter
     */
    public boolean isCreateIntersectionLetter() {
        return CREATEINTERSECTIONLETTER;
    }

    /**
     * @param createIntersectionLetter
     *            the createIntersectionLetter to set
     */
    public void setCreateIntersectionLetter(boolean createIntersectionLetter) {
        CREATEINTERSECTIONLETTER = createIntersectionLetter;
    }

    /**
     * @return the intersectionHeight
     */
    public int getIntersectionHeight() {
        return intersectionHeight;
    }

    /**
     * @param intersectionHeight
     *            the intersectionHeight to set
     */
    private void setIntersectionHeight(int intersectionHeight) {
        this.intersectionHeight = intersectionHeight;
    }

    /**
     * @return the intersectionStartX
     */
    public int getIntersectionStartX() {
        return intersectionStartX;
    }

    /**
     * @param intersectionStartX
     *            the intersectionStartX to set
     */
    private void setIntersectionStartX(int intersectionStartX) {
        this.intersectionStartX = intersectionStartX;
    }

    /**
     * @return the intersectionStartY
     */
    public int getIntersectionStartY() {
        return intersectionStartY;
    }

    /**
     * @param intersectionStartY
     *            the intersectionStartY to set
     */
    private void setIntersectionStartY(int intersectionStartY) {
        this.intersectionStartY = intersectionStartY;
    }

    /**
     * @return the intersectionWidth
     */
    public int getIntersectionWidth() {
        return intersectionWidth;
    }

    /**
     * @param intersectionWidth
     *            the intersectionWidth to set
     */
    private void setIntersectionWidth(int intersectionWidth) {
        this.intersectionWidth = intersectionWidth;
    }

    /**
     * @return the pixelErrorA
     */
    public double getPixelErrorA() {
        return pixelErrorA;
    }

    /**
     * @param pixelErrorA
     *            the pixelErrorA to set
     */
    private void setPixelErrorA(double pixelErrorA) {
        this.pixelErrorA = pixelErrorA;
    }

    /**
     * @return the totalPixelError
     */
    public double getTotalPixelError() {
        return totalPixelError;
    }

    /**
     * @param totalPixelError
     *            the totalPixelError to set
     */
    private void setTotalPixelError(double totalPixelError) {
        this.totalPixelError = totalPixelError;
    }

    /**
     * @return the pixelErrorB
     */
    public double getPixelErrorB() {
        return pixelErrorB;
    }

    /**
     * @param pixelErrorB
     *            the pixelErrorB to set
     */
    private void setPixelErrorB(double pixelErrorB) {
        this.pixelErrorB = pixelErrorB;
    }

    /**
     * Gibt einen laaaangenstring mit den meisten entscheidungsparametern
     * aus.Kann zum verlich verwendet werden
     * 
     * @return parameterstring
     */
    public String toString() {
        Hashtable<String, Object> hs = new Hashtable<String, Object>();
//if(getDecodedValue().equalsIgnoreCase("v")&&getBothElementsNum()==3){
//    logger.info("V");
//}
        hs.put("DecodedValue", getDecodedValue());
        hs.put("widthFaktor", this.getWidthFaktor() + "/" + (this.getWidthFaktor() * intersectionDimensionWeight));
        hs.put("heightFaktor", this.getHeightFaktor() + "/" + (this.getHeightFaktor() * intersectionDimensionWeight));
        hs.put("pixelErrorA", this.getPixelErrorA());
        hs.put("pixelErrorB", this.getPixelErrorB());
        hs.put("pixelErrorTotal", this.getTotalPixelError());
        hs.put("ValidtyPercent", this.getValityPercent());
        hs.put("pixelAButNotB", this.getPixelANotB());
        hs.put("pixelBButNotA", this.getPixelBNotA());
        hs.put("pixelBoth", this.getPixelBoth());
        hs.put("coverageA", this.getCoverageFaktorA() + "/" + (this.getCoverageFaktorA() * coverageFaktorAWeight));
        hs.put("coverageB", this.getCoverageFaktorB() + "/" + (this.getCoverageFaktorB() * coverageFaktorBWeight));
        hs.put("bothElements", getBothElementsNum());
        hs.put("preCompare", this.getPreValityPercent());

        double tmpError = this.getTotalPixelError();
        tmpError += Math.min(1.0, this.getCoverageFaktorA()) * coverageFaktorAWeight;
        tmpError += Math.min(1.0, this.getCoverageFaktorB()) * coverageFaktorBWeight;
        tmpError += Math.min(1.0, this.getHeightFaktor()) * intersectionDimensionWeight;
        tmpError += Math.min(1.0, this.getWidthFaktor()) * intersectionDimensionWeight;
        tmpError += (getBothElementsNum() - 1) * cleftFaktor;
        tmpError /= 6.0;
        tmpError = Math.min(1.0, tmpError);

        hs.put("totalFaktor", tmpError);
        return hs.toString();

    }

    /**
     * @return the pixelANotB
     */
    public int getPixelANotB() {
        return pixelANotB;
    }

    /**
     * @param pixelANotB
     *            the pixelANotB to set
     */
    private void setPixelANotB(int pixelANotB) {
        this.pixelANotB = pixelANotB;
    }

    /**
     * @return the pixelBNotA
     */
    public int getPixelBNotA() {
        return pixelBNotA;
    }

    /**
     * @param pixelBNotA
     *            the pixelBNotA to set
     */
    private void setPixelBNotA(int pixelBNotA) {
        this.pixelBNotA = pixelBNotA;
    }

    /**
     * @return the pixelBoth
     */
    public int getPixelBoth() {
        return pixelBoth;
    }

    /**
     * @param pixelBoth
     *            the pixelBoth to set
     */
    private void setPixelBoth(int pixelBoth) {
        this.pixelBoth = pixelBoth;
    }

    /**
     * @return the heightPercent
     */
    public double getHeightFaktor() {
        return heightFaktor;
    }

    /**
     * @param heightPercent
     *            the heightPercent to set
     */
    public void setHeightFaktor(double heightPercent) {
        this.heightFaktor = heightPercent;
    }

    /**
     * @return the widthPercent
     */
    public double getWidthFaktor() {
        return widthFaktor;
    }

    /**
     * @param widthPercent
     *            the widthPercent to set
     */
    public void setWidthFaktor(double widthPercent) {
        this.widthFaktor = widthPercent;
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
     * @param coverageFaktor
     *            the coverageFaktor to set
     */
    private void setCoverageFaktorA(double coverageFaktor) {
        this.coverageFaktorA = coverageFaktor;
    }

    private void setCoverageFaktorB(double coverageFaktor) {
        this.coverageFaktorB = coverageFaktor;
    }

    /**
     * TODO: Gleiche buchtaben als nächsten nachbarn ignoriere Setzte den
     * reliability wert. Er ist de Abstand zum nächstmöglichen Treffer. Und
     * sagtdeshalb etwas über die ERkennungswarscheinlichkeit aus
     * 
     * @param d
     */
    public void setReliability(double d) {
        this.reliability = d;

    }

    /**
     * 
     * @return ReliabilityValue. ABstand zum nächstbesten Buchstaben
     */
    public double getReliability() {
        return reliability;
    }

    /**
     * 
     * @return Kombinierter Wert aus Valityvalue und Reliability. Kann zur
     *         berechnung der erkennungssicherheit verwendet werden
     */
    public double getIdentificationReliability() {
        return getValityPercent() - getReliability();

    }

    /**
     * Setzt die art der ERkennung. Siehe Detection IDS
     * 
     * @param matchtype
     */
    public void setDetectionType(int matchtype) {
        this.detectionType = matchtype;
    }

    /**
     * 
     * @return Detection ID
     */
    public int getDetectionType() {
        return this.detectionType;
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

    /**
     * @return the scanVarianceX
     */
    public int getScanVarianceX() {
        if (scanVarianceX >= 0) return scanVarianceX;
        return jas.getInteger("scanVarianceX");
    }

    /**
     * @return the scanVarianceY
     */
    public int getScanVarianceY() {
        if (scanVarianceX >= 0) return scanVarianceY;
        return jas.getInteger("scanVarianceY");
    }

    /**
     * @return the scanTime
     */
    public int getScanTime() {
        return scanTime;
    }

    public int[] getOffset() {
        return offset;
    }

    public int[] getImgOffset() {
        return imgOffset;
    }

    public int[] getIntersectionDimension() {
        return intersectionDimension;
    }

}
