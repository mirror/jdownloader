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

import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.pixelobject.PixelObject;
import jd.captcha.utils.UTILITIES;

/**
 * 
 * 
 * @author JD-Team
 */
public class Filefactory {

    // private static final double OBJECTCOLORCONTRAST = 0.01;

    private static final double OBJECTDETECTIONCONTRAST = 0.95;

    private static ArrayList<PixelObject> getObjects(PixelGrid grid, int tollerance) {
        ArrayList<PixelObject> ret = new ArrayList<PixelObject>();
        ArrayList<PixelObject> merge;
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                if (grid.getGrid()[x][y] == 0xffffff) continue;

                PixelObject n = new PixelObject(grid);
                n.add(x, y, grid.getGrid()[x][y]);

                merge = new ArrayList<PixelObject>();
                for (Iterator<PixelObject> it = ret.iterator(); it.hasNext();) {
                    PixelObject o = it.next();

                    if (o.isTouching(x, y, true, 5, 5) && UTILITIES.getColorDifference(grid.getGrid()[x][y], o.getAverage()) < 15) {

                        merge.add(o);
                        // n.add(o);
                        //                     
                        // it.remove();
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

                // BasicWindow.showImage(grid.getImage(3), x+"-"+y);
                // ret = ret;
            }
            // BasicWindow.showImage(grid.getImage(6), x+"-");
        }

        return ret;
    }

    @SuppressWarnings("unchecked")
    public static Letter[] getLetters(Captcha captcha) {
        captcha.cleanByRGBDistance(-1, 20);
        // long t = System.currentTimeMillis();
        ArrayList<PixelObject> os = getObjects(captcha, 0);

        Collections.sort(os);
        for (Iterator<PixelObject> it = os.iterator(); it.hasNext();) {
            PixelObject akt = it.next();
            // if(akt.getSize()>20)
            // BasicWindow.showImage(akt.toLetter().getImage(5), "fil " +
            // akt.getArea() + " -" + ((double) akt.getArea() / (double)
            // akt.getSize()) + " - " + akt.getHeight() + " - " +
            // akt.getWidth());
            if (akt.getArea() == 1224) {

                System.out.println("II");

            }
            if (akt.getArea() > 1800) {
                it.remove();
            } else if (akt.getArea() < 130) {
                it.remove();
            } else if ((double) akt.getArea() / (double) akt.getSize() < 1.2) {
                it.remove();
            } else if (akt.getArea() / akt.getSize() > 10) {
                it.remove();
            } else if (akt.getHeight() < 15 || akt.getWidth() < 5) {
                it.remove();
            }
            // if (true && (|| akt.getArea() < 130 || (akt.getArea() > 600 &&
            // (double) akt.getArea() / (double) akt.getSize() < 1.2) ||
            // akt.getArea() / akt.getSize() > 10 || akt.getHeight() < 10 ||
            // akt.getWidth() < 5)) {
            // it.remove();
            // // BasicWindow.showImage(akt.toLetter().getImage(5),"fil
            // // "+akt.getArea()+" -"+((double)akt.getArea() /
            // // (double)akt.getSize())+" - "+akt.getHeight()+" -
            // // "+akt.getWidth());
            //
            // } else {
            //
            // }

        }

        ArrayList<Letter> ret = new ArrayList<Letter>();
        int i = 0;
        for (PixelObject pixelObject : os) {
            Letter let = pixelObject.toLetter();

            let.blurIt(2);
            let.toBlackAndWhite(1.16);
            let.removeSmallObjects(0.8, 0.8, 30);
            let.clean();
            let = let.align(-40, 40);

            i++;
            PixelObject akt = let.toPixelObject(OBJECTDETECTIONCONTRAST);
            if (pixelObject.getArea() == 1224) {

                System.out.println("II");

            }
            if (akt.getArea() > 2200) {

            } else if (akt.getArea() < 130) {

            } else if ((double) akt.getArea() / (double) akt.getSize() < 1.2) {

            } else if (akt.getArea() / akt.getSize() > 10) {

            } else if (akt.getHeight() < 15 || akt.getWidth() < 5) {

            } else {
                ret.add(let);
            }

            // }
        }

        // for (int i = 0; i < letters.size(); i++) {
        // BasicWindow.showImage(letters.elementAt(i).toLetter().getImage(), "im
        // " + i);
        // PixelObject obj = letters.elementAt(i);
        //
        // Letter l = obj.toLetter();
        // //
        // l.removeSmallObjects(captcha.owner.getJas().getDouble(
        // "ObjectColorContrast"),
        // // captcha.owner.getJas().getDouble("ObjectDetectionContrast"));
        // captcha.owner.getJas().executeLetterPrepareCommands(l);
        // // if(owner.getJas().getInteger("leftAngle")!=0 ||
        // // owner.getJas().getInteger("rightAngle")!=0) l =
        // //
        // l.align(owner.getJas().getDouble("ObjectDetectionContrast"),owner.
        // getJas
        // ().getInteger("leftAngle"),owner.getJas().getInteger("rightAngle"));
        // // l.reduceWhiteNoise(2);
        // // l.toBlackAndWhite(0.6);
        //
        // ret[i] =
        // l.getSimplified(captcha.owner.getJas().getDouble("simplifyFaktor"));
        //
        // }
        // ret=ret.subList(0,4).toArray(a);
        return ret.toArray(new Letter[] {});

    }

    public static Letter[] letterFilter(Letter[] org, JAntiCaptcha jac) {
        int ths = Runtime.getRuntime().availableProcessors();
        MultiThreadDetection mtd = new MultiThreadDetection(ths, jac);
        // Vector<Letter> ret = new Vector<Letter>();

        ArrayList<Letter> r = new ArrayList<Letter>();
        int iii = 0;
        for (Letter l : org) {
            iii++;
            mtd.queueDetection(l);
            r.add(l);
        }
        mtd.waitFor(null);
        int id = 0;
        for (Iterator<Letter> it = r.iterator(); it.hasNext();) {
            Letter akt = it.next();

            if (akt.detected.getDecodedValue().equals("1") || akt.detected.getDecodedValue().equals(".")) {
                it.remove();
            } else {
                akt.id = id++;
            }
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
            list = r.subList(0, 4);
        } catch (Exception e) {
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