package jd.captcha.specials;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import jd.captcha.easy.CPoint;
import jd.captcha.easy.ColorTrainer;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.pixelobject.PixelObject;
import jd.nutils.Colors;

public class TrbBt {
    public static ArrayList<PixelObject> getObjects(PixelGrid grid, int neighbourradius) {
        ArrayList<PixelObject> ret = new ArrayList<PixelObject>();
        ArrayList<PixelObject> merge;
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                int c = grid.grid[x][y];
                if (c == 0xffffff) continue;

                PixelObject n = new PixelObject(grid);
                n.add(x, y, c);

                merge = new ArrayList<PixelObject>();
                for (PixelObject o : ret) {
                    if (Colors.getColorDifference(c, o.getMostcolor()) < 20 && o.isTouching(x, y, true, neighbourradius, neighbourradius)) {
                        merge.add(o);
                    }
                }
                if (merge.size() == 0) {
                    ret.add(n);
                } else if (merge.size() == 1) {
                    merge.get(0).add(n);
                } else {
                    for (PixelObject po : merge) {
                        ret.remove(po);
                        n.add(po);
                    }
                    ret.add(n);
                }

            }
        }

        return ret;
    }

    private static void toBlack(PixelGrid captcha) {
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    captcha.grid[x][y] = 0x000000;
                }

            }
        }
    }

    private static PixelObject getNexTOS(PixelObject aos, List<PixelObject> os) {
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
        return nextos;
    }

    private static void merge(List<PixelObject> os) {
        if (os.size() == 4) return;
        double mergeos = Double.MAX_VALUE;
        PixelObject aos = null;
        for (PixelObject pixelObject : os) {
            int mg = pixelObject.getSize();
            PixelObject nos = getNexTOS(pixelObject, os);
            double cd = Colors.getColorDifference(pixelObject.getMostcolor(), nos.getMostcolor()) / 100 + mg;
            if (mergeos > cd) {
                mergeos = cd;
                aos = pixelObject;
            }
        }
        EasyCaptcha.mergeos(aos, os);
        merge(os);
    }

    public static Letter[] getLetters(Captcha captcha) {
        captcha.crop(0, 5, 0, 5);
        File file = captcha.owner.getResourceFile("CPoints.xml");
        Vector<CPoint> ret = ColorTrainer.load(file);

        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                double bestDist1 = Double.MAX_VALUE;
                CPoint cpBestDist1 = null;
                double bestDist2 = Double.MAX_VALUE;
                CPoint cpBestDist2 = null;
                for (CPoint cp : ret) {
                    double dist = cp.getColorDifference(captcha.grid[x][y]);

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
                    if (!cpBestDist2.isForeground()) captcha.setPixelValue(x, y, 0xffffff);

                } else if (cpBestDist1 != null) {
                    if (!cpBestDist1.isForeground()) captcha.setPixelValue(x, y, 0xffffff);
                }
            }
        }
        ArrayList<PixelObject> os = getObjects(captcha, 5);
        merge(os);
        while (os.size() > 4) {
            PixelObject smallest = null;
            for (PixelObject pixelObject : os) {
                if (smallest == null || pixelObject.getSize() < smallest.getSize()) smallest = pixelObject;
            }
            os.remove(smallest);
        }
        Collections.sort(os);
        Letter[] lets = new Letter[os.size()];
        for (int i = 0; i < lets.length; i++) {
            lets[i] = os.get(i).toLetter();
            toBlack(lets[i]);
            lets[i].autoAlign();
            lets[i].resizetoHeight(25);
        }
        return lets;
    }
}
