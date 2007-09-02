package jd.captcha.pixelgrid;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.logging.Logger;

import jd.captcha.JAntiCaptcha;
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
 * @author coalado
 */

public class PixelGrid {
    /**
     * Logger
     */
    public Logger       logger = UTILITIES.getLogger();

    /**
     * ParameterDump
     */
    public JAntiCaptcha owner;

    /**
     * Internes grid
     */
    public int[][]      grid;
 


    /**
     * Pixel Array
     */
    public int[]        pixel;
    /**
     * Gibt eine Prozentzahl aus. 0 = super 100= ganz schlimm
     * @param value
     * @param owner
     * @return Prozent der Erkennungssicherheit
     */
    public static int getValityPercent(int value, JAntiCaptcha owner){
        if(value<0){
            return 100;
        }
        return (int)((100.0*(double)value)/(double)getMaxPixelValue(owner));
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
    public void normalize(){
        normalize(1);
        
    }
    /**
     * Normalisiert Pixel und Multipliziert deren wert mit multi. Der Kontrast wird dabei künstlich erhöht bzw erniedrigt.
     * @param multi
     */
 public void normalize(double multi){
     normalize(multi,0,0);
 }
 
 /**
  * Normalisiert den Bereich zwischen cutMin und CutMax
 * @param multi
 * @param cutMax
 * @param cutMin
 */
public void normalize(double multi,double cutMax, double cutMin){
     int max=0;
     int min= Integer.MAX_VALUE;
     int akt;
     cutMin*=getMaxPixelValue();
     cutMax*=getMaxPixelValue();
     cutMax=getMaxPixelValue()-cutMax;
     for (int y = 0; y < getHeight(); y++) {
         for (int x = 0; x < getWidth(); x++) {
             akt= getPixelValue(x,y);
             if(akt<min && akt >cutMin)min=akt;
             if(akt>max && akt < cutMax)max=akt;
         }     }


Double faktor=(double)(max-min)/(double)getMaxPixelValue();
logger.fine(min+" <> "+max+" : "+faktor);
     for (int y = 0; y < getHeight(); y++) {
         for (int x = 0; x < getWidth(); x++) {
             akt= getPixelValue(x,y);
             if(akt<=cutMin){
                 setPixelValue(x,y,0);
                 continue;
             }
             if(akt>=cutMax){
                 setPixelValue(x,y,getMaxPixelValue());
                 continue;  
             }
             
             
             
             akt-=min;
             akt/=faktor;
             akt*=multi;
             //logger.fine(getPixelValue(x,y)+" = "+akt);
           akt=Math.min(akt, getMaxPixelValue());
           akt=Math.max(akt, 0);
             setPixelValue(x,y,(int)akt);
        
         }
     }
     
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
     * @return Höhe
     */
    public int getHeight() {
        if (grid.length == 0)
            return 0;
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
     * @param faktor
     * @return maxpixelvalue
     */
    public int getMaxPixelValue(double faktor) {
        return (int)((double)getMaxPixelValue(owner)*faktor);
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
        localGrid[x][y] = value;
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
        if(x<0||x>=grid.length)return -1;
        if(y<0||grid.length==0||y>=grid[0].length)return -1;
        return grid[x][y];       

    }

    /**
     * Gibt den Durchschnittlichen pixelwert des Bildes zurück
     * 
     * @return int
     */
    public int getAverage() {
        int[] avg = {0,0,0};
        int[] bv;
        int i = 0;

        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                // Nicht die Colormix Funktion verwenden!!! DIe gibt nur in zurück, das ist nicht ausreichend
                bv = UTILITIES.hexToRgb(getPixelValue(x, y));
                avg[0]+=bv[0];
                avg[1]+=bv[1];                
                avg[2]+=bv[2];              
                i++;

            }
        }
        if(i==0)return 0;
        avg[0]/=i;
        avg[1]/=i;                
        avg[2]/=i;        
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
                bv = UTILITIES.hexToRgb(getPixelValue(x, y));
                avg[0]+=bv[0];
                avg[1]+=bv[1];                
                avg[2]+=bv[2];              
                i++;

            }
        }

