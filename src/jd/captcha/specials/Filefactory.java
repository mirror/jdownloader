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

package jd.captcha.specials;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import jd.captcha.JAntiCaptcha;
import jd.captcha.gui.BasicWindow;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelobject.PixelObject;
import jd.captcha.utils.UTILITIES;
import jd.utils.JDUtilities;

/**
 * 
 * 
 * @author JD-Team
 */
public class Filefactory {

    private static final double OBJECTCOLORCONTRAST = 0.01;

    private static final double OBJECTDETECTIONCONTRAST = 0.95;

    private static final int MINAREA = 200;

    private static final int MAXAREA = 1200;

    // private static final int LETTERNUM = 4;

    private static final double FILLEDMAX = 0.9;

    private static final double FILLEDMIN = 0.2;

    private static final int MINWIDTH = 8;

    private static final int MINHEIGHT = 15;

    private static final double MINWIDTHTOHEIGHT = 0.2;

    private static final double MAXWIDTHTOHEIGHT = 2;

    private static final int MAXHEIGHT = 30;

    private static final int MAXWIDTH = 40;

    private static Logger logger = JDUtilities.getLogger();

    public static Letter[] getLetters(Captcha captcha) {
        ArrayList<PixelObject> os = new ArrayList<PixelObject>();
        for (int y = 0; y < captcha.getHeight(); y++) {
        for (int x = 0; x < captcha.getWidth(); x++) {
           
                int color = captcha.getPixelValue(x, y);

                double bestvalue = 100.0;
                PixelObject bo = null;
                for (PixelObject akt : os) {
                    double d;
                   
                    
                    if (akt.getDistanceTo(x,y) < 10 && (d = UTILITIES.getColorDifference(akt.elementAt(0)[2], color)) < bestvalue) {
                        bestvalue = d;
                        bo = akt;
                    }

                }
                if (bestvalue <= 30.0) {
                    bo.add(x, y, color);

                } else {

                    PixelObject po = new PixelObject(captcha);
                    po.add(x, y, color);
                    os.add(po);
                }

            }
        }
Collections.sort(os);
        for (Iterator<PixelObject> it = os.iterator(); it.hasNext();) {
            PixelObject akt = it.next();
            if (true && (akt.getArea() > 1800 || akt.getArea() < 200 || (akt.getArea() > 600 && ((double)akt.getArea() / (double)akt.getSize()) < 1.2) || (akt.getArea() / akt.getSize()) > 10 || akt.getHeight() < 10 || akt.getWidth() < 5)) {
                it.remove();
                //BasicWindow.showImage(akt.toLetter().getImage(5),"fil "+akt.getArea()+" -"+((double)akt.getArea() / (double)akt.getSize())+" - "+akt.getHeight()+" - "+akt.getWidth());
                
            } else {
                
            }    

        }

        ArrayList<PixelObject> os2 = new ArrayList<PixelObject>();

        ArrayList<Letter> ret = new ArrayList<Letter>();
        int i = 0;
        for (Iterator<PixelObject> it = os.iterator(); it.hasNext();) {
            Letter let = it.next().toLetter();
          
            let.blurIt(2);
            let.toBlackAndWhite(1.16);
            let.removeSmallObjects(OBJECTDETECTIONCONTRAST, OBJECTCOLORCONTRAST, 10);
            let.clean();
            let=let.align(-20, 20);
          
            i++;
            PixelObject akt = let.toPixelObject(OBJECTDETECTIONCONTRAST);
           
            if (akt.getArea() > 1800 || akt.getArea() < 200 || (akt.getArea() > 600 &&  ((double)akt.getArea() / (double)akt.getSize()) < 1.2) || (akt.getArea() / akt.getSize()) > 8 || akt.getHeight() < 15 || akt.getWidth() < 5) {
                //BasicWindow.showImage(akt.toLetter().getImage(5),"fil "+akt.getArea()+" -"+((double)akt.getArea() / (double)akt.getSize())+" - "+akt.getHeight()+" - "+akt.getWidth());
            } else {
                ret.add(let);
                
            }
        }

        // for (int i = 0; i < letters.size(); i++) {
        // BasicWindow.showImage(letters.elementAt(i).toLetter().getImage(), "im
        // " + i);
        // PixelObject obj = letters.elementAt(i);
        //
        // Letter l = obj.toLetter();
        // //
        // l.removeSmallObjects(captcha.owner.getJas().getDouble("ObjectColorContrast"),
        // // captcha.owner.getJas().getDouble("ObjectDetectionContrast"));
        // captcha.owner.getJas().executeLetterPrepareCommands(l);
        // // if(owner.getJas().getInteger("leftAngle")!=0 ||
        // // owner.getJas().getInteger("rightAngle")!=0) l =
        // //
        // l.align(owner.getJas().getDouble("ObjectDetectionContrast"),owner.getJas().getInteger("leftAngle"),owner.getJas().getInteger("rightAngle"));
        // // l.reduceWhiteNoise(2);
        // // l.toBlackAndWhite(0.6);
        //
        // ret[i] =
        // l.getSimplified(captcha.owner.getJas().getDouble("simplifyFaktor"));
        //
        // }
       // ret=ret.subList(0,4).toArray(a);
        return ret.toArray(new Letter[]{});

    }

    public static Letter[] letterFilter(Letter[] org, JAntiCaptcha jac) {
        int ths = Runtime.getRuntime().availableProcessors();
        MultiThreadDetection mtd = new MultiThreadDetection(ths, jac);
        Vector<Letter> ret = new Vector<Letter>();

        Vector<PixelObject> sp;
        ArrayList<Letter> r = new ArrayList<Letter>();
        int iii = 0;
        for (Letter l : org) {
            iii++;
            mtd.queueDetection(l);
            r.add(l);
        }
        mtd.waitFor(null);
        int id=0;
        for (Iterator<Letter> it = r.iterator(); it.hasNext();) {
            Letter akt = it.next();
           
            if(akt.detected.getDecodedValue().equals("1")){
                it.remove();
            }else{
                akt.id=id++; 
            }
        }
        Collections.sort(r, new Comparator<Letter>() {
            public int compare(Letter o1, Letter o2) {
                if(o1.detected.getValityPercent()>o2.detected.getValityPercent())return 1;
                if(o1.detected.getValityPercent()<o2.detected.getValityPercent())return -1;
                return 0;
            }
        });
        List<Letter> list = r.subList(0,4);
        
        Collections.sort(list, new Comparator<Letter>() {
            public int compare(Letter o1, Letter o2) {
                if(o1.id>o2.id)return 1;
                if(o1.id<o2.id)return -1;
                return 0;
            }
        });
        
        return r.subList(0,4).toArray(new Letter[]{});
    }

}