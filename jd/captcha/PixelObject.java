package jd.captcha;

import java.util.Vector;
import java.util.logging.Logger;

import jd.plugins.Plugin;
/**
 * Diese Klasse  ist wie die Letterklasse ein PixelContainer. Allerdings werden nur Pixel mit INhalt aufgenommen und intern in einem vector abgelegt.
 * Es müssen nicht so viele Pixel verarbeitet werden. Dies Klasse eignet sich also um Pbjekte abzulegen um diese dann zu drehen doer zu transformieren.
 * Für die Objektsuche eignet sich diese Klasse wegen des internen vectors besser
 * @author coalado
 *
 */
public class PixelObject implements Comparable {
    /**
     * Logger
     */
    public Logger       logger = Plugin.getLogger();

    /**
     * Interner Vector
     */
    private Vector<int[]> object;
    /**
     * Farbdurschnitt des Onkekts
     */
    private int           avg          = 0;
    /**
     * Interne prüfvariable die hochgezählt wird wenn dieneuen Pixel keine Durchschnissänderung hervorrufen
     */
    private int           noAvgChanges = 0;
    /**
     * Anzahl der gleichbleibenden add Aufrufe bis ein durchschnitt as icher angesehen wird
     */
    private int           avgIsSaveNum = 10;
    /**
     * Als sicher angenommener Farb durchschnitt
     */
    private int           saveAvg      = 0;
    /**
     * Minimaler x Wert
     */
    private int           xMin      = Integer.MAX_VALUE;
    /**
     * Maximaler X Wert
     */
    private int           xMax      = Integer.MIN_VALUE;
    /**
     * Minimaler y Wert
     */
    private int           yMin      = Integer.MAX_VALUE;
    /**
     * Maximaler Y Wert
     */
    private int           yMax      = Integer.MIN_VALUE;
    /**
     * captcha als owner. Über owner kann auf den Parameter Dump zugegriffen werden
     */
    private PixelGrid       owner;
    
    /**
     * Kontrastwert für die durchschnisserkennung
     */
    private double        contrast;

    private double whiteContrast=1;

    /**
     * @param owner
     */
    public PixelObject(PixelGrid owner) {
        this.owner = owner;
        object = new Vector<int[]>();
    }
/**
 * Fügt einen neuen Pixel bei x,y hinzu. mit Color wird die originalfarbe des pixels übergeben.
 * @param x
 * @param y
 * @param color
 */
    public void add(int x, int y, int color) {
        int[] tmp = { x, y,color };
        int tmpAvg = avg;
        avg = UTILITIES.mixColors(avg, color, getSize(), 1);
//UTILITIES.trace("AVG "+avg);
        if (Math.abs(avg - tmpAvg) < (owner.getMaxPixelValue() * this.contrast)) {
            noAvgChanges++;
            if (avgIsSaveNum <= noAvgChanges&&saveAvg==0){
                saveAvg = avg;
          
            }
        } else {
            noAvgChanges = 0;
        }
       
        object.add(tmp);
        xMin=Math.min(x,xMin);
        xMax=Math.max(x, xMax);
        yMin=Math.min(y,yMin);
        yMax=Math.max(y,yMax);
        
    }
    /**
     * Gibt die resulitierende Bildbreite zurück
     * @return Breite
     */
    public int getWidth(){
        return xMax-xMin+1;
    }
    /**
     * Gibt die resulztierende Bildhöhe zurück
     * @return Höhe
     */
    public int getHeight(){
        return yMax-yMin+1;
    }
    /**
     * 
     * @return Gibt die Position (Links oben) des Objekts im gesamtCaptcha an
     */
    public int[] getLocation(){
        
    int[] ret={xMin,yMin};
    return ret;
    }
    /**
     * 
     * @return Gibt die Fläche des Objekts zurück
     */
    public int getArea(){
        return getWidth()*getHeight();
    }

