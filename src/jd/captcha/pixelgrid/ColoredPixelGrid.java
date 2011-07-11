//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.captcha.pixelgrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import jd.captcha.pixelobject.PixelObject;
import jd.nutils.Colors;

public class ColoredPixelGrid extends PixelGrid {
    private static final long serialVersionUID = 1L;

    public ColoredPixelGrid(int width, int height) {
        super(width, height);
    }

    @Override
    public Vector<PixelObject> getColorObjects(int letterNum) {

        // int percent =
        // owner.getJas().getInteger("colorObjectDetectionPercent");
        // int running =
        // owner.getJas().getInteger("colorObjectDetectionRunningAverage");
        logger.info("Max pixel value: " + this.getMaxPixelValue());
        // Erstelle Farbverteilungsmap
        HashMap<Integer[], PixelObject> map = new HashMap<Integer[], PixelObject>();
        logger.info("" + Colors.getColorDifference(new int[] { 0, 0, 204 }, new int[] { 0, 0, 184 }));
        logger.info("" + Colors.getColorDifference(new int[] { 0, 0, 204 }, new int[] { 60, 10, 240 }));

        logger.info("" + Colors.getColorDifference(new int[] { 255, 255, 255 }, new int[] { 0, 0, 0 }));
        final int avg = getAverage();
        int intensivity = 9;
        int h = getWidth() / letterNum / 4;
        Integer[] last = null;
        int d = 0;
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {

                Integer key = getPixelValue(x, y);
                int[] rgbA = Colors.hexToRgb(key);

                if (isElement(key, avg) || Colors.rgb2hsb(rgbA[0], rgbA[1], rgbA[2])[0] * 100 > 0) {
                    // TODO map.get(key) is wrong; map is defined as
                    // <Integer[],...> not <Integer,...>
                    // so practically map.get(key) will always be null
                    if (map.get(key) == null) {
                        if (d++ < getHeight() * 2) {
                            d = 0;
                            int[] bv = Colors.hexToRgb(key);
                            boolean found = false;
                            if (last != null && Colors.getHueColorDifference(bv, Colors.hexToRgb(map.get(last).getAverage())) < intensivity) {
                                map.get(last).add(x, y, key);
                                found = true;
                            } else {
                                Iterator<Integer[]> iterator = map.keySet().iterator();
                                Iterator<PixelObject> valsiter = map.values().iterator();
                                Integer[] bestKey = new Integer[] { -1, -1 };
                                double bestValue = 255;
                                double dif = 255;
                                while (iterator.hasNext() && valsiter.hasNext()) {
                                    Integer[] key2 = iterator.next();
                                    PixelObject object = valsiter.next();
                                    if (Math.abs((double) (x - key2[1] - object.getWidth())) < h) {
                                        dif = Colors.getHueColorDifference(bv, Colors.hexToRgb(object.getAverage()));

                                        if (dif < bestValue) {
                                            bestKey = key2;
                                            bestValue = dif;
                                            // map.get(key2).add(x, y,
                                            // getPixelValue(x,
                                            // y));

                                        }
                                    }

                                }
                                if (bestValue < intensivity) {
                                    map.get(bestKey).add(x, y, key);
                                    found = true;
                                }
                            }
                            if (!found) {
                                PixelObject object = new PixelObject(this);
                                object.add(x, y, key);
                                last = new Integer[] { key, x };
                                map.put(last, object);
                            }
                        } else {
                            PixelObject object = new PixelObject(this);
                            object.add(x, y, key);
                            last = new Integer[] { key, x };
                            map.put(last, object);
                            d = 0;
                        }

                    } else {
                        // TODO map.get(key) is wrong; map is defined as
                        // <Integer[],...> not <Integer,...>
                        // so practically map.get(key) will always be null
                        map.get(key).add(x, y, key);
                    }
                } else {
                    d++;
                }
            }
        }

        // int total = getWidth() * getHeight();
        ArrayList<Object[]> els = new ArrayList<Object[]>();

        Iterator<PixelObject> vals = map.values().iterator();
        Iterator<Integer[]> keys = map.keySet().iterator();
        while (keys.hasNext() && vals.hasNext()) {
            PixelObject ob = vals.next();
            els.add(new Object[] { keys.next(), ob });

        }
        Collections.sort(els, new Comparator<Object[]>() {
            public int compare(Object[] o1, Object[] o2) {
                Letter letter1 = ((PixelObject) o1[1]).toLetter();
                Letter letter2 = ((PixelObject) o2[1]).toLetter();
                if (letter1.getElementPixel() > letter2.getElementPixel()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        int c = map.size();
        if (c > letterNum) {
            Iterator<Object[]> iter = els.iterator();

            double addd = intensivity / 2;
            while (c > letterNum) {
                if (!iter.hasNext()) {
                    iter = els.iterator();
                    addd++;
                }
                Object[] thisel = iter.next();
                Integer[] integers = (Integer[]) thisel[0];
                PixelObject object = (PixelObject) thisel[1];
                Iterator<Object[]> iterator = els.iterator();
                Integer[] bestKey = null;
                PixelObject bestobj = null;
                double bestValue = Double.MAX_VALUE;
                double dif = Double.MAX_VALUE;
                double dif2 = Double.MAX_VALUE;
                while (iterator.hasNext()) {
                    Object[] it = iterator.next();
                    PixelObject obj = (PixelObject) it[1];
                    Integer[] key2 = (Integer[]) it[0];
                    if (key2 != integers) {
                        dif = key2[1] - integers[1];
                        dif2 = Math.abs((double) (key2[1] + obj.getWidth() - (integers[1] + object.getWidth())));

                        if (dif == 0 || dif2 == 0 || dif < 0 && dif + obj.getWidth() > 0) {
                            map.get(key2).add(object);
                            map.remove(integers);
                            iter.remove();
                            c--;
                            bestKey = null;
                            break;
                        }
                        if (Math.abs(dif) < bestValue) {
                            bestKey = key2;
                            bestobj = obj;
                            bestValue = Math.abs(dif);
                            // map.get(key2).add(x, y, getPixelValue(x, y));
                        }
                        if (dif2 < bestValue) {
                            bestKey = key2;
                            bestobj = obj;
                            bestValue = dif2;
                        }
                    }

                }
                if (bestKey != null) {
                    dif = Colors.getHueColorDifference(Colors.hexToRgb(bestobj.getAverage()), Colors.hexToRgb(object.getAverage()));
                    if (dif < addd) {
                        map.get(bestKey).add(object);
                        map.remove(integers);
                        iter.remove();
                        c--;
                    }
                }
            }
        }

        ArrayList<Integer[]> ar = new ArrayList<Integer[]>();
        ar.addAll(map.keySet());
        Collections.sort(ar, new Comparator<Integer[]>() {
            public int compare(Integer[] o1, Integer[] o2) {
                return o1[1].compareTo(o2[1]);
            }
        });
        Iterator<Integer[]> iterator2 = ar.iterator();
        Vector<PixelObject> ret = new Vector<PixelObject>();
        while (iterator2.hasNext()) {
            PixelObject it = map.get(iterator2.next());
            ret.add(it);
        }
        return ret;
    }

}
