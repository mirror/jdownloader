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

import jd.captcha.JAntiCaptcha;
import jd.captcha.LevenShteinLetterComperator;
import jd.captcha.easy.BackGroundImageManager;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.pixelobject.PixelObject;

public class NrDr {
    public static boolean checkLine(int x0, int x1, int y0, int y1, PixelGrid grid, int color) {
        int dy = y1 - y0;
        int dx = x1 - x0;
        int stepx, stepy;

        if (dy < 0) {
            dy = -dy;
            stepy = -1;
        } else {
            stepy = 1;
        }
        if (dx < 0) {
            dx = -dx;
            stepx = -1;
        } else {
            stepx = 1;
        }

        // if(grid.grid[x0][ y0]!= 0xffffff)return false;
        // if(grid.grid[x1][ y1]!= 0xffffff)return false;
        if (dx > dy) {
            int length = (dx - 1) >> 2;
            int extras = (dx - 1) & 3;
            int incr2 = (dy << 2) - (dx << 1);
            if (incr2 < 0) {
                int c = dy << 1;
                int incr1 = c << 1;
                int d = incr1 - dx;
                for (int i = 0; i < length; i++) {
                    x0 += stepx;
                    x1 -= stepx;
                    if (d < 0) { // Pattern:
                        if (grid.grid[x1][y1] != color) return false;

                        if (grid.grid[x0][y0] != color) return false; //
                        if (grid.grid[x0 += stepx][y0] != color) return false; // x
                        // o
                        // o
                        if (grid.grid[x1][y1] != color) return false; //
                        if (grid.grid[x1 -= stepx][y1] != color) return false;
                        d += incr1;
                    } else {
                        if (d < c) { // Pattern:
                            if (grid.grid[x0][y0] != color) return false; // o
                            if (grid.grid[x0 += stepx][y0 += stepy] != color) return false; // x
                            // o
                            if (grid.grid[x1][y1] != color) return false; //
                            if (grid.grid[x1 -= stepx][y1 -= stepy] != color) return false;
                        } else {
                            if (grid.grid[x0][y0 += stepy] != color) return false; // Pattern:
                            if (grid.grid[x0 += stepx][y0] != color) return false; // o
                            // o
                            if (grid.grid[x1][y1 -= stepy] != color) return false; // x
                            if (grid.grid[x1 -= stepx][y1] != color) return false; //
                        }
                        d += incr2;
                    }
                }
                if (extras > 0) {
                    if (d < 0) {
                        if (grid.grid[x0 += stepx][y0] != color) return false;
                        if (extras > 1) if (grid.grid[x0 += stepx][y0] != color) return false;
                        if (extras > 2) if (grid.grid[x1 -= stepx][y1] != color) return false;
                    } else if (d < c) {
                        if (grid.grid[x0 += stepx][y0] != color) return false;
                        if (extras > 1) if (grid.grid[x0 += stepx][y0 += stepy] != color) return false;
                        if (extras > 2) if (grid.grid[x1 -= stepx][y1] != color) return false;
                    } else {
                        if (grid.grid[x0 += stepx][y0 += stepy] != color) return false;
                        if (extras > 1) if (grid.grid[x0 += stepx][y0] != color) return false;
                        if (extras > 2) if (grid.grid[x1 -= stepx][y1 -= stepy] != color) return false;
                    }
                }
            } else {
                int c = (dy - dx) << 1;
                int incr1 = c << 1;
                int d = incr1 + dx;
                for (int i = 0; i < length; i++) {
                    x0 += stepx;
                    x1 -= stepx;
                    if (d > 0) {
                        if (grid.grid[x0][y0 += stepy] != color) return false; // Pattern:
                        if (grid.grid[x0 += stepx][y0 += stepy] != color) return false; // o
                        if (grid.grid[x1][y1 -= stepy] != color) return false; // o
                        if (grid.grid[x1 -= stepx][y1 -= stepy] != color) return false; // x
                        d += incr1;
                    } else {
                        if (d < c) {
                            if (grid.grid[x0][y0] != color) return false; // Pattern:
                            if (grid.grid[x0 += stepx][y0 += stepy] != color) return false; // o
                            if (grid.grid[x1][y1] != color) return false; // x
                            // o
                            if (grid.grid[x1 -= stepx][y1 -= stepy] != color) return false; //
                        } else {
                            if (grid.grid[x0][y0 += stepy] != color) return false; // Pattern:
                            if (grid.grid[x0 += stepx][y0] != color) return false; // o
                            // o
                            if (grid.grid[x1][y1 -= stepy] != color) return false; // x
                            if (grid.grid[x1 -= stepx][y1] != color) return false; //
                        }
                        d += incr2;
                    }
                }
                if (extras > 0) {
                    if (d > 0) {
                        if (grid.grid[x0 += stepx][y0 += stepy] != color) return false;
                        if (extras > 1) if (grid.grid[x0 += stepx][y0 += stepy] != color) return false;
                        if (extras > 2) if (grid.grid[x1 -= stepx][y1 -= stepy] != color) return false;
                    } else if (d < c) {
                        if (grid.grid[x0 += stepx][y0] != color) return false;
                        if (extras > 1) if (grid.grid[x0 += stepx][y0 += stepy] != color) return false;
                        if (extras > 2) if (grid.grid[x1 -= stepx][y1] != color) return false;
                    } else {
                        if (grid.grid[x0 += stepx][y0 += stepy] != color) return false;
                        if (extras > 1) if (grid.grid[x0 += stepx][y0] != color) return false;
                        if (extras > 2) {
                            if (d > c) if (grid.grid[x1 -= stepx][y1 -= stepy] != color)
                                return false;
                            else if (grid.grid[x1 -= stepx][y1] != color) return false;
                        }
                    }
                }
            }
        } else {
            int length = (dy - 1) >> 2;
            int extras = (dy - 1) & 3;
            int incr2 = (dx << 2) - (dy << 1);
            if (incr2 < 0) {
                int c = dx << 1;
                int incr1 = c << 1;
                int d = incr1 - dy;
                for (int i = 0; i < length; i++) {
                    y0 += stepy;
                    y1 -= stepy;
                    if (d < 0) {
                        if (grid.grid[x0][y0] != color) return false;
                        if (grid.grid[x0][y0 += stepy] != color) return false;
                        if (grid.grid[x1][y1] != color) return false;
                        if (grid.grid[x1][y1 -= stepy] != color) return false;
                        d += incr1;
                    } else {
                        if (d < c) {
                            if (grid.grid[x0][y0] != color) return false;
                            if (grid.grid[x0 += stepx][y0 += stepy] != color) return false;
                            if (grid.grid[x1][y1] != color) return false;
                            if (grid.grid[x1 -= stepx][y1 -= stepy] != color) return false;
                        } else {
                            if (grid.grid[x0 += stepx][y0] != color) return false;
                            if (grid.grid[x0][y0 += stepy] != color) return false;
                            if (grid.grid[x1 -= stepx][y1] != color) return false;
                            if (grid.grid[x1][y1 -= stepy] != color) return false;
                        }
                        d += incr2;
                    }
                }
                if (extras > 0) {
                    if (d < 0) {
                        if (grid.grid[x0][y0 += stepy] != color) return false;
                        if (extras > 1) if (grid.grid[x0][y0 += stepy] != color) return false;
                        if (extras > 2) if (grid.grid[x1][y1 -= stepy] != color) return false;
                    } else if (d < c) {
                        if (grid.grid[stepx][y0 += stepy] != color) return false;
                        if (extras > 1) if (grid.grid[x0 += stepx][y0 += stepy] != color) return false;
                        if (extras > 2) if (grid.grid[x1][y1 -= stepy] != color) return false;
                    } else {
                        if (grid.grid[x0 += stepx][y0 += stepy] != color) return false;
                        if (extras > 1) if (grid.grid[x0][y0 += stepy] != color) return false;
                        if (extras > 2) if (grid.grid[x1 -= stepx][y1 -= stepy] != color) return false;
                    }
                }
            } else {
                int c = (dx - dy) << 1;
                int incr1 = c << 1;
                int d = incr1 + dy;
                for (int i = 0; i < length; i++) {
                    y0 += stepy;
                    y1 -= stepy;
                    if (d > 0) {
                        if (grid.grid[x0 += stepx][y0] != color) return false;
                        if (grid.grid[x0 += stepx][y0 += stepy] != color) return false;
                        if (grid.grid[x1 -= stepy][y1] != color) return false;
                        if (grid.grid[x1 -= stepx][y1 -= stepy] != color) return false;
                        d += incr1;
                    } else {
                        if (d < c) {
                            if (grid.grid[x0][y0] != color) return false;
                            if (grid.grid[x0 += stepx][y0 += stepy] != color) return false;
                            if (grid.grid[x1][y1] != color) return false;
                            if (grid.grid[x1 -= stepx][y1 -= stepy] != color) return false;
                        } else {
                            if (grid.grid[x0 += stepx][y0] != color) return false;
                            if (grid.grid[x0][y0 += stepy] != color) return false;
                            if (grid.grid[x1 -= stepx][y1] != color) return false;
                            if (grid.grid[x1][y1 -= stepy] != color) return false;
                        }
                        d += incr2;
                    }
                }
                if (extras > 0) {
                    if (d > 0) {
                        if (grid.grid[x0 += stepx][y0 += stepy] != color) return false;
                        if (extras > 1) if (grid.grid[x0 += stepx][y0 += stepy] != color) return false;
                        if (extras > 2) if (grid.grid[x1 -= stepx][y1 -= stepy] != color) return false;
                    } else if (d < c) {
                        if (grid.grid[x0][y0 += stepy] != color) return false;
                        if (extras > 1) if (grid.grid[x0 += stepx][y0 += stepy] != color) return false;
                        if (extras > 2) if (grid.grid[x1][y1 -= stepy] != color) return false;
                    } else {
                        if (grid.grid[x0 += stepx][y0 += stepy] != color) return false;
                        if (extras > 1) if (grid.grid[x0][y0 += stepy] != color) return false;
                        if (extras > 2) {
                            if (d > c) if (grid.grid[x1 -= stepx][y1 -= stepy] != color)
                                return false;
                            else if (grid.grid[x1][y1 -= stepy] != color) return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    static void follow(int x0, int y0, PixelGrid px, PixelGrid captcha, double dmin, double dmax) {
        px.grid[x0][y0] = 0x000000;

        int x = Math.max(0, x0 - 1);
        int y = Math.max(0, y0 - 1);
        for (x0 = x; x0 < Math.min(x + 2, captcha.getWidth()); x0++) {
            for (y0 = y; y0 < Math.min(y + 2, captcha.getHeight()); y0++) {
                if (px.grid[x0][y0] == 0xffffff) {
                    px.grid[x0][y0] = 0x000000;
                    // follow(x0, y0, px, captcha, dmin, dmax);
                }
            }

        }

    }

    static void fill(PixelGrid captcha, double dmin, double dmax) {

        PixelGrid px = new PixelGrid(captcha.getWidth(), captcha.getHeight());
        px.grid = new int[captcha.getWidth()][captcha.getHeight()];
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                px.grid[x][y] = 0xffffff;
            }
        }
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (px.grid[x][y] == 0xffffff && captcha.grid[x][y] != 0xffffff) {

                    for (int x1 = x; x1 < Math.min(x + dmax, captcha.getWidth()); x1++) {
                        for (int y1 = y; y1 < Math.min(y + dmax, captcha.getHeight()); y1++) {
                            if (px.grid[x1][y1] == 0xffffff && captcha.grid[x1][y1] != 0xffffff) {
                                double xd = (x1 - x);
                                double yd = (y1 - y);
                                double diff = Math.sqrt(xd*xd + yd*yd);

                                if (diff > dmin && diff < dmax) {
                                    if (checkLine(x, x1, y, y1, captcha, 0xffffff)) {

                                        int xadd;
                                        int yadd;
                                        int x1add;
                                        int y1add;
                                        int di = 3;
                                        if (xd == 0) {
                                            xadd = 0;
                                            yadd = di;
                                            x1add = 0;
                                            y1add = di - 1;
                                        } else if (yd == 0) {
                                            xadd = di;
                                            x1add = di - 1;
                                            y1add = 0;
                                            yadd = 0;
                                        } else {
                                            double m = yd / xd;
                                            xadd = (int) Math.floor((1 - m) * di);
                                            yadd = (int) Math.floor(m * di);
                                            x1add = (int) Math.ceil((1 - m) * (di - 1));
                                            y1add = (int) Math.floor(m * (di - 1));

                                        }
                                        // follow((x1-x)/2+x,(y1-y)/2+y, px,
                                        // captcha, dmin, dmax);
                                        int f = y - yadd;
                                        int fx = x - xadd;
                                        if (f > 0 && fx > 0 && f < captcha.getHeight() && fx < captcha.getWidth() && captcha.grid[fx][f] != 0xffffff) {
                                            try {
                                                if (checkLine(x, x - x1add, y, y - y1add, captcha, 0x000000)) {
                                                    f = y1 + yadd;
                                                    fx = x1 + xadd;
                                                    if (f > 0 && fx > 0 && f < captcha.getHeight() && fx < captcha.getWidth() && captcha.grid[fx][f] != 0xffffff && checkLine(x1, x1 + x1add, y1, y1 + y1add, captcha, 0x000000)) {
                                                        follow(x, y, px, captcha, dmin, dmax);
                                                        follow(x1, y1, px, captcha, dmin, dmax);
                                                    }

                                                }
                                            } catch (Exception e) {
                                                px.grid[x][y] = captcha.grid[x][y];

                                            }

                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] == 0xffffff) {
                    px.grid[x][y] = 0xffffff;
                }
            }
        }
        captcha.grid = px.grid;
    }

    static void toBlack(PixelGrid captcha) {
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xffffff && captcha.grid[x][y] != 0xff0000) {
                    captcha.grid[x][y] = 0x000000;
                } else
                    captcha.grid[x][y] = 0xffffff;

            }
        }
    }

    private static PixelObject[] getObjects(Captcha captcha) {
        int startX = 1;
        outer: for (; startX < captcha.getWidth(); startX++) {
            for (int y = 1; y < captcha.getHeight() - 1; y++) {
                if (captcha.grid[startX][y] != 0xffffff) break outer;

            }
        }
        int xEnd = captcha.getWidth() - 2;
        outer: for (; xEnd > 0; xEnd--) {
            for (int y = 1; y < captcha.getHeight() - 1; y++) {
                if (captcha.grid[xEnd][y] != 0xffffff) break outer;
            }
        }
        int wi = (xEnd - startX) / 6;
        PixelObject[] ret = new PixelObject[6];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = new PixelObject(captcha);

        }
        int add = 3;
        for (int x = startX; x < startX + wi + add; x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    ret[0].add(x, y, captcha.grid[x][y]);
                }

            }
        }
        for (int x = startX + wi; x < startX + wi * 2 + add; x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    ret[1].add(x, y, captcha.grid[x][y]);
                }

            }
        }
        for (int x = startX + wi * 2; x < startX + wi * 3 + add; x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    ret[2].add(x, y, captcha.grid[x][y]);
                }

            }
        }
        for (int x = startX + wi * 3; x < startX + wi * 4 + add; x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    ret[3].add(x, y, captcha.grid[x][y]);
                }

            }
        }
        for (int x = startX + wi * 4; x < startX + wi * 5 + add; x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    ret[4].add(x, y, captcha.grid[x][y]);
                }

            }
        }
        for (int x = startX + wi * 5; x < startX + wi * 6 + add; x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    ret[5].add(x, y, captcha.grid[x][y]);
                }

            }
        }
        return ret;
    }

    public static Letter[] getLetters(Captcha captcha) throws InterruptedException{
        BackGroundImageManager bgit = new BackGroundImageManager(captcha);
        bgit.clearCaptchaAll();
        // clearlines(captcha);
        // clearlines(captcha);

        toBlack(captcha);
        fill(captcha, 1, 9);

        PixelObject[] os = getObjects(captcha);
        // for (PixelObject pixelObject : os) {
        // BasicWindow.showImage(pixelObject.toLetter().getImage());

        // }
        captcha.reset();
        Letter[] lets = new Letter[os.length];
        for (int i = 0; i < lets.length; i++) {
            lets[i] = os[i].toLetter();
            int[] l = lets[i].getLocation();
            for (int x = 0; x < lets[i].getWidth(); x++) {
                for (int y = 0; y < lets[i].getHeight(); y++) {
                    lets[i].grid[x][y] = captcha.getPixelValue(x + l[0] - 1, y + l[1] - 1);
                }
            }
            // blurIt(lets[i], 3);
            toBlack(lets[i]);
            lets[i].resizetoHeight(30);
            // BasicWindow.showImage(lets[i].getImage());

        } //
        // BasicWindow.showImage(captcha.getImage());

        return lets;
    }

    public static Letter[] letterFilter(Letter[] org, JAntiCaptcha jac) {
        LevenShteinLetterComperator lvs = new LevenShteinLetterComperator(jac);
        // lvs.costs=20;
        lvs.run(org);
        return org;
    }
}
