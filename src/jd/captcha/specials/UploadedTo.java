package jd.captcha.specials;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelobject.PixelObject;
import jd.utils.JDUtilities;

/**
 * 
 * 
 * @author JD-Team
 */
public class UploadedTo {

    private static final double OBJECTCOLORCONTRAST     = 0.001;

    private static final double OBJECTDETECTIONCONTRAST = 1;

    private static final int    MINAREA                 = 200;

    private static final int    MAXAREA                 = 1200;

   // private static final int    LETTERNUM               = 4;

    private static final double FILLEDMAX               = 0.9;

    private static final double FILLEDMIN               = 0.2;

    private static final int MINWIDTH = 8;

    private static final int MINHEIGHT = 15;

    private static final double MINWIDTHTOHEIGHT = 0.2;

    private static final double MAXWIDTHTOHEIGHT = 2;

    private static final int MAXHEIGHT = 30;

    private static final int MAXWIDTH = 40;
    


    private static Logger       logger                  = JDUtilities.getLogger();

    public static Letter[] getLetters(Captcha captcha) {
      
        Vector<PixelObject> letters = getObjects(captcha);
    
        if (letters == null) return null;
       
        Letter[] ret = new Letter[letters.size()];
        for (int i = 0; i < letters.size(); i++) {

            PixelObject obj = letters.elementAt(i);

            Letter l = obj.toLetter();
            // l.removeSmallObjects(captcha.owner.getJas().getDouble("ObjectColorContrast"),
            // captcha.owner.getJas().getDouble("ObjectDetectionContrast"));
            captcha.owner.getJas().executeLetterPrepareCommands(l);
            // if(owner.getJas().getInteger("leftAngle")!=0 ||
            // owner.getJas().getInteger("rightAngle")!=0) l =
            // l.align(owner.getJas().getDouble("ObjectDetectionContrast"),owner.getJas().getInteger("leftAngle"),owner.getJas().getInteger("rightAngle"));
            // l.reduceWhiteNoise(2);
            // l.toBlackAndWhite(0.6);

            ret[i] = l.getSimplified(captcha.owner.getJas().getInteger("simplifyFaktor"));

        }
        return ret;

    }
    
  



              
    public static Vector<PixelObject> getObjects(Captcha captcha) {
       // int splitter;
       // int splitNum;
       // int found = 0;

       // int minWidth = Integer.MAX_VALUE;
       // int maxWidth;
        // Alle Objekte aus dem captcha holen. Sie sind nach der Größe Sortiert
     
        Vector<PixelObject> objects = captcha.getObjects(OBJECTCOLORCONTRAST, OBJECTDETECTIONCONTRAST);
//        if(JAntiCaptcha.isLoggerActive())logger.info(""+objects);
        if(JAntiCaptcha.isLoggerActive())logger.info("start");
        Collections.sort(objects, new Comparator<PixelObject> (){
            public int compare(PixelObject obj1, PixelObject obj2)
            {
         
                if(obj1.getLocation()[0]<obj2.getLocation()[0])return 1;
                if(obj1.getLocation()[0]>obj2.getLocation()[0])return -1;
                return 0;
            }});
        if(JAntiCaptcha.isLoggerActive())logger.info("end");
//        if(JAntiCaptcha.isLoggerActive())logger.info(""+objects);
        Vector<PixelObject> filtered = new Vector<PixelObject>();
   
        for (int i = objects.size() - 1; i >= 0; i--) {
        
            PixelObject obj = objects.elementAt(i);
        
            if(objects.elementAt(i).getArea() > MINAREA){
            Letter letter = obj.toLetter();
//            BasicWindow.showImage(letter.getImage(),obj.getLocation()[0]+" px");
            letter.reduceBlackNoise(3, 0.3);
            letter.toBlackAndWhite(0.5);
//            BasicWindow.showImage(letter.getImage());
            obj=letter.toPixelObject(0.85);
         
            objects.setElementAt(obj, i);
            }
            
        
        }
        
        // Kleine Objekte ausfiltern
        for (int i = objects.size() - 1; i >= 0; i--) {
            double filled = ((double) objects.elementAt(i).getSize()) / ((double) objects.elementAt(i).getArea());
            if (objects.elementAt(i).getArea() < MAXAREA && objects.elementAt(i).getArea() > MINAREA) {
               
                if (filled < FILLEDMAX && filled > FILLEDMIN) {
                    
                    if(objects.elementAt(i).getHeight()>MINHEIGHT &&objects.elementAt(i).getWidth()>MINWIDTH){
                        if(objects.elementAt(i).getHeight()<MAXHEIGHT &&objects.elementAt(i).getWidth()<MAXWIDTH){
                        if(objects.elementAt(i).getWidthToHeight()>MINWIDTHTOHEIGHT && objects.elementAt(i).getWidthToHeight()<MAXWIDTHTOHEIGHT){
                    filtered.add(objects.elementAt(i));
//                    BasicWindow.showImage(objects.elementAt(i).toLetter().getImage(), "OK "+filled);
                    continue;
                        }
                    }}
                }
            }

//            BasicWindow.showImage(objects.elementAt(i).toLetter().getImage(), "NEIN "+filled);

        }

        objects = filtered;

        // entfernt Überflüssige Objekte und
//        for (int ii = objects.size() - 1; ii >= found; ii--) {
//            objects.remove(ii);
//        }
        // Sortiert die Objekte nun endlich in der richtigen Reihenfolge (von
        // link nach rechts)
       
        if(JAntiCaptcha.isLoggerActive())logger.finer("Found " + objects.size() + " Elements");
        return objects;
    }

}