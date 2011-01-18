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

import java.awt.image.BufferedImage;

import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.utils.GifDecoder;
import jd.nutils.Colors;

public class DtnKlnT {

    /**
     * overwrite the colored dots in digits with black dots die bunten punkte in
     * den Zahlen werden mit schwarzen punkten ersetzt
     * 
     * @param captcha
     */
    static void setDotsInDigits(Captcha captcha) {

        int[][] grid = PixelGrid.getGridCopy(captcha.grid);
        for (int x = 1; x < captcha.getWidth() - 1; x++) {
            for (int y = 1; y < captcha.getHeight() - 1; y++) {
                if (captcha.grid[x][y] == 0xffffff) {
                    int w = (captcha.grid[x + 1][y]) != 0xffffff ? 1 : 0;
                    w += (captcha.grid[x + 1][y + 1]) != 0xffffff ? 1 : 0;
                    w += (captcha.grid[x][y + 1]) != 0xffffff ? 1 : 0;
                    w += (captcha.grid[x - 1][y + 1]) != 0xffffff ? 1 : 0;
                    w += (captcha.grid[x - 1][y - 1]) != 0xffffff ? 1 : 0;
                    w += (captcha.grid[x + 1][y - 1]) != 0xffffff ? 1 : 0;
                    w += (captcha.grid[x - 1][y]) != 0xffffff ? 1 : 0;
                    w += (captcha.grid[x][y - 1]) != 0xffffff ? 1 : 0;
                    if (w > 4) grid[x][y] = 0x000000;

                }
            }
        }
        captcha.grid = grid;
    }

    public static Letter[] getLetters(Captcha captcha) {
        try {
            JAntiCaptcha jac = captcha.owner;
            jac.getJas().setColorType("RGB");
            GifDecoder d = new GifDecoder();
            d.read(captcha.getCaptchaFile().getAbsolutePath());
            int n = d.getFrameCount();
            Captcha[] frames = new Captcha[d.getFrameCount()];
            for (int i = 0; i < n; i++) {
                BufferedImage frame = d.getFrame(i);
                frames[i] = jac.createCaptcha(frame);

            }
            int[][] grid = new int[frames[0].getWidth()][frames[0].getHeight()];
            int thold = 55;
            for (int x = 0; x < grid.length; x++) {
                for (int y = 0; y < grid[0].length; y++) {
                    for (int i = 0; i < frames.length; i++) {
                        int[] hsv = Colors.rgb2hsv(frames[i].getGrid()[x][y]);
                        if (hsv[2] > thold) {
                            grid[x][y] = 0xffffff;
                        } else {
                            grid[x][y] = 0x000000;
                            break;
                        }

                    }
                }
            }
            captcha.setGrid(grid);
            int[] gabGrid = new int[grid.length];
            setDotsInDigits(captcha);
            for (int x = 0; x < grid.length; x++) {
                int i = 0;
                for (int y = 0; y < grid[0].length; y++) {
                    if (captcha.grid[x][y] == 0x000000) i++;
                }
                gabGrid[x] = i;
            }
            int w = grid.length / 4;
            int cw = w / 2;
            int last = 0;
            int length = 0;
            Letter[] ret = new Letter[4];
            for (int i = w; i < gabGrid.length + cw && length < 3; i += w, length++) {
                int best = i;
                for (int j = -cw; j < cw + 1; j++) {
                    try {
                        int cur = i + j;
                        if (gabGrid[cur] < gabGrid[best]) best = cur;
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }
                int[][] letgrid = new int[best - last][grid[0].length];
                for (int x = last, x2 = 0; x < best; x++, x2++) {
                    for (int y = 0; y < grid[0].length; y++) {
                        letgrid[x2][y] = captcha.grid[x][y];
                    }
                }
                // captcha.grid[best][1]=0xff0000;
                ret[length] = captcha.createLetter();
                ret[length].setGrid(letgrid);
                ret[length].removeSmallObjects(0.9, 0.9, 3, 2, 2);
                ret[length].clean();
                last = best;
            }
            int[][] letgrid = new int[grid.length - last][grid[0].length];
            for (int x = last, x2 = 0; x < grid.length; x++, x2++) {
                for (int y = 0; y < grid[0].length; y++) {
                    letgrid[x2][y] = captcha.grid[x][y];
                }
            }
            ret[3] = captcha.createLetter();
            ret[3].setGrid(letgrid);
            ret[3].removeSmallObjects(0.9, 0.9, 3, 2, 2);
            ret[3].clean();

            // BasicWindow.showImage(captcha.getImage(1));
            // ImageIO.write(captcha.getImage(1), "png",
            // captcha.getCaptchaFile());
            return ret;
        } catch (Exception e) {

        }
        return null;

    }
}
