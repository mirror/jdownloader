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
import java.util.Map.Entry;
import java.util.Vector;

import jd.captcha.JAntiCaptcha;
import jd.captcha.LevenShteinLetterComperator;
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
        int best;
        int matching = Math.min((aos.getXMax()), (nextos.getXMax())) - Math.max(aos.getXMin(), nextos.getXMin());
        if (matching >= 0)
            best = -matching;
        else
            best = Math.min((Math.min(Math.abs((aos.getXMin() + aos.getWidth()) - (nextos.getXMin())), Math.abs((aos.getXMin()) - (nextos.getXMin() + nextos.getWidth())))), (Math.min(Math.abs(aos.getXMin() - nextos.getXMin()), Math.abs((aos.getXMin() + aos.getWidth()) - (nextos.getXMin() + nextos.getWidth())))));
        for (int i = 1; i < os.size(); i++) {
            PixelObject b = os.get(i);
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
        nextos.add(aos);
    }

    public static int[] mergeObjectsBasic(List<PixelObject> os, Captcha captcha, int gab) {
        int area = 0;
        int[] ret = null;
        int osSize = 0;
        try {
            PixelObject aos = os.get(0);
            PixelObject biggest = aos;
            for (PixelObject pixelObject : os) {
                area += pixelObject.getArea();
                osSize += pixelObject.getSize();

                if (aos.getWidth() > pixelObject.getWidth()) aos = pixelObject;
                if (biggest.getArea() < pixelObject.getArea()) biggest = pixelObject;
            }
            ret = new int[] { area / (os.size() * 3), osSize / (os.size() * 3) };

            for (PixelObject pixelObject : os) {
                if (pixelObject.getArea() > (area / 3) && (osSize / (captcha.owner.getLetterNum())) / 5 > pixelObject.getSize()) {
                    os.remove(pixelObject);
                    return mergeObjectsBasic(os, captcha, gab);
                }
            }
            for (int i = 0; i < os.size(); i++) {
                PixelObject b = os.get(i);
                for (int j = 0; j < i; j++) {
                    PixelObject a = os.get(j);
                    if (a.getWidth() < b.getWidth()) {
                        if ((b.getXMin() - a.getXMin()) <= 0 && (b.getXMax() - (a.getXMax()) >= 0)) {
                            b.add(a);
                            os.remove(a);
                            return mergeObjectsBasic(os, captcha, gab);
                        }
                    }
                }

            }

            for (int i = 0; i < os.size(); i++) {
                PixelObject a = os.get(i);
                if (a.getSize() < (osSize / (captcha.owner.getLetterNum() / 2))) {
                    PixelObject nextos = null;
                    int best = Integer.MAX_VALUE;
                    for (int j = 0; j < i; j++) {
                        PixelObject b = os.get(j);
                        if (b == a) continue;
                        int ib;
                        int matching = Math.min((aos.getXMax()), (b.getXMax())) - Math.max(aos.getXMin(), b.getXMin());
                        if (matching >= 0)
                            ib = -matching;
                        else
                            ib = Math.min((Math.min(Math.abs((aos.getXMin() + aos.getWidth()) - (b.getXMin())), Math.abs((aos.getXMin()) - (b.getXMin() + b.getWidth())))), (Math.min(Math.abs(aos.getXMin() - b.getXMin()), Math.abs((aos.getXMin() + aos.getWidth()) - (b.getXMin() + b.getWidth())))));
                        if (ib < best) {
                            best = ib;
                            nextos = b;
                        }
                    }
                    if (best < (gab / 60) && nextos != a) {
                        nextos.add(a);
                        os.remove(a);
                        return mergeObjectsBasic(os, captcha, gab);
                    }
                }

            }

            if (os.size() > captcha.owner.getLetterNum()) {

                for (int i = 0; i < os.size(); i++) {
                    PixelObject a = os.get(i);
                    if (a.getArea() < area) {
                        PixelObject nextos = null;
                        int best = Integer.MAX_VALUE;
                        for (int j = 0; j < i; j++) {
                            PixelObject b = os.get(j);
                            if (b == a) continue;
                            int ib;
                            int matching = Math.min((aos.getXMax()), (b.getXMax())) - Math.max(aos.getXMin(), b.getXMin());
                            if (matching >= 0)
                                ib = -matching;
                            else
                                ib = Math.min((Math.min(Math.abs((aos.getXMin() + aos.getWidth()) - (b.getXMin())), Math.abs((aos.getXMin()) - (b.getXMin() + b.getWidth())))), (Math.min(Math.abs(aos.getXMin() - b.getXMin()), Math.abs((aos.getXMin() + aos.getWidth()) - (b.getXMin() + b.getWidth())))));
                            if (ib < best) {
                                best = ib;
                                nextos = b;
                            }
                        }
                        if (best < (gab / 60) && nextos != a) {
                            nextos.add(a);
                            os.remove(a);
                            return mergeObjectsBasic(os, captcha, gab);
                        }
                    }

                }
            }
            // if(true) return ret;

            if ((biggest.getArea() / (captcha.owner.getLetterNum() * 2)) > aos.getArea() || (osSize / (captcha.owner.getLetterNum() * 2)) > aos.getSize()) {
                mergeos(aos, os);
                return mergeObjectsBasic(os, captcha, gab);

            }

            /*
             * ist raus wegen schrägen letters
             * 
             * for (PixelObject b : os) { if (b != aos && (b.getXMin() -
             * aos.getXMin()) <= 0 && ((b.getXMin() + b.getWidth()) -
             * ((aos.getXMin() + aos.getWidth())) >= 0)) { b.add(aos);
             * os.remove(aos); return mergeObjectsBasic(os, captcha, gab);
             * 
             * } }
             */
            if (os.size() <= captcha.owner.getLetterNum()) {
                area /= os.size() * 2;
                osSize /= os.size() * 2;
            } else {
                osSize /= os.size();
                area /= os.size();

            }

            if (aos.getArea() < area || aos.getSize() < osSize) {
                mergeos(aos, os);
                return mergeObjectsBasic(os, captcha, gab);

            }
            for (PixelObject pixelObject : os) {
                if (pixelObject != aos && pixelObject.getSize() < (aos.getSize() / 2)) {
                    mergeos(pixelObject, os);
                    return mergeObjectsBasic(os, captcha, gab);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ret;
    }

    private static int[] findGab(PixelObject biggest, Captcha captcha) {
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
        int gabAverage = 0;
        for (int g : gab) {
            gabAverage += g;
        }
        gabAverage /= gab.length;
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
            }
        }
        if (gab[best] == 0) return new int[] { best, gab[best], gabAverage };
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
        }
        return new int[] { best, gab[best], gabAverage };
    }

    public static List<PixelObject> getRightletters(List<PixelObject> os, Captcha captcha, int[] pixels, int[] mergeInfos, int i) {
        i++;
        if (i > captcha.owner.getLetterNum() * 2) return os;
        for (Iterator<PixelObject> iterator = os.iterator(); iterator.hasNext();) {
            PixelObject elem = iterator.next();
            if (os.size() < 3 || os.size() < os.size() - 1) break;
            if (elem.getArea() < (mergeInfos[0] / (captcha.owner.getLetterNum() * 4)) || elem.getSize() < (mergeInfos[1] / (captcha.owner.getLetterNum() * 5))) {
                iterator.remove();
            }
        }
        if (os.size() >= captcha.owner.getLetterNum()) return os;
        int minw = pixels[0] / (captcha.owner.getLetterNum() * 3 / 2);
        if (os.size() >= captcha.owner.getLetterNum() - 1) minw = pixels[0] / (captcha.owner.getLetterNum());
        PixelObject biggest = null;
        for (PixelObject po : os) {
            if (biggest == null || (po.getWidth() > biggest.getWidth())) {
                if (po.detected == null) {
                    biggest = po;
                }
            }
        }
        // System.out.println(biggest);
        if (biggest != null && biggest.getWidth() > minw) {

            int[] gabBiggest = findGab(biggest, captcha);
            PixelObject[] bs = biggest.splitAt(gabBiggest[0]);
            // BasicWindow.showImage(biggest.toLetter().getImage(),"biggest"+gabBiggest[1]);

            if (gabBiggest[1] != 0 && captcha.owner.letterDB.size() > 3) {
                LevenShteinLetterComperator lc = new LevenShteinLetterComperator(captcha.owner);
                lc.detectHorizonalOffset = true;
                lc.detectVerticalOffset = true;
                Letter bestBiggest = biggest.toLetter();
                bestBiggest.toBlackAndWhite();
                captcha.owner.jas.executeLetterPrepareCommands(bestBiggest);
                lc.run(bestBiggest);
                Letter bestBiggestBack = bestBiggest;

                Letter bestA = biggest.toLetter();
                bestA.toBlackAndWhite();
                lc.run(bestA);

                Letter bestB = bs[1].toLetter();
                bestB.toBlackAndWhite();
                captcha.owner.jas.executeLetterPrepareCommands(bestB);
                lc.run(bestB);
                boolean be = true;
                if ((((bestBiggest.getDecodedValue() != null && (bestBiggest.getDecodedValue().toLowerCase().equals("n") || bestBiggest.getDecodedValue().toLowerCase().equals("v") || bestBiggest.getDecodedValue().toLowerCase().equals("i")))) && bestBiggest.detected.getValityPercent() < bestB.detected.getValityPercent()) || (bestB.getDecodedValue() != null && (bestB.getDecodedValue().toLowerCase().equals("n") || bestB.getDecodedValue().toLowerCase().equals("v") || bestB.getDecodedValue().toLowerCase().equals("i")))) {
                    bestB = bestBiggest;
                    be = false;
                }
                if (bestA.detected.getValityPercent() <= bestBiggest.detected.getValityPercent() && bestA.detected.getValityPercent() < 30) {
                    os.get(os.indexOf(biggest)).detected = bestA.detected;
                    return getRightletters(os, captcha, pixels, mergeInfos, i);
                }

                if (bestBiggest.getDecodedValue() != null && bestBiggest.detected.getValityPercent() < 30) {

                    int[] offset = bestBiggest.detected.getOffset();
                    // System.out.println(offset[0]);
                    if (offset != null) {
                        double bwd = (double) biggest.getWidth() / (double) bestBiggestBack.getWidth();
                        int gab = 0;
                        if (be)
                            gab = (int) (biggest.getWidth() - ((offset[0] + bestBiggest.detected.getB().getWidth()) * bwd));
                        else
                            gab = (int) ((offset[0] + bestBiggest.detected.getB().getWidth()) * bwd);
                        if (gab == biggest.getWidth() || gab == 0) {
                            gab = gabBiggest[0];
                        }
                        bs = biggest.splitAt(gab);
                        // BasicWindow.showImage(biggest.toLetter().getImage(),""+gab);

                    }
                    // BasicWindow.showImage(r.getB().getImage(),""+r.getDecodedValue());

                }

            }

            os.remove(biggest);
            for (PixelObject pixelObject : bs) {
                if (pixelObject != null) {
                    os.add(pixelObject);
                }
            }

            return getRightletters(os, captcha, pixels, mergeInfos, i);
        }
        return os;
    }

    static void mergeColors(List<PixelObject> os, int osSize, Captcha captcha) {
        if (os.size() < 2) return;
        for (PixelObject os2 : os) {
            if (os2.getSize() < (osSize / (captcha.owner.getLetterNum() * 2))) {
                try {
                    mergeos(os2, os);
                    mergeColors(os, osSize, captcha);
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    // os.add(os2);
                }
            }

        }

    }

    public static Object[] clean(Captcha captcha) {
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
        boolean bw = captcha.owner.jas.getBoolean("easyCaptchaBW");
        java.util.List<PixelObject> reto = new ArrayList<PixelObject>();
        java.util.List<PixelObject> merge;
        int nrx = (int) (captcha.getWidth() / (captcha.owner.getLetterNum() * 1.2));
        int nry = captcha.getHeight();
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
                        if (bw) captcha.setPixelValue(x, y, 0x000000);

                        bcuy++;
                    }

                } else if (cpBestDist1 != null) {
                    if (!cpBestDist1.isForeground())
                        captcha.setPixelValue(x, y, 0xFFFFFF);
                    else {
                        add = true;
                        captcha.setPixelValue(x, y, 0xFFFFFF);
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
                        int mc = o.getMostcolor();
                        if (o.isTouching(x, y, true, nrx, nry) && ((rgbdiff = Colors.getRGBColorDifference2(gc, mc)) < 6 || (rgbdiff < 25 && Colors.getColorDifference(gc, mc) < 8))) {
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
            if (bcuy > retYmax) retYmax = bcuy;
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
        if (bw) {
            captcha.setOrgGrid(captcha.grid);
        }
        int gab = lastgap / (captcha.owner.getLetterNum());
        java.util.List<PixelObject> reto2 = new ArrayList<PixelObject>();
        int osSize = 0;
        for (PixelObject pixelObject : reto) {
            osSize += pixelObject.getSize();
        }
        // mergeColors(reto, osSize, captcha);
        for (PixelObject pixelObject : reto) {
            try {
                Vector<PixelObject> co = getSWCaptcha(pixelObject).getObjects(0.5, 0.5);
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
        java.util.List<PixelObject> os = (java.util.List<PixelObject>) cl[3];
        Collections.sort(os);
        int gab = pixels[2] / (captcha.owner.getLetterNum());

        int[] mergeInfos = mergeObjectsBasic(os, captcha, gab);
        getRightletters(os, captcha, pixels, mergeInfos, 0);
        Collections.sort(os);
        java.util.List<Letter> ret = new ArrayList<Letter>();
        for (PixelObject pixelObject : os) {
            if (pixelObject.getArea() > (mergeInfos[0] / (captcha.owner.getLetterNum() * 3)) && pixelObject.getSize() > (mergeInfos[1] / (captcha.owner.getLetterNum() * 5))) {

                Letter let = pixelObject.toLetter();
                if (captcha.owner.jas.getBoolean("easyCaptchaRemoveSmallObjects")) {
                    let.removeSmallObjects(0.75, 0.75, pixelObject.getSize() / 10, pixelObject.getWidth() / 3, pixelObject.getHeight() / 3);
                    let = let.toPixelObject(0.75).toLetter();
                }
                captcha.owner.jas.executeLetterPrepareCommands(let);
                ret.add(let);
            }
        }
        return ret.toArray(new Letter[] {});

    }

    public static Letter[] letterFilter(Letter[] org, JAntiCaptcha jac) {
        new LevenShteinLetterComperator(jac).run(org);
        return org;
    }
}