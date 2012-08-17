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

package jd.captcha.specials;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelobject.PixelObject;
import jd.nutils.Colors;

import org.jdownloader.logging.LogController;

public class Circle {
    int                       inBorder        = 4;
    int                       outBorder       = 2;

    int                       minArea         = 150;
    private Captcha           captcha;
    int                       backgroundColor = 0xffffff;
    private List<PixelObject> objectArray;
    private Letter            openCircle;

    public Circle(Captcha captcha, List<PixelObject> objectArray) {
        this.captcha = captcha;
        this.objectArray = objectArray;
    }

    /**
     * is the color a color of the element 1=true 0=false
     */
    Comparator<Integer> isElementColor = new Comparator<Integer>() {

                                           public int compare(Integer o1, Integer o2) {
                                               int c = o1;
                                               int c2 = o2;
                                               if (isBackground(o1) || isBackground(o2)) return 0;
                                               if (c == 0x000000 || c2 == 0x000000) return c == c2 ? 1 : 0;
                                               int[] hsvC = Colors.rgb2hsv(c);
                                               int[] hsvC2 = Colors.rgb2hsv(c2);
                                               // TODO The "hsvC[1] / hsvC2[2] == 1" is repeated twice
                                               // Is it a typo? Was a different comparison meant in the second place?
                                               return ((hsvC[0] == hsvC2[0] && (hsvC[1] == hsvC2[1] || hsvC[2] == hsvC2[2] || hsvC[1] / hsvC2[2] == 1 || hsvC[1] / hsvC2[2] == 1)) && Colors.getRGBColorDifference2(c, c2) < 80) ? 1 : 0;
                                           }

                                       };

    private boolean equalElements(int c, int c2) {
        return isElementColor.compare(c, c2) == 1;
    }

    private boolean isBackground(int c) {
        return c < 0 || c == backgroundColor;
    }

    private PixelObject getCircle(int x, int y, int r) {
        PixelObject b = new PixelObject(captcha);
        int ret = 0;
        for (int i = -inBorder; i < outBorder / 2; i++) {
            PixelObject n = new PixelObject(captcha);
            ret += circle(x, y, r + i, n);
            if (n.getSize() > 0) b.add(n);
        }
        if (b.getSize() > 10 && b.getArea() > 30) { return b; }
        return null;
    }

    private int checkBackground(int x, int y, PixelObject n) {
        int c = captcha.getPixelValue(x, y);
        boolean b = isBackground(c);
        if (!b) n.add(x, y, c);
        return b ? 1 : 0;
    }

    // Midpoint circle algorithm
    private int circle(int cx, int cy, int radius, PixelObject n) {
        int error = -radius;
        int x = radius;
        int y = 0;
        int ret = 0;
        while (x >= y) {
            ret += plot8points(cx, cy, x, y, n);
            ret += plot8points(cx - 1, cy, x, y, n);
            ret += plot8points(cx, cy - 1, x, y, n);

            error += y;
            ++y;
            error += y;

            if (error >= 0) {
                --x;
                error -= x;
                error -= x;
            }
        }
        return ret;
    }

    // Midpoint circle algorithm
    private int plot8points(int cx, int cy, int x, int y, PixelObject n) {
        int ret = 0;
        ret += plot4points(cx, cy, x, y, n);
        if (x != y) {
            ret += plot4points(cx, cy, y, x, n);
        }
        return ret;
    }

    // Midpoint circle algorithm
    private int plot4points(int cx, int cy, int x, int y, PixelObject n) {
        int ret = 0;
        ret += checkBackground(cx + x, cy + y, n);
        if (x != 0) ret += checkBackground(cx - x, cy + y, n);
        if (y != 0) ret += checkBackground(cx + x, cy - y, n);
        if (x != 0 && y != 0) ret += checkBackground(cx - x, cy - y, n);
        return ret;
    }

    /**
     * returns the Circles Bounds on the Captcha TODO geht nur bei x entlang sollte noch bei y gemacht werden um bessere ergebnisse zu bekommen
     * 
     * @param pixelObject
     * @param captcha
     * @return
     */
    private int[] getBounds(PixelObject pixelObject) {
        if (pixelObject.getSize() < 5 || pixelObject.getArea() < minArea) return null;
        Letter let = pixelObject.toColoredLetter();
        int r = let.getWidth() / 2;
        try {
            int ratio = pixelObject.getHeight() * 100 / pixelObject.getWidth();
            if ((ratio > 95 && ratio < 105) || equalElements(let.getGrid()[r][0], let.getGrid()[0][r]) || equalElements(let.getGrid()[r][let.getWidth() - 1], let.getGrid()[0][r]) || equalElements(let.getGrid()[r][0], let.getGrid()[let.getWidth() - 1][r]) || equalElements(let.getGrid()[r][let.getWidth() - 1], let.getGrid()[let.getWidth() - 1][r])) return new int[] { let.getLocation()[0] + r, let.getLocation()[1] + let.getWidth() };

        } catch (Exception e) {
        }
        java.util.List<int[]> best = new ArrayList<int[]>();
        int h = let.getLocation()[1] + let.getHeight();

        for (int x = let.getLocation()[0]; x < let.getLocation()[0] + let.getWidth(); x++) {
            int y = let.getLocation()[1];
            int c = captcha.grid[x][y];

            if (!isBackground(c)) {

                y++;

                for (; y < h; y++) {

                    if (isBackground(captcha.grid[x][y])) break;
                }

                // if (oldy == y || h < y) continue;
                int oldy = y;

                for (; y < h; y++) {
                    if (!isBackground(captcha.grid[x][y]) && equalElements(c, captcha.grid[x][y])) break;
                }
                if (oldy == y || h < y) continue;
                oldy = y;

                for (; y < h; y++) {
                    if (isBackground(captcha.grid[x][y])) break;
                }

                if (oldy == y) continue;
                if (y == let.getHeight() && Math.abs(let.getHeight() - let.getWidth()) > 15) continue;
                if (best.size() > 0) {
                    if (y > best.get(0)[0]) {
                        best = new ArrayList<int[]>();
                        best.add(new int[] { x, y });
                    } else if (y == best.get(0)[1]) {
                        best.add(new int[] { x, y });
                    }
                } else {
                    best.add(new int[] { x, y });
                }
            }
        }
        if (best.size() == 0)
            return null;
        else {
            int x = 0;
            for (int[] is : best) {
                x += is[0];
            }
            return new int[] { x / best.size(), best.get(0)[1] };
        }
    }

