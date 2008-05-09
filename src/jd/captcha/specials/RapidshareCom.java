//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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
import java.util.Vector;
import java.util.logging.Logger;

import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.gui.BasicWindow;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.pixelobject.PixelObject;
import jd.captcha.utils.UTILITIES;
import jd.utils.JDUtilities;

/**
 * 
 * 
 * @author JD-Team
 */
public class RapidshareCom {

    private static final Byte CAT = 1;

    private static final Byte DOG = 0;

    private static Logger logger = JDUtilities.getLogger();

    private static int fak = 2;

    private static int cats;

    private static JAntiCaptcha JAC;

    public static void prepareCaptcha(Captcha captcha) {

        getSimplified(captcha, fak);

    }

    public static void getSimplified(Captcha captcha, double faktor) {

        int newWidth = (int) Math.ceil((double) captcha.getWidth() / faktor);
        int newHeight = (int) Math.ceil((double) captcha.getHeight() / faktor);

        int avg = captcha.getAverage();

        int[][] newGrid = new int[newWidth][newHeight];

        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                PixelGrid.setPixelValue(x, y, newGrid, captcha.getMaxPixelValue(), captcha.owner);

            }
        }

        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {

                if (captcha.grid[x][y] <= 180) {
                    int newX = (int) Math.floor((double) x / faktor);
                    int newY = (int) Math.floor((double) y / faktor);
                    PixelGrid.setPixelValue(newX, newY, newGrid, 0, captcha.owner);

                }

            }
        }

        captcha.setGrid(newGrid);
        // BasicWindow.showImage(captcha.getImage(3));
        captcha.clean();

    }

    public static void onlyCats(Vector<LetterComperator> lcs, JAntiCaptcha owner) {
        if (true) return;
       

    }

   
    public static boolean isInMap(Vector<byte[]> map, ArrayList<Byte> list) {
        if (list.size() == 0) return true;

        for (Iterator<byte[]> it = map.iterator(); it.hasNext();) {
            byte[] next = it.next();
            if (next.length == list.size()) {
                int ii = 0;
                boolean c = true;
                for (int i : next) {
                    if (i != list.get(ii)) {
                        c = false;
                        break;
                    }
                    ii++;
                }
                if (c) { return true; }
            }

        }

        return false;

    }

    public static boolean preFormFilter(LetterComperator lc, JAntiCaptcha jac) {
        Letter db = lc.getB();
        Letter ca = lc.getA();
        if (db.getDecodedValue().equalsIgnoreCase("o")) {
            Object whites = ca.getProperty("whiteSpaces");
            if (whites == null) {
                whites = getWhiteObjects(ca, jac);
                ca.setProperty("whiteSpaces", whites);
            }
            Vector<PixelObject> whiteSpace = (Vector<PixelObject>) whites;
            if (whiteSpace.firstElement().getSize() < 100) {
                // BasicWindow.showImage(whiteSpace.firstElement().toLetter().getImage(2));
                return false;
            }

        }
        if (db.getDecodedValue().equalsIgnoreCase("q")) {
            Object whites = ca.getProperty("whiteSpaces");
            if (whites == null) {
                whites = getWhiteObjects(ca, jac);
                ca.setProperty("whiteSpaces", whites);
            }
            Vector<PixelObject> whiteSpace = (Vector<PixelObject>) whites;
            if (whiteSpace.firstElement().getSize() < 100) {
                // BasicWindow.showImage(whiteSpace.firstElement().toLetter().getImage(2));
                return false;
            }

        }
        if (db.getDecodedValue().equalsIgnoreCase("d")) {
            Object whites = ca.getProperty("whiteSpaces");
            if (whites == null) {
                whites = getWhiteObjects(ca, jac);
                ca.setProperty("whiteSpaces", whites);
            }
            Vector<PixelObject> whiteSpace = (Vector<PixelObject>) whites;
            if (whiteSpace.firstElement().getSize() < 100) {
                // BasicWindow.showImage(whiteSpace.firstElement().toLetter().getImage(2));
                return false;
            }

        }
        if (db.getDecodedValue().equalsIgnoreCase("s")) {
            Object whites = ca.getProperty("whiteSpaces");
            if (whites == null) {
                whites = getWhiteObjects(ca, jac);
                ca.setProperty("whiteSpaces", whites);
            }
            Vector<PixelObject> whiteSpace = (Vector<PixelObject>) whites;
            if (whiteSpace.firstElement().getSize() >50) {
                // BasicWindow.showImage(whiteSpace.firstElement().toLetter().getImage(2));
                return false;
            }

        }
        
        
        if (db.getDecodedValue().equalsIgnoreCase("8")) {
            Object whites = ca.getProperty("whiteSpaces");
            if (whites == null) {
                whites = getWhiteObjects(ca, jac);
                ca.setProperty("whiteSpaces", whites);
            }
            Vector<PixelObject> whiteSpace = (Vector<PixelObject>) whites;
            if (whiteSpace.firstElement().getSize() <50) {
                // BasicWindow.showImage(whiteSpace.firstElement().toLetter().getImage(2));
                return false;
            }

        }
        if (db.getDecodedValue().equalsIgnoreCase("9")) {
            Object whites = ca.getProperty("whiteSpaces");
            if (whites == null) {
                whites = getWhiteObjects(ca, jac);
                ca.setProperty("whiteSpaces", whites);
            }
            Vector<PixelObject> whiteSpace = (Vector<PixelObject>) whites;
            if (whiteSpace.firstElement().getSize() <50) {
                // BasicWindow.showImage(whiteSpace.firstElement().toLetter().getImage(2));
                return false;
            }

        }
        if (db.getDecodedValue().equalsIgnoreCase("b")) {
            Object whites = ca.getProperty("whiteSpaces");
            if (whites == null) {
                whites = getWhiteObjects(ca, jac);
                ca.setProperty("whiteSpaces", whites);
            }
            Vector<PixelObject> whiteSpace = (Vector<PixelObject>) whites;
            if (whiteSpace.firstElement().getSize() <50) {
                // BasicWindow.showImage(whiteSpace.firstElement().toLetter().getImage(2));
                return false;
            }

        }
        if (db.getDecodedValue().equalsIgnoreCase("c")) {
        //   return false;

        }
        return true;
    }

    public static Letter[] letterFilter(Letter[] org, JAntiCaptcha jac) {
        JAC = jac;
        int ths = Runtime.getRuntime().availableProcessors();
        MultiThreadDetection mtd = new MultiThreadDetection(ths, jac);
        Vector<Letter> ret = new Vector<Letter>();

        Vector<PixelObject> sp;

        // dummy.setValityPercent(100.0);

        int count = org.length;

        Letter ll;
        LetterComperator resletter;
        int bx = jac.getJas().getInteger("borderVarianceX");

        for (Letter l : org) {
            if (l.getArea() < jac.getJas().getInteger("minimumObjectArea")) count--;
        }
        int iii = 0;
        for (Letter l : org) {
            iii++;
            //BasicWindow.showImage(l.getImage(2), "" + iii);
            if (l.getWidth() > 150 / fak && count < 6) {
                sp = l.toPixelObject(0.85).split(3, 10 / fak);
                jac.getJas().set("borderVarianceX", 40);

                for (int i = 0; i < sp.size(); i++) {
                    // if(i==2){
                    // jac.setShowDebugGui(true);
                    // LetterComperator.CREATEINTERSECTIONLETTER = true;
                    // }
                    ll = sp.get(i).toLetter();
                    ll.setProperty("ValityLimit", 25);
                    mtd.queueDetection(ll);
                    ret.add(ll);
                    // mtd.waitFor(ret);
                    //
                    //
                    // jac.setShowDebugGui(false);
                    // LetterComperator.CREATEINTERSECTIONLETTER = false;

                }
                jac.getJas().set("borderVarianceX", bx);
                count += 2;
            } else if (l.getWidth() > 110 / fak && count < 7) {
                sp = l.toPixelObject(0.85).split(2, 10 / fak);
                jac.getJas().set("borderVarianceX", 40);
                for (int i = 0; i < sp.size(); i++) {
                    ll = sp.get(i).toLetter();
                    ll.setProperty("ValityLimit", 8);
                    mtd.queueDetection(ll);
                    ret.add(ll);
                }
                jac.getJas().set("borderVarianceX", bx);
                count++;
            } else if (l.getArea() >= jac.getJas().getInteger("minimumObjectArea")) {
                mtd.queueDetection(l);
                
                ret.add(l);
            }
        }
        mtd.waitFor(ret);

        double eaw = jac.jas.getDouble("errorAWeight");
        double iaww = jac.jas.getDouble("intersectionAWidthWeight");
        double idw = jac.jas.getDouble("intersectionDimensionWeight");
        jac.jas.set("errorAWeight", 4.0);
        jac.jas.set("intersectionAWidthWeight", 2.0);
        jac.jas.set("intersectionDimensionWeight", 2.0);

        for (int i = 0; i < ret.size(); i++) {
            Letter l = ret.get(i);
            int w = l.getWidth();
            double vp = l.detected.getValityPercent();

            Letter dif = l.detected.getDifference();
            int pixp = (int) (dif.getElementPixel() * 100.0 / dif.getArea());
            //BasicWindow.showImage(l.getImage(2), i + "");
            // l.getIntegerProperty("ValityLimit", 0)logger.info("kkkk"+pixp+" -
            // "+vp);
            if (pixp >= 26 && vp > (25.0 + l.getIntegerProperty("ValityLimit", 0)) && w > 2 * jac.getJas().getInteger("minimumLetterWidth") + 5/*
                                                                                                                                                 * &&
                                                                                                                                                 * count <
                                                                                                                                                 * 7
                                                                                                                                                 */) {

                ret.remove(i);
                sp = l.toPixelObject(0.85).split(2, 10 / fak);
                jac.getJas().set("borderVarianceX", 20);
                for (int ii = 0; ii < sp.size(); ii++) {
                    ll = sp.get(ii).toLetter();
                    ll.setProperty("ValityLimit", 8);
                    // BasicWindow.showImage(ll.getImage(2));
                    mtd.queueDetection(ll);
                    ret.add(i + ii, ll);
                }
                i++;
                jac.getJas().set("borderVarianceX", bx);
                count++;

            }

        }
        mtd.waitFor(ret);

        jac.jas.set("errorAWeight", eaw);
        jac.jas.set("intersectionAWidthWeight", iaww);
        jac.jas.set("intersectionDimensionWeight", idw);
        for (Iterator<Letter> it = ret.iterator(); it.hasNext();) {

            Letter l = it.next();
            Vector<PixelObject> objects = getWhiteObjects(l, jac);
        }
        // for (Iterator<Letter> it = ret.iterator(); it.hasNext();) {
        // if (it.next().detected.getValityPercent() >= 90) {
        // // it.remove();
        // }
        //            
        //           
        //
        // }

        jac.setLetterNum(ret.size());

        return filterCats(ret.toArray(new Letter[] {}), jac);
    }

    private static Letter[] filterCats(Letter[] letters, JAntiCaptcha jac) {
        // if (true) return letters;

        String methodsPath = UTILITIES.getFullPath(new String[] { JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath(), "jd", "captcha", "methods" });
        String hoster = "rscat.com";

        JAntiCaptcha vjac = new JAntiCaptcha(methodsPath, hoster);

        // vjac.setShowDebugGui(true);
        // LetterComperator.CREATEINTERSECTIONLETTER = true;
        int ths = Runtime.getRuntime().availableProcessors();
        MultiThreadDetection mtd = new MultiThreadDetection(ths, vjac);
        Vector<byte[]> map = new Vector<byte[]>();
        map.add(new byte[] { 1, 1, 1, 1 });
        map.add(new byte[] { 1, 0, 1, 1, 1 });
        map.add(new byte[] { 0, 1, 1, 1, 1 });
        map.add(new byte[] { 1, 1, 1, 0, 1 });
        map.add(new byte[] { 1, 1, 0, 1, 1 });

        map.add(new byte[] { 0, 1, 0, 1, 1, 1 });
        map.add(new byte[] { 0, 1, 1, 0, 1, 1 });
        map.add(new byte[] { 1, 0, 1, 0, 1, 1 });
        map.add(new byte[] { 1, 1, 0, 1, 0, 1 });
        map.add(new byte[] { 1, 0, 1, 1, 0, 1 });
        map.add(new byte[] { 0, 1, 1, 1, 0, 1 });

        map.add(new byte[] { 0, 1, 0, 1, 1, 0, 1 });
        map.add(new byte[] { 0, 1, 1, 0, 1, 0, 1 });
        map.add(new byte[] { 1, 0, 1, 0, 1, 0, 1 });
        map.add(new byte[] { 0, 1, 0, 1, 0, 1, 1 });
        ArrayList<Byte> catList = new ArrayList<Byte>();
        LetterComperator dummy;
        int cats = 0;
        int count = 0;

        Vector<Letter> lets = new Vector<Letter>();
        int id = 0;
        for (Letter l : letters) {
            if (l != letters[letters.length - 1]) {
                Letter d = new Letter();

                d.setOwner(l.owner);
                d.setGridCopy(l.grid, 0, 0, 0, 0);
                lets.add(d);
                d.setProperty("org", l);
                l.id = id++;
                mtd.queueDetection(d);
                // Letter dif = l.detected.getDifference();
                // BasicWindow.showImage(dif.getImage(4), "letter " + count++);
                // if (isCat(dif, map, catList, letters.length)) {
                // l.colorize(0xff0000);
                // catList.add(CAT);
                // } else {
                // catList.add(DOG);
                // }
            } else {
                Letter d = new Letter();

                d.setOwner(l.owner);
                d.setGridCopy(l.grid, 0, 0, 0, 0);
                lets.add(d);
                d.setProperty("org", l);
                l.id = id++;
                d.detected = new LetterComperator(d, d);
                d.detected.setValityPercent(0.0);
                d.detected.getB().setDecodedValue("k");
            }

        }
        mtd.waitFor(null);

        Collections.sort(lets, new Comparator<Letter>() {
            public int compare(Letter a, Letter b) {
                if (a.detected.getValityPercent() > b.detected.getValityPercent()) return 1;
                if (a.detected.getValityPercent() < b.detected.getValityPercent()) return -1;
                return 0;
            }

        });

        // while (true) {
        // ArrayList<Byte> catlist = new ArrayList<Byte>();
        // for (int letNum = 0; letNum < letters.length; letNum++) {
        // catlist.add((byte) 0);
        // }
        // for (int i = 0; i < Math.min(4, lets.size()); i++) {
        // Letter l = ((Letter) lets.get(i).getProperty("org"));
        // for (int letNum = 0; letNum < letters.length; letNum++) {
        // if (letters[letNum] == l) {
        // catlist.set(letNum, (byte) 1);
        // break;
        // }
        // }
        // // ((Letter)lets.get(i).getProperty("org")).colorize(0xff0000);
        // }
        // if (isInMap(map, catlist)) break;
        // lets.remove(3);
        // if (lets.size() < 4) break;
        // }
        ArrayList<Letter> filtered = new ArrayList<Letter>();
        for (int i = 0; i < Math.min(4, lets.size()); i++) {
            Letter l = ((Letter) lets.get(i).getProperty("org"));
            l.colorize(0x000000);
            filtered.add(l);

            // ((Letter)lets.get(i).getProperty("org")).colorize(0xff0000);
        }
        Collections.sort(filtered, new Comparator<Letter>() {
            public int compare(Letter a, Letter b) {
                if (a.id > b.id) return 1;
                if (a.id < b.id) return -1;
                return 0;
            }

        });
        // for (Letter l : letters) {
        //     
        // // Letter dif = l.detected.getDifference();
        // BasicWindow.showImage(l.getImage(4), "letter "
        // +l.detected.getValityPercent());
        // // if (isCat(dif, map, catList, letters.length)) {
        // //// l.colorize(0xff0000);
        // //
        // }
        //return letters;
         return filtered.toArray(new Letter[]{});
    }

    // private static boolean isCat(Letter l, Vector<byte[]> map,
    // ArrayList<Byte> catList, int count) {
    //
    // // byte[] pos = isPossible(map, catList, count);
    // // if (pos[0] == 0 && pos[1] == 0) {
    // // BasicWindow.showImage(l.getImage(2), "letter " + catList + " - false
    // // ");
    // // return false;
    // // }
    // // if (pos[0] == 0) {
    // // BasicWindow.showImage(l.getImage(2), "letter " + catList + " - true
    // // ");
    // // return true;
    // // }
    // // if (pos[1] == 0) {
    // // BasicWindow.showImage(l.getImage(2), "letter " + catList + " - false
    // // ");
    // // return false;
    // // }
    // String methodsPath = UTILITIES.getFullPath(new String[] {
    // JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath(), "jd",
    // "captcha", "methods" });
    // String hoster = "rscat.com";
    //
    // JAntiCaptcha jac = new JAntiCaptcha(methodsPath, hoster);
    // jac.setShowDebugGui(true);
    // LetterComperator.CREATEINTERSECTIONLETTER = true;
    //
    // LetterComperator c = jac.getLetter(l);
    // l.detected = c;
    // boolean ret = c.getDecodedValue().equalsIgnoreCase("k");
    // // BasicWindow.showImage(l.getImage(2), "letter " + catList + " - " +
    // // ret + " - " + c.getValityPercent());
    // return ret;
    //
    // }

    // private static Letter[] fill(Letter[] org, JAntiCaptcha jac) {
    // if (true) return org;
    // for (Letter l : org) {
    //
    // fillLetter(l, jac);
    //
    // }
    // return org;
    // }

    private static Vector<PixelObject> getWhiteObjects(Letter l, JAntiCaptcha jac) {

        int limit = 200;
        int[][] tmp = new int[l.getWidth()][l.getHeight()];
        Vector<PixelObject> ret = new Vector<PixelObject>();
        for (int x = 0; x < l.getWidth(); x++) {
            for (int y = 0; y < l.getHeight(); y++) {
                if (l.grid[x][y] > limit && tmp[x][y] != 1) {
                    PixelObject p = new PixelObject(l);
                    recFill(p, l, x, y, tmp, 0);

                    if (p.isBordered()) {
                        ret.add(p);
                        p.setColor(0x000000);
                    }

                    // BasicWindow.showImage(p.toLetter().getImage(), x+" -
                    // "+y);

                }
            }
        }
        Collections.sort(ret, new Comparator<PixelObject>() {
            public int compare(PixelObject a, PixelObject b) {
                if (a.getSize() < b.getSize()) return 1;
                if (a.getSize() > b.getSize()) return -1;
                return 0;
            }

        });
        return ret;

    }

    private static void recFill(PixelObject p, Letter l, int x, int y, int[][] tmp, int i) {
        i++;
        if (x >= 0 && y >= 0 && x < l.getWidth() && y < l.getHeight() && l.grid[x][y] > 200 && tmp[x][y] != 1) {
            if (x == 0 || y == 0 || x == l.getWidth() - 1 || y == l.getHeight() - 1) {
                p.setBordered(false);
            }
            p.add(x, y, 0xff0000);
            tmp[x][y] = 1;
            recFill(p, l, x - 1, y, tmp, i);
            // getObject(x - 1, y - 1, tmpGrid, object);
            recFill(p, l, x, y - 1, tmp, i);
            // getObject(x + 1, y - 1, tmpGrid, object);
            recFill(p, l, x + 1, y, tmp, i);
            // getObject(x + 1, y + 1, tmpGrid, object);
            recFill(p, l, x, y + 1, tmp, i);
            // getObject(x - 1, y + 1, tmpGrid, object);

        }

    }

}