        avg[0]/=i;
        avg[1]/=i;                
        avg[2]/=i;        
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
                if (x != px || y != py) {

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
     * Verwendet die SampleDown Methode um ein reines Schwarzweißbild zu
     * erzeugen
     */
    public void toBlackAndWhite() {
        toBlackAndWhite(1);
    }

    /**
     * Erzeugt ein schwarzweiß bild
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
     * @param leftPadding
     * @param topPadding
     * @param rightPadding
     * @param bottomPadding
     */
    public void crop(int leftPadding, int topPadding, int rightPadding, int bottomPadding) {
        int newWidth = getWidth() - (leftPadding + rightPadding);
        int newHeight = getHeight() - (topPadding + bottomPadding);
        
        int[][] newGrid = new int[newWidth][newHeight];

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
                ret[x][y]=grid[x][y];
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
     * Entfernt Alle Pixel die über getBackgroundSampleCleanContrast an avg liegen
     * @param avg
     */
    public void cleanBackgroundByColor(int avg) {

        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                int dif = Math.abs(avg - getPixelValue(x, y));
             //  logger.info(getPixelValue(x, y)+"_");
 if (dif < (int)(getMaxPixelValue() * owner.getJas().getDouble("BackgroundSampleCleanContrast"))) {

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
            logger.severe("Dimensionen falsch: " + this.getDim());
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
            logger.severe("Dimensionen falsch: " + this.getDim());
            return null;
        }
        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                graphics.setColor(new Color(getPixelValue(x, y)==0?0:0xffffff));
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
            //logger.severe("Bild zu Klein. Fehler!!. Buhcstbaen nicht richtig erkannt?");
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
        logger.info("\r\n" + getString());
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
            ;
            for (int y = 0; y < getHeight(); y++) {

                if (isElement(getPixelValue(x, y), avg)) {
                    rowIsClear = false;
                    break;
                }
            }
            if (!rowIsClear)
                break;
            leftLines++;
        }

        for (int x = getWidth() - 1; x >= 0; x--) {
            boolean rowIsClear = true;
            ;
            for (int y = 0; y < getHeight(); y++) {

                if (isElement(getPixelValue(x, y), avg)) {
                    rowIsClear = false;
                    break;
                }
            }
            if (!rowIsClear)
                break;

            rightLines++;
        }

        if (leftLines >= getWidth() || (getWidth() - rightLines) > getWidth()) {
            logger.severe("cleaning failed. nothing left1");
            
            grid = new int[0][0];
            return false;

        }
        for (int y = 0; y < getHeight(); y++) {
            boolean lineIsClear = true;
            ;
            for (int x = leftLines; x < getWidth() - rightLines; x++) {
                if (isElement(getPixelValue(x, y), avg)) {
                    lineIsClear = false;
                    break;
                }
            }
            if (!lineIsClear)
                break;
            topLines++;
        }

        for (int y = getHeight() - 1; y >= 0; y--) {
            boolean lineIsClear = true;
            ;
            for (int x = leftLines; x < getWidth() - rightLines; x++) {
                if (isElement(getPixelValue(x, y), avg)) {
                    lineIsClear = false;
                    break;
                }
            }
            if (!lineIsClear)
                break;
            bottomLines++;
        }

        if ((getWidth() - leftLines - rightLines) < 0 || (getHeight() - topLines - bottomLines) < 0) {
            logger.severe("cleaning failed. nothing left");
            grid = new int[0][0];
            return false;
        }
        int[][] ret = new int[getWidth() - leftLines - rightLines][getHeight() - topLines - bottomLines];

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
 * @param contrast
 * @param objectContrast
 */
public void removeSmallObjects(double contrast, double objectContrast){
    Vector<PixelObject> ret=getObjects(contrast, objectContrast);

    for( int i=1;i<ret.size();i++){
      
        this.removeObjectFromGrid(ret.elementAt(i));
        
    }
}

/**
 * @param contrast
 * @param objectContrast
 * @param maxSize
 */
public void removeSmallObjects(double contrast, double objectContrast,int maxSize){
    Vector<PixelObject> ret=getObjects(contrast, objectContrast);

    for( int i=0;i<ret.size();i++){
      if(ret.elementAt(i).getSize()<maxSize)
        this.removeObjectFromGrid(ret.elementAt(i));
        
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
        PixelObject lastObject=null;
        PixelObject object;
//        ScrollPaneWindow w= new ScrollPaneWindow(this.owner);
//        w.setTitle("getObjects");
        int line=0;
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                if (tmpGrid[x][y] < 0)
                    continue;

                
                if (getPixelValue(x, y) < (objectContrast * getMaxPixelValue())) {
             
                 //Füge 2 Objekte zusammen die scheinbar zusammen gehören
                    
                    dist=100;
                    if(lastObject!=null){
                        dist=(int)(Math.pow(x-(lastObject.getXMin()+lastObject.getWidth()/2),2)+Math.pow(y-(lastObject.getYMin()+lastObject.getHeight()/2),2));
                    }
                    
                    if(lastObject!=null &&lastObject.getArea()<owner.getJas().getInteger("minimumObjectArea")&&dist<Math.pow(owner.getJas().getInteger("minimumLetterWidth")/2+1,2)){
                        
                        object=lastObject;
                        for (int i = 0; i < ret.size(); i++) {
                            if (ret.elementAt(i)==object) {
                                ret.remove(i);
                                break;
                            }
                        }
//                        logger.finer("Verfolge weiter Letztes Object: area:"+lastObject.getArea()+" dist: "+dist);
                   
                    }else{
                        object = new PixelObject(this);
                        object.setContrast(contrast);
                        object.setWhiteContrast(objectContrast); 
                    }
//                    w.setImage(0,line,getImage());
                    getObject(x, y, tmpGrid, object);
                  // logger.info(object.getSize()+" avg "+object.getAverage()+" area: "+object.getArea());
//                   w.setImage(1,line,getImage());
//                   w.setText(2,line,"Size: "+ object.getSize());
//                   w.setText(3,line,"AVG: "+object.getAverage());
//                   w.setText(4,line,"Area: "+object.getArea());
//                   w.setImage(5,line,object.toLetter().getImage());
//                   w.setText(6,line,object.toLetter().getDim());
                   line++;
                    lastObject=object;
                    for (int i = 0; i < ret.size(); i++) {
                        if (object.getArea() > ret.elementAt(i).getArea()) {
                          
                            ret.add(i, object);
//                            logger.finer("Found Object size:"+object.getSize()+" "+object.getWidth()+" - "+object.getArea());
                            
                            object = null;
                            break;
                        }
                    }
                    if (object != null){
                        ret.add(object); 
//                        logger.finer("Found Object size:"+object.getSize()+" "+object.getWidth()+" - "+object.getArea());
                        
                    }
                    
                    
                }
            }
        }
//        w.refreshUI();
        return ret;

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

        if (x < 0 || y < 0 || tmpGrid.length <= x || tmpGrid[0].length <= y || tmpGrid[x][y] < 0)
            return;
        int localValue = PixelGrid.getPixelValue(x, y, tmpGrid, owner);
//UTILITIES.trace(x+"/"+y);
        if (object.doesColorAverageFit(localValue)) {
            object.add(x, y, localValue);
            tmpGrid[x][y] = -1;
            
           //Achtung!! Algos funktionieren nur auf sw basis richtig 
         //grid[x][y] = 254;
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


}