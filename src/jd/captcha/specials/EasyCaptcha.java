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
import java.util.List;
import java.util.Vector;
import java.util.Map.Entry;

import jd.captcha.easy.BackGroundImageManager;
import jd.captcha.easy.CPoint;
import jd.captcha.easy.ColorTrainer;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelobject.PixelObject;
import jd.nutils.Colors;

/**
 * 
 * 
 * @author JD-Team
 */
public class EasyCaptcha {
    public static void mergeos(PixelObject aos, List<PixelObject> os) {
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
    }

    public static int mergeObjectsBasic(List<PixelObject> os, Captcha captcha, int gab) {
        int area = 0;
        int ret = 0;
        try {

            PixelObject aos = os.get(0);
            PixelObject biggest = aos;
            for (PixelObject pixelObject : os) {
                area += pixelObject.getArea();
                if (aos.getWidth() > pixelObject.getWidth()) aos = pixelObject;
                if (biggest.getArea() < pixelObject.getArea()) biggest = pixelObject;
            }
            if ((biggest.getArea() / captcha.owner.getLetterNum()) > aos.getArea() || (biggest.getSize() / captcha.owner.getLetterNum()) > aos.getSize()) {
                mergeos(aos, os);
                return mergeObjectsBasic(os, captcha, gab);

            }
            for (PixelObject pixelObject : os) {
                if(pixelObject!=aos && pixelObject.getSize()<aos.getSize())
                {
                    mergeos(pixelObject, os);
                    return mergeObjectsBasic(os, captcha, gab);
                }
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
                mergeos(aos, os);
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

    public static ArrayList<PixelObject> getRightletters(ArrayList<PixelObject> os, Captcha captcha, int[] pixels, int area) {
        for (Iterator<PixelObject> iterator = os.iterator(); iterator.hasNext();) {
            PixelObject elem = iterator.next();
            if (elem.getArea() < (area / 2)) {
                iterator.remove();
            } 
        }
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
            return getRightletters(os, captcha, pixels, area);
        }
        return os;
    }

    private static Object[] clean(Captcha captcha) {
        captcha.owner.jas.executePrepareCommands(captcha.getCaptchaFile(), captcha);
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
        int[][] grid = captcha.grid;

        ArrayList<PixelObject> reto = new ArrayList<PixelObject>();
        ArrayList<PixelObject> merge;
        // farbunterscheidung durch ebenen einbauen
        for (int x = 0; x < captcha.getWidth(); x++) {
            int bcuy = 0;
            for (int y = 0; y < captcha.getHeight(); y++) {
                double bestDist1 = Double.MAX_VALUE;
                CPoint cpBestDist1 = null;
                double bestDist2 = Double.MAX_VALUE;
                CPoint cpBestDist2 = null;
                for (CPoint cp : ret) {
                    double dist = cp.getColorDifference(grid[x][y]);

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
                boolean add = false;
                if (cpBestDist2 != null) {
                    if (!cpBestDist2.isForeground())
                        captcha.setPixelValue(x, y, 0xFFFFFF);
                    else {
                        add = true;
                        bcuy++;
                    }

                } else if (cpBestDist1 != null) {
                    if (!cpBestDist1.isForeground())
                        captcha.setPixelValue(x, y, 0xFFFFFF);
                    else {
                        add = true;
                        bcuy++;
                    }

                } else {
                    add = true;
                }
                if (add) {
                    PixelObject n = new PixelObject(captcha);
                    int gc = captcha.getGrid()[x][y];
                    n.add(x, y, gc);
                    merge = new ArrayList<PixelObject>();
                    for (PixelObject o : reto) {
                        double rgbdiff;
                        if ((rgbdiff = Colors.getRGBColorDifference2(gc, o.getAverage())) < 6 || (rgbdiff < 25 && Colors.getColorDifference(gc, o.getAverage()) < 8)) {
                            merge.add(o);
                        }
                    }
                    if (merge.size() == 0) {
                        reto.add(n);
                    } else if (merge.size() == 1) {
                        merge.get(0).add(n);
                    } else {
                        for (PixelObject po : merge) {
                            reto.remove(po);
                            n.add(po);
                        }
                        reto.add(n);
                    }
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
        int gab = lastgap / (captcha.owner.getLetterNum());

        ArrayList<PixelObject> reto2 = new ArrayList<PixelObject>();
        for (PixelObject pixelObject : reto) {
            try {
                Vector<PixelObject> co = getSWCaptcha(pixelObject).getObjects(0.5, 0.5);
                // BasicWindow.showImage( getSWCaptcha(pixelObject).getImage());

                mergeObjectsBasic(co, captcha, gab);
                reto2.addAll(co);
            } catch (Exception e) {
            }

        }
        return new Object[] { retx, retYmax, lastgap, reto2 };
    }

    private static Captcha getSWCaptcha(PixelObject obj) {
        int[][] lgrid = new int[obj.owner.getWidth()][obj.owner.getHeight()];
        for (int x = 0; x < obj.owner.getWidth(); x++) {
            for (int y = 0; y < obj.owner.getHeight(); y++) {
                lgrid[x][y] = 0xffffff;

            }
        }
        for (int d = 0; d < obj.getSize(); d++) {
            int[] akt = obj.elementAt(d);
            lgrid[akt[0]][akt[1]] = 0x000000;
        }
        Captcha c = new Captcha(obj.owner.getWidth(), obj.owner.getHeight());
        c.owner = obj.owner.owner;
        c.setGrid(lgrid);
        return c;
    }

    public static Letter[] getLetters(Captcha captcha) {
        Object[] cl = clean(captcha);
        int[] pixels = new int[] { (Integer) cl[0], (Integer) cl[1], (Integer) cl[2] };
        @SuppressWarnings("unchecked")
        ArrayList<PixelObject> os = (ArrayList<PixelObject>) cl[3];
        Collections.sort(os);
        int gab = pixels[2] / (captcha.owner.getLetterNum());

        int area = mergeObjectsBasic(os, captcha, gab);

        getRightletters(os, captcha, pixels, area);
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