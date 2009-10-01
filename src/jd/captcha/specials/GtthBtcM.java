package jd.captcha.specials;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

import jd.captcha.gui.BasicWindow;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.pixelobject.PixelObject;
import jd.nutils.Colors;

public class GtthBtcM {
    // private static int[] findGab(PixelObject biggest, Captcha captcha) {
    // int[][] grid = captcha.getOrgGridCopy();
    //
    // int[] gab = new int[biggest.getWidth()];
    // @SuppressWarnings("unchecked")
    // HashMap<Integer, Integer>[] colorG = new HashMap[gab.length];
    // for (int i = 0; i < biggest.getSize(); i++) {
    // int[] akt = biggest.elementAt(i);
    // int x = akt[0] - biggest.getXMin();
    // gab[x]++;
    // if (colorG[x] == null) {
    // colorG[x] = new HashMap<Integer, Integer>();
    // colorG[x].put(grid[akt[0]][akt[1]], 0);
    // } else {
    // if (colorG[x].containsKey(grid[akt[0]][akt[1]]))
    // colorG[x].put(grid[akt[0]][akt[1]], colorG[x].get(grid[akt[0]][akt[1]]) +
    // 1);
    // else
    // colorG[x].put(grid[akt[0]][akt[1]], 0);
    // }
    // }
    // int gabAverage = 0;
    // for (int g : gab) {
    // gabAverage += g;
    // }
    // gabAverage /= gab.length;
    // int[] colorGab = new int[gab.length];
    // for (int i = 0; i < colorGab.length; i++) {
    // try {
    // Iterator<Entry<Integer, Integer>> cga = colorG[i].entrySet().iterator();
    // if (cga.hasNext()) {
    // Entry<Integer, Integer> bc = cga.next();
    // while (cga.hasNext()) {
    // Entry<Integer, Integer> bc2 = cga.next();
    // if (bc2.getValue() > bc.getValue()) bc = bc2;
    // }
    // colorGab[i] = bc.getKey();
    // } else {
    // colorGab[i] = -1;
    // }
    // } catch (Exception e) {
    // colorGab[i] = -1;
    // }
    //
    // }
    //
    // int best = gab.length / 4;
    // double bestCGab = Double.MIN_VALUE;
    // int bestCGabPos = best + 1;
    // for (int i = best + 1; i < gab.length * 3 / 4; i++) {
    // try {
    // double dif = Colors.getColorDifference(colorGab[i - 1], colorGab[i]);
    // if (dif > bestCGab) {
    // bestCGab = dif;
    // bestCGabPos = i;
    // }
    // if (gab[i] < gab[best]) {
    // best = i;
    // }
    // } catch (Exception e) {
    // }
    // }
    // if (gab[best] == 0) return new int[] { best, gab[best], gabAverage };
    // try {
    // double t = Colors.getBrightnessColorDifference(colorGab[bestCGabPos],
    // colorGab[bestCGabPos - 1]) / 2;
    // double t2 = Colors.getHueColorDifference(colorGab[bestCGabPos],
    // colorGab[bestCGabPos - 1]) / 2;
    //
    // if (Colors.getBrightnessColorDifference(colorGab[best], colorGab[best -
    // 1]) < t && Colors.getBrightnessColorDifference(colorGab[best],
    // colorGab[best + 1]) < t && Colors.getHueColorDifference(colorGab[best],
    // colorGab[best - 1]) < t2 && Colors.getHueColorDifference(colorGab[best],
    // colorGab[best + 1]) < t2) {
    // if (bestCGab > 2 && gab[best] * 2 / 3 < gab[bestCGabPos]) {
    // best = bestCGabPos;
    // } else if (bestCGab > 13) {
    // best = bestCGabPos;
    // }
    // }
    // } catch (Exception e) {
    // }
    // return new int[] { best, gab[best], gabAverage };
    // }