    /**
     * 
     * @param color
     * @return Prüft ob die farbe color zum Objekt passt
     */
    public boolean doesColorAverageFit(int color) {
        if(getSize()>3000){
            logger.severe("Objekt scheint sehr groß zu werden. objectColorContrast zu hoch?");
            return false;
        }
        
        //Filtere zu helle Pixel aus
        if(color>(int)(whiteContrast*owner.getMaxPixelValue()))return false;
        if(getSize()==0)return true;
      int tavg=saveAvg==0?avg:saveAvg;
 
        //UTILITIES.trace(tavg+" : "+(int)Math.abs(tavg - color)+"<"+(int)(owner.getMaxPixelValue() * this.contrast)+" = "+(((int)Math.abs(tavg - color) < (int)(owner.getMaxPixelValue() * this.contrast))));
            return ((int)Math.abs(tavg - color) < (int)(owner.getMaxPixelValue() * this.contrast));
       
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
    public int[] elementAt(int i){
        return object.elementAt(i);
    }
    /**
     * Teilt das Objekt in splitNum gleich große Objekte auf
     * @param splitNum
     * @return splitNum gleich große Objekte
     */
    public Vector<PixelObject> split(int splitNum) {
        Vector<PixelObject> ret= new Vector<PixelObject>();
        for(int t=0;t<splitNum;t++){
            ret.add(new PixelObject(owner)); 
        }
        int part=getWidth()/splitNum;
        for( int i=0; i<getSize();i++){
           int[] akt=elementAt(i);
           for(int x=0;x<splitNum;x++){
               if(akt[0]<=(xMin+(x+1)*part)){
                   ret.elementAt(x).add(akt[0], akt[1], this.saveAvg);
                   break;
               }
           }
           
        }
        return ret;
    }
    /**
     * 
     * @return Gibt einen Entsprechenden Sw-Letter zurück
     */
    public Letter toLetter(){
        int[][] ret= new int[getWidth()][getHeight()];
        for( int x=0; x<getWidth();x++){
            for(int y=0; y<getHeight();y++){
                ret[x][y]=owner.getMaxPixelValue();
            }
        }
        for( int i=0; i<getSize();i++){
            int[] akt=elementAt(i);
         ret[akt[0]-xMin][akt[1]-yMin]=0;   
        }
        Letter l= owner.createLetter();
        l.setGrid(ret);
        return l;
        
    }
    /**
     * 
     * @return Gibt das Breite/Höhe Verjältniss zurück
     */
    public double getWidthToHeight(){
        return (double)getWidth()/(double)getHeight();
    }
    
    /**
     * Schnelle aber ungenauere align Methode
     * @return  Versucht das Objekt automatisch auszurichten
     */
    public PixelObject align(){
        int accuracy=1;
        PixelObject r= turn(-accuracy);
        PixelObject l= turn(accuracy);

        int angle;
     // UTILITIES.trace(getWidthToHeight()+" : right:"+r.getWidthToHeight()+" left:"+l.getWidthToHeight());
        if(r.getWidthToHeight()>=getWidthToHeight() && l.getWidthToHeight()>=getWidthToHeight())return this;
        int steps=r.getWidthToHeight()<l.getWidthToHeight()?-accuracy:accuracy;
        angle=steps*2;
        PixelObject ret= r.getWidthToHeight()<l.getWidthToHeight()?r:l;
        PixelObject next;
        while((next=turn(angle)).getWidthToHeight()<ret.getWidthToHeight()){    
          // UTILITIES.trace("akt angle: "+angle+" wh: "+next.getWidthToHeight());
            ret=next;
         
            angle+=steps;
        }
        return ret;
        
    }
    /**
     * Gibt sucht von winkael A bis Winkel B die Beste DRehposition und gibt diese zurück
     * Langsammer, aber genauer
     * @param angleA
     * @param angleB
     * @return Ausgerichtetes PixelObjekt
     */
    public PixelObject align(int angleA, int angleB){
        if(angleB<angleA){
            int tmp=angleB;
            angleB=angleA;
            angleA=tmp;
        }
        int accuracy=owner.owner.getJas().getAlignAngleSteps();
        double bestValue=Double.MAX_VALUE;
        PixelObject res=null;
        PixelObject tmp;
   //  UTILITIES.trace("ALIGN "+this.getWidthToHeight());
     for( int angle=angleA;angle<angleB;angle+=accuracy){   
       
         tmp= turn(angle<0?360+angle:angle);    
         
        // UTILITIES.trace((angle<0?360+angle:angle)+"  test "+this.getWidthToHeight());
         if(tmp.getWidthToHeight()<bestValue){
             bestValue=tmp.getWidthToHeight();
             res=tmp;           
         }         
     }
   
        return res;
    }
    /**
     * Dreht das Objekt um angle grad
     * @param angle
     * @return gedrehtes Pixelobjekt
     */
    public PixelObject turn(double angle) {
        PixelObject po= new PixelObject(owner);        
        for( int i=0; i<getSize();i++){
            int[] akt=elementAt(i);
        int[] n= UTILITIES.turnCoordinates(akt[0], akt[1], xMin+getWidth()/2, yMin+getHeight()/2, angle) ;
        po.add(n[0],n[1],avg);
        }
        return po;
    }
    public int compareTo(Object arg) {
        if(((PixelObject)arg).getLocation()[0]<this.getLocation()[0])return 1;
        if(((PixelObject)arg).getLocation()[0]>this.getLocation()[0])return -1;
        return 0;
    }
    
    /**
     * Setzte den WhiteKontrast
     * @param objectContrast
     */
    public void setWhiteContrast(double objectContrast) {
        this.whiteContrast=objectContrast;
        
    }
}