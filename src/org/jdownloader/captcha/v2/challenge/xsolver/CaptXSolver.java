package org.jdownloader.captcha.v2.challenge.xsolver;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import jd.captcha.JACMethod;
import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelobject.PixelObject;
import jd.captcha.utils.GifDecoder;
import jd.nutils.Colors;

/**
 * Solves a CaptX captcha (linkcrypt). That are the captcha where you have to click into the open circle
 *
 * @author flubshi
 *
 */
public class CaptXSolver {

    /**
     * Calculates coordinates of the open circle within the image
     *
     * @param captchaFile
     *            image with open circle drawn on it
     * @return coordinates of the circle
     * @throws Exception
     */
    public static Point solveCaptXCaptcha(byte[] bytes) throws Exception {
        final String method = "lnkcrptwsCircles";
        if (JACMethod.hasMethod(method)) {
            final JAntiCaptcha jac = new JAntiCaptcha(method);
            final BufferedImage image = CaptXSolver.toBufferedImage(new ByteArrayInputStream(bytes));
            final Captcha captcha = Captcha.getCaptcha(image, jac);
            final Point point = getOpenCircleCentrePoint(captcha);
            return point;
        }
        return null;
    }

    private static boolean equalElements(int c, int c2) {
        return c == c2;
    }

    private static boolean isWhite(int c) {
        return c < 0 || c == 0xffffff;
    }

    /**
     * get objects with different color
     *
     * @param grid
     * @return
     */
    public static java.util.List<PixelObject> getObjects(Captcha grid) {
        java.util.List<PixelObject> ret = new ArrayList<PixelObject>();
        java.util.List<PixelObject> merge;
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                int c = grid.getGrid()[x][y];
                if (isWhite(c)) {
                    continue;
                }
                PixelObject n = new PixelObject(grid);
                n.add(x, y, c);
                merge = new ArrayList<PixelObject>();
                for (PixelObject o : ret) {
                    if (o.isTouching(x, y, true, 5, 5) && equalElements(c, o.getMostcolor())) {
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

    public static Point getOpenCircleCentrePoint(Captcha captcha) throws InterruptedException {
        java.util.List<PixelObject> ob = getObjects(captcha);
        // delete the lines
        for (Iterator<PixelObject> iterator = ob.iterator(); iterator.hasNext();) {
            PixelObject pixelObject = iterator.next();
            int ratio = pixelObject.getHeight() * 100 / pixelObject.getWidth();
            if (ratio > 110 || ratio < 90) {
                iterator.remove();
            }
        }

        Circle circle = new Circle(captcha, ob);
        circle.inBorder = 3;
        circle.outBorder = 2;
        circle.isElementColor = new Comparator<Integer>() {

            public int compare(Integer o1, Integer o2) {
                return o1.equals(o2) ? 1 : 0;
            }
        };
        Letter openCircle = circle.getOpenCircle();
        int x = openCircle.getLocation()[0] + (openCircle.getWidth() / 2);
        int y = openCircle.getLocation()[1] + (openCircle.getHeight() / 2);
        return new Point(x, y);
    }

    public static BufferedImage toBufferedImage(InputStream is) throws InterruptedException {

        try {
            JAntiCaptcha jac = new JAntiCaptcha("easycaptcha");
            jac.getJas().setColorType("RGB");
            GifDecoder d = new GifDecoder();
            d.read(is);
            int n = d.getFrameCount();
            Captcha[] frames = new Captcha[d.getFrameCount()];
            for (int i = 0; i < n; i++) {
                BufferedImage frame = d.getFrame(i);
                frames[i] = jac.createCaptcha(frame);

            }
            int[][] grid = new int[frames[0].getWidth()][frames[0].getHeight()];

            for (int x = 0; x < grid.length; x++) {
                for (int y = 0; y < grid[0].length; y++) {
                    int max = 0;
                    HashMap<Integer, Integer> colors = new HashMap<Integer, Integer>();
                    for (int i = 0; i < frames.length; i++) {
                        float[] hsb = Colors.rgb2hsb(frames[i].getGrid()[x][y]);
                        int distance = Colors.getRGBDistance(frames[i].getGrid()[x][y]);
                        if (!colors.containsKey(frames[i].getGrid()[x][y])) {
                            colors.put(frames[i].getGrid()[x][y], 1);
                        } else {
                            colors.put(frames[i].getGrid()[x][y], colors.get(frames[i].getGrid()[x][y]) + 1);
                        }
                        if (hsb[2] < 0.2 && distance < 100) {
                            continue;
                        }

                        max = Math.max(max, frames[i].getGrid()[x][y]);
                    }
                    int mainColor = 0;
                    int mainCount = 0;
                    for (Entry<Integer, Integer> col : colors.entrySet()) {
                        if (col.getValue() > mainCount && col.getKey() > 10) {
                            mainCount = col.getValue();
                            mainColor = col.getKey();
                        }
                    }
                    grid[x][y] = mainColor;
                }
            }
            int gl1 = grid[0].length - 1;
            for (int x = 0; x < grid.length; x++) {
                int bl1 = 0;
                int bl2 = 0;
                for (int i = Math.max(0, x - 6); i < Math.min(grid.length, x + 6); i++) {
                    if (grid[i][0] == 0x000000) {
                        bl1++;
                    }
                    if (grid[i][gl1] == 0x000000) {
                        bl2++;
                    }
                }
                if (bl1 == 12) {
                    cleanBlack(x, 0, grid);
                }
                if (bl2 == 12) {
                    cleanBlack(x, gl1, grid);
                }
            }
            gl1 = grid.length - 1;

            for (int y = 0; y < grid.length; y++) {
                int bl1 = 0;
                int bl2 = 0;
                for (int i = Math.max(0, y - 6); i < Math.min(grid[0].length, y + 6); i++) {
                    if (grid[0][i] == 0x000000) {
                        bl1++;
                    }
                    if (grid[gl1][i] == 0x000000) {
                        bl2++;
                    }
                }
                if (bl1 == 12) {
                    cleanBlack(0, y, grid);
                }
                if (bl2 == 12) {
                    cleanBlack(gl1, y, grid);
                }
            }
            frames[0].setGrid(grid);

            return frames[0].getImage(1);

        } finally {

        }
    }

    // stuff for cleaning black animations
    public static void cleanBlack(int x, int y, int[][] grid) {
        for (int x1 = Math.max(x - 2, 0); x1 < Math.min(x + 2, grid.length); x1++) {
            for (int y1 = Math.max(y - 2, 0); y1 < Math.min(y + 2, grid[0].length); y1++) {
                if (grid[x1][y1] == 0x000000) {
                    grid[x1][y1] = 0xffffff;
                    cleanBlack(x1, y1, grid);
                }
            }
        }
    }
}