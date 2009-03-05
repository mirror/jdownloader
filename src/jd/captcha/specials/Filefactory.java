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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import jd.captcha.JAntiCaptcha;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.pixelobject.PixelObject;
import jd.captcha.utils.UTILITIES;

/**
 * 
 * 
 * @author JD-Team
 */
public class Filefactory {

    // private static final double OBJECTCOLORCONTRAST = 0.01;

    private static final double OBJECTDETECTIONCONTRAST = 0.95;

    private static ArrayList<PixelObject> getColorObjects(PixelGrid grid, int color, int tollerance) {
        ArrayList<PixelObject> ret = new ArrayList<PixelObject>();
        ArrayList<PixelObject> merge;
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                if (UTILITIES.getColorDifference(grid.getGrid()[x][y], color) > tollerance) continue;

                PixelObject n = new PixelObject(grid);
                n.add(x, y, grid.getGrid()[x][y]);

                merge = new ArrayList<PixelObject>();
                for (Iterator<PixelObject> it = ret.iterator(); it.hasNext();) {
                    PixelObject o = it.next();

                    if (o.isTouching(x, y, true, 6, 6)) {

                        merge.add(o);
                        // n.add(o);
                        //                     
                        // it.remove();
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
            // BasicWindow.showImage(grid.getImage(6), x+"-");
        }

        return ret;
    }

    private static ArrayList<PixelObject> getObjects(PixelGrid grid, int tollerance) {
        ArrayList<PixelObject> ret = new ArrayList<PixelObject>();
        ArrayList<PixelObject> merge;
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                if (grid.getGrid()[x][y] == 0xffffff) continue;

                PixelObject n = new PixelObject(grid);
                n.add(x, y, grid.getGrid()[x][y]);

                merge = new ArrayList<PixelObject>();
                for (Iterator<PixelObject> it = ret.iterator(); it.hasNext();) {
                    PixelObject o = it.next();

                    if (o.isTouching(x, y, true, 6, 6) && UTILITIES.getColorDifference(grid.getGrid()[x][y], o.getAverage()) < 25) {

                        merge.add(o);
                        // n.add(o);
                        //                     
                        // it.remove();
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
            // BasicWindow.showImage(grid.getImage(6), x+"-");
        }

        return ret;
    }

    private static ArrayList<PixelObject> getSmallObjects(PixelGrid grid, int tollerance) {
        ArrayList<PixelObject> ret = new ArrayList<PixelObject>();
        ArrayList<PixelObject> merge;
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                if (grid.getGrid()[x][y] == 0xffffff) continue;

                PixelObject n = new PixelObject(grid);
                n.add(x, y, grid.getGrid()[x][y]);

                merge = new ArrayList<PixelObject>();
                for (Iterator<PixelObject> it = ret.iterator(); it.hasNext();) {
                    PixelObject o = it.next();

                    if (o.isTouching(x, y, true, 2, 2) && UTILITIES.getColorDifference(grid.getGrid()[x][y], o.getAverage()) < 25) {

                        merge.add(o);
                        // n.add(o);
                        //                     
                        // it.remove();
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
            // BasicWindow.showImage(grid.getImage(6), x+"-");
        }

        return ret;
    }

    public static Letter[] getLetters(Captcha captcha) {
        captcha.cleanByRGBDistance(-1, 20);
        // long t = System.currentTimeMillis();
        clearCaptcha(captcha);

        ArrayList<PixelObject> os = getObjects(captcha, 0);
        mergeObjects(os);
        Collections.sort(os);
        // ScrollPaneWindow w = new ScrollPaneWindow(null);
        // w.setTitle("First filter");
        int y = 0;
        for (Iterator<PixelObject> it = os.iterator(); it.hasNext();) {
            PixelObject akt = it.next();

            // w.setImage(0, y, akt.toLetter().getImage(3));

            if (akt.getArea() > 1800) {
                it.remove();
                // w.setText(1, y, "REM 1");
                y++;
            } else if (akt.getArea() < 80) {
                it.remove();
                // w.setText(1, y, "REM 2");
                y++;
                continue;
            } else if ((double) akt.getArea() / (double) akt.getSize() < 1.2) {
                // w.setText(1, y, "REM 3");
                y++;
                it.remove();
                continue;
            } else if (akt.getArea() / akt.getSize() > 15) {
                // w.setText(1, y, "REM 4");
                y++;
                it.remove();
                continue;
            } else if (akt.getHeight() < 10 || akt.getWidth() < 5) {
                // w.setText(1, y, "REM 5");
                y++;
                it.remove();
                continue;
            }
            // w.setText(1, y, "OK");
            y++;

            // BasicWindow.showImage(akt.toLetter().getImage(4), akt + "");
            // if (true && (|| akt.getArea() < 130 || (akt.getArea() > 600 &&
            // (double) akt.getArea() / (double) akt.getSize() < 1.2) ||
            // akt.getArea() / akt.getSize() > 10 || akt.getHeight() < 10 ||
            // akt.getWidth() < 5)) {
            // it.remove();
            // // BasicWindow.showImage(akt.toLetter().getImage(5),"fil
            // // "+akt.getArea()+" -"+((double)akt.getArea() /
            // // (double)akt.getSize())+" - "+akt.getHeight()+" -
            // // "+akt.getWidth());
            //
            // } else {
            //
            // }

        }

        ArrayList<Letter> ret = new ArrayList<Letter>();
        // int i = 0;
        // w = new ScrollPaneWindow(null);
        // w.setTitle("second filter");
        y = 0;
        for (PixelObject pixelObject : os) {
            Letter let = pixelObject.toLetter();

            // w.setImage(0, y, let.getImage(3));
            // let.blurIt(2);
            // let.toBlackAndWhite(1.16);

            for (int x = 0; x < let.getWidth(); x++) {
                for (int yy = 0; yy < let.getHeight(); yy++) {

                    if (UTILITIES.getColorDifference(let.getPixelValue(x, yy), let.getMaxPixelValue()) > 30) let.setPixelValue(x, yy, 0);

                }
            }
            // let.removeSmallObjects(0.8, 0.8, 10);
            let.clean();
            let = let.align(-40, 40);
            if (let == null) {
                // w.setText(1, y, "Let=null");
                y++;
                continue;
            }
            // w.setImage(1, y, let.getImage(3));

            PixelObject akt = let.toPixelObject(OBJECTDETECTIONCONTRAST);

            if (akt.getSize() > 1000) {
                // w.setText(2, y, "rem b 1");
                y++;
            } else if (akt.getArea() < 40) {
                // w.setText(2, y, "rem b 2");
                y++;
            } else if ((double) akt.getArea() / (double) akt.getSize() < 1.2) {
                // w.setText(2, y, "rem b 3");
                y++;
            } else if (akt.getArea() / akt.getSize() > 15) {
                // w.setText(2, y, "rem b 4");
                y++;
            } else if (akt.getHeight() < 10 || akt.getWidth() < 2) {
                // w.setText(2, y, "rem b 5");
                y++;
            } else {
                ret.add(let);
                // w.setText(2, y, "OK");
                y++;

            }

            // }
        }

        // for (int i = 0; i < letters.size(); i++) {
        // BasicWindow.showImage(letters.elementAt(i).toLetter().getImage(), "im
        // " + i);
        // PixelObject obj = letters.elementAt(i);
        //
        // Letter l = obj.toLetter();
        // //
        // l.removeSmallObjects(captcha.owner.getJas().getDouble(
        // "ObjectColorContrast"),
        // // captcha.owner.getJas().getDouble("ObjectDetectionContrast"));
        // captcha.owner.getJas().executeLetterPrepareCommands(l);
        // // if(owner.getJas().getInteger("leftAngle")!=0 ||
        // // owner.getJas().getInteger("rightAngle")!=0) l =
        // //
        // l.align(owner.getJas().getDouble("ObjectDetectionContrast"),owner.
        // getJas
        // ().getInteger("leftAngle"),owner.getJas().getInteger("rightAngle"));
        // // l.reduceWhiteNoise(2);
        // // l.toBlackAndWhite(0.6);
        //
        // ret[i] =
        // l.getSimplified(captcha.owner.getJas().getDouble("simplifyFaktor"));
        //
        // }
        // ret=ret.subList(0,4).toArray(a);
        if (ret.size() < 4) return null;
        return ret.toArray(new Letter[] {});

    }

    private static void mergeObjects(ArrayList<PixelObject> os) {
        for (PixelObject a : os) {
            for (PixelObject b : os) {
                if (a == b) continue;

                int xMin = Math.max(a.getXMin(), b.getXMin());
                int xMax = Math.min(a.getXMin() + a.getWidth(), b.getXMin() + b.getWidth());
                if (xMax <= xMin) continue;
                int yMin = Math.max(a.getYMin(), b.getYMin());
                int yMax = Math.min(a.getYMin() + a.getHeight(), b.getYMin() + b.getHeight());
                if (yMax <= yMin) continue;
                int area = (xMax - xMin) * (yMax - yMin);
                if (area > 10) {
                    if (a.getArea() < area * 1.5 || b.getArea() < area * 1.5) {
                        if (UTILITIES.getColorDifference(a.getAverage(), b.getAverage()) < 180) {

                            // ScrollPaneWindow w = new ScrollPaneWindow(null);
                            // //w.setImage(0, 1, a.toLetter().getImage(3));
                            // //w.setImage(0, 2, b.toLetter().getImage(3));
                            a.add(b);
                            // //w.setImage(0, 3, a.toLetter().getImage(3));
                            os.remove(b);
                            mergeObjects(os);
                            return;
                        }
                    }
                }
            }
        }

    }

    private static void clearCaptcha(Captcha captcha) {
        // ScrollPaneWindow w = new ScrollPaneWindow(null);
        // w.setTitle("Clearer");
        int y = 0;
        // w.setImage(0, y, captcha.getImage(3));
        // w.setText(1, y, "ORG");
        y++;
        int[][] copy = captcha.getGridCopy();
        cleanVerticalLines(captcha, copy);

        // w.setImage(0, y, captcha.getImage(3));
        // w.setText(1, y, "cleanVerticalLines");
        y++;
        cleanHorizontalLines(captcha, copy);
        // w.setImage(0, y, captcha.getImage(3));
        // w.setText(1, y, "cleanHorizontalLines");
        y++;
        cleanXLinesY(captcha, copy);
        // w.setImage(0, y, captcha.getImage(3));
        // w.setText(1, y, "cleanXLinesY");
        y++;
        cleanYLinesX(captcha, copy);
        // w.setImage(0, y, captcha.getImage(3));
        // w.setText(1, y, "cleanYLinesX");
        y++;
        captcha.cleanWithDetailMask(captcha.owner.createCaptcha(UTILITIES.loadImage(captcha.owner.getResourceFile("bg_mask.png"))), 10);
        captcha.cleanWithDetailMask(captcha.owner.createCaptcha(UTILITIES.loadImage(captcha.owner.getResourceFile("bg_mask_2.png"))), 5);
        captcha.cleanWithDetailMask(captcha.owner.createCaptcha(UTILITIES.loadImage(captcha.owner.getResourceFile("bg_mask_3.png"))), 5);
        captcha.cleanWithDetailMask(captcha.owner.createCaptcha(UTILITIES.loadImage(captcha.owner.getResourceFile("bg_mask_4.png"))), 5);
        captcha.cleanWithDetailMask(captcha.owner.createCaptcha(UTILITIES.loadImage(captcha.owner.getResourceFile("bg_mask_5.png"))), 5);

        // w.setImage(0, y, captcha.getImage(3));
        // w.setText(1, y, "cleanWithDetailMask");
        y++;
        // BasicWindow.showImage(captcha.getImage(3));

        cleanColor(captcha, 0x8080FF);
        cleanColor(captcha, 0xDFB27D);
        cleanColor(captcha, 0xA15FF4);
        cleanColor(captcha, 0xDEDF7D);
        cleanColor(captcha, 0xDF7D7D);
        cleanColor(captcha, 0xDF7DCB);

        // w.setImage(0, y, captcha.getImage(3));
        // w.setText(1, y, "cleanColor");
        y++;

        ArrayList<PixelObject> objs = getSmallObjects(captcha, 15);
        for (PixelObject po : objs) {
            if (po.getSize() < 3) {
                captcha.removeObjectFromGrid(po);
            }
        }
        // w.setImage(0, y, captcha.getImage(3));
        // w.setText(1, y, "removeSmallObjects");
        y++;
        // cleanColor(captcha,0xDFB27D);

        // BasicWindow.showImage(captcha.getImage(3));
    }

    public static void cleanColor(Captcha captcha, int avg) {
        ArrayList<PixelObject> objs = getColorObjects(captcha, avg, 5);
        for (PixelObject po : objs) {
            int size = po.getSize();
            PixelObject al = po.align(-90, 90);
            int w = al.getWidth();
            int h = al.getHeight();

            if (size > 100 && w < 16 && h > 35) {
                captcha.removeObjectFromGrid(po);
            }

        }

        // for (int x = 0; x < captcha.getWidth(); x++) {
        // for (int y = 0; y < captcha.getHeight(); y++) {
        //
        // double dif = UTILITIES.getColorDifference(captcha.grid[x][y], avg);
        // // if(JAntiCaptcha.isLoggerActive())logger.info(getPixelValue(x,
        // // y)+"_");
        // if (dif < 2) captcha.setPixelValue(x, y, captcha.getMaxPixelValue());
        //
        // }
        //
        // }
    }

    // grid = newgrid;

    /**
     * Entfernt striche von links unten nach rechts oben
     * 
     * @param captcha
     * @param copy
     */

    private static void cleanYLinesX(Captcha captcha, int[][] copy) {
        int lastX = captcha.getWidth() - 1;
        int lastY = captcha.getHeight() - 1;
        // schleife sucht vomlinken rand nach rechts oben
        for (int y = 0; y < captcha.getHeight(); y++) {
            double dist01 = y - 1 >= 0 ? UTILITIES.getColorDifference(copy[0][y], copy[1][y - 1]) : 0;
            double dist02 = y - 2 >= 0 ? UTILITIES.getColorDifference(copy[0][y], copy[2][y - 2]) : 0;
            double dist03 = y - 3 >= 0 ? UTILITIES.getColorDifference(copy[0][y], copy[3][y - 3]) : 0;
            if (dist01 < 10 && dist02 < 10 && dist03 < 10) {
                int c = 0;
                while (c < captcha.getWidth() && (y - c) >= 0) {

                    if (UTILITIES.getColorDifference(copy[0][y], captcha.grid[c][y - c]) < 5 && isLine(copy, c, y - c)) {
                        captcha.grid[c][y - c] = captcha.getMaxPixelValue();
                    }
                    c++;
                }

            }
        }
        // von unten nach rechts oben
        for (int x = 0; x < captcha.getWidth(); x++) {
            if (UTILITIES.getColorDifference(copy[x][lastY], PixelGrid.getMaxPixelValue(captcha.owner)) < 5) continue;
            double dist01 = x + 1 < captcha.getWidth() ? UTILITIES.getColorDifference(copy[x][lastY], copy[x + 1][lastY - 1]) : 0;
            double dist02 = x + 2 < captcha.getWidth() ? UTILITIES.getColorDifference(copy[x][lastY], copy[x + 2][lastY - 2]) : 0;
            double dist03 = x + 3 < captcha.getWidth() ? UTILITIES.getColorDifference(copy[x][lastY], copy[x + 3][lastY - 3]) : 0;
            if (dist01 < 10 && dist02 < 10 && dist03 < 10) {
                int c = 0;
                while (c < captcha.getHeight() && (x + c) < captcha.getWidth()) {

                    if (UTILITIES.getColorDifference(copy[x][lastY], captcha.grid[x + c][lastY - c]) < 5 && isLine(copy, x + c, lastY - c)) {
                        captcha.grid[x + c][lastY - c] = PixelGrid.getMaxPixelValue(captcha.owner);
                        // PixelGrid.getMaxPixelValue(captcha.owner);

                    }
                    c++;
                }

            }
        }
        // von rechts nach links unten
        for (int y = 0; y < captcha.getHeight(); y++) {
            double dist01 = y + 1 <= lastY ? UTILITIES.getColorDifference(copy[lastX][y], copy[lastX - 1][y + 1]) : 0;
            double dist02 = y + 2 <= lastY ? UTILITIES.getColorDifference(copy[lastX][y], copy[lastX - 2][y + 2]) : 0;
            double dist03 = y + 3 <= lastY ? UTILITIES.getColorDifference(copy[lastX][y], copy[lastX - 3][y + 3]) : 0;
            if (dist01 < 10 && dist02 < 10 && dist03 < 10) {
                int c = 0;
                while ((y + c) <= lastY) {

                    if (UTILITIES.getColorDifference(copy[lastX][y], captcha.grid[lastX - c][y + c]) < 5 && isLine(copy, lastX - c, y + c)) {
                        captcha.grid[lastX - c][y + c] = captcha.getMaxPixelValue();
                    }
                    c++;
                }

            }
        }
        // vom oberen rand nach links unten
        for (int x = 0; x < captcha.getWidth(); x++) {
            if (UTILITIES.getColorDifference(copy[x][0], PixelGrid.getMaxPixelValue(captcha.owner)) < 5) continue;
            double dist01 = x - 1 >= 0 ? UTILITIES.getColorDifference(copy[x][0], copy[x - 1][1]) : 0;
            double dist02 = x - 2 >= 0 ? UTILITIES.getColorDifference(copy[x][0], copy[x - 2][2]) : 0;
            double dist03 = x - 3 >= 0 ? UTILITIES.getColorDifference(copy[x][0], copy[x - 3][3]) : 0;
            if (dist01 < 10 && dist02 < 10 && dist03 < 10) {
                int c = 0;
                while (c < captcha.getHeight() && (x - c) >= 0) {

                    if (UTILITIES.getColorDifference(copy[x][0], captcha.grid[x - c][c]) < 5 && isLine(copy, x - c, c)) {
                        captcha.grid[x - c][c] = PixelGrid.getMaxPixelValue(captcha.owner);

                    }
                    c++;
                }

            }
        }

    }

    /**
     * Entfernt striche von links oben nach rechts unten
     * 
     * @param captcha
     * @param copy
     */
    private static void cleanXLinesY(Captcha captcha, int[][] copy) {
        int lastX = captcha.getWidth() - 1;
        int lastY = captcha.getHeight() - 1;
        // schleife sucht vom linken rand nach rechts unten
        for (int y = 0; y < captcha.getHeight(); y++) {
            double dist01 = y + 1 < captcha.getHeight() ? UTILITIES.getColorDifference(copy[0][y], copy[1][y + 1]) : 0;
            double dist02 = y + 2 < captcha.getHeight() ? UTILITIES.getColorDifference(copy[0][y], copy[2][y + 2]) : 0;
            double dist03 = y + 3 < captcha.getHeight() ? UTILITIES.getColorDifference(copy[0][y], copy[3][y + 3]) : 0;
            if (dist01 < 10 && dist02 < 10 && dist03 < 10) {
                int c = 0;
                while (c < captcha.getWidth() && (y + c) < captcha.getHeight()) {

                    if (UTILITIES.getColorDifference(copy[0][y], captcha.grid[c][y + c]) < 5 && isLine(copy, c, y + c)) {
                        captcha.grid[c][y + c] = captcha.getMaxPixelValue();
                    }
                    c++;
                }

            }
        }
        // schleife sucht vom oberen rand nach rechts unten
        for (int x = 0; x < captcha.getWidth(); x++) {
            if (UTILITIES.getColorDifference(copy[x][0], PixelGrid.getMaxPixelValue(captcha.owner)) < 5) continue;
            double dist01 = x + 1 < captcha.getWidth() ? UTILITIES.getColorDifference(copy[x][0], copy[x + 1][1]) : 0;
            double dist02 = x + 2 < captcha.getWidth() ? UTILITIES.getColorDifference(copy[x][0], copy[x + 2][2]) : 0;
            double dist03 = x + 3 < captcha.getWidth() ? UTILITIES.getColorDifference(copy[x][0], copy[x + 3][3]) : 0;
            if (dist01 < 10 && dist02 < 10 && dist03 < 10) {
                int c = 0;
                while (c < captcha.getHeight() && (x + c) < captcha.getWidth()) {

                    if (UTILITIES.getColorDifference(copy[x][0], captcha.grid[x + c][c]) < 5 && isLine(copy, x + c, c)) {
                        captcha.grid[x + c][c] = PixelGrid.getMaxPixelValue(captcha.owner);

                    }
                    c++;
                }

            } else {
                // captcha.grid[x][0] = 0xff0000;
            }
        }

        // schleife sucht vom rechten rand nachlinks oben
        for (int y = 0; y < captcha.getHeight(); y++) {
            double dist01 = y - 1 >= 0 ? UTILITIES.getColorDifference(copy[lastX][y], copy[lastX - 1][y - 1]) : 0;
            double dist02 = y - 2 >= 0 ? UTILITIES.getColorDifference(copy[lastX][y], copy[lastX - 2][y - 2]) : 0;
            double dist03 = y - 3 >= 0 ? UTILITIES.getColorDifference(copy[lastX][y], copy[lastX - 3][y - 3]) : 0;
            if (dist01 < 10 && dist02 < 10 && dist03 < 10) {
                int c = 0;
                while ((y - c) >= 0) {

                    if (UTILITIES.getColorDifference(copy[lastX][y], captcha.grid[lastX - c][y - c]) < 5 && isLine(copy, lastX - c, y - c)) {
                        captcha.grid[lastX - c][y - c] = captcha.getMaxPixelValue();
                    }
                    c++;
                }

            }
        }
        // schlaufe sucht vom unteren rand nach links oben
        for (int x = 0; x < captcha.getWidth(); x++) {
            if (UTILITIES.getColorDifference(copy[x][lastY], PixelGrid.getMaxPixelValue(captcha.owner)) < 5) continue;
            double dist01 = x - 1 >= 0 ? UTILITIES.getColorDifference(copy[x][lastY], copy[x - 1][lastY - 1]) : 0;
            double dist02 = x - 2 >= 0 ? UTILITIES.getColorDifference(copy[x][lastY], copy[x - 2][lastY - 2]) : 0;
            double dist03 = x - 3 >= 0 ? UTILITIES.getColorDifference(copy[x][lastY], copy[x - 3][lastY - 3]) : 0;
            if (dist01 < 10 && dist02 < 10 && dist03 < 10) {
                int c = 0;
                while (c < captcha.getHeight() && (x - c) >= 0) {

                    if (UTILITIES.getColorDifference(copy[x][lastY], captcha.grid[x - c][lastY - c]) < 5 && isLine(copy, x - c, lastY - c)) {
                        captcha.grid[x - c][lastY - c] = PixelGrid.getMaxPixelValue(captcha.owner);

                    }
                    c++;
                }

            } else {
                // captcha.grid[x][0] = 0xff0000;
            }
        }

    }

    private static void cleanHorizontalLines(Captcha captcha, int[][] copy) {

        for (int y = 0; y < captcha.getHeight(); y++) {
            double dist01 = UTILITIES.getColorDifference(copy[0][y], copy[1][y]);
            double dist02 = UTILITIES.getColorDifference(copy[0][y], copy[2][y]);
            double dist03 = UTILITIES.getColorDifference(copy[0][y], copy[3][y]);

            double dist11 = UTILITIES.getColorDifference(copy[captcha.getWidth() - 1][y], copy[captcha.getWidth() - 1 - 1][y]);
            double dist12 = UTILITIES.getColorDifference(copy[captcha.getWidth() - 1][y], copy[captcha.getWidth() - 1 - 2][y]);
            double dist13 = UTILITIES.getColorDifference(copy[captcha.getWidth() - 1][y], copy[captcha.getWidth() - 1 - 3][y]);

            if ((dist01 < 10 && dist02 < 10 && dist03 < 10)) {
                for (int x = 0; x < captcha.getWidth(); x++) {
                    if (UTILITIES.getColorDifference(copy[0][y], captcha.grid[x][y]) < 5 && isLine(copy, x, y)) {

                        captcha.grid[x][y] = captcha.getMaxPixelValue();
                    }
                }
            }

            if ((dist11 < 10 && dist12 < 10 && dist13 < 10)) {
                for (int x = 0; x < captcha.getWidth(); x++) {
                    if (UTILITIES.getColorDifference(copy[captcha.getWidth() - 1][y], captcha.grid[x][y]) < 5 && isLine(copy, x, y)) {
                        captcha.grid[x][y] = captcha.getMaxPixelValue();
                    }
                }
            }
        }

    }

    private static boolean isLine(int[][] copy, int xx, int yy) {
        return true;
        // int same=0;
        // int dif=0;
        // for(int x=-1; x<=1; x++){
        // for(int y=-1; y<=1; y++){
        // if(x==0&&y==0)continue;
        // if(xx+x<0 ||xx+x>copy.length-1)continue;
        // if(yy+y<0 ||yy+y>copy[0].length-1)continue;
        // if(UTILITIES.getColorDifference(copy[xx+x][yy+y], copy[xx][yy])<5){
        // same++;
        // }else{
        // dif++;
        // }
        //            
        // }
        // }
        // return same-dif<=0;
    }

    private static void cleanVerticalLines(Captcha captcha, int[][] copy) {

        for (int x = 0; x < captcha.getWidth(); x++) {
            double dist01 = UTILITIES.getColorDifference(copy[x][0], copy[x][1]);
            double dist02 = UTILITIES.getColorDifference(copy[x][0], copy[x][2]);
            double dist03 = UTILITIES.getColorDifference(copy[x][0], copy[x][3]);

            double dist11 = UTILITIES.getColorDifference(copy[x][captcha.getHeight() - 1], copy[x][captcha.getHeight() - 1 - 1]);
            double dist12 = UTILITIES.getColorDifference(copy[x][captcha.getHeight() - 1], copy[x][captcha.getHeight() - 1 - 2]);
            double dist13 = UTILITIES.getColorDifference(copy[x][captcha.getHeight() - 1], copy[x][captcha.getHeight() - 1 - 3]);

            if ((dist01 < 10 && dist02 < 10 && dist03 < 10)) {
                for (int y = 0; y < captcha.getHeight(); y++) {
                    if (UTILITIES.getColorDifference(copy[x][0], captcha.grid[x][y]) < 5 && isLine(copy, x, y)) {
                        captcha.grid[x][y] = captcha.getMaxPixelValue();
                    }
                }
            }

            if ((dist11 < 10 && dist12 < 10 && dist13 < 10)) {
                for (int y = 0; y < captcha.getHeight(); y++) {
                    if (UTILITIES.getColorDifference(copy[x][captcha.getHeight() - 1], captcha.grid[x][y]) < 5 && isLine(copy, x, y)) {
                        captcha.grid[x][y] = captcha.getMaxPixelValue();
                    }
                }
            }
        }
    }

    public static Letter[] letterFilter(Letter[] org, JAntiCaptcha jac) {
        int ths = Runtime.getRuntime().availableProcessors();
        MultiThreadDetection mtd = new MultiThreadDetection(ths, jac);
        // Vector<Letter> ret = new Vector<Letter>();

        ArrayList<Letter> r = new ArrayList<Letter>();
        int iii = 0;
        for (Letter l : org) {
            iii++;
            mtd.queueDetection(l);
            r.add(l);
        }
        mtd.waitFor(null);
        int id = 0;
        for (Iterator<Letter> it = r.iterator(); it.hasNext();) {
            Letter akt = it.next();
            // |||
            // |||
            if (true && (akt.detected.getDecodedValue().equals("1") || akt.detected.getDecodedValue().equals("."))) {
                it.remove();
            } else {
                akt.setId(id++);
            }
        }
        Collections.sort(r, new Comparator<Letter>() {
            public int compare(Letter o1, Letter o2) {
                if (o1.detected.getValityPercent() > o2.detected.getValityPercent()) { return 1; }
                if (o1.detected.getValityPercent() < o2.detected.getValityPercent()) { return -1; }
                return 0;
            }
        });
        List<Letter> list;
        try {
            list = r.subList(0, 4);
            // list = r;
        } catch (Exception e) {
            return null;
        }
        ;

        Collections.sort(list, new Comparator<Letter>() {
            public int compare(Letter o1, Letter o2) {
                if (o1.getId() > o2.getId()) { return 1; }
                if (o1.getId() < o2.getId()) { return -1; }
                return 0;
            }
        });

        if (list.size() < 4) return null;
        return list.toArray(new Letter[] {});
    }

}