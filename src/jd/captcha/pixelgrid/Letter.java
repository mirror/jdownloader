package jd.captcha.pixelgrid;

import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelobject.PixelObject;
import jd.captcha.utils.UTILITIES;

/**
 * Diese Klasse beinhaltet alle Methoden für einzellne Letter.
 * 
 * @author coalado
 */
public class Letter extends PixelGrid {

    /*
     * der decoded Value wird heir abgelegt
     */
    private String decodedValue;

    /**
     * Hash des sourcecaptchas (Fürs training wichtig)
     */
    private String sourcehash;
  
    
    /**
     * Gibt an wie oft dieser letter positov aufgefallen ist
     */
    private int    goodDetections = 0;
    
    private int elementPixel=0;
    /**
     * Gibt an wie oft dieser letter negativ aufgefallen ist
     */
    private int    badDetections  = 0;
    /**
     * ID des letters in der Lettermap
     */
    public int     id;
 

    private int angle;

    /**
     * 
     */
    public Letter() {
        super(0, 0);
    }

    /**
     * TODO: EINE saubere verkleinerung
     *      * gibt den Letter um den faktor faktor verkleinert zurück. Es wird ein
     * Kontrastvergleich vorgenommen
     * 
     * @param faktor
     * @return Vereinfachter Buchstabe
     */
    public Letter getSimplified(int faktor) {
        if(faktor==1)return this;
        int newWidth = (int) Math.ceil(getWidth() / faktor);
        int newHeight = (int) Math.ceil(getHeight() / faktor);
        Letter ret = new Letter();;
        ret.setOwner(this.owner);
        int avg = getAverage();
        int value;
        int[][] newGrid = new int[newWidth][newHeight];
        elementPixel=0;
        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
  
               
               value=0;
                for (int gx = 0; gx < faktor; gx++) {
                    for (int gy = 0; gy < faktor; gy++) {
                        int newX = x * faktor + gx;
                        int newY = y * faktor + gy;
                        if (newX > getWidth() || newY > getHeight()) {
                            continue;
                        }
//                       
                        if(isElement(getPixelValue(newX, newY), avg)){
                            value++;                           
                        }
                    }
                   
                }
               
               // setPixelValue(x, y, newGrid, getPixelValue(x* faktor, y* faktor), this.owner);
                setPixelValue(x, y, newGrid, ((value*100)/(faktor*faktor))>50?0:getMaxPixelValue(), this.owner);
                if(newGrid[x][y]==0)elementPixel++;
            }
        }

        ret.setGrid(newGrid);

        ret.clean();

        return ret;
    }

 
    /**
     * Entfernt die Reihen 0-left und right-ende aus dem interne Grid
     * 
     * @param left
     * @param right
     * @return true bei Erfolg, sonst false
     */
    public boolean trim(int left, int right) {
        int width = right - left;
        int[][] tmp = new int[width][getHeight()];
        if (getWidth() < right) {
            logger.severe("Letter dim: " + getWidth() + " - " + getHeight() + ". Cannot trim to " + left + "-" + right);
            return false;
        }
        for (int x = 0; x < width; x++) {

            tmp[x] = grid[x + left];
            
        }
        grid = tmp;
        return true;
    }

    /**
     * Setzt das grid aus einem TextString. PixelSeperator: * Zeilensperator: |
     * 
     * @param content
     *            PixelString
     * @return true/false
     */
    public boolean setTextGrid(String content) {
        String[] code = content.split("\\|");
        grid = null;
        int width = 0;
        int elementPixel=0;
        for (int y = 0; y < code.length; y++) {
            String line = code[y];
            width = line.length();
            if (grid == null) {
                grid = new int[width][code.length];
                if (width < 2 || code.length < 2)
                    return false;

            }
            for (int x = 0; x < width; x++) {
                grid[x][y] = Integer.parseInt(String.valueOf(line.charAt(x))) * getMaxPixelValue();
if(grid[x][y]==0)elementPixel++;
            }
        }
        this.setElementPixel(elementPixel);
        return true;

    }

    /**
     * Gibt den Pixelstring zurück. Pixelsep: * ZeilenSep: |
     * 
     * @return Pixelstring 1*0*1|0*0*1|...
     */
    public String getPixelString() {

        String ret = "";

        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                ret += (int) (getPixelValue(x, y) / getMaxPixelValue()) + "";

            }

            ret += "|";
        }
        ret = ret.substring(0, ret.length() - 1);
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
     * @return the decodedValue
     */
    public String getDecodedValue() {
        return decodedValue;
    }

    /**
     * @param decodedValue
     *            the decodedValue to set
     */
    public void setDecodedValue(String decodedValue) {
        this.decodedValue = decodedValue;
    }

    /**
     * @return the sourcehash
     */
    public String getSourcehash() {
        return sourcehash;
    }

    /**
     * @param sourcehash
     *            the sourcehash to set
     */
    public void setSourcehash(String sourcehash) {
        this.sourcehash = sourcehash;
    }



    /**
     * @return Anzahl der Erfolgreichen Erkennungen durch diesen letter
     */
    public int getGoodDetections() {
        return goodDetections;

    }

    /**
     * @return the badDetections
     */
    public int getBadDetections() {
        return badDetections;
    }

    /**
     * @param badDetections
     *            the badDetections to set
     */
    public void setBadDetections(int badDetections) {
        this.badDetections = badDetections;
    }

    /**
     * @param goodDetections
     *            the goodDetections to set
     */
    public void setGoodDetections(int goodDetections) {
        this.goodDetections = goodDetections;
    }




    /**
     * Addiert eine gute Erkennung zu dem letter
     */
    public void markGood() {
        this.goodDetections++;
        logger.warning("GOOD detection : (" + this.toString() + ") ");
    }

    /**
     * Addiert eine Schlechte Erkennung
     */
    public void markBad() {
        this.badDetections++;
     
        logger.warning("Bad detection : (" + this.toString() + ") ");

    }

    public String toString() {
        return this.getDecodedValue() + " [" + this.getSourcehash() + "][" + this.getGoodDetections() + "/" + this.getBadDetections() + "]";
    }



    /**
     * Versucht den Buchstaben automatisch auszurichten. Als kriterium dient das
     * minimale Breite/Höhe Verhältniss Es wird zuerst die optimale DRehrichtung
     * ermittelt und dann gedreht. Die Methode funktioniert nicht immer
     * zuverlässig. align(double contrast,double objectContrast,int angleA, int
     * angleB) braucht länger, liefert aber bessere Ergebnisse
     * 
     * @param objectContrast
     * @return Ausgerichteter Buchstabe
     */
    public Letter align(double objectContrast) {
        PixelObject obj = this.toPixelObject(objectContrast);

        PixelObject aligned = obj.align();
        Letter newLetter = aligned.toLetter();
        this.setGrid(newLetter.grid);
        return this;

    }

    /**
     * Gibt einen Ausgerichteten Buchstaben zurück. Es wird vom Winkel angleA
     * bis angleB nach der Besten Ausrichtung (Breite/Höhe) gesucht. Ist
     * zuverlässiger als align(double contrast,double objectContrast)
     * 
     * @param objectContrast
     * @param angleA
     * @param angleB
     * @return Ausgerichteter Buchstabe
     */
    public Letter align(double objectContrast, int angleA, int angleB) {
        PixelObject obj = this.toPixelObject(objectContrast);

        PixelObject aligned = obj.align(angleA, angleB);
        Letter newLetter = aligned.toLetter();

        this.setGrid(newLetter.grid);
        return this;

    }
