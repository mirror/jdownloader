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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import jd.captcha.easy.BackGroundImageManager;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.pixelobject.PixelObject;
import jd.nutils.Colors;

public class PpscnRg {
    static void clearlines(Captcha captcha) {
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (Colors.rgb2hsv(captcha.grid[x][y])[2] > 40) captcha.grid[x][y] = 0xffffff;
            }
        }
        int[][] grid = PixelGrid.getGridCopy(captcha.grid);
        for (int x = 1; x < captcha.getWidth() - 1; x++) {
            for (int y = 1; y < captcha.getHeight() - 1; y++) {
                if (captcha.grid[x][y] != 0xffffff && Colors.rgb2hsv(captcha.grid[x][y])[2] > 38) {
                    int c = captcha.grid[x][y];
                    int w = captcha.grid[x + 1][y] == c ? 1 : 0;
                    w += captcha.grid[x + 1][y + 1] == c ? 2 : 0;
                    w += captcha.grid[x][y + 1] == c ? 2 : 0;
                    w += captcha.grid[x - 1][y + 1] == c ? 2 : 0;
                    w += captcha.grid[x - 1][y - 1] == c ? 2 : 0;
                    w += captcha.grid[x + 1][y - 1] == c ? 2 : 0;
                    w += captcha.grid[x - 1][y] == c ? 1 : 0;
                    w += captcha.grid[x][y - 1] == c ? 2 : 0;

                    if (w < 6) grid[x][y] = 0xffffff;

                }
            }
        }
        captcha.grid = grid;
    }

    static void clearlines2(Captcha captcha) {

        int[][] grid = PixelGrid.getGridCopy(captcha.grid);
        for (int x = 1; x < captcha.getWidth() - 1; x++) {
            for (int y = 1; y < captcha.getHeight() - 1; y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    int w = captcha.grid[x + 1][y] != 0xffffff ? 1 : 0;
                    w += captcha.grid[x + 1][y + 1] != 0xffffff ? 2 : 0;
                    w += captcha.grid[x][y + 1] != 0xffffff ? 2 : 0;
                    w += captcha.grid[x - 1][y + 1] != 0xffffff ? 2 : 0;
                    w += captcha.grid[x - 1][y - 1] != 0xffffff ? 2 : 0;
                    w += captcha.grid[x + 1][y - 1] != 0xffffff ? 2 : 0;
                    w += captcha.grid[x - 1][y] != 0xffffff ? 1 : 0;
                    w += captcha.grid[x][y - 1] != 0xffffff ? 2 : 0;

                    if (w < 2) grid[x][y] = 0xffffff;

                }
            }
        }
        captcha.grid = grid;
    }

    private static void toBlack(PixelGrid captcha) {
        for (int x = 1; x < captcha.getWidth() - 1; x++) {
            for (int y = 1; y < captcha.getHeight() - 1; y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    captcha.grid[x][y] = 0x000000;
                }

            }
        }
    }

    public static ArrayList<PixelObject> getObjects(PixelGrid grid, int neighbourradius) {
        ArrayList<PixelObject> ret = new ArrayList<PixelObject>();
        ArrayList<PixelObject> merge;
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                int c = grid.grid[x][y];
                if (c == 0xffffff) continue;

                PixelObject n = new PixelObject(grid);
                n.add(x, y, c);

                merge = new ArrayList<PixelObject>();
                for (PixelObject o : ret) {
                    if (c == o.getMostcolor() && o.isTouching(x, y, true, neighbourradius, neighbourradius)) {
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

    private static void merge(List<PixelObject> os) {
        if (os.size() == 4) return;
        PixelObject aos = null;
        for (Iterator<PixelObject> iterator = os.iterator(); iterator.hasNext();) {
            PixelObject pixelObject = iterator.next();
            if (pixelObject.getSize() < 2) iterator.remove();
        }
        int mergeos = Integer.MAX_VALUE;
        for (PixelObject pixelObject : os) {
            int mg = pixelObject.getSize();
            if (mergeos > mg) {
                mergeos = mg;
                aos = pixelObject;
            }
        }
        EasyCaptcha.mergeos(aos, os);
        merge(os);
    }

    public static Letter[] getLetters(Captcha captcha) {
        BackGroundImageManager bgit = new BackGroundImageManager(captcha);
        bgit.setBackGroundImageListFileName("bgimages2.xml");
        bgit.clearCaptchaAll();
        clearlines(captcha);
        // clearlines(captcha);

        // clearlines2(captcha);

        // clearlines2(captcha);
        captcha.crop(0, 2, 0, 20);
        // clearlines2(captcha);

        // clearlines2(captcha);

        // BasicWindow.showImage(captcha.getImage());
        ArrayList<PixelObject> os = getObjects(captcha, 2);
        merge(os);
        Collections.sort(os);
        Letter[] letters = new Letter[os.size()];
        for (int i = 0; i < letters.length; i++) {
            letters[i] = os.get(i).toLetter();
            toBlack(letters[i]);
            letters[i].autoAlign();
            letters[i].resizetoHeight(25);
        }
        return letters;
    }
}