    private static PixelObject[] getObjects(Captcha captcha) {
        int startX = 1;
        // BasicWindow.showImage(captcha.getImage());

        captcha.autoBottomTopAlign();

        outer: for (; startX < captcha.getWidth(); startX++) {
            for (int y = 1; y < captcha.getHeight() - 1; y++) {
                if (Colors.rgb2hsv(captcha.grid[startX][y])[2] < 90) break outer;

            }
        }
        int xEnd = captcha.getWidth() - 2;
        outer: for (; xEnd > 0; xEnd--) {
            for (int y = 1; y < captcha.getHeight() - 1; y++) {
                if (Colors.rgb2hsv(captcha.grid[xEnd][y])[2] < 90) break outer;
            }
        }
        captcha.crop(startX, 0, captcha.getWidth() - xEnd, 0);
        int points = 12;
        double w = captcha.getWidth() / (double) points;
        int[][] bestxy = new int[points + 1][2];
        for (int i = 0; i < points; i++) {
            int besty = -1;
            int bestx = -1;

            for (int x = (int) (i * w); x < (i + 1) * w; x += 1) {
                for (int y = captcha.getHeight() - 1; y >= 0; y--) {
                    if (Colors.rgb2hsv(captcha.grid[x][y])[2] < 90) {
                        if (y > besty) {
                            besty = y;
                            bestx = x;
                        }
                        break;

                    }

                }
            }
            bestxy[i] = new int[] { bestx, besty };

        }
        bestxy[points] = new int[] { captcha.getWidth(), bestxy[points - 1][1] };

        int[][] bestxy2 = new int[points + 1][2];
        for (int i = 0; i < points; i++) {
            int besty = Integer.MAX_VALUE;
            int bestx = -1;

            for (int x = (int) (i * w); x < (i + 1) * w; x += 1) {
                for (int y = 0; y < captcha.getHeight(); y++) {
                    if (Colors.rgb2hsv(captcha.grid[x][y])[2] < 90) {
                        if (y < besty) {
                            besty = y;
                            bestx = x;
                        }
                        break;

                    }

                }
            }
            bestxy2[i] = new int[] { bestx, besty };

        }
        bestxy2[points] = new int[] { captcha.getWidth(), bestxy2[points - 1][1] };
        int[][] bestxy3 = new int[points + 1][2];
        for (int i = 0; i < bestxy3.length; i++) {
            bestxy3[i] = new int[] { (bestxy[i][0] + bestxy2[i][0]) / 2, bestxy2[0][1] / 2 + (bestxy[i][1] + bestxy2[i][1]) / 2 };
        }
        BufferedImage img = captcha.getImage();

        Graphics2D g2D = img.createGraphics();
        g2D.setColor(Color.magenta);

        GeneralPath oddShape = new GeneralPath();

        oddShape.moveTo(0, bestxy3[0][1]);
        for (int i = 0; i < bestxy.length; i += 1) {
            oddShape.lineTo(bestxy3[i][0], bestxy3[i][1]);
        }
        g2D.draw(oddShape);
        BasicWindow.showImage(img);
        return null;
    }

    private static void toBlack(PixelGrid captcha) {
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (Colors.rgb2hsv(captcha.grid[x][y])[2] < 90) {
                    captcha.grid[x][y] = 0x000000;
                } else
                    captcha.grid[x][y] = 0xffffff;

            }
        }
    }

    public static Letter[] getLetters(Captcha captcha) {
        // clearlines(captcha);
        // clearlines(captcha);
        PixelObject[] os = getObjects(captcha);
        if (true) return null;
        toBlack(captcha);

        // for (PixelObject pixelObject : os) {
        // BasicWindow.showImage(pixelObject.toLetter().getImage());

        // }
        Letter[] lets = new Letter[os.length];
        for (int i = 0; i < lets.length; i++) {
            lets[i] = os[i].toLetter();
            // blurIt(lets[i], 3);
            lets[i].resizetoHeight(30);
            // BasicWindow.showImage(lets[i].getImage());

        } //
        // BasicWindow.showImage(captcha.getImage());

        return lets;
    }
}
