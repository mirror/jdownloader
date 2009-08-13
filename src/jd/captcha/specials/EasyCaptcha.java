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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import jd.captcha.easy.CPoint;
import jd.captcha.easy.ColorTrainer;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelobject.PixelObject;

/**
 * 
 * 
 * @author JD-Team
 */
public class EasyCaptcha {

    private static void mergeObjects(Vector<PixelObject> os, Captcha captcha, int[] pixels) {
        if (os.size() <= captcha.owner.getLetterNum()) return;
        int minh = pixels[1] * 2 / 3;
        int minw = pixels[0] / (captcha.owner.getLetterNum() * 2 / 3);
        int gab = pixels[2] / (captcha.owner.getLetterNum());
        for (PixelObject a : os) {
            for (PixelObject b : os) {
                if (a == b) continue;

                if (!(b.getWidth() < minw || b.getHeight() < minh || a.getWidth() < minw || a.getHeight() < minh)) continue;
                int xMin = Math.max(a.getXMin(), b.getXMin());
                int xMax = Math.min(a.getXMin() + a.getWidth(), b.getXMin() + b.getWidth());
                if (Math.abs(xMax - xMin) > gab / 3 && xMax < xMin) continue;
                if (Math.abs(xMax - xMin) < minw) {
                    a.add(b);
                    os.remove(b);
                    mergeObjects(os, captcha, pixels);
                    return;
                }
            }
        }

    }

    private static Vector<PixelObject> getRightletters(Vector<PixelObject> os, Captcha captcha, int[] pixels) {
        if (os.size() >= captcha.owner.getLetterNum()) return os;
        int minw = pixels[0] / (captcha.owner.getLetterNum() * 3 / 2);
        PixelObject biggest = os.get(0);
        for (int i = 1; i < os.size(); i++) {
            PixelObject po = os.get(i);
            if (po.getWidth() > biggest.getWidth()) biggest = po;
        }
        if (biggest.getWidth() > minw) {
            PixelObject[] bs = biggest.cut(biggest.getWidth() / 2, biggest.getWidth(), 0);
            os.remove(biggest);

            for (PixelObject pixelObject : bs) {
                if (pixelObject != null) os.add(pixelObject);
            }
            return getRightletters(os, captcha, pixels);
        }
        return os;
    }

    private static int[] clean(Captcha captcha) {
        File file = captcha.owner.getResourceFile("CPoints.xml");
        // System.out.println(file);
        Vector<CPoint> ret = ColorTrainer.load(file);
        // gibt an welche höhe der größte Buchstabe hat
        int retYmax = 0;
        // breite aller Buchstaben
        int retx = 0;
        int gap = 0;
        int lastgap = -1;

        // farbunterscheidung durch ebenen einbauen
        for (int x = 0; x < captcha.getWidth(); x++) {
            int bcuy = 0;
            for (int y = 0; y < captcha.getHeight(); y++) {

                captcha.grid[x][y] = captcha.getPixelValue(x, y);
                double bestDist1 = Double.MAX_VALUE;
                CPoint cpBestDist1 = null;
                double bestDist2 = Double.MAX_VALUE;
                CPoint cpBestDist2 = null;
                for (CPoint cp : ret) {
                    double dist = cp.getColorDifference(captcha.getPixelValue(x, y));

                    if (bestDist1 > dist) {
                        bestDist1 = dist;
                        cpBestDist1 = cp;
                    }
                    if (dist < cp.getDistance()) {
                        if (bestDist2 > dist) {
                            bestDist2 = 0;
                            cpBestDist2 = cp;
                        }
                    }
                }
                if (cpBestDist2 != null) {
                    if (!cpBestDist2.isForeground())
                        captcha.setPixelValue(x, y, 0xFFFFFF);
                    else {
                        captcha.setPixelValue(x, y, 0x000000);
                        bcuy++;
                    }

                } else if (cpBestDist1 != null) {
                    if (!cpBestDist1.isForeground())
                        captcha.setPixelValue(x, y, 0xFFFFFF);
                    else {
                        captcha.setPixelValue(x, y, 0x000000);
                        bcuy++;
                    }

                } else {
                    captcha.setPixelValue(x, y, 0x000000);
                }

            }
            if (bcuy > retYmax) retYmax += bcuy;
            if (bcuy > 0) {
                if (lastgap == -1) {
                    lastgap = 0;
                } else
                    lastgap += gap;
                retx++;
                gap = 0;
            } else
                gap++;

        }
        return new int[] { retx, retYmax, lastgap };
    }

    public static Letter[] getLetters(Captcha captcha) {
        int[] pixels = clean(captcha);
        Vector<PixelObject> os = captcha.getObjects(0.5, 0.5);
        Collections.sort(os);
        mergeObjects(os, captcha, pixels);
        getRightletters(os, captcha, pixels);
        Collections.sort(os);
        ArrayList<Letter> ret = new ArrayList<Letter>();
        for (PixelObject pixelObject : os) {
            if (pixelObject.getArea() > 50) {
                Letter let = pixelObject.toLetter();
                let.removeSmallObjects(0.75, 0.75, 6);
                ret.add(let);
            }
        }
        return ret.toArray(new Letter[] {});

    }
}