/**
 * Autoasurichtung. Diese Funktion geht nicht den Umweg über ein Pixelobject. Braucht etwas mehr zeit und liefrt dafür deutlich bessere Ergebnisse!
 * @param angleA
 * @param angleB
 * @return Gedrehter buchstabe
 */
    public Letter align(int angleA, int angleB) {
        if(angleB<angleA){
            int tmp=angleB;
            angleB=angleA;
            angleA=tmp;
        }
        int accuracy=owner.getJas().getInteger("AlignAngleSteps");
        double bestValue=Double.MAX_VALUE;
        Letter res=null;
        Letter tmp;
//logger.info("JJ"+angleA+" - "+angleB);
     for( int angle=angleA;angle<angleB;angle+=accuracy){   
       
         tmp= turn(angle<0?360+angle:angle);    
         //logger.info(".. "+((double)tmp.getWidth()/(double)tmp.getHeight()));
         //BasicWindow.showImage(tmp.getImage(1),(angle<0?360+angle:angle)+"_");
         if(((double)tmp.getWidth()/(double)tmp.getHeight())<bestValue){
             bestValue=((double)tmp.getWidth()/(double)tmp.getHeight());
             res=tmp;           
         }         
     }
   

        return res;
      
          
      }
/**
 * Resize auf newHeight. die proportionen bleiben erhalten
 * @param newHeight
 */
