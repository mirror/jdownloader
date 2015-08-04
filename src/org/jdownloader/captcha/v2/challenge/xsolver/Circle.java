package org.jdownloader.captcha.v2.challenge.xsolver;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelobject.PixelObject;
import jd.nutils.Colors;

// CaptX autosolve stuff
class Circle {
    int                       inBorder        = 4;
    int                       outBorder       = 2;

    int                       minArea         = 170;
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
                                               if (isBackground(o1) || isBackground(o2)) {
                                                   return 0;
                                               }
                                               if (c == 0x000000 || c2 == 0x000000) {
                                                   return c == c2 ? 1 : 0;
                                               }
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
            if (n.getSize() > 0) {
                b.add(n);
            }
        }
        if (b.getSize() > 10 && b.getArea() > 30) {
            return b;
        }
        return null;
    }

    private int checkBackground(int x, int y, PixelObject n) {
        int c = captcha.getPixelValue(x, y);
        boolean b = isBackground(c);
        if (!b) {
            n.add(x, y, c);
        }
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
        if (x != 0) {
            ret += checkBackground(cx - x, cy + y, n);
        }
        if (y != 0) {
            ret += checkBackground(cx + x, cy - y, n);
        }
        if (x != 0 && y != 0) {
            ret += checkBackground(cx - x, cy - y, n);
        }
        return ret;
    }

    /**
     * returns the Circles Bounds on the Captcha TODO geht nur bei x entlang sollte noch bei y gemacht werden um bessere ergebnisse zu
     * bekommen
     *
     * @param pixelObject
     * @param captcha
     * @return
     */
    private int[] getBounds(PixelObject pixelObject) {
        if (pixelObject.getSize() < 5 || pixelObject.getArea() < minArea) {
            return null;
        }
        Letter let = pixelObject.toColoredLetter();
        int r = let.getWidth() / 2;
        try {
            int ratio = pixelObject.getHeight() * 100 / pixelObject.getWidth();
            if ((ratio > 95 && ratio < 105) || equalElements(let.getGrid()[r][0], let.getGrid()[0][r]) || equalElements(let.getGrid()[r][let.getWidth() - 1], let.getGrid()[0][r]) || equalElements(let.getGrid()[r][0], let.getGrid()[let.getWidth() - 1][r]) || equalElements(let.getGrid()[r][let.getWidth() - 1], let.getGrid()[let.getWidth() - 1][r])) {
                return new int[] { let.getLocation()[0] + r, let.getLocation()[1] + let.getWidth() };
            }

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

                    if (isBackground(captcha.grid[x][y])) {
                        break;
                    }
                }

                // if (oldy == y || h < y) continue;
                int oldy = y;

                for (; y < h; y++) {
                    if (!isBackground(captcha.grid[x][y]) && equalElements(c, captcha.grid[x][y])) {
                        break;
                    }
                }
                if (oldy == y || h < y) {
                    continue;
                }
                oldy = y;

                for (; y < h; y++) {
                    if (isBackground(captcha.grid[x][y])) {
                        break;
                    }
                }

                if (oldy == y) {
                    continue;
                }
                if (y == let.getHeight() && Math.abs(let.getHeight() - let.getWidth()) > 15) {
                    continue;
                }
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
        if (best.size() == 0) {
            return null;
        } else {
            int x = 0;
            for (int[] is : best) {
                x += is[0];
            }
            return new int[] { x / best.size(), best.get(0)[1] };
        }
    }

    private void addCircles(PixelObject pixelObject, java.util.List<PixelObject> obnew) {
        if (pixelObject.getArea() < minArea) {
            return;
        }
        int[] bounds = getBounds(pixelObject);
        int r = 0;
        if (bounds != null) {
            r = (bounds[1] - pixelObject.getLocation()[1]) / 2;

            PixelObject object = getCircle(bounds[0], bounds[1] - r, r);

            if (object != null) {
                int ratio = object.getHeight() * 100 / object.getWidth();
                if (ratio > 90 && ratio < 110) {
                    obnew.add(object);
                    pixelObject.del(object);
                }
            }
        }

    }

    private static BufferedImage copyImage(BufferedImage image) {
        ColorModel colorModel = image.getColorModel();
        WritableRaster raster = image.copyData(null);
        return new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied(), null);
    }

    /**
     * returns true if coordinates are out of the bounds of the image
     *
     * @param img
     * @param x
     * @param y
     * @return
     */
    private boolean outOfBounds(BufferedImage img, int x, int y) {
        if (x >= img.getWidth()) {
            return true;
        }
        if (x < 0) {
            return true;
        }
        if (y >= img.getHeight()) {
            return true;
        }
        if (y < 0) {
            return true;
        }
        return false;
    }

    /**
     * expand the circle by expanding colored pixels to their neighbors
     *
     * @param src
     *            the image
     * @return a copy of the image
     */
    private BufferedImage expandImage(BufferedImage src) {
        BufferedImage tgt = copyImage(src);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int c = src.getRGB(x, y);
                if (Colors.getCMYKColorDifference1(c, Color.WHITE.getRGB()) > 4) {
                    tgt.setRGB(x, y, c);
                    if (x < src.getWidth() - 1) {
                        tgt.setRGB(x, y, c);
                        if (!outOfBounds(src, x, y - 1)) {
                            tgt.setRGB(x, y - 1, c);
                        }
                        if (!outOfBounds(src, x + 1, y)) {
                            tgt.setRGB(x + 1, y, c);
                        }
                        if (!outOfBounds(src, x, y + 1)) {
                            tgt.setRGB(x, y + 1, c);
                        }
                        if (!outOfBounds(src, x - 1, y)) {
                            tgt.setRGB(x - 1, y, c);
                        }
                    }
                }
            }
        }
        return tgt;
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
                addCircles(pixelObject, obnew);
            }
        }
        return obnew;
    }

    /**
     * Gets the longest part of a circle which is missing
     *
     * @param img
     *            image containing a circle (with center in center)
     * @param r
     *            radius of circle to compare
     * @return longest missing part
     */
    private int getLongestOff(BufferedImage img, int r) {
        int xc = (int) (new Double(img.getWidth()) / 2.0d);
        int yc = (int) (new Double(img.getHeight()) / 2.0d);

        int longestOff = 0;
        int tmp = 0;

        for (Double theta = 0.0d; theta < 3 * Math.PI; theta += 0.1d) {
            double x = r * Math.cos(theta) + xc;
            double y = r * Math.sin(theta) + yc;

            int color = img.getRGB((int) x, (int) y);
            if (Colors.getCMYKColorDifference1(Color.white.getRGB(), color) > 5) {
                tmp = 0;
            } else {
                // TODO: limit to 1/3*2pi (radian)
                tmp++;
                if (tmp > longestOff) {
                    longestOff = tmp;
                }
            }

        }
        return longestOff;

    }

    /**
     * returns the open circle
     *
     * @return
     */
    Letter getOpenCircle() {
        if (openCircle != null) {
            return openCircle;
        }
        objectArray = getCircles();
        Letter best = null;
        int maxLongestOff = Integer.MIN_VALUE;
        for (PixelObject pixelObject : objectArray) {
            Letter let = pixelObject.toColoredLetter();

            int tmp = getLongestOff(expandImage(let.getImage()), (int) (new Double(Math.min(let.getHeight(), let.getWidth())) / 2.0d) - 2);
            if (tmp > maxLongestOff) {
                maxLongestOff = tmp;
                best = let;
            }
        }
        openCircle = best;
        return best;
    }
}