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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Vector;

import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.pixelobject.PixelObject;

/**
 * das ist krank bitte nicht anschauen
 * 
 * @author dwd
 * 
 */
public class Dlm {
    /**
     * prüft ob das objekt an der stelle x y höher ist als 14px
     * 
     * @param captcha
     * @param x
     * @param y
     * @return
     */
    private static boolean checkAt(final Captcha captcha, final int x, final int y) {
        final int yMax = Math.min(captcha.getHeight(), y + 7);
        final int yMin = Math.max(0, y - 7);
        for (int i = yMin; i < yMax; i++) {
            if (captcha.getPixelValue(x, i) == 0xffffff) { return true; }
        }
        return false;
    }

    /**
     * löscht alle objekte die höher sind als 14px
     * 
     * @param captcha
     */
    private static void clear(final Captcha captcha) {
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.getPixelValue(x, y) != 0xffffff && !checkAt(captcha, x, y)) {
                    clearAt(captcha, x, y);
                }
            }

        }
    }

    /**
     * löscht ein objekt an der position x y
     * 
     * @param captcha
     * @param x
     * @param y
     */
    private static void clearAt(final Captcha captcha, final int x, final int y) {
        if (captcha.getHeight() >= y && captcha.getWidth() >= x && x > 0 && y > 0 && captcha.getPixelValue(x, y) != 0xffffff) {
            captcha.grid[x][y] = 0xffffff;
            clearAt(captcha, x + 1, y + 1);
            clearAt(captcha, x, y + 1);
            clearAt(captcha, x + 1, y);
            clearAt(captcha, x - 1, y - 1);
            clearAt(captcha, x - 1, y);
            clearAt(captcha, x, y - 1);
            clearAt(captcha, x + 1, y - 1);
            clearAt(captcha, x - 1, y + 1);
        }
    }

    /**
     * schneidet ein 30px breites und 25px hohes objekt an der position int[]
     * {x,y} aus
     * 
     * @param captcha
     * @param header1
     * @return
     */
    private static Letter createLetter(final Captcha captcha, final int[] header1) {
        captcha.crop(Math.max(0, header1[0] - 15), header1[1], Math.max(0, captcha.getWidth() - header1[0] - 15), Math.max(0, captcha.getHeight() - header1[1] - 25));
        final Letter l = captcha.createLetter();
        captcha.toBlackAndWhite();
        l.setLocation(header1);
        l.setGrid(captcha.getGrid());
        captcha.reset();
        return l;
    }

    /**
     * gibt die position aus wo im oberen Teil des Bildes ein objekt anfängt
     * 
     * @param captcha
     * @param xMin
     * @param xMax
     * @return
     */
    private static int[] getHeader(final Captcha captcha, final int xMin, final int xMax) {
        for (int y = 0; y < captcha.getHeight() / 8; y++) {
            for (int x = xMin; x < xMax; x++) {
                if (captcha.getPixelValue(x, y) != 0xffffff) { return new int[] { x, y }; }
            }
        }
        return null;

    }

    public static Letter[] getLetters(final Captcha captcha) throws Exception {
        captcha.owner.loadMTHFile(captcha.owner.getResourceFile("lettershead.mth"));
        // es wird der Buchstabe M bzw F von unten ausgeschnitten
        captcha.crop(206, 150, 236, 0);
        final File captchaFile = new File("C:\\Users\\sonar_w7\\.jd_home\\captchas\\3dltv_namen\\name_" + System.currentTimeMillis() + ".jpg");
        Vector<PixelObject> obj = captcha.getObjects(0.7, 0.7);
        // es gibt nur ein objekt M oder F
        Letter let = obj.get(0).toLetter();
        // invertiere macht den vergleich schneller
        let.invert();
        let.normalize();
        let = let.toPixelObject(0.7).toLetter();
        final LetterComperator r = captcha.owner.getLetter(let);
        let.detected = r;
        let.setDecodedValue(r.getDecodedValue());

        captcha.reset();

        final int xMax = captcha.getWidth() / 3;
        // holt die drei Köpfe
        final Letter head1 = createLetter(captcha, getHeader(captcha, 0, xMax));
        final Letter head2 = createLetter(captcha, getHeader(captcha, xMax, xMax * 2));
        final Letter head3 = createLetter(captcha, getHeader(captcha, xMax * 2, xMax * 3));
        int pos = 0;

        final LetterComperator rc1 = captcha.owner.getLetter(head1);
        head1.detected = rc1;
        head1.setDecodedValue(rc1.getDecodedValue());
        // schaut welcher der 3 köpfe Mann bzw Frau ist
        if (head1.getDecodedValue().equals(let.getDecodedValue())) {
            pos = 0;
        } else {
            final LetterComperator rc2 = captcha.owner.getLetter(head2);
            head2.detected = rc2;
            head2.setDecodedValue(rc2.getDecodedValue());

            if (head2.getDecodedValue().equals(let.getDecodedValue())) {
                pos = 1;
            } else {
                final LetterComperator rc3 = captcha.owner.getLetter(head3);
                head3.detected = rc3;
                head3.setDecodedValue(rc3.getDecodedValue());

                if (head3.getDecodedValue().equals(let.getDecodedValue())) {
                    pos = 2;
                } else {// wenn keines der 3 letters zutrifft es ist bestimmt
                        // das
                    // welches am schlechtesten erkennt wurde
                    if (head1.detected.getValityPercent() > head2.detected.getValityPercent()) {
                        if (head1.detected.getValityPercent() > head3.detected.getValityPercent()) {
                            pos = 0;
                        } else {
                            pos = 2;
                        }
                    } else {
                        if (head2.detected.getValityPercent() > head3.detected.getValityPercent()) {
                            pos = 1;
                        } else {
                            pos = 2;
                        }
                    }
                }

            }
        }
        // lösche die untere zeile
        captcha.crop(0, 0, 0, 20);
        // entferne alle männchen
        clear(captcha);
        // captcha.removeSmallObjects(0.7, 0.7, 6);
        captcha.toBlackAndWhite(0.8);
        // BasicWindow.showImage(captcha.getImage());
        // holt die 3 buchstabenpackete
        obj = getObjects(captcha, 3);
        Collections.sort(obj);
        if (obj.size() > 3) {
            for (final Iterator<PixelObject> iterator = obj.iterator(); iterator.hasNext();) {
                final PixelObject pixelObject = iterator.next();
                if (pixelObject.getArea() < 80) {
                    iterator.remove();
                }
            }
        }
        captcha.owner.loadMTHFile();
        captcha.owner.getJas().set("minimumObjectArea", 1);
        captcha.owner.getJas().set("minimumLetterWidth", 1);
        captcha.grid = obj.get(pos).toLetter().grid;
        obj = captcha.getObjects(0.7, 0.7);
        int merge = 0;
        for (final PixelObject pixelObject : obj) {
            if (pixelObject.getArea() < 4) {
                merge++;
            }
        }
        // die objekte die kleiner sind als 4 pixel können gemerged werden
        captcha.owner.setLetterNum(obj.size() - merge);
        EasyCaptcha.mergeObjectsBasic(obj, captcha, 2);

        Collections.sort(obj);
        final ArrayList<Letter> ret = new ArrayList<Letter>();
        final PixelObject last = obj.get(0);
        for (final ListIterator<PixelObject> iterator = obj.listIterator(); iterator.hasNext();) {
            final PixelObject pixelObject = iterator.next();
            pixelObject.toLetter().saveImageasJpg(captchaFile);
            // if ((pixelObject.getArea() > 4 || pixelObject.getHeight() > 5) &&
            // pixelObject.getArea() < 250 && (Math.abs(last.getYMin() -
            // pixelObject.getYMin()) < 3 || iterator.hasNext() &
            // Math.abs(iterator.next().getYMin() - pixelObject.getYMin()) < 3 &
            // iterator.previous() == pixelObject)) {
            // // Letter let1 = pixelObject.toLetter();
            // // let1.toBlackAndWhite();
            // // LetterComperator r2 = captcha.owner.getLetter(let1);
            // // let1.detected = r2;
            // ret.addAll(getSplitted(pixelObject, captcha, 0));
            // // last=pixelObject;
            // }
            ret.addAll(getSplittedA(pixelObject, captcha, 0));
        }
        // for (final Letter l : ret) {
        // captchaFile = new
        // File("C:\\Users\\sonar_w7\\.jd_home\\captchas\\3dltv_namen\\name_" +
        // System.currentTimeMillis() + ".jpg");
        // l.saveImageasJpg(captchaFile);
        // }
        replaceLetters(ret);
        return ret.toArray(new Letter[] {});
    }

    /**
     * hiermit lassen sich große objekte schnell heraus suchen
     * 
     * @param grid
     * @param neighbourradius
     * @return
     */
    static Vector<PixelObject> getObjects(final PixelGrid grid, final int neighbourradius) {
        final Vector<PixelObject> ret = new Vector<PixelObject>();
        Vector<PixelObject> merge;
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                if (grid.getGrid()[x][y] < 0 || grid.getGrid()[x][y] != 0x000000) {
                    continue;
                }

                final PixelObject n = new PixelObject(grid);
                n.add(x, y, grid.getGrid()[x][y]);

                merge = new Vector<PixelObject>();
                for (final PixelObject o : ret) {
                    if (o.isTouching(x, y, true, neighbourradius, neighbourradius)) {
                        merge.add(o);
                    }
                }
                if (merge.size() == 0) {
                    ret.add(n);
                } else if (merge.size() == 1) {
                    merge.get(0).add(n);
                } else {
                    for (final PixelObject po : merge) {
                        ret.remove(po);
                        n.add(po);
                    }
                    ret.add(n);
                }

            }
        }

        return ret;
    }

    /**
     * teilt recht zuverlässig ist aber auch ressourcen lastig sollte nur bei
     * kleinen Letters bentzt werden
     * 
     * @param pixelObject
     * @param captcha
     * @param index
     * @return
     */
    private static ArrayList<Letter> getSplitted(final PixelObject pixelObject, final Captcha captcha, int index) {
        final ArrayList<Letter> ret = new ArrayList<Letter>();
        if (pixelObject.getArea() < 4) { return ret; }
        final Letter let1 = pixelObject.toLetter();
        let1.toBlackAndWhite();
        LetterComperator r2 = captcha.owner.getLetter(let1);
        let1.detected = r2;
        if (r2.getValityPercent() < 3 || index == 2 || pixelObject.getArea() < 15) {
            ret.add(let1);
            return ret;
        } else if (r2.getValityPercent() < 10) {
            final PixelObject[] bestAArray = pixelObject.splitAt(r2.getB().getWidth());
            if (bestAArray[0].getArea() > 4) {
                final Letter bestA = bestAArray[0].toLetter();
                bestA.toBlackAndWhite();
                r2 = captcha.owner.getLetter(bestA);
                bestA.detected = r2;
                ret.add(bestA);
            }
            if (bestAArray[1].getArea() > 4) {
                final Letter bestB = bestAArray[1].toLetter();
                bestB.toBlackAndWhite();
                final LetterComperator r3 = captcha.owner.getLetter(bestB);
                bestB.detected = r3;
                ret.add(bestB);
            }
            return ret;
        }
        index++;
        int b = pixelObject.getWidth() / 2;
        PixelObject[] bestAArray = pixelObject.splitAt(b);
        Letter bestA = bestAArray[0].toLetter();
        bestA.toBlackAndWhite();
        r2 = captcha.owner.getLetter(bestA);
        bestA.detected = r2;
        if (r2.getValityPercent() < 1) {
            ret.add(bestA);
            ret.addAll(getSplitted(bestAArray[1], captcha, index));
            return ret;
        }
        PixelObject[] bestBArray = pixelObject.splitAt(b);
        Letter bestB = bestAArray[0].toLetter();
        for (int a = b + 1; a < pixelObject.getWidth() / 3 * 2; a++) {
            b--;
            final PixelObject[] aArray = pixelObject.splitAt(a);
            final Letter letA = aArray[0].toLetter();
            letA.toBlackAndWhite();
            final LetterComperator ra = captcha.owner.getLetter(letA);
            letA.detected = ra;
            if (ra != null && (bestA.detected == null || ra.getValityPercent() < bestA.detected.getValityPercent())) {
                bestA = letA;
                bestAArray = aArray;
                if (ra.getValityPercent() < 1) {
                    break;
                }
            }
            final PixelObject[] bArray = pixelObject.splitAt(b);
            final Letter letB = bArray[0].toLetter();
            letB.toBlackAndWhite();
            final LetterComperator rb = captcha.owner.getLetter(letB);
            letB.detected = rb;
            if (rb != null && (bestB.detected == null || rb.getValityPercent() < bestB.detected.getValityPercent())) {
                bestB = letB;
                bestBArray = bArray;
                if (rb.getValityPercent() < 1) {
                    break;
                }
            }
        }
        if (let1.detected != null && (bestA.detected == null || let1.detected.getValityPercent() < bestA.detected.getValityPercent() || bestB.detected == null || let1.detected.getValityPercent() < bestB.detected.getValityPercent())) {
            if (index == 1) {
                b = pixelObject.getWidth() / 2;
                bestAArray = pixelObject.splitAt(b);
                bestA = bestAArray[0].toLetter();
                bestA.toBlackAndWhite();
                r2 = captcha.owner.getLetter(bestA);
                bestA.detected = r2;
                ret.add(bestA);
                bestB = bestAArray[1].toLetter();
                bestB.toBlackAndWhite();
                r2 = captcha.owner.getLetter(bestB);
                bestB.detected = r2;
                ret.add(bestB);
                return ret;
            } else {
                ret.add(let1);
                return ret;
            }
        } else if (bestA.detected != null && bestB.detected == null || bestA.detected != null && bestA.detected.getValityPercent() < bestB.detected.getValityPercent()) {
            ret.add(bestB);
            ret.addAll(getSplitted(bestAArray[1], captcha, index));
            return ret;
        } else if (bestB.detected != null) {
            ret.add(bestB);
            ret.addAll(getSplitted(bestBArray[1], captcha, index));
            return ret;
        } else {
            if (index == 1) {
                b = pixelObject.getWidth() / 2;
                bestAArray = pixelObject.splitAt(b);
                bestA = bestAArray[0].toLetter();
                bestA.toBlackAndWhite();
                r2 = captcha.owner.getLetter(bestA);
                bestA.detected = r2;
                ret.add(bestA);
                bestB = bestAArray[1].toLetter();
                bestB.toBlackAndWhite();
                r2 = captcha.owner.getLetter(bestB);
                bestB.detected = r2;
                ret.add(bestB);
                return ret;
            } else {
                ret.add(let1);
                return ret;
            }

        }
    }

    private static ArrayList<Letter> getSplittedA(PixelObject pixelObject, final Captcha captcha, int index) {
        final ArrayList<Letter> ret = new ArrayList<Letter>();
        if (pixelObject.getArea() < 4) { return ret; }
        // wenn pixelobject aus nur einem Character besteht
        final Letter let1 = pixelObject.toLetter();
        let1.toBlackAndWhite();
        final LetterComperator r1 = captcha.owner.getLetter(let1);
        let1.detected = r1;
        final double a = r1.getValityPercent();
        final int b = pixelObject.getArea();
        if (r1.getValityPercent() < 3 || pixelObject.getArea() < 15) {
            ret.add(let1);
            return ret;
        }
        // hier werden komplette Worte gesplittet
        final ArrayList<Integer> splitMap = new ArrayList<Integer>();
        final Hashtable<String, Vector<Integer>> testMap = new Hashtable<String, Vector<Integer>>();
        final Vector<Integer> one = new Vector<Integer>();
        final Vector<Integer> two = new Vector<Integer>();
        final Vector<Integer> three = new Vector<Integer>();
        int eins = 0, zwei = 0, drei = 0;
        for (int x = pixelObject.getWidth() - 1; x > 0; x--) {
            int black = 0;
            for (int y = 0; y < pixelObject.getHeight(); y++) {
                if (pixelObject.getGrid()[x][y] < 0 || pixelObject.getGrid()[x][y] != 0x000000) {
                    continue;
                }
                black++;
            }
            if (black == index) {
                if (splitMap != null && splitMap.size() > 0) {
                    final int p = splitMap.get(splitMap.size() - 1);
                    if (p == x + 1 || p == x + 2 || x == 0) {
                        continue;
                    }
                }
                splitMap.add(x);
            }
            if (black == 1) {
                eins++;
                one.add(x);
            } else if (black == 2) {
                zwei++;
                two.add(x);
            } else if (black == 3) {
                drei++;
                three.add(x);
            }
            // System.out.println("X:" + x + " black: " + black);
            testMap.put("eins", one);
            testMap.put("zwei", two);
            testMap.put("drei", three);
        }
        Collections.reverse(splitMap);
        // wenn splits vorhanden sind aber einige chars zusammenkleben
        final int CharAnzahl = (pixelObject.getWidth() - pixelObject.getWidth() % 5) / 5;
        if (CharAnzahl == splitMap.size() + 2) {
            // mögliche Position suchen
            int sPos = 0, xPos = 0, maxPos = 0;
            for (final ListIterator<Integer> i = splitMap.listIterator(); i.hasNext();) {
                if (i.hasPrevious()) {
                    xPos = i.previous();
                    i.next();
                    sPos = i.next() - xPos;
                } else {
                    i.next();
                }
                maxPos = sPos > maxPos ? sPos : maxPos;
            }
            if (maxPos > 0) {
                System.out.println("geschätzte Splitposition : " + maxPos);
                splitMap.add(maxPos);
                Collections.sort(splitMap);
            }
            if (CharAnzahl == splitMap.size() + 2) {
                for (final int c : testMap.get("eins")) {
                    for (final int let : new ArrayList<Integer>(splitMap)) {
                        if (c < let) {
                            continue;
                        }
                        System.out.println("geschätzte Splitposition : " + c);
                        splitMap.add(c);
                        Collections.sort(splitMap);
                    }
                }
            }
        }

        int xm = 0;
        PixelObject[] bArray = null;
        LetterComperator r2;
        Letter splittedLetter;
        // wenn kein weissraum vorhanden, splitte bei 1,2 oder 3 Pixel
        if (splitMap == null || splitMap.size() == 0) {
            if (eins == 0 && zwei == 0 && drei == 0) { return ret; }
            index = eins == 0 && zwei >= drei ? 3 : 2;
            index = eins > 0 ? 1 : index;
            if (index == 0) { return ret; }
            return getSplittedA(pixelObject, captcha, index);
        }
        for (final int let : splitMap) {
            bArray = pixelObject.splitAt(let - xm);
            pixelObject = bArray[1];
            splittedLetter = bArray[0].toLetter();
            splittedLetter.toBlackAndWhite();
            r2 = captcha.owner.getLetter(splittedLetter);
            splittedLetter.detected = r2;
            ret.add(splittedLetter);
            xm = let + 1;
        }
        splittedLetter = bArray[1].toLetter();
        splittedLetter.toBlackAndWhite();
        r2 = captcha.owner.getLetter(splittedLetter);
        splittedLetter.detected = r2;
        ret.add(splittedLetter);
        return ret;
    }

    public static Letter[] letterFilter(final Letter[] org, final JAntiCaptcha jac) {
        return org;
    }

    /**
     * es wird z.B. letter 1 mit letter r erstetzt und ein letter i hinzugefügt
     * 
     * @param lets
     */
    private static void replaceLetters(final ArrayList<Letter> lets) {
        int i = 0;
        String add = null;
        for (final Letter letter : lets) {
            if (letter.detected.getDecodedValue().equals("1")) {
                letter.detected.getB().setDecodedValue("r");
                letter.setDecodedValue("r");
                add = "i";
                break;
            } else if (letter.detected.getDecodedValue().equals("2")) {
                letter.detected.getB().setDecodedValue("r");
                letter.setDecodedValue("r");
                add = "l";
                break;

            } else if (letter.detected.getDecodedValue().equals("3")) {
                letter.detected.getB().setDecodedValue("r");
                letter.setDecodedValue("r");
                add = "t";
                break;

            } else if (letter.detected.getDecodedValue().equals("4")) {
                letter.detected.getB().setDecodedValue("r");
                letter.setDecodedValue("r");
                add = "n";
                break;

            }

            i++;
        }
        if (add != null) {
            final Letter re = new Letter();
            re.setDecodedValue(add);
            final LetterComperator let1 = new LetterComperator(re, re);
            let1.setValityPercent(0);
            re.detected = let1;
            lets.add(i + 1, re);
            replaceLetters(lets);
        }
    }

}