    private void addCircles(PixelObject pixelObject, java.util.List<PixelObject> obnew) {
        if (pixelObject.getArea() < minArea) return;
        int[] bounds = getBounds(pixelObject);
        int r = 0;
        if (bounds != null) {
            r = (bounds[1] - pixelObject.getLocation()[1]) / 2;

            // System.out.println(r + ":" + bounds[0] + ":" + bounds[1]);
            // System.out.println(pixelObject.getHeight() / 2);

            PixelObject object = getCircle(bounds[0], bounds[1] - r, r);

            if (object != null) {
                int ratio = object.getHeight() * 100 / object.getWidth();
                // BasicWindow.showImage(object.toColoredLetter().getImage(),""+ratio);

                if (ratio > 90 && ratio < 110) {
                    obnew.add(object);
                    int oldArea = pixelObject.getArea();
                    // BasicWindow.showImage(object.toColoredLetter().getImage(),""+pixelObject.getArea());

                    pixelObject.del(object);

                    if (oldArea != pixelObject.getArea()) addCircles(pixelObject, obnew);
                }
            }
        } else {
            LogController.CL().warning("can not detect circle bounds");
            // BasicWindow.showImage(pixelObject.toColoredLetter().getImage(),""+pixelObject.getArea());

        }

    }

    /**
     * returns the detected circles
     * 
     * @return
     */
    public java.util.List<PixelObject> getCircles() {
        java.util.List<PixelObject> obnew = new ArrayList<PixelObject>();
        for (PixelObject pixelObject : objectArray) {

            if (pixelObject.getArea() > minArea) {
                // TODO
                // BasicWindow.showImage(pixelObject.toColoredLetter().getImage());

                addCircles(pixelObject, obnew);

            }
        }
        objectArray = obnew;
        return obnew;
    }

    public Letter[] getOpenCirclePositionAsLetters() {
        openCircle = getOpenCircle();
        if (openCircle == null) return null;
        int x = openCircle.getLocation()[0] + (openCircle.getWidth() / 2);
        int y = openCircle.getLocation()[1] + (openCircle.getHeight() / 2);
        return getPostionLetters(x, y);
    }

    public static Letter[] getPostionLetters(int x, int y) {
        char[] tx = (x + ":" + y).toCharArray();
        Letter[] ret = new Letter[tx.length];
        for (int i = 0; i < ret.length; i++) {
            Letter re = new Letter();
            re.setDecodedValue("" + tx[i]);
            LetterComperator let = new LetterComperator(re, re);
            let.setValityPercent(0);
            re.detected = let;
            ret[i] = re;
        }
        return ret;
    }

    /**
     * returns the open circle
     * 
     * @return
     */
    public Letter getOpenCircle() {
        if (openCircle != null) return openCircle;
        // Graphics g = image.getGraphics();
        // g.setColor(Color.black);
        // g.drawOval(55, 55, 18, 18);
        getCircles();
        // if(true)return null;
        Letter best = null;
        int bestwda = Integer.MIN_VALUE;
        for (PixelObject pixelObject : objectArray) {
            Letter let = pixelObject.toColoredLetter();
            int w = 0;

            for (int x = 0; x < let.getWidth(); x++) {
                for (int y = 0; y < let.getHeight(); y++) {
                    if (isBackground(let.getPixelValue(x, y)))
                        w++;
                    else
                        break;
                }
            }

            for (int y = 0; y < let.getHeight(); y++) {
                for (int x = 0; x < let.getWidth(); x++) {
                    if (isBackground(let.getPixelValue(x, y)))
                        w++;
                    else
                        break;
                }
            }

            for (int x = 0; x < let.getWidth(); x++) {
                for (int y = let.getHeight() - 1; y > 0; y--) {
                    if (isBackground(let.getPixelValue(x, y)))
                        w++;
                    else
                        break;
                }
            }

            for (int y = 0; y < let.getHeight(); y++) {
                for (int x = let.getWidth() - 1; x > 0; x--) {
                    if (isBackground(let.getPixelValue(x, y)))
                        w++;
                    else
                        break;
                }
            }

            int wda = w * 100 / let.getArea();
            // BasicWindow.showImage(pixelObject.toColoredLetter().getImage(),
            // ""+wda);
            // TODO
            if (wda > bestwda && let.getArea() > minArea) {

                best = let;
                bestwda = wda;
            }
        }
        openCircle = best;
        return best;
    }
}
