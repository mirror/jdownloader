package jd.captcha.specials;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

import jd.captcha.easy.CPoint;
import jd.captcha.easy.ColorTrainer;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.pixelobject.PixelObject;

public class WbShRnt {
    private static int follow(PixelGrid captcha, int x, int y, int color, int lastC, int distance) {
        int i = 0;
        if (x >= 0 && y >= 0 && captcha.getWidth() > x && captcha.getHeight() > y && captcha.grid[x][y] >= 0 && captcha.grid[x][y] != 0xffffff && captcha.grid[x][y] == lastC) {
            captcha.grid[x][y] = color;
            i++;
            for (int x1 = -distance; x1 <= distance; x1++) {
                for (int y1 = -distance; y1 <= distance; y1++) {
                    i += follow(captcha, x + x1, y + y1, color, lastC, distance);

                }
            }

        }
        return i;
    }

    private static void toBlack(PixelGrid captcha) {
        for (int x = 1; x < captcha.getWidth() - 1; x++) {
            for (int y = 1; y < captcha.getHeight() - 1; y++) {
                if (captcha.grid[x][y] != 0xffffff && captcha.grid[x][y] != -1) {
                    captcha.grid[x][y] = 0x000000;
                } else
                    captcha.grid[x][y] = 0xffffff;

            }
        }
    }

    public static Letter[] getLetters(Captcha captcha) {
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
                    if (!cpBestDist2.isForeground()) captcha.grid[x][y] = -1;
                } else if (cpBestDist1 != null) {
                    if (!cpBestDist1.isForeground()) captcha.grid[x][y] = -1;
                }
            }
        }
        int[][] grid = PixelGrid.getGridCopy(captcha.grid);
        int i = -2;
        class colorD implements Comparable<colorD> {
            public int color;
            public int anzahl;

            public colorD(int color, int anzahl) {
                super();
                this.color = color;
                this.anzahl = anzahl;
            }

            public int compareTo(colorD o) {
                return Integer.valueOf(o.anzahl).compareTo(anzahl);
            }

        }
        java.util.List<colorD> colors = new ArrayList<colorD>();
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] >= 0) {
                    colors.add(new colorD(i, follow(captcha, x, y, i, captcha.grid[x][y], 18)));
                    i--;
                }

            }
        }
        Collections.sort(colors);
        for (colorD colorD : colors) {
            if (colorD.anzahl <= 15) {
                for (int x = 0; x < captcha.getWidth(); x++) {
                    for (int y = 0; y < captcha.getHeight(); y++) {
                        if (captcha.grid[x][y] == colorD.color) {
                            captcha.grid[x][y] = -1;
                        }
                    }
                }
            }
        }
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] == -1)
                    captcha.grid[x][y] = 0xffffff;
                else
                    captcha.grid[x][y] *= -500;
            }
        }
        java.util.List<PixelObject> coLetters = new ArrayList<PixelObject>();
        for (int x = 0; x < captcha.getWidth(); x++) {
            outerY: for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    for (PixelObject pixelObject : coLetters) {
                        if (pixelObject.getAverage() == captcha.grid[x][y]) {
                            pixelObject.add(x, y, captcha.grid[x][y]);
                            continue outerY;
                        }
                    }
                    PixelObject pix = new PixelObject(captcha);
                    pix.add(x, y, captcha.grid[x][y]);
                    coLetters.add(pix);

                }
            }
        }
        int[][] grid2 = PixelGrid.getGridCopy(captcha.grid);

        captcha.grid = grid;

        for (Iterator<PixelObject> iterator = coLetters.iterator(); iterator.hasNext();) {
            PixelObject pixelObject = iterator.next();
            if (pixelObject.getArea() / pixelObject.getSize() > 10 || pixelObject.getHeight() > 70 || pixelObject.getWidth() > 70) {
                captcha.removeObjectFromGrid(pixelObject);
                iterator.remove();
            } else {
                if (pixelObject.getArea() / pixelObject.getSize() > 5) {
                    Letter lgrid = pixelObject.toLetter();
                    int b = 0;
                    for (int x = 0; x < lgrid.getWidth(); x++) {
                        for (int y = 0; y < lgrid.getHeight(); y++) {
                            if (lgrid.grid[x][y] >= 0 && lgrid.grid[x][y] != 0xffffff) {
                                follow(lgrid, x, y, -1, lgrid.grid[x][y], 1);
                                b++;
                            }

                        }
                    }
                    if (b <= 1) {
                        captcha.removeObjectFromGrid(pixelObject);
                        iterator.remove();
                    }
                }

            }
        }
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != -1 && captcha.grid[x][y] != 0xffffff && (grid2[x][y] == -1 || grid2[x][y] == 0xffffff)) {
                    PixelObject bestobject = null;
                    int bestdist = Integer.MAX_VALUE;
                    for (PixelObject pixelObject : coLetters) {
                        int dist = pixelObject.getXDistanceTo(x);
                        if (dist < bestdist) {
                            bestdist = dist;
                            bestobject = pixelObject;
                        }
                    }
                    if (bestobject != null) {
                        bestobject.add(x, y, captcha.grid[x][y]);
                    }
                }
            }
        }
        Letter[] letters = new Letter[coLetters.size()];
        Collections.sort(coLetters);
        i = 0;
        for (PixelObject pixelObject : coLetters) {
            Letter let = pixelObject.toLetter();
            toBlack(let);
            let.removeSmallObjects(0.9, 0.9, 4, 2, 2);
            let.clean();
            let.autoAlign();
            let.resizetoHeight(25);
            letters[i] = let;
            i++;
        }
        return letters;
    }
}