public void resizetoHeight(int newHeight){
    double faktor= (double)newHeight/(double)getHeight();
    
    int newWidth=(int)Math.ceil((int)((double)getWidth()*faktor));

    int[][] newGrid= new int[newWidth][newHeight];
    
    for (int x = 0; x < newWidth; x++) {
        for (int y = 0; y < newHeight; y++) {
            int v=grid[(int)Math.round((double)x/faktor)][(int)Math.round((double)y/faktor)];
            newGrid[x][y]=v;
            
        }}   
    
    this.setGrid(newGrid);
    
    
    
}
    /**
     * Dreht den buchstaben um angle. Dabei wird breite und höhe angepasst. Das
     * drehen dauert länger als über PixelObject, leidet dafür deutlich weniger
     * unter Pixelfehlern
     * 
     * @param angle
     * @return new letter
     */
    public Letter turn(double angle) {
     
        while(angle<0)angle+=360;
        angle /= 180;  
        Letter l = createLetter();

        int newWidth = (int) (Math.abs(Math.cos(angle * Math.PI) * getWidth()) + Math.abs(Math.sin(angle * Math.PI) * getHeight()));
        int newHeight = (int)( Math.abs(Math.sin(angle * Math.PI) * getWidth()) + Math.abs(Math.cos(angle * Math.PI) * getHeight()));
      //logger.info(getWidth()+"/"+getHeight()+" --> "+newWidth+"/"+newHeight +"("+angle+"/"+(angle*180)+"/"+Math.cos(sizeAngle * Math.PI)+"/"+Math.sin(sizeAngle * Math.PI));
        int left = (newWidth - getWidth()) / 2;
        int top = (newHeight - getHeight()) / 2;
        int elementPixel=0;
        int[][] newGrid = new int[newWidth][newHeight];
        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                int[] n = UTILITIES.turnCoordinates(x - left, y - top, getWidth() / 2, getHeight() / 2, -(angle * 180));
                if (n[0] < 0 || n[0] >= getWidth() || n[1] < 0 || n[1] >= getHeight()) {
                    newGrid[x][y] = owner.getJas().getColorFaktor() - 1;
                    if(newGrid[x][y]==0)elementPixel++;
                    continue;
                }

                newGrid[x][y] = grid[n[0]][n[1]];
                if(newGrid[x][y]==0)elementPixel++;

            }
        }
        l.setGrid(newGrid);
        l.setElementPixel(elementPixel);
        l.clean();
        l.setAngle((int)(angle*180.0));
      //  BasicWindow.showImage(l.getImage(), sizeAngle+" angle "+angle+" - "+newWidth+"/"+newHeight+" - "+getWidth()+"/"+getHeight());
       
        return l;

    }

    private void setAngle(int angle) {
       this.angle=angle;
        
    }

    private PixelObject toPixelObject(double objectContrast) {
        PixelObject object = new PixelObject(this);
        object.setWhiteContrast(objectContrast);
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                if (getPixelValue(x, y) < (getMaxPixelValue() * objectContrast)) {
                    object.add(x, y, getPixelValue(x, y));
                }
            }
        }

        return object;
    }
/**
 * Gib den drehwinkel des letterszurück
 * @return drehwinkel
 */
    public int getAngle() {
        return this.angle;
      
    }
/**
 * Skaliert den Letter auf die höhe
 * @param newHeight
 * @param d Grenzfaktor. darunter wird nicht skaliert
 */
    public void resizetoHeight(int newHeight, double d) {
        double faktor= (double)newHeight/(double)getHeight();
        if(Math.abs(faktor-1)<d)resizetoHeight(newHeight);
       
        
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @return the elementPixel
     */
    public int getElementPixel() {
       if(elementPixel>0) return elementPixel;
       elementPixel=0;
       for (int x = 0; x < getWidth(); x++) {
           for (int y = 0; y < getHeight(); y++) {
               if(grid[x][y]==0)elementPixel++;
           }
       }     
       return elementPixel;
    }

    /**
     * @param elementPixel the elementPixel to set
     */
    public void setElementPixel(int elementPixel) {
        this.elementPixel = elementPixel;
    }

}