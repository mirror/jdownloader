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

import java.awt.Color;

import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.nutils.Colors;

/**
 * 
 * 
 * @author JD-Team
 */
public class EasyShare {
    /*
     * private static void mergeObjects(Vector<PixelObject> os) { for
     * (PixelObject a : os) { for (PixelObject b : os) { if (a == b) continue;
     * 
     * int xMin = Math.max(a.getXMin(), b.getXMin()); int xMax =
     * Math.min(a.getXMin() + a.getWidth(), b.getXMin() + b.getWidth()); if
     * (xMax <= xMin) continue; int yMin = Math.max(a.getYMin(), b.getYMin());
     * int yMax = Math.min(a.getYMin() + a.getHeight(), b.getYMin() +
     * b.getHeight());
     * 
     * if (((xMax - xMin) < 30) && ((yMax - yMin) < 30)) { a.add(b);
     * os.remove(b); mergeObjects(os); return; } } }
     * 
     * }
     */
    private static void clean(Captcha captcha) {
        int[][] newgrid = new int[captcha.getWidth()][captcha.getHeight()];
        int[] avgBG = { 0, 0, 0 };
        int[] bv;
        int[] avgFG = { 0, 0, 0 };
        int fg = 0;
        int bg = 0;
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {

                int p = captcha.getPixelValue(x, y);
                bv = Colors.hexToRgb(p);

                Color c = new Color(p);
                if (c.getBlue() > 180 && c.getRed() > 180 && c.getGreen() > 180) {
                    newgrid[x][y] = 0xffffff;
                    avgBG[0] += bv[0];
                    avgBG[1] += bv[1];
                    avgBG[2] += bv[2];
                    bg++;
                } else {
                    avgFG[0] += bv[0];
                    avgFG[1] += bv[1];
                    avgFG[2] += bv[2];
                    fg++;
                    newgrid[x][y] = 0x000000;
                }

            }
        }
        if (fg != 0 && bg != 0) {
            avgBG[0] /= bg;
            avgBG[1] /= bg;
            avgBG[2] /= bg;
            avgFG[0] /= fg;
            avgFG[1] /= fg;
            avgFG[2] /= fg;
            bg = Colors.rgbToHex(avgBG);
            fg = Colors.rgbToHex(avgFG);

            for (int x = 0; x < captcha.getWidth(); x++) {
                for (int y = 0; y < captcha.getHeight(); y++) {
                    int p = captcha.getPixelValue(x, y);
                    if (Colors.getColorDifference(bg, p) < Colors.getColorDifference(fg, p))
                        newgrid[x][y] = 0xffffff;
                    else
                        newgrid[x][y] = 0x000000;

                }
            }
        }

        captcha.grid = newgrid;
    }

    /*
     * private static Vector<PixelObject> getRightletters(Vector<PixelObject>
     * os, Captcha captcha) { if (os.size() > 5) return os; PixelObject biggest
     * = os.get(0); for (int i = 1; i < os.size(); i++) { PixelObject po =
     * os.get(i); if (po.getWidth() > biggest.getWidth()) biggest = po; } if
     * (biggest.getWidth() > 20) { if (os.size() == 5 && biggest.getWidth() <
     * 25) return os; PixelObject[] bs = biggest.cut(biggest.getWidth() / 2,
     * biggest.getWidth(), 0); os.remove(biggest);
     * 
     * for (PixelObject pixelObject : bs) { if (pixelObject != null)
     * os.add(pixelObject); } return getRightletters(os, captcha); } return os;
     * }
     */
    static int getlength(Captcha captcha) {
        int x = 3;
        outerx: for (; x < captcha.getWidth() - 3; x++) {
            for (int y = 3; y < captcha.getHeight() - 3; y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    break outerx;
                }

            }
        }
        int xo = captcha.getWidth() - 4;
        outerx: for (; xo > 4; xo--) {
            for (int y = 3; y < captcha.getHeight() - 3; y++) {
                if (captcha.grid[xo][y] != 0xffffff) {
                    break outerx;
                }

            }
        }
        int xb = x;
        int dw = 0;
        int lastX = -2;
        int count = 0;
        outerx: for (; xb < xo; xb++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[xb][y] != 0xffffff) {
                    continue outerx;
                }

            }
            if (lastX + 1 != xb) {
                count++;
            }
            lastX = xb;
            dw++;
        }
        int ret = xo - x - dw;
        double mult = 1;
        if (ret>70 && count > 4||count>6)
            mult=1.35;
        else if(count>5)
            mult=1.2;
        return (int) (ret * mult);

    }

    public static Letter[] getLetters(Captcha captcha) {
        clean(captcha);
//        captcha.removeSmallObjects(0.75, 0.75, 6);
        captcha.autoBottomTopAlign();
        captcha.setOrgGrid(captcha.grid);
        if (getlength(captcha) < 77)
            captcha.owner.setLetterNum(5);
        else
            captcha.owner.setLetterNum(6);
        return EasyCaptcha.getLetters(captcha);

    }

}