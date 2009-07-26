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

package jd.captcha.specials;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelobject.PixelObject;
import jd.captcha.utils.Utilities;

/**
 * 
 * 
 * @author JD-Team
 */
public class LinkbaseBiz {

    private static void mergeObjects2(Vector<PixelObject> os) {
        for (PixelObject a : os) {
            for (PixelObject b : os) {
                if (a == b) continue;

                int xMin = Math.max(a.getXMin(), b.getXMin());
                int xMax = Math.min(a.getXMin() + a.getWidth(), b.getXMin() + b.getWidth());
                if (xMax+2 <= xMin ) 
                	continue;
                int yMin = Math.max(a.getYMin(), b.getYMin());
                int yMax = Math.min(a.getYMin() + a.getHeight(), b.getYMin() + b.getHeight());

                if (((xMax - xMin)<20) && ((yMax - yMin)<20)) {
                            a.add(b);
                            os.remove(b);
                            mergeObjects2(os);
                            return;
                }
            }
        }

    }
    private static void mergeObjects1(Vector<PixelObject> os) {
        for (PixelObject a : os) {
            for (PixelObject b : os) {
                if (a == b) continue;

                int xMin = Math.max(a.getXMin(), b.getXMin());
                int xMax = Math.min(a.getXMin() + a.getWidth(), b.getXMin() + b.getWidth());
                if (xMax <= xMin ) 
                	continue;
                int yMin = Math.max(a.getYMin(), b.getYMin());
                int yMax = Math.min(a.getYMin() + a.getHeight(), b.getYMin() + b.getHeight());

                if (((xMax - xMin)<20) && ((yMax - yMin)<20)) {
                            a.add(b);
                            os.remove(b);
                            mergeObjects1(os);
                            return;
                }
            }
        }

    }
    public static Letter[] getLetters1(Captcha captcha) {

        captcha.cleanByRGBDistance(1, 10);

        captcha.toBlackAndWhite(0.75);
//        captcha.removeSmallObjects(0.75, 0.75);
        // long t = System.currentTimeMillis();
//        clearCaptcha(captcha);
        Vector<PixelObject> os = captcha.getObjects(0.75, 0.75);
        if(os.size()>10)
        {

            captcha.cleanWithDetailMask(captcha.owner.createCaptcha(Utilities.loadImage(captcha.owner.getResourceFile("bgmask_1.png"))), 1);    
            captcha.blurIt(2);
            captcha.toBlackAndWhite(0.6);
            os = captcha.getObjects(0.75, 0.75);
            if(os.size()>10)
            {
            	captcha.reset();
                captcha.cleanByRGBDistance(1, 10);
                captcha.toBlackAndWhite(0.6);
                captcha.cleanWithDetailMask(captcha.owner.createCaptcha(Utilities.loadImage(captcha.owner.getResourceFile("bgmask.png"))), 2,50);    
                captcha.blurIt(2);
                captcha.toBlackAndWhite(0.6);
                os = captcha.getObjects(0.75, 0.75);
            }
        }

        Collections.sort(os);
        mergeObjects1(os);
        ArrayList<Letter> ret = new ArrayList<Letter>();
        for (PixelObject pixelObject : os) {
        	if(pixelObject.getArea()>90)
        	{
            Letter let = pixelObject.toLetter();
            ret.add(let);
        	}
        }
        if(ret.size()<3)
        	return null;
        return ret.toArray(new Letter[] {});

    }

    public static Letter[] getLetters2(Captcha captcha) {

        captcha.cleanByRGBDistance(1, 10);

        captcha.toBlackAndWhite(0.75);
//        captcha.removeSmallObjects(0.75, 0.75);
        // long t = System.currentTimeMillis();
//        clearCaptcha(captcha);
        Vector<PixelObject> os = captcha.getObjects(0.75, 0.75);
        if(os.size()>15)
        {

            captcha.cleanWithDetailMask(captcha.owner.createCaptcha(Utilities.loadImage(captcha.owner.getResourceFile("bgmask_1.png"))), 1);    
            captcha.blurIt(2);
            captcha.toBlackAndWhite(0.6);
            os = captcha.getObjects(0.75, 0.75);
            if(os.size()>15)
            {
            	captcha.reset();
                captcha.cleanByRGBDistance(1, 10);
                captcha.toBlackAndWhite(0.6);
                captcha.cleanWithDetailMask(captcha.owner.createCaptcha(Utilities.loadImage(captcha.owner.getResourceFile("bgmask.png"))), 2,50);    
                captcha.blurIt(2);
                captcha.toBlackAndWhite(0.6);
                os = captcha.getObjects(0.75, 0.75);
            }
        }

        Collections.sort(os);
        mergeObjects2(os);
        ArrayList<Letter> ret = new ArrayList<Letter>();
        for (PixelObject pixelObject : os) {
        	if(pixelObject.getArea()>10)
        	{
            Letter let = pixelObject.toLetter();
            ret.add(let);
        	}
        }

        return ret.toArray(new Letter[] {});

    }






}