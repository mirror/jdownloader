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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import jd.captcha.LetterComperator;

import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;

/**
 * 
 * 
 * @author JD-Team
 */
public class MegaUpload {
    private static final double OBJECTDETECTIONCONTRAST = 0.95;

    public static Captcha delLetter(LetterComperator lc, Captcha captcha, int loca) {
        try {
            int xx = lc.getImgOffset()[0];
            int yy = lc.getImgOffset()[1];
            int left = lc.getOffset()[0];
            int top = lc.getOffset()[1];
            int tmpIntersectionWidth = lc.getIntersectionDimension()[0];
            int tmpIntersectionHeight = lc.getIntersectionDimension()[1];
            int overlayNoiseSize = captcha.owner.getJas().getInteger("overlayNoiseSize");
//            System.out.println(loc[0]);
            int[][] gird = captcha.grid;
            int val = captcha.getMaxPixelValue();
            for (int x = 0; x < tmpIntersectionWidth; x++) {
                for (int y = 0; y < tmpIntersectionHeight; y++) {

                    int pixelType = lc.getPixelType(x, y, xx, yy, left, top);
                    int va = lc.getB().getPixelValue(x + left, y + top);
                    if(pixelType!=1&&pixelType!=2)
                    {
                            captcha.setPixelValue(x+loca+xx, y+yy, val);
                    }
                    else
                    {

                        if (lc.hasNeighbour(x, y, xx, yy, left, top, pixelType) > overlayNoiseSize)
                        {
                            captcha.setPixelValue(x+loca+xx, y+yy, val);
                        }
                            try {
                                if(x>tmpIntersectionWidth/2)
                                {
                                if(va!=val && gird[x+loca+xx+3][y+yy]!=val)
                                {
                                    captcha.setPixelValue(x+loca+xx, y+yy, 0);
                                    captcha.setPixelValue(x+loca+xx+1, y+yy, 0);
                                    captcha.setPixelValue(x+loca+xx+2, y+yy, 0);
                                }
                                }
                                else
                                {
                                    if(va!=val && gird[x+loca+xx-3][y+yy]!=val)
                                    {
                                        captcha.setPixelValue(x+loca+xx, y+yy, 0);
                                        captcha.setPixelValue(x+loca+xx-1, y+yy, 0);
                                        captcha.setPixelValue(x+loca+xx-2, y+yy, 0);
                                    }
                                }
                            } catch (Exception e) {
                                // TODO: handle exception
                            }

                    }
                }
            }
//            captcha.removeSmallObjects(val, OBJECTDETECTIONCONTRAST);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return captcha;
    }

    public static Letter[] getLetters(Captcha captcha) {
        int w = captcha.getWidth() / 4;
        int val = captcha.getMaxPixelValue();
        // captcha.rapidshareSpecial(object)
        // System.out.println(lets[0].getDecodedValue());
        // System.out.println(lets[0].detected.getIntersection().toString());
        // System.out.println(captcha.getString());
        Letter[] retcaps = new Letter[4];
        try {
            for (int c = 0; c < retcaps.length; c++) {
                Letter[] lets = captcha.getLetters0(4, new int[] { w, w * 2, w * 3, w * 4 });
                for (Letter letter : lets) {
                    letter.removeSmallObjects(val, OBJECTDETECTIONCONTRAST);
                }
                Letter[] lf1 = getlf(lets, captcha.owner);

                captcha.reset();
                double last = 100;
                int delcap = 0;
                for (int i = 0; i < lf1.length; i++) {
                    Letter letter = lf1[i];
                    if(retcaps[i]==null&&last>letter.detected.getValityPercent())
                    {
                        last=letter.detected.getValityPercent();
                        delcap = i;
                    }
                    if(retcaps[i]==null||lf1[i].detected.getValityPercent()<retcaps[i].detected.getValityPercent())
                        retcaps[i]=lf1[i];
                }
                captcha=delLetter(retcaps[delcap].detected, captcha, w*delcap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retcaps;

    }

    public static Letter[] letterFilter(Letter[] org, JAntiCaptcha jac) {

        return org;
    }

    public static Letter[] getlf(Letter[] org, JAntiCaptcha jac) {
        int ths = Runtime.getRuntime().availableProcessors();
        MultiThreadDetection mtd = new MultiThreadDetection(ths, jac);
        // Vector<Letter> ret = new Vector<Letter>();

        ArrayList<Letter> r = new ArrayList<Letter>();
        int iii = 0;
        for (Letter l : org) {
            iii++;
            if(l!=null)
            mtd.queueDetection(l);
            r.add(l);
        }
        mtd.waitFor(null);
        int id = 0;
        for (Iterator<Letter> it = r.iterator(); it.hasNext();) {
            Letter akt = it.next();
            akt.id = id++;
        }
        Collections.sort(r, new Comparator<Letter>() {
            public int compare(Letter o1, Letter o2) {
                if (o1.detected.getValityPercent() > o2.detected.getValityPercent()) { return 1; }
                if (o1.detected.getValityPercent() < o2.detected.getValityPercent()) { return -1; }
                return 0;
            }
        });
        List<Letter> list;
        try {
            list = r.subList(0, r.size());
            // list = r;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        ;

        Collections.sort(list, new Comparator<Letter>() {
            public int compare(Letter o1, Letter o2) {
                if (o1.id > o2.id) { return 1; }
                if (o1.id < o2.id) { return -1; }
                return 0;
            }
        });

        return list.toArray(new Letter[] {});
    }

}