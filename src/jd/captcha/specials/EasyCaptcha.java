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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;
import jd.captcha.easy.ColorTrainer;
import jd.captcha.easy.BackGroundImageManager;
import jd.nutils.Colors;
import jd.captcha.easy.CPoint;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelobject.PixelObject;

/**
 * 
 * 
 * @author JD-Team
 */
public class EasyCaptcha {
    private static int mergeObjectsBasic(Vector<PixelObject> os, Captcha captcha, int gab) {
        int area = 0;
        int ret = 0;
        try {

            PixelObject aos = os.get(0);
            for (PixelObject pixelObject : os) {
                area += pixelObject.getArea();
                if (aos.getWidth() < pixelObject.getWidth()) aos = pixelObject;
            }
            ret = area / (os.size() * 3);
            for (PixelObject b : os) {
                if (b != aos && (b.getXMin() - aos.getXMin()) <= 0 && ((b.getXMin() + b.getWidth()) - ((aos.getXMin() + aos.getWidth())) >= 0)) {
                    b.add(aos);
                    os.remove(aos);
                    return mergeObjectsBasic(os, captcha, gab);

                }
            }
            if (os.size() > captcha.owner.getLetterNum()) {
                PixelObject bestOs = null;
                PixelObject bestA = null;

                int bestOut = Integer.MAX_VALUE;
                for (int i = 0; i < os.size(); i++) {
                    PixelObject a = os.get(i);
                    PixelObject nextos = null;
                    int best = Integer.MAX_VALUE;
                    for (int j = 0; j < i; j++) {
                        PixelObject b = os.get(j);
                        if (b == a) continue;
                        int ib = Math.abs(a.getXMin() - b.getXMin()) + Math.abs((a.getXMin() + a.getWidth()) - (b.getXMin() + b.getWidth()));
                        if (ib < best) {
                            best = ib;
                            nextos = b;
                        }
                    }
                    if (best < bestOut) {
                        bestOs = nextos;
                        bestA = a;
                        bestOut = best;

                    }
                }
                if (bestOs != null) {
                    bestOs.add(bestA);
                    os.remove(bestA);
                    return mergeObjectsBasic(os, captcha, gab);
                }

            }
            for (int i = 0; i < os.size(); i++) {
                PixelObject b = os.get(i);
                for (int j = 0; j < i; j++) {
                    PixelObject a = os.get(j);
                    if (a.getWidth() < b.getWidth()) {
                        if ((b.getXMin() - a.getXMin()) <= 0 && ((b.getXMin() + b.getWidth()) - ((a.getXMin() + a.getWidth())) >= 0)) {
                            b.add(a);
                            os.remove(a);
                            return mergeObjectsBasic(os, captcha, gab);

                        }
                    }
                }

            }

            if (os.size() <= captcha.owner.getLetterNum())
                area /= os.size() * 2;
            else
                area /= os.size();
            if (aos.getArea() < area) {
                os.remove(aos);

                PixelObject nextos = os.get(0);

                int best = Math.abs(aos.getXMin() - nextos.getXMin()) + Math.abs((aos.getXMin() + aos.getWidth()) - (nextos.getXMin() + nextos.getWidth()));
                for (int i = 1; i < os.size(); i++) {
                    PixelObject b = os.get(i);
                    int ib = Math.abs(aos.getXMin() - b.getXMin()) + Math.abs((aos.getXMin() + aos.getWidth()) - (b.getXMin() + b.getWidth()));
                    if (ib < best) {
                        best = ib;
                        nextos = b;
                    }
                }
                nextos.add(aos);
                return mergeObjectsBasic(os, captcha, gab);

            }
            for (int i = 0; i < os.size(); i++) {
                PixelObject a = os.get(i);
                if (a.getArea() < area) {
                    PixelObject nextos = null;
                    int best = Integer.MAX_VALUE;
                    for (int j = 0; j < i; j++) {
                        PixelObject b = os.get(j);
                        if (b == a) continue;
                        int ib = Math.abs(a.getXMin() - b.getXMin()) + Math.abs((a.getXMin() + a.getWidth()) - (b.getXMin() + b.getWidth()));
                        if (ib < best) {
                            best = ib;
                            nextos = b;
                        }
                    }
                    if (best < gab && nextos != a) {
                        nextos.add(a);
                        os.remove(a);
                        return mergeObjectsBasic(os, captcha, gab);
                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ret;
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
            os.remove(biggest);

            int[][] grid = captcha.getOrgGridCopy();

            int[] gab = new int[biggest.getWidth()];
            @SuppressWarnings("unchecked")
            HashMap<Integer, Integer>[] colorG = new HashMap[gab.length];
            for (int i = 0; i < biggest.getSize(); i++) {
                int[] akt = biggest.elementAt(i);
                int x = akt[0] - biggest.getXMin();
                gab[x]++;
                if (colorG[x] == null) {
                    colorG[x] = new HashMap<Integer, Integer>();
                    colorG[x].put(grid[akt[0]][akt[1]], 0);
                } else {
                    if (colorG[x].containsKey(grid[akt[0]][akt[1]]))
                        colorG[x].put(grid[akt[0]][akt[1]], colorG[x].get(grid[akt[0]][akt[1]]) + 1);
                    else
                        colorG[x].put(grid[akt[0]][akt[1]], 0);
                }
            }
            int[] colorGab = new int[gab.length];
            for (int i = 0; i < colorGab.length; i++) {
                try {
                    Iterator<Entry<Integer, Integer>> cga = colorG[i].entrySet().iterator();
                    if (cga.hasNext()) {
                        Entry<Integer, Integer> bc = cga.next();
                        while (cga.hasNext()) {
                            Entry<Integer, Integer> bc2 = cga.next();
                            if (bc2.getValue() > bc.getValue()) bc = bc2;
                        }
                        colorGab[i] = bc.getKey();
                    } else {
                        colorGab[i] = -1;
                    }
                } catch (Exception e) {
                    colorGab[i] = -1;
                }

            }

            // Vector<CPoint> ret = ColorTrainer.load(file);

            int best = gab.length / 4;
            double bestCGab = Double.MIN_VALUE;
            int bestCGabPos = best + 1;
            for (int i = best + 1; i < gab.length * 3 / 4; i++) {
                try {
                    double dif = Colors.getColorDifference(colorGab[i - 1], colorGab[i]);
                    if (dif > bestCGab) {
                        bestCGab = dif;
                        bestCGabPos = i;
                    }
                    if (gab[i] < gab[best]) {
                        best = i;
                    }
                } catch (Exception e) {
                    // TODO: handle exception
                }
            }
            try {
                double t = Colors.getBrightnessColorDifference(colorGab[bestCGabPos], colorGab[bestCGabPos - 1]) / 2;
                double t2 = Colors.getHueColorDifference(colorGab[bestCGabPos], colorGab[bestCGabPos - 1]) / 2;

                if (Colors.getBrightnessColorDifference(colorGab[best], colorGab[best - 1]) < t && Colors.getBrightnessColorDifference(colorGab[best], colorGab[best + 1]) < t && Colors.getHueColorDifference(colorGab[best], colorGab[best - 1]) < t2 && Colors.getHueColorDifference(colorGab[best], colorGab[best + 1]) < t2) {
                    if (bestCGab > 2 && gab[best] * 2 / 3 < gab[bestCGabPos]) {
                        best = bestCGabPos;
                    } else if (bestCGab > 13) {
                        best = bestCGabPos;
                    }
                }
            } catch (Exception e) {
                // TODO: handle exception
            }

            PixelObject[] bs = biggest.splitAt(best);

            for (PixelObject pixelObject : bs) {
                if (pixelObject != null) os.add(pixelObject);
            }
            return getRightletters(os, captcha, pixels);
        }
        return os;
    }

    private static int[] clean(Captcha captcha) {
        File file = captcha.owner.getResourceFile("CPoints.xml");
        BackGroundImageManager bgit = new BackGroundImageManager(captcha);
        bgit.clearCaptchaAll();
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
        int gab = pixels[2] / (captcha.owner.getLetterNum());

        int area = mergeObjectsBasic(os, captcha, gab);
        for (Iterator<PixelObject> iterator = os.iterator(); iterator.hasNext();) {
            if (iterator.next().getArea() < area / 2) {
                iterator.remove();
            }
        }
        getRightletters(os, captcha, pixels);
        Collections.sort(os);
        ArrayList<Letter> ret = new ArrayList<Letter>();
        for (PixelObject pixelObject : os) {
            Letter let = pixelObject.toLetter();
            let.removeSmallObjects(0.75, 0.75, area / 10);
            let = let.toPixelObject(0.75).toLetter();
            ret.add(let);
        }
        return ret.toArray(new Letter[] {});

    }
}