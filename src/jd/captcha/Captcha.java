package jd.captcha;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Vector;
import java.util.logging.Logger;

import com.sun.image.codec.jpeg.ImageFormatException;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

/**
 * Diese Klasse behinhaltet Methoden zum verarbeiten von Captchas. Also Grafiken die Mehr als ein Element enthalten
 * 
 * @author coalado
 */
public class Captcha extends PixelGrid {

    /**
     * Speichert die Positiond es letzten erkannten Letters
     */
    private int       lastletterX = 0;

    /**
     * Speichert die Information ob der captcha schon vorverarbeitet wurde
     */
    private boolean   prepared    = false;

    /**
     * Falls der captcha schond decoded wurde, werden hier die gefundenen
     * letters abgelegt
     */
    private Letter[]  decodedLetters;
    /**
     * Temp Array für die getrennten letters; *
     */
    private Letter[]  seperatedLetters;
    /**
     * Wert der angibt mit welcher Sicherheit der capture encoded wurde
     */
    private int       valityValue;

    /**
     * Array der länge getWidth()+1. hier werden gefundene Gaps abgelegt.
     * Einträge mit true bedeuten eine Lücke
     */
    private boolean[] gaps;
    /**
     * Speichert das original RGB Pixelgrid
     */
    public int[][]    rgbGrid;
    private static Logger     logger = UTILITIES.getLogger();
    private ColorModel colorModel;

    /**
     * Diese Klasse beinhaltet ein 2D-Pixel-Grid. Sie stellt mehrere Methoden
     * zur verfügung dieses Grid zu bearbeiten Um Grunde sind hier alle Methoden
     * zu finden um ein captcha als ganzes zu bearbeiten
     * 
     * @param width 
     * @param height 
     */

    public Captcha(int width, int height) {
        super(width, height);
        rgbGrid = new int[width][height];

    }

    /**
     * Gibt die Breite des internen captchagrids zurück
     * @return breite
     */
    public int getWidth() {
        return grid.length;
    }

    /**
     * Gibt die Höhe des internen captchagrids zurück
     * @return  Höhe
     */
    public int getHeight() {
        if (grid.length == 0)
            return 0;
        return grid[0].length;
    }

    /**
     * Nimmt ein int-array auf und wandelt es in das interne Grid um
     * @param pixel 
     */
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
     * KOnvertiert den Captcha gemäß dem neuen newColorFormat (Mix aus RGB oder hsb)
     * Als Ausgangsgrid dienen die originalFarben
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
        int[] avg = {0,0,0};
        int[] bv;
        int i = 0;
        int halfW = width / 2;
        int halfH = height / 2;
        if (width == 1 && px == 0)
            width = 2;
        if (height == 1 && py == 0)
            height = 2;
        for (int x = Math.max(0, px - halfW); x < Math.min(px + width - halfW, getWidth()); x++) {
            for (int y = Math.max(0, py - halfH); y < Math.min(py + height - halfH, getHeight()); y++) {
                if (mask.getPixelValue(x, y) > (getMaxPixelValue() * owner.getJas().getDouble("getBlackPercent"))) {
                    bv = UTILITIES.hexToRgb(getPixelValue(x, y));
                    avg[0]+=bv[0];
                    avg[1]+=bv[1];                
                    avg[2]+=bv[2];              
                    i++;

                }

            }
        }
        avg[0]/=i;
        avg[1]/=i;                
        avg[2]/=i;        
        return UTILITIES.rgbToHex(avg);
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

        if (mask.getWidth() != getWidth() || mask.getHeight() != getHeight()) {
            logger.info("ERROR Maske und Bild passen nicht zusammmen");
            return;
        }

        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                if (mask.getPixelValue(x, y) < (getMaxPixelValue() * owner.getJas().getDouble("getBlackPercent"))) {
                    newgrid[x][y] = getAverage(x, y, width, height, mask);

                } else {
                    newgrid[x][y] = getPixelValue(x, y);
                }
            }
        }
        grid = newgrid;

    }

    /**
     * Gibt einen vereifgachten captcha zurück. /gröber
     * 
     * @param faktor
     *            der vereinfachung
     * @return neuer captcha
     */
    public Captcha getSimplified(int faktor) {
        int newWidth = (int) Math.ceil(getWidth() / faktor);
        int newHeight = (int) Math.ceil(getHeight() / faktor);
        Captcha ret = new Captcha(newWidth, newHeight);
        int[][] newGrid = new int[newWidth][newHeight];
        int avg = getAverage();

        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                long v = 0;
                int values = 0;
                for (int gx = 0; gx < faktor; gx++) {
                    for (int gy = 0; gy < faktor; gy++) {
                        int newX = x * faktor + gx;
                        int newY = y * faktor + gy;
                        if (newX > getWidth() || newY > getHeight()) {
                            continue;
                        }
                        values++;
                        v += getPixelValue(newX, newY);
                    }
                }
                v /= values;

                PixelGrid.setPixelValue(x, y, newGrid, isElement((int) v, avg) ? 0 : (int) getMaxPixelValue(), owner);

            }
        }

        ret.setGrid(newGrid);

        ret.printGrid();

        return ret;
    }

    /**
     * Benutzt die Objekterkennung um alle Buchstaben zu finden
     * @param letterNum Anzahl der zu suchenen Buchstaben
     * @param contrast  Kontrast innerhalb der Elemente
     * @param objectContrast  Kontrast der Elemente zum Hintergrund
     * @param minArea MindestFläche eines Elements
     * @return Erkannte Buchstaben
     */
    public Letter[] getLetters(int letterNum, double contrast, double objectContrast, int minArea) {
        Vector<PixelObject> letters = getBiggestObjects(letterNum, minArea, contrast, objectContrast);
        if (letters == null)
            return null;
        this.gaps = new boolean[getWidth() + 1];
        Letter[] ret = new Letter[letterNum];
        for (int i = 0; i < letters.size(); i++) {

            PixelObject obj = letters.elementAt(i);

            Letter l = obj.toLetter();
            l.removeSmallObjects(owner.getJas().getDouble("ObjectColorContrast"), owner.getJas().getDouble("ObjectDetectionContrast"));
            owner.getJas().executeLetterPrepareCommands(l);
        //  if(owner.getJas().getInteger("leftAngle")!=0 || owner.getJas().getInteger("rightAngle")!=0)  l = l.align(owner.getJas().getDouble("ObjectDetectionContrast"),owner.getJas().getInteger("leftAngle"),owner.getJas().getInteger("rightAngle"));
           // l.reduceWhiteNoise(2);
            //l.toBlackAndWhite(0.6);
         
            ret[i] = l.getSimplified(this.owner.getJas().getInteger("simplifyFaktor"));
            
            this.gaps[letters.elementAt(i).getLocation()[0] + letters.elementAt(i).getWidth()] = true;
        }
        return ret;

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
        if (seperatedLetters != null)
            return seperatedLetters;

        if (owner.getJas().getBoolean("useObjectDetection")) {
            logger.finer("Use Object Detection");
            Letter[] ret = this.getLetters(letterNum, owner.getJas().getDouble("ObjectColorContrast"), owner.getJas().getDouble("ObjectDetectionContrast"), owner.getJas().getInteger("MinimumObjectArea"));
            if (ret != null) {
                seperatedLetters = ret;
                return ret;
            } else {
                logger.severe("Object detection failed. Try alternative Methods");
            }
        }

        if (!owner.getJas().getBoolean("UseAverageGapDetection") && !owner.getJas().getBoolean("UsePeakGapdetection") && owner.getJas().getGaps() != null) {

            logger.finer("Use predefined Gaps");
            return getLetters(letterNum, owner.getJas().getGaps());
        }
        logger.finer("Use Line Detection");
        this.gaps = new boolean[getWidth() + 1];
        Letter[] ret = new Letter[letterNum];
        lastletterX = 0;

        for (int letterId = 0; letterId < letterNum; letterId++) {
            ret[letterId] = getNextLetter(letterId);

            if (ret[letterId] == null) {
                if (owner.getJas().getGaps() != null) {
                    return getLetters(letterNum, owner.getJas().getGaps());
                } else {
                    return null;
                }
                // ret[letterId]= ret[letterId].getSimplified(SIMPLIFYFAKTOR);
            } else {
                owner.getJas().executeLetterPrepareCommands(ret[letterId]);
               // if(owner.getJas().getInteger("leftAngle")!=0 || owner.getJas().getInteger("rightAngle")!=0)  ret[letterId] = ret[letterId].align( owner.getJas().getDouble("ObjectDetectionContrast"),owner.getJas().getInteger("leftAngle"),owner.getJas().getInteger("rightAngle"));
                
                ret[letterId] = ret[letterId].getSimplified(this.owner.getJas().getInteger("simplifyFaktor"));
               
            }

        }
        seperatedLetters = ret;
        return ret;
    }

    /**
     * Gibt die Buchstaben zurück. Trennkriterium ist das Array gaps in dem die Trenn-X-Wert abgelegt sind
     * @param letterNum
     * @param gaps
     * @return Erkannte Buchstaben
     */
    public Letter[] getLetters(int letterNum, int[] gaps) {

        if (seperatedLetters != null) {
            return seperatedLetters;
        }

        Letter[] ret = new Letter[letterNum];
        lastletterX = 0;
        this.gaps = new boolean[getWidth() + 1];
        for (int letterId = 0; letterId < letterNum; letterId++) {
            ret[letterId] = getNextLetter(letterId, gaps);

            if (ret[letterId] == null) {

                return null;
                // ret[letterId]= ret[letterId].getSimplified(SIMPLIFYFAKTOR);
            } else {
               // if(owner.getJas().getInteger("leftAngle")!=0 || owner.getJas().getInteger("rightAngle")!=0)  ret[letterId] = ret[letterId].align( owner.getJas().getDouble("ObjectDetectionContrast"),owner.getJas().getInteger("leftAngle"),owner.getJas().getInteger("rightAngle"));
                owner.getJas().executeLetterPrepareCommands(ret[letterId]);
                ret[letterId] = ret[letterId].getSimplified(this.owner.getJas().getInteger("simplifyFaktor"));
                
            }

        }
        seperatedLetters = ret;
        return ret;
    }

    /**
     * Gibt in prozent zurück wie sicher die erkennung war (0. top sicher 100
     * schlecht)
     * 
     * @return int validprozent
     */
    public int getValityPercent() {
        if (this.valityValue < 0)
            return 100;
        return (int) ((100.0 * (double) this.valityValue) / (double) this.getMaxPixelValue());
    }

    /**
     * Sucht angefangen bei der aktullen Positiond en ncähsten letter und gibt
     * ihn zurück
     * 
     * @param letterId
     *            Id des Letters (0-letterNum-1)
     * @return Letter gefundener Letter
     */
    private Letter getNextLetter(int letterId) {
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
                        rowAverage[x] = UTILITIES.mixColors(rowAverage[x],getPixelValue(x + line, y),count,1);
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
                    isAverageGap = rowAverage[x] > (average * owner.getJas().getDouble("GapDetectionAverageContrast")) || !owner.getJas().getBoolean("UseAverageGapDetection");

                    isOverPeak = rowPeak[x] < (average * owner.getJas().getDouble("GapDetectionPeakContrast"));
                   isPeakGap = (lastOverPeak && !isOverPeak) || !owner.getJas().getBoolean("UsePeakGapdetection");
                    
                    isGap = isAverageGap && isPeakGap;

                } else {
                    isAverageGap = rowAverage[x] > (average * owner.getJas().getDouble("GapDetectionAverageContrast")) && owner.getJas().getBoolean("UseAverageGapDetection");
                    isOverPeak = rowPeak[x] < (average * owner.getJas().getDouble("GapDetectionPeakContrast"));
                    isPeakGap = (lastOverPeak && !isOverPeak) || !owner.getJas().getBoolean("UsePeakGapdetection");
                    isGap = isAverageGap || isPeakGap;
                }
                lastOverPeak = isOverPeak;

                if (isGap && noGapCount > owner.getJas().getInteger("MinimumLetterWidth")) {
                    break;
                } else if (rowAverage[x] < (average * owner.getJas().getDouble("GapDetectionAverageContrast"))) {

                    noGapCount++;
                }
            

        }

        ret.setGrid(letterGrid);

        if (!ret.trim(lastletterX, x))
            return null;

        if (!ret.clean())
            return null;

        lastletterX = x;

        gaps[Math.min(lastletterX, getWidth())] = true;
        return ret;
    }

    /**
     * Alternativ Methode über das gaps array.
     * 
     * @param letterId
     * @param gaps
     * @return Nächster Buchstabe in der Reihe
     */
    private Letter getNextLetter(int letterId, int[] gaps) {
        Letter ret = createLetter();

        int nextGap = -1;
        if (gaps != null && gaps.length > letterId) {
            nextGap = gaps[letterId];
        }
        if (gaps==null || gaps.length==0) {
            logger.severe("Das Gaps Array wurde nicht erstellt");
        }
        if (letterId> (gaps.length-1)) {
            logger.severe("LetterNum und Gaps Array passen nicht zusammen. Siemüssen die selbe Länge haben!");
        }
        if (letterId > 0 && nextGap <= gaps[letterId - 1]) {
            logger.severe(letterId+" Das Userdefinierte gaps array ist falsch!. Die Gaps müssen aufsteigend sortiert sein!");
        }
        int[][] letterGrid = new int[getWidth()][getHeight()];
        int x;
       
        for (x = lastletterX; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                letterGrid[x][y] = getPixelValue(x, y);
            }
            if (nextGap == x) {
                break;
            }        }

        ret.setGrid(letterGrid);

        if (!ret.trim(lastletterX, x))
            return null;

        if (!ret.clean())
            return null;

        lastletterX = x;

        this.gaps[Math.min(lastletterX, getWidth())] = true;
        return ret;
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
     * Speichert den captcha als Bild mit Trennstrichen ab
     * 
     * @param file .
     *            ZielPfad
     */
    public void saveImageasJpgWithGaps(File file) {
        BufferedImage bimg = null;

        bimg = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        bimg.setRGB(0, 0, getWidth(), getHeight(), getPixelWithGaps(), 0, getWidth());

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
                if (gaps[x] == true)
                    pix[pixel] = 0;
                pixel++;
            }
        }
        return pix;
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
                if (gaps[x] == true)
                    pix[x][y] = 0;

            }
        }
        return pix;
    }

    /**
     * Gibt das Pixel Array zurück.
     * @return Pixel Array
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
    public void printCaptcha() {
        logger.info("\r\n" + getString());
    }

    /**
     * Gibt einen ASCII String des Bildes zurück
     * @return ASCII Bild
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
     * factory Methode für eine captchainstanz
     * @param image 
     * @param owner 
     * @return neuer Captcha
     */
    public static Captcha getCaptcha(Image image, JAntiCaptcha owner) {

        int width = image.getWidth(null);
        int height = image.getHeight(null);
        if (width <= 0 || height <= 0) {
            logger.severe("ERROR: Image nicht korrekt. Kein Inhalt. Pfad URl angaben Korrigieren");
        }
        PixelGrabber pg = new PixelGrabber(image, 0, 0, width, height, false);
        
        try {
            pg.grabPixels();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Captcha ret = new Captcha(width, height);
        ret.setOwner(owner);
        logger.fine(width+"/"+height);
        
        ret.setColorModel(pg.getColorModel());
        ColorModel cm=pg.getColorModel();
        
        if (!(cm instanceof IndexColorModel))
        {
          // not an indexed file (ie: not a gif file)
            ret.setPixel((int[])pg.getPixels());
        }
        else
        {
          
;           //UTILITIES.trace("COLORS: "+numColors);
ret.setPixel((byte[])pg.getPixels());
        }
 
        //BasicWindow.showImage(ret.getImage());
       
        return ret;

    }

    void setPixel(byte[] bpixel) {
        this.pixel = new int[bpixel.length];
        int i = 0;
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                // grid[x][y] = pixel[i++];
               this.pixel[i]=((IndexColorModel)colorModel).getRGB(bpixel[i]);
           
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

    void setColorModel(ColorModel colorModel) {
       this.colorModel=colorModel;
        
    }

    /**
     * Captcha.getCaptcha(Letter a, Letter b) Gibt einen captcha zurück der aus
     * a +6pxTrennlinie +b besteht. (Addiert 2 Letter)
     * @param a 
     * @param b 
     * @return Neuer captcha
     */
    public static Captcha getCaptcha(Letter a, Letter b) {

        int newWidth = (a.getWidth() + b.getWidth() + 6);
        int newHeight = Math.max(a.getHeight(), b.getHeight());
        Captcha ret = new Captcha(newWidth, newHeight);
        if (a.owner != null)
            ret.setOwner(a.owner);
        if (ret.owner == null)
            ret.setOwner(b.owner);
        if (ret.owner == null)
            logger.warning("Owner konnte nicht bestimmt werden!Dieser captcha ist nur eingeschränkt verwendbar.");
        ret.grid = new int[newWidth][newHeight];
        for (int x = 0; x < a.getWidth(); x++) {
            for (int y = 0; y < newHeight; y++) {
                ret.grid[x][y] = y < a.getHeight() ? a.grid[x][y] : (int) a.getMaxPixelValue();

            }
        }
        for (int x = a.getWidth(); x < a.getWidth() + 6; x++) {
            for (int y = 0; y < newHeight; y++) {
                ret.grid[x][y] = (x == a.getWidth() + 2 || x == a.getWidth() + 3) ? 0 : (int) a.getMaxPixelValue();

            }
        }

        for (int x = a.getWidth() + 6; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                ret.grid[x][y] = y < b.getHeight() ? b.grid[x - (a.getWidth() + 6)][y] : (int) b.getMaxPixelValue();

            }
        }
        return ret;

    }

    /**
     * Factory Funktion gibt einen captcha von File zurück
     * 
     * @param file
     *            Pfad zum bild
     * @param owner
     *            Parameter Dump JAntiCaptcha
     * @return Captcha neuer captcha
     */
    public static Captcha getCaptcha(File file, JAntiCaptcha owner) {
        Image img = UTILITIES.loadImage(file);
        Captcha ret = Captcha.getCaptcha(img, owner);
        return ret;

    }

    /**
     * Setter Methode für die decoded Letters
     * 
     * @param newLetters
     */
    public void setDecodedLetters(Letter[] newLetters) {
        this.decodedLetters = newLetters;

    }

    /**
     * Setter methode
     * 
     * @param value
     */
    public void setValityValue(int value) {
        this.valityValue = value;
    }

    /**
     * @return the prepared
     */
    public boolean isPrepared() {
        return prepared;
    }

    /**
     * @param prepared
     *            the prepared to set
     */
    public void setPrepared(boolean prepared) {
        this.prepared = prepared;
    }

    /**
     * Diese Funktion gibt die erkannten buchstaben zurück. Wichtig: Es handelt
     * sich um die letter aus der Db. Es sind nicht!!! die wirlichen Letter des
     * captchas. Die wirklichen letter gibt captcha.getLetters(letterNum) zurück
     * 
     * @return the decodedLetters
     */
    public Letter[] getDecodedLetters() {
        return decodedLetters;
    }

    /**
     * @return the valityValue
     */
    public int getValityValue() {
        return valityValue;
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
    
    @SuppressWarnings("unchecked")
    public Vector<PixelObject> getBiggestObjects(int letterNum, int minArea, double contrast, double objectContrast) {
        Vector<PixelObject> objects = getObjects(contrast, objectContrast);
        int found = 0;
        int i = 0;
        int minWidth = Integer.MAX_VALUE;
        while (i < objects.size() && objects.elementAt(i++).getArea() > minArea) {
            
           
            found++;
        }
        int maxWidth;
       
        Vector<PixelObject> splitObjects;
        logger.fine("found " + found);
        // Teil die größten Objekte bis man die richtige anzahl an lettern hat
        while (objects.size() > 0 && found < letterNum) {
            PixelObject po = objects.remove(0);
            found--;
            maxWidth = po.getWidth();
            minWidth = minArea/po.getHeight();
            int splitter = 1;
            int splitNum;
logger.info(maxWidth+"/"+minWidth);
            while ((splitNum = Math.min((int) Math.ceil((double) maxWidth / ((double) minWidth / (double) splitter)), letterNum - found)) < 2) {
                splitter++;
            }
            while ((found + splitNum) > letterNum)
                splitNum--;
            logger.finer("teile erstes element " + po.getWidth() + " : splitnum " + splitNum);
            if ((found + splitNum - 1) > letterNum || splitNum < 2) {
                logger.severe("Richtige Letteranzahl 1 konnte nicht ermittelt werden");
                return null;
            }

            // found += splitNum - 1;

            splitObjects = po.split(splitNum);
            logger.finer("Got spliited: " + splitObjects.size());
            for (int t = 0; t < splitNum; t++) {

                for (int s = 0; s < objects.size(); s++) {
                    if (splitObjects.elementAt(t).getArea() > objects.elementAt(s).getArea()) {
                        objects.add(s, splitObjects.elementAt(t));
                        splitObjects.setElementAt(null, t);
                        found++;
                        logger.finer("add split " + found);

                        break;
                    }
                   
                }
                if(splitObjects.elementAt(t)!=null){
                    objects.add(splitObjects.elementAt(t));
                    splitObjects.setElementAt(null, t);
                    found++;
                    logger.finer("add split " + found);
                }

            }
            logger.finer("splitted ... treffer: " + found);

        }
        if (found != letterNum) {
            logger.severe("Richtige Letteranzahl 2 konnte nicht ermittelt werden");
            return null;
        }
        // entfernt Überflüssige Objekte und
        for (int ii = objects.size() - 1; ii >= found; ii--) {
            objects.remove(ii);
        }
        Collections.sort(objects);
        logger.finer("Found "+objects.size()+" Elements");
        return objects;
    }

    /**
     * Fügt 2 Capchtas zusammen. Schwarze stellen werden dabei ignoriert.
     * @param tmp
     */
    public void concat(Captcha tmp) {
        if(this.getWidth()!=tmp.getWidth() ||this.getHeight()!=tmp.getHeight()){
            logger.severe("Concat fehlgeschlagen Dimensions nicht gleich");
            return;
        }
        

        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                if(PixelGrid.getPixelValue(x, y, tmp.grid, owner)>owner.getJas().getDouble("getBlackPercent")*getMaxPixelValue()){
                    
            
         int newPixelValue=UTILITIES.mixColors(getPixelValue(x,y), PixelGrid.getPixelValue(x, y, tmp.grid, owner));
         setPixelValue(x,y,newPixelValue);
                }
            }
        }
        
    }

}