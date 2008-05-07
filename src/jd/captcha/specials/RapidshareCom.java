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
import java.util.HashMap;
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

        // if (faktor == 1.0) return this;
        // int newWidth = (int) Math.ceil(getWidth() / faktor);
        // int newHeight = (int) Math.ceil(getHeight() / faktor);
        // Letter ret = new Letter();
        // ;
        // ret.setOwner(this.owner);
        // int avg = getAverage();
        // int value;
        // int[][] newGrid = new int[newWidth][newHeight];
        // elementPixel = 0;
        // for (int x = 0; x < newWidth; x++) {
        // for (int y = 0; y < newHeight; y++) {
        //
        // value = 0;
        // for (int gx = 0; gx < faktor; gx++) {
        // for (int gy = 0; gy < faktor; gy++) {
        // int newX = x * faktor + gx;
        // int newY = y * faktor + gy;
        // if (newX > getWidth() || newY > getHeight()) {
        // continue;
        // }
        // //
        // if (isElement(getPixelValue(newX, newY), avg)) {
        // value++;
        // }
        // }
        //
        // }
        //
        // // setPixelValue(x, y, newGrid, getPixelValue(x* faktor, y*
        // // faktor), this.owner);
        // setPixelValue(x, y, newGrid, ((value * 100) / (faktor * faktor)) > 50
        // ? 0 : getMaxPixelValue(), this.owner);
        // if (newGrid[x][y] == 0) elementPixel++;
        // }
        // }
        //
        // ret.setGrid(newGrid);
        //
        // ret.clean();
        //
        // return ret;
    }

    public static Captcha rsDesin(Captcha captcha, double maxx, double omegax, double phix, double maxy, double omegay, double phiy) {
        int shift;
        omegax = 2 * Math.PI / omegax;
        omegay = 2 * Math.PI / omegay;

        int bestArea = 0;
        int[][] bestGrid = null;

        // all: for (int ax = 0; ax < 2; ax++) {
        // for (int ay = 0; ay < 2; ay++) {

        int[][] tmp = new int[captcha.getWidth()][captcha.getHeight()];
        int[][] tmp2 = new int[captcha.getWidth()][captcha.getHeight()];

        for (int y = 0; y < captcha.getHeight(); y++) {

            shift = (int) (maxy * Math.sin(omegay * (y + phiy)));

            for (int x = 0; x < captcha.getWidth(); x++) {

                tmp[x][y] = (x + shift < captcha.getWidth() && x + shift >= 0) ? captcha.grid[x + shift][y] : 0xFF;
            }
            tmp[25 + shift][y] = 0xff0000 + 0x00ff00;

        }
        for (int x = 0; x < captcha.getWidth(); x++) {

            shift = (int) ((maxx * Math.sin(omegax * (x + phix))));

            for (int y = 0; y < captcha.getHeight(); y++) {

                tmp2[x][y] = (y + shift < captcha.getHeight() && y + shift >= 0) ? tmp[x][y + shift] : 0xFF;
            }
            tmp2[x][33 + shift] = 0xff0000 + 0x00ff00;

        }

        // PixelGrid p = new PixelGrid(getWidth(),getHeight());
        // p.setGrid(tmp2);
        // BasicWindow.showImage(p.getImage(),i+"");

        // }
        // }

        Captcha ret = new Captcha(captcha.getWidth(), captcha.getHeight());
        ret.setOwner(captcha.owner);

        ret.setGrid(tmp2);
        ret.setOrgGrid(PixelGrid.getGridCopy(tmp2));
        return ret;

        // }
        // // }
        // Collections.sort(sorter, new Comparator<Object>() {
        // public int compare(Object a, Object b) {
        // return map.get(a).compareTo(map.get(b));
        //
        // }
        //
        // });
        // Vector<LetterComperator> sorted = new Vector<LetterComperator>();
        // String methodsPath = UTILITIES.getFullPath(new String[] {
        // JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath(),
        // "jd", "captcha", "methods" });
        // String hoster = "rscat.com";
        //
        // JAntiCaptcha jac = new JAntiCaptcha(methodsPath, hoster);
        // jac.getJas().set("preScanFilter", 40);
        // jac.getJas().set("LetterSearchLimitPerfectPercent", 0);
        //
        // jac.getJas().set("coverageFaktorAWeight", 0.0);
        // jac.getJas().set("scanstepy", 2);
        // for (Iterator<int[][]> it = sorter.iterator(); it.hasNext();) {
        // int[][] next = it.next();
        // Letter l = new Letter();
        //
        // l.setOwner(owner);
        // // LetterComperator.CREATEINTERSECTIONLETTER = true;
        // // jac.setShowDebugGui(true);
        // l.setGridCopy(next, 0, 0, PixelGrid.getGridWidth(next) - 300, 0);
        // l.setProperty("org", next);
        // if (owner.isShowDebugGui()) BasicWindow.showImage(l.getImage(),
        // "pre");
        //
        // // l.crop(0, 0, l.getWidth()-80, 0);
        // l.clean();
        // // if (owner.isShowDebugGui()) BasicWindow.showImage(l.getImage(),
        // // "post");
        // @SuppressWarnings("unused")
        // LetterComperator lc = jac.getLetter(l);
        // int o = 0;
        // do {
        // if (o == sorted.size()) {
        // sorted.add(lc);
        // break;
        // }
        // if (sorted.get(o).getValityPercent() > lc.getValityPercent()) {
        // sorted.add(0, lc);
        // break;
        // }
        // o++;
        // } while (true);
        //
        // }
        //
        // logger.info("USE VARIANT: " +
        // owner.getJas().getInteger("desinvariant"));
        // this.setGrid((int[][])
        // sorted.get(owner.getJas().getInteger("desinvariant")).getA().getProperty("org"));
        //
        // if (owner.isShowDebugGui()) BasicWindow.showImage(this.getImage(),
        // "USE VARIANT: " + owner.getJas().getInteger("desinvariant"));

    }

    public static void onlyCats(Vector<LetterComperator> lcs, JAntiCaptcha owner) {
        if (true) return;
//        boolean first = false;
//        if (owner.getJas().getInteger("desinvariant") == 0) {
//            owner.getWorkingCaptcha().setProperty("variants", new ArrayList<Vector<LetterComperator>>());
//            first = true;
//        }
//        int count = 0;
//        if (lcs != null && lcs.size() > 0) {
//            for (Iterator<LetterComperator> it = lcs.iterator(); it.hasNext();) {
//                LetterComperator next = it.next();
//                logger.info("--> " + next.getDecodedValue());
//                count += next.getValityPercent() >= 60.0 ? 1 : 0;
//            }
//
//        }
//        Vector<LetterComperator> tmp = new Vector<LetterComperator>();
//        tmp.addAll(lcs);
//        ((ArrayList<Vector<LetterComperator>>) owner.getWorkingCaptcha().getProperty("variants")).add(tmp);
//
//        if (count > 3 || lcs == null || lcs.size() < 4) {
//            logger.severe("ACHTUNG ERKENNUNGSFEHLER " + count);
//            lcs.removeAllElements();
//            retry(owner, lcs);
//            if (first) {
//                double bestValue = 100.0;
//                Vector<LetterComperator> bestLcs = null;
//                ArrayList<Vector<LetterComperator>> variants = ((ArrayList<Vector<LetterComperator>>) owner.getWorkingCaptcha().getProperty("variants"));
//                for (Iterator<Vector<LetterComperator>> it = variants.iterator(); it.hasNext();) {
//                    Vector<LetterComperator> next = it.next();
//                    if (next != null && next.size() >= 4) {
//                        double cor = 0;
//                        for (Iterator<LetterComperator> it2 = next.iterator(); it2.hasNext();) {
//                            cor += it2.next().getValityPercent();
//                        }
//                        cor /= next.size();
//                        if (cor < bestValue) {
//                            logger.info("new Best LCS: " + cor);
//                            bestLcs = next;
//                            bestValue = cor;
//                        }
//                    }
//                }
//                if (lcs != bestLcs) {
//                    lcs.removeAllElements();
//                    lcs.addAll(bestLcs);
//                }
//            }
//            return;
//        }
//        if (true) return;
//        final HashMap<LetterComperator, LetterComperator> map = new HashMap<LetterComperator, LetterComperator>();
//
//        String methodsPath = UTILITIES.getFullPath(new String[] { JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath(), "jd", "captcha", "methods" });
//        String hoster = "rscat.com";
//        final Vector<LetterComperator> cats = new Vector<LetterComperator>();
//   
//        JAntiCaptcha jac = new JAntiCaptcha(methodsPath, hoster);
//        jac.setShowDebugGui(true);
//        LetterComperator.CREATEINTERSECTIONLETTER = true;
//        for (Iterator<LetterComperator> it = lcs.iterator(); it.hasNext();) {
//            LetterComperator next = it.next();
//            Letter dif = next.getDifference();
//            dif.removeSmallObjects(0.8, 0.8, 5);
//            dif.clean();
//
//            LetterComperator c = jac.getLetter(dif);
//            if (!c.getDecodedValue().equalsIgnoreCase("k")) {
//                it.remove();
//            } else {
//                map.put(next, c);
//                cats.add(c);
//            }
//
//        }
//
//        Collections.sort(cats, new Comparator<LetterComperator>() {
//            public int compare(LetterComperator obj1, LetterComperator obj2) {
//
//                if (obj1.getValityPercent() < obj2.getValityPercent()) return -1;
//                if (obj1.getValityPercent() > obj2.getValityPercent()) return 1;
//                return 0;
//            }
//        });
//
//        // schlechte entfernen
//
//        for (int i = cats.size() - 1; i >= 4; i--) {
//            cats.remove(i);
//        }
//        for (Iterator<LetterComperator> it = lcs.iterator(); it.hasNext();) {
//            LetterComperator next = it.next();
//            if (!map.containsKey(next) || !cats.contains(map.get(next))) it.remove();
//        }
//        logger.info("LENGTH : " + lcs.size());

    }

//    private static void retry(JAntiCaptcha jac, Vector<LetterComperator> lcs) {
//        int desiinvariant = jac.getJas().getInteger("desinvariant") + 1;
//        if (desiinvariant > 4) { return; }
//        jac.getJas().set("desinvariant", desiinvariant);
//        Captcha captcha = jac.getWorkingCaptcha();
//        captcha.reset();
//        jac.getJas().executePrepareCommands(captcha);
//        int ln = jac.getLetterNum();
//        Letter[] letters = captcha.getLetters(ln);
//        if (letters == null) return;
//        // LetterComperator[] newLetters = new LetterComperator[letters.length];
//
//        LetterComperator akt;
//
//        if (letters == null) {
//            captcha.setValityPercent(100.0);
//            if (JAntiCaptcha.isLoggerActive()) logger.severe("Captcha konnte nicht erkannt werden!");
//            return;
//        }
//
//        Vector<LetterComperator> newLettersVector = new Vector<LetterComperator>();
//        for (int i = 0; i < letters.length; i++) {
//
//            if (letters[i].detected != null)
//                akt = letters[i].detected;
//            else
//                akt = jac.getLetter(letters[i]);
//
//            akt.getA().id = i;
//            newLettersVector.add(akt);
//
//        }
//        onlyCats(newLettersVector, jac);
//        if (newLettersVector.size() > 0) {
//            lcs.removeAllElements();
//            lcs.addAll(newLettersVector);
//        }
//
//    }

    /**
     * Filtert kleine Buchstaben
     * 
     * @param count
     * 
     * @param org
     * @param jac
     * @return
     */
//    public static byte[] isPossible(Vector<byte[]> map, ArrayList<Byte> list, int count) {
//        if (list.size() == 0 || true) return new byte[] { 1, 1 };
//
//        byte[] ret = new byte[2];
//        ret[0] = 0;
//        ret[1] = 0;
//        for (Iterator<byte[]> it = map.iterator(); it.hasNext();) {
//            byte[] next = it.next();
//            if (next.length < count || next.length <= list.size() || next[list.size() - 1] != list.get(list.size() - 1)) {
//                it.remove();
//            }
//        }
//        for (Iterator<byte[]> it = map.iterator(); it.hasNext();) {
//            byte[] next = it.next();
//
//            if (next[list.size()] == 1) {
//                ret[1] = 1;
//            } else {
//                ret[0] = 1;
//            }
//        }
//
//        return ret;
//
//    }

    public static boolean isInMap(Vector<byte[]> map, ArrayList<Byte> list) {
        if (list.size() == 0) return true;

        for (Iterator<byte[]> it = map.iterator(); it.hasNext();) {
            byte[] next = it.next();
            if (next.length == list.size()) {
                int ii = 0;
                boolean c=true;
                for (int i : next) {
                    if (i != list.get(ii)){
                        c=false;
                        break;
                    }
                    ii++;
                }
                if(c){
                return true;
                }
            }

        }

        return false;

    }

    //
    // public static Letter[] letterFilter(Letter[] org, JAntiCaptcha jac) {
    // Vector<byte[]> map = new Vector<byte[]>();
    // map.add(new byte[] { 1, 1, 1, 1 });
    // map.add(new byte[] { 1, 0, 1, 1, 1 });
    // map.add(new byte[] { 0, 1, 1, 1, 1 });
    // map.add(new byte[] { 1, 1, 1, 0, 1 });
    // map.add(new byte[] { 1, 1, 0, 1, 1 });
    //
    // map.add(new byte[] { 0, 1, 0, 1, 1, 1 });
    // map.add(new byte[] { 0, 1, 1, 0, 1, 1 });
    // map.add(new byte[] { 1, 0, 1, 0, 1, 1 });
    // map.add(new byte[] { 1, 1, 0, 1, 0, 1 });
    // map.add(new byte[] { 1, 0, 1, 1, 0, 1 });
    // map.add(new byte[] { 0, 1, 1, 1, 0, 1 });
    //
    // map.add(new byte[] { 0, 1, 0, 1, 1, 0, 1 });
    // map.add(new byte[] { 0, 1, 1, 0, 1, 0, 1 });
    // map.add(new byte[] { 1, 0, 1, 0, 1, 0, 1 });
    // map.add(new byte[] { 0, 1, 0, 1, 0, 1, 1 });
    //
    // Vector<Letter> ret = new Vector<Letter>();
    // ArrayList<Byte> catList = new ArrayList<Byte>();
    // Vector<PixelObject> sp;
    // LetterComperator dummy;
    // // dummy.setValityPercent(100.0);
    //
    // int count = org.length;
    // int cats = 0;
    // Letter ll;
    // LetterComperator resletter;
    // int bx = jac.getJas().getInteger("borderVarianceX");
    //
    // for (Letter l : org) {
    // if (l.getArea() < jac.getJas().getInteger("minimumObjectArea")) count--;
    // }
    // int iii=0;
    // for (Letter l : org) {
    // iii++;
    // BasicWindow.showImage(l.getImage(2),l+" "+iii);
    //
    // if (l.getWidth() > 140 / fak && count < 6) {
    // sp = l.toPixelObject(0.85).split(3, 10 / fak);
    // jac.getJas().set("borderVarianceX", 20);
    // for (int i = 0; i < sp.size(); i++) {
    // ll = sp.get(i).toLetter();
    // dummy = new LetterComperator(ll, null);
    // if (isCat(ll, map, catList, count)) {
    // catList.add(CAT);
    // cats++;
    //
    // resletter = jac.getLetter(ll);
    // ll.detected = resletter;
    // ll.colorize(0xff0000);
    // } else {
    // catList.add(DOG);
    // ll.detected = dummy;
    // }
    // ret.add(ll);
    // }
    // jac.getJas().set("borderVarianceX", bx);
    // count += 2;
    // } else if (l.getWidth() > 110 / fak && count < 7) {
    // sp = l.toPixelObject(0.85).split(2, 10 / fak);
    // jac.getJas().set("borderVarianceX", 20);
    // for (int i = 0; i < sp.size(); i++) {
    // ll = sp.get(i).toLetter();
    // dummy = new LetterComperator(ll, null);
    // if (isCat(ll, map, catList, count)) {
    // cats++;
    // catList.add(CAT);
    // resletter = jac.getLetter(ll);
    // ll.detected = resletter;
    // ll.colorize(0xff0000);
    // } else {
    // catList.add(DOG);
    // ll.detected = dummy;
    // }
    // ret.add(ll);
    // }
    // jac.getJas().set("borderVarianceX", bx);
    // count++;
    // } else if (l.getArea() >= jac.getJas().getInteger("minimumObjectArea")) {
    // resletter = jac.getLetter(l);
    // l.detected = resletter;
    // if (l.getWidth() > 80 / fak && count < 7) {
    //                  
    // if (resletter.getValityPercent() > 30) {
    // sp = l.toPixelObject(0.85).split(2, 10 / fak);
    // int p = 0;
    // Letter[] lll = new Letter[2];
    // jac.getJas().set("borderVarianceX", 20);
    //
    // for (int i = 0; i < sp.size(); i++) {
    // ll = sp.get(i).toLetter();
    // dummy = new LetterComperator(ll, null);
    // if (isCat(ll, map, catList, count)) {
    // cats++;
    // resletter = jac.getLetter(ll);
    // ll.detected = resletter;
    // catList.add(CAT);
    // p = Math.max(p, (int) resletter.getValityPercent());
    //
    // } else {
    // catList.add(DOG);
    // ll.detected = dummy;
    // }
    //
    // lll[i] = ll;
    // }
    // jac.getJas().set("borderVarianceX", bx);
    //
    // if (p < l.detected.getValityPercent()) {
    // for (int i = 0; i < sp.size(); i++) {
    // // dummy = new LetterComperator(lll[i], null);
    // // if (isCat(lll[i], map, catList, count)) {
    // // cats++;
    // // catList.add(CAT);
    // // resletter = jac.getLetter(lll[i]);
    // // lll[i].detected = resletter;
    // // } else {
    // // lll[i].detected = dummy;
    // // catList.add(DOG);
    // // }
    // if(lll[i].detected.getValityPercent()<100.0) lll[i].colorize(0xff0000);
    // ret.add(lll[i]);
    // }
    // count++;
    // } else {
    // cats-=2;
    // catList.remove(catList.size()-1);
    // catList.remove(catList.size()-1);
    // dummy = new LetterComperator(l, null);
    // if (isCat(l, map, catList, count)) {
    // cats++;
    // catList.add(CAT);
    // l.colorize(0xff0000);
    // // resletter = jac.getLetter(l);
    // // l.detected = resletter;
    // } else {
    // catList.add(DOG);
    // // l.detected = dummy;
    // }
    // ret.add(l);
    // }
    //
    // } else {
    // dummy = new LetterComperator(l, null);
    // if (isCat(l, map, catList, count)) {
    // cats++;
    // catList.add(CAT);
    // l.colorize(0xff0000);
    // // resletter = jac.getLetter(l);
    // // l.detected = resletter;
    // } else {
    // catList.add(DOG);
    // // l.detected = dummy;
    // }
    // ret.add(l);
    // }
    // } else {
    // dummy = new LetterComperator(l, null);
    // if (isCat(l, map, catList, count)) {
    // cats++;
    // catList.add(CAT);
    // // resletter = jac.getLetter(l);
    // // l.detected = resletter;
    // l.colorize(0xff0000);
    // } else {
    // catList.add(DOG);
    // // l.detected = dummy;
    // }
    // ret.add(l);
    // }
    // }
    // }
    //
    // logger.info("Found " + cats + " Cats");
    // jac.setLetterNum(ret.size());
    // ;
    // return fill(ret.toArray(new Letter[] {}), jac);
    // }

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
            // BasicWindow.showImage(l.getImage(2), ""+iii);
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
            } else if (l.getWidth() > 100 / fak && count < 7) {
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
                // if (l.getWidth() > 80 / fak && count < 7 && false) {
                //
                // if (resletter.getValityPercent() > 30) {
                // sp = l.toPixelObject(0.85).split(2, 10 / fak);
                // int p = 0;
                // Letter[] lll = new Letter[2];
                // jac.getJas().set("borderVarianceX", 20);
                //
                // for (int i = 0; i < sp.size(); i++) {
                // ll = sp.get(i).toLetter();
                //
                // p = Math.max(p, (int) resletter.getValityPercent());
                //
                // lll[i] = ll;
                // }
                // jac.getJas().set("borderVarianceX", bx);
                //
                // if (p < l.detected.getValityPercent()) {
                // for (int i = 0; i < sp.size(); i++) {
                // ret.add(lll[i]);
                // }
                // count++;
                // } else {
                //
                // ret.add(l);
                // }
                //
                // } else {
                //
                // ret.add(l);
                // }
                ret.add(l);
            }
        }
        mtd.waitFor(ret);

        for (int i = 0; i < ret.size(); i++) {
            Letter l = ret.get(i);
            int w = l.getWidth();
            double vp = l.detected.getValityPercent();

            // BasicWindow.showImage(l.getImage(2));
            if (vp > (68.0 + l.getIntegerProperty("ValityLimit", 0)) && w > 3 * jac.getJas().getInteger("minimumLetterWidth") + 5 && count < 7) {

                ret.remove(i);
                sp = l.toPixelObject(0.85).split(2, 10 / fak);
                jac.getJas().set("borderVarianceX", 40);
                for (int ii = 0; ii < sp.size(); ii++) {
                    ll = sp.get(ii).toLetter();
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

        for (Iterator<Letter> it = ret.iterator(); it.hasNext();) {
            if (it.next().detected.getValityPercent() >= 90) {
                // it.remove();
            }

        }
        jac.setLetterNum(ret.size());

        return filterCats(ret.toArray(new Letter[] {}), jac);
    }

    private static Letter[] filterCats(Letter[] letters, JAntiCaptcha jac) {
        // if (true) return letters;

        String methodsPath = UTILITIES.getFullPath(new String[] { JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath(), "jd", "captcha", "methods" });
        String hoster = "rscat.com";

        JAntiCaptcha vjac = new JAntiCaptcha(methodsPath, hoster);
        
//        vjac.setShowDebugGui(true);
//        LetterComperator.CREATEINTERSECTIONLETTER = true;
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
        int id=0;
        for (Letter l : letters) {
            Letter d = new Letter();

            d.setOwner(l.owner);
            d.setGridCopy(l.grid, 0, 0, 0, 0);
            lets.add(d);
            d.setProperty("org", l);
            l.id=id++;
            mtd.queueDetection(d);
            // Letter dif = l.detected.getDifference();
            // BasicWindow.showImage(dif.getImage(4), "letter " + count++);
            // if (isCat(dif, map, catList, letters.length)) {
            // l.colorize(0xff0000);
            // catList.add(CAT);
            // } else {
            // catList.add(DOG);
            // }

        }
        mtd.waitFor(null);

        Collections.sort(lets, new Comparator<Letter>() {
            public int compare(Letter a, Letter b) {
                if (a.detected.getValityPercent() > b.detected.getValityPercent()) return 1;
                if (a.detected.getValityPercent() < b.detected.getValityPercent()) return -1;
                return 0;
            }

        });

        while (true) {
            ArrayList<Byte> catlist = new ArrayList<Byte>();
            for (int letNum = 0; letNum < letters.length; letNum++) {
                catlist.add((byte)0);
            }
            for (int i = 0; i < Math.min(4, lets.size()); i++) {
                Letter l = ((Letter) lets.get(i).getProperty("org"));
                for (int letNum = 0; letNum < letters.length; letNum++) {
                    if (letters[letNum] == l) {
                        catlist.set(letNum, (byte)1);
                        break;
                    }
                }
                // ((Letter)lets.get(i).getProperty("org")).colorize(0xff0000);
            }
            if(isInMap(map,catlist))break;
            lets.remove(3);
            if(lets.size()<4)break;
        }
        ArrayList<Letter> filtered = new    ArrayList<Letter>();
        for (int i = 0; i < Math.min(4, lets.size()); i++) {
            Letter l = ((Letter) lets.get(i).getProperty("org"));
           l.colorize(0xff0000);
           filtered.add(l);
           
            // ((Letter)lets.get(i).getProperty("org")).colorize(0xff0000);
        }
        Collections.sort(filtered,new Comparator<Letter>() {
            public int compare(Letter a, Letter b) {
                if (a.id>b.id)return 1;
                if (a.id<b.id)return -1;
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
        return filtered.toArray(new Letter[]{});
    }

    private static boolean isCat(Letter l, Vector<byte[]> map, ArrayList<Byte> catList, int count) {

        // byte[] pos = isPossible(map, catList, count);
        // if (pos[0] == 0 && pos[1] == 0) {
        // BasicWindow.showImage(l.getImage(2), "letter " + catList + " - false
        // ");
        // return false;
        // }
        // if (pos[0] == 0) {
        // BasicWindow.showImage(l.getImage(2), "letter " + catList + " - true
        // ");
        // return true;
        // }
        // if (pos[1] == 0) {
        // BasicWindow.showImage(l.getImage(2), "letter " + catList + " - false
        // ");
        // return false;
        // }
        String methodsPath = UTILITIES.getFullPath(new String[] { JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath(), "jd", "captcha", "methods" });
        String hoster = "rscat.com";

        JAntiCaptcha jac = new JAntiCaptcha(methodsPath, hoster);
        jac.setShowDebugGui(true);
        LetterComperator.CREATEINTERSECTIONLETTER = true;

        LetterComperator c = jac.getLetter(l);
        l.detected = c;
        boolean ret = c.getDecodedValue().equalsIgnoreCase("k");
        // BasicWindow.showImage(l.getImage(2), "letter " + catList + " - " +
        // ret + " - " + c.getValityPercent());
        return ret;

    }

    private static Letter[] fill(Letter[] org, JAntiCaptcha jac) {
        if (true) return org;
        for (Letter l : org) {

            fillLetter(l, jac);

        }
        return org;
    }

    private static void fillLetter(Letter l, JAntiCaptcha jac) {

        int limit = 200;
        int[][] tmp = new int[l.getWidth()][l.getHeight()];

        for (int x = 0; x < l.getWidth(); x++) {
            for (int y = 0; y < l.getHeight(); y++) {
                if (l.grid[x][y] > limit && tmp[x][y] != 1) {
                    PixelObject p = new PixelObject(l);
                    recFill(p, l, x, y, tmp, 0);
                    if (p.isBordered() && p.getSize() < 10) {
                        l.fillWithObject(p, 0);
                    }
                    // BasicWindow.showImage(l.getImage(2), x+" - "+y);

                }
            }
        }

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