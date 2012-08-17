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

import jd.captcha.easy.BackGroundImageManager;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
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
    
    /*
    private static Object[] getNexTOS(PixelObject aos, List<PixelObject> os) {
        int i = 0;
        PixelObject nextos;
        while ((nextos = os.get(i)) == aos) {
            i++;

        }
        i++;
        int best;
        int matching = Math.min((aos.getXMax()), (nextos.getXMax())) - Math.max(aos.getXMin(), nextos.getXMin());
        if (matching >= 0)
            best = -matching;
        else
            best = Math.min((Math.min(Math.abs((aos.getXMin() + aos.getWidth()) - (nextos.getXMin())), Math.abs((aos.getXMin()) - (nextos.getXMin() + nextos.getWidth())))), (Math.min(Math.abs(aos.getXMin() - nextos.getXMin()), Math.abs((aos.getXMin() + aos.getWidth()) - (nextos.getXMin() + nextos.getWidth())))));
        for (; i < os.size(); i++) {
            PixelObject b = os.get(i);
            if (b == aos) continue;
            int ib;
            matching = Math.min((aos.getXMax()), (b.getXMax())) - Math.max(aos.getXMin(), b.getXMin());
            if (matching >= 0)
                ib = -matching;
            else
                ib = Math.min((Math.min(Math.abs((aos.getXMin() + aos.getWidth()) - (b.getXMin())), Math.abs((aos.getXMin()) - (b.getXMin() + b.getWidth())))), (Math.min(Math.abs(aos.getXMin() - b.getXMin()), Math.abs((aos.getXMin() + aos.getWidth()) - (b.getXMin() + b.getWidth())))));

            if (ib < best) {
                best = ib;
                nextos = b;
            }
        }
        return new Object[] {nextos, best};
    }

    private static void merge(List<PixelObject> os) {
        if (os.size() <= 4) return;
        PixelObject aos = null;

        for (PixelObject pixelObject : os) {
            if(aos==null||pixelObject.getSize()<aos.getSize())
            {
                aos=pixelObject;
            }
        }
        if(aos.getSize()<80)
        {
            EasyCaptcha.mergeos(aos, os);
            merge(os);
            return;
        }
        int mergeos = Integer.MAX_VALUE;
        
        for (PixelObject pixelObject : os) {
            Object[] nos = getNexTOS(pixelObject, os);
            int dist = (Integer)nos[1];
            if (mergeos > dist) {
                mergeos = dist;
                aos = pixelObject;
            }
        }
        if(mergeos<3)
        {
        EasyCaptcha.mergeos(aos, os);
        merge(os);
        }
    }
    */

    static boolean isRgb(int color)
    {
        return color!=0x000000;
    }
    /**
     * overwrite the colored dots in digits with black dots
     * die bunten punkte in den Zahlen werden mit schwarzen punkten ersetzt
     * @param captcha
     */
    static void setDotsInDigits(Captcha captcha) {

        int[][] grid = PixelGrid.getGridCopy(captcha.grid);
        for (int x = 1; x < captcha.getWidth() - 1; x++) {
            for (int y = 1; y < captcha.getHeight() - 1; y++) {
                if (isRgb(captcha.grid[x][y])) {
                    int co;
                    int w = (co = captcha.grid[x + 1][y]) != 0xffffff && !isRgb(co) ? 1 : 0;
                    w += (co = captcha.grid[x + 1][y + 1]) != 0xffffff && !isRgb(co) ? 1 : 0;
                    w += (co = captcha.grid[x][y + 1]) != 0xffffff && !isRgb(co) ? 1 : 0;
                    w += (co = captcha.grid[x - 1][y + 1]) != 0xffffff && !isRgb(co) ? 1 : 0;
                    w += (co = captcha.grid[x - 1][y - 1]) != 0xffffff && !isRgb(co) ? 1 : 0;
                    w += (co = captcha.grid[x + 1][y - 1]) != 0xffffff && !isRgb(co) ? 1 : 0;
                    w += (co = captcha.grid[x - 1][y]) != 0xffffff && !isRgb(co) ? 1 : 0;
                    w += (co = captcha.grid[x][y - 1]) != 0xffffff && !isRgb(co) ? 1 : 0;
                    if (w > 4) grid[x][y] = 0x000000;

                }
            }
        }
        captcha.grid = grid;
    }
    static int getlength(Captcha captcha) {
        int x = 3;
        outerx:for (; x < captcha.getWidth()-3; x++) {
            for (int y = 3; y < captcha.getHeight()-3; y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    break outerx;
                }

            }
        }
        int xo = captcha.getWidth()-4;
        outerx:for (; xo >4 ; xo--) {
            for (int y = 3; y < captcha.getHeight()-3; y++) {
                if (captcha.grid[xo][y] != 0xffffff) {
                    break outerx;
                }

            }
        }
        return xo-x;
    }
    public static Letter[] getLetters1(Captcha captcha) {
        BackGroundImageManager bgit = new BackGroundImageManager(captcha);
        bgit.setBackGroundImageListFileName("bgimages2.xml");

        bgit.clearCaptchaAll();
        setDotsInDigits(captcha);
        setDotsInDigits(captcha);
        setDotsInDigits(captcha);
        setDotsInDigits(captcha);
//        captcha.autoBottomTopAlign();

        if(getlength(captcha)<88)
        captcha.owner.setLetterNum(4);
        else        captcha.owner.setLetterNum(5);

        return EasyCaptcha.getLetters(captcha);

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
        java.util.List<Letter> ret = new ArrayList<Letter>();
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