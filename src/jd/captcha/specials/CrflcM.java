package jd.captcha.specials;

import java.awt.Color;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.nutils.Colors;

public class CrflcM {
    static private int follow(int x, int y, int cx, int cy, Captcha captcha) {
        int ret = 0;
        if (captcha.getHeight()>y&&captcha.getWidth()>x&&Colors.getColorDifference(0xffffff, captcha.grid[x][y]) > 30) {
            ret++;
            if (cx >= 0) {
                ret += follow(x + 1, y, cx - 1, cy, captcha);
            }
            if (cy >= 0) {
                ret += follow(x, y + 1, cx, cy - 1, captcha);
            }
        }
        return ret;
    }

    static void clearlines2(Captcha captcha, int val) {

        int[][] grid = PixelGrid.getGridCopy(captcha.grid);
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
//                System.out.println(follow(x, y, 3, 3, captcha));
                    if (follow(x, y, 2, 2, captcha) < val) grid[x][y] = 0xffffff;
            }
        }
        captcha.grid=grid;
    }

    static void clearlines(Captcha captcha) {
        int diff = 14;
        int[][] grid = PixelGrid.getGridCopy(captcha.grid);
        for (int x = 1; x < captcha.getWidth() - 1; x++) {
            for (int y = 1; y < captcha.getHeight() - 1; y++) {
                if (Colors.getColorDifference(0xffffff, captcha.grid[x][y]) < 30) {
                    grid[x][y] = 0xffffff;
                } else {
                    int val = captcha.grid[x][y];
                    int w = Colors.getColorDifference(val, captcha.grid[x + 1][y]) > diff ? 1 : 0;
                    w += Colors.getColorDifference(val, captcha.grid[x + 1][y + 1]) > diff ? 1 : 0;
                    w += Colors.getColorDifference(val, captcha.grid[x][y + 1]) > diff ? 1 : 0;
                    w += Colors.getColorDifference(val, captcha.grid[x - 1][y + 1]) > diff ? 1 : 0;
                    w += Colors.getColorDifference(val, captcha.grid[x - 1][y - 1]) > diff ? 1 : 0;
                    w += Colors.getColorDifference(val, captcha.grid[x + 1][y - 1]) > diff ? 1 : 0;
                    w += Colors.getColorDifference(val, captcha.grid[x - 1][y]) > diff ? 1 : 0;
                    w += Colors.getColorDifference(val, captcha.grid[x][y - 1]) > diff ? 1 : 0;
                    if (w < 8)
                        grid[x][y] = captcha.grid[x][y];
                    else
                        grid[x][y] = 0xffffff;

                }
            }
        }

        captcha.grid = grid;
    }

    private static void clean(Captcha captcha) {
        int[][] newgrid = new int[captcha.getWidth()][captcha.getHeight()];
        int diff = 15;
        Color lastC = null;
        int[] lastpos = new int[] { 0, 0 };
        int d1 = 85;
        int d2 = 110;
        int w = 9;
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int d = 0; d < w; d++) {

                int val = captcha.getPixelValue(x, d);
                for (int y = 0; y < captcha.getHeight(); y++) {
                    int p = captcha.getPixelValue(x, y);

                    Color c = new Color(p);
                    if (c.getBlue() > d1 && c.getRed() > d1 && c.getGreen() > d1 || c.getBlue() > d2 || c.getRed() > d2 || c.getGreen() > d2) {
                        if (lastC == null || !((Math.abs(lastpos[0] - x) + Math.abs(lastpos[1] - y)) < 3 && Colors.getColorDifference(lastC.getRGB(), c.getRGB()) < 3))
                            PixelGrid.setPixelValue(x, y, newgrid, captcha.getMaxPixelValue());
                        else
                            newgrid[x][y] = captcha.grid[x][y];
                    } else {
                        if (Colors.getColorDifference(val, captcha.grid[x][y]) < diff) {
                            val = captcha.grid[x][y];
                            newgrid[x][y] = 0xffffff;
                        } else
                            newgrid[x][y] = captcha.grid[x][y];
                        lastC = c;
                        lastpos = new int[] { x, y };
                    }

                }

            }
            for (int d = captcha.getHeight() - w; d < captcha.getHeight(); d++) {

                int val = captcha.getPixelValue(x, d);
                for (int y = 0; y < captcha.getHeight(); y++) {
                    int p = captcha.getPixelValue(x, y);

                    Color c = new Color(p);
                    if (c.getBlue() > d1 && c.getRed() > d1 && c.getGreen() > d1 || c.getBlue() > d2 || c.getRed() > d2 || c.getGreen() > d2) {
                        if (lastC == null || !((Math.abs(lastpos[0] - x) + Math.abs(lastpos[1] - y)) < 3 && Colors.getColorDifference(lastC.getRGB(), c.getRGB()) < 3))
                            PixelGrid.setPixelValue(x, y, newgrid, captcha.getMaxPixelValue());
                        else
                            newgrid[x][y] = captcha.grid[x][y];
                    } else {
                        if (Colors.getColorDifference(val, captcha.grid[x][y]) < diff) {
                            val = captcha.grid[x][y];
                            newgrid[x][y] = 0xffffff;
                        } else
                            newgrid[x][y] = captcha.grid[x][y];
                        lastC = c;
                        lastpos = new int[] { x, y };
                    }

                }

            }
        }
        lastC = null;
        lastpos = new int[] { 0, 0 };
        for (int y = 0; y < captcha.getHeight(); y++) {
            for (int d = 0; d < w; d++) {

                int val = captcha.getPixelValue(d, y);
                for (int x = 0; x < captcha.getWidth(); x++) {
                    int p = captcha.getPixelValue(x, y);

                    Color c = new Color(p);
                    if (c.getBlue() > d1 && c.getRed() > d1 && c.getGreen() > d1 || c.getBlue() > d2 || c.getRed() > d2 || c.getGreen() > d2) {
                        if (lastC == null || !((Math.abs(lastpos[0] - x) + Math.abs(lastpos[1] - y)) < 3 && Colors.getColorDifference(lastC.getRGB(), c.getRGB()) < 3))
                            PixelGrid.setPixelValue(x, y, newgrid, captcha.getMaxPixelValue());
                        else
                            newgrid[x][y] = captcha.grid[x][y];
                    } else {
                        if (Colors.getColorDifference(val, captcha.grid[x][y]) < diff) {
                            val = captcha.grid[x][y];
                            newgrid[x][y] = 0xffffff;
                        } else
                            newgrid[x][y] = captcha.grid[x][y];
                        lastC = c;
                        lastpos = new int[] { x, y };
                    }

                }

            }
            for (int d = captcha.getWidth() - w; d < captcha.getWidth(); d++) {

                int val = captcha.getPixelValue(d, y);
                for (int x = 0; x < captcha.getWidth(); x++) {
                    int p = captcha.getPixelValue(x, y);

                    Color c = new Color(p);
                    if (c.getBlue() > d1 && c.getRed() > d1 && c.getGreen() > d1 || c.getBlue() > d2 || c.getRed() > d2 || c.getGreen() > d2) {
                        if (lastC == null || !((Math.abs(lastpos[0] - x) + Math.abs(lastpos[1] - y)) < 3 && Colors.getColorDifference(lastC.getRGB(), c.getRGB()) < 3))
                            PixelGrid.setPixelValue(x, y, newgrid, captcha.getMaxPixelValue());
                        else
                            newgrid[x][y] = captcha.grid[x][y];
                    } else {
                        if (Colors.getColorDifference(val, captcha.grid[x][y]) < diff) {
                            val = captcha.grid[x][y];
                            newgrid[x][y] = 0xffffff;
                        } else
                            newgrid[x][y] = captcha.grid[x][y];
                        lastC = c;
                        lastpos = new int[] { x, y };
                    }

                }

            }
        }

        captcha.grid = newgrid;

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
    public static Letter[] getLetters(Captcha captcha) {
        // captcha.cleanByRGBDistance(1, 25);
        clearlines2(captcha,14);

        clearlines(captcha);

//        clearlines2(captcha,2);

        clean(captcha);


//         clearlines(captcha);
//         clearlines2(captcha,4);

        captcha.crop(8, 6,28, 8);


        toBlack(captcha);

//        BasicWindow.showImage(captcha.getImage());

        return EasyCaptcha.getLetters(captcha);

    }
}
