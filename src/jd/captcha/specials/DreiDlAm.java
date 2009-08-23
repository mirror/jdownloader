package jd.captcha.specials;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Vector;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.JAntiCaptcha;
import jd.captcha.utils.Utilities;
import jd.captcha.LetterComperator;
import jd.captcha.pixelobject.PixelObject;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
/**
 * das ist krank bitte nicht anschauen
 * @author dwd
 *
 */
public class DreiDlAm {
    private static void clearAt(Captcha captcha, int x, int y) {
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

    private static boolean checkAt(Captcha captcha, int x, int y) {
        int yMax = Math.min(captcha.getHeight(), y + 7);
        int yMin = Math.max(0, y - 7);
        for (int i = yMin; i < yMax; i++) {
            if (captcha.getPixelValue(x, i) == 0xffffff) return true;
        }
        return false;
    }

    private static void clear(Captcha captcha) {
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {

                if (captcha.getPixelValue(x, y) != 0xffffff && !checkAt(captcha, x, y)) clearAt(captcha, x, y);

            }

        }
    }

    private static int[] getHeader(Captcha captcha, int xMin, int xMax) {
        for (int y = 0; y < captcha.getHeight() / 8; y++) {
            for (int x = xMin; x < xMax; x++) {

                if (captcha.getPixelValue(x, y) != 0xffffff) return new int[] { x, y };
            }
        }
        return null;

    }

    private static Letter createLetter(Captcha captcha, int[] header1) {
        captcha.crop(Math.max(0, header1[0] - 15), header1[1], Math.max(0,captcha.getWidth() - header1[0] - 15), Math.max(0,captcha.getHeight() - header1[1] - 25));
        Letter l = captcha.createLetter();
        captcha.toBlackAndWhite();
        l.setLocation(header1);
        l.setGrid(captcha.getGrid());
        captcha.reset();
        return l;
    }

    public static Letter[] getLetters(Captcha captcha) throws Exception {

        captcha.crop(206, 150, 233, 0);
        // captcha.invert();
        Vector<PixelObject> obj = captcha.getObjects(0.7, 0.7);

        Letter let = obj.get(0).toLetter();
        let.invert();
        let = let.toPixelObject(0.7).toLetter();
        LetterComperator r = captcha.owner.getLetter(let);
        let.detected = r;
        let.setDecodedValue(r.getDecodedValue());

        captcha.reset();

        int xMax = captcha.getWidth() / 3;
        Letter head1 = createLetter(captcha, getHeader(captcha, 0, xMax));
        Letter head2 = createLetter(captcha, getHeader(captcha, xMax, xMax * 2));
        Letter head3 = createLetter(captcha, getHeader(captcha, xMax * 2, xMax * 3));
        int pos = 0;
        String hoster = "3dl.amHeads";
        JAntiCaptcha jac = new JAntiCaptcha(Utilities.getMethodDir(), hoster);
        LetterComperator rc1 = jac.getLetter(head1);
        head1.detected = rc1;
        head1.setDecodedValue(rc1.getDecodedValue());
        if (head1.getDecodedValue().equals(let.getDecodedValue()))
            pos = 0;
        else {
            LetterComperator rc2 = jac.getLetter(head2);
            head2.detected = rc2;
            head2.setDecodedValue(rc2.getDecodedValue());

            if (head2.getDecodedValue().equals(let.getDecodedValue()))
                pos = 1;
            else {
                LetterComperator rc3 = jac.getLetter(head3);
                head3.detected = rc3;
                head3.setDecodedValue(rc3.getDecodedValue());

                if (head3.getDecodedValue().equals(let.getDecodedValue()))
                    pos = 2;
                else {
                    if (head1.detected.getValityPercent() > head2.detected.getValityPercent()) {
                        if (head1.detected.getValityPercent() > head3.detected.getValityPercent())
                            pos = 0;
                        else
                            pos = 2;
                    } else {
                        if (head2.detected.getValityPercent() > head3.detected.getValityPercent())
                            pos = 1;
                        else
                            pos = 2;
                    }
                }

            }
        }
        captcha.crop(0, 0, 0, 20);
        clear(captcha);
        // captcha.removeSmallObjects(0.7, 0.7, 6);
        // BasicWindow.showImage(captcha.getImage());
        captcha.toBlackAndWhite(0.8);
        obj = getObjects(captcha, 3);
        Collections.sort(obj);
        if (obj.size() > 3) for (Iterator<PixelObject> iterator = obj.iterator(); iterator.hasNext();) {
            PixelObject pixelObject = (PixelObject) iterator.next();
            if (pixelObject.getArea() < 80) iterator.remove();
        }
        captcha.owner.getJas().set("minimumObjectArea", 1);
        captcha.owner.getJas().set("minimumLetterWidth", 1);
        captcha.grid = obj.get(pos).toLetter().grid;
        obj = captcha.getObjects(0.7, 0.7);
        int merge = 0;
        for (PixelObject pixelObject : obj) {
            if (pixelObject.getArea() < 4) merge++;
        }
        captcha.owner.setLetterNum(obj.size() - merge);
        EasyCaptcha.mergeObjectsBasic(obj, captcha, 2);

        Collections.sort(obj);
        ArrayList<Letter> ret = new ArrayList<Letter>();
        PixelObject last = obj.get(0);
        for (ListIterator<PixelObject> iterator = obj.listIterator(); iterator.hasNext();) {
            PixelObject pixelObject = (PixelObject) iterator.next();
            if ((pixelObject.getArea() > 4 || pixelObject.getHeight() > 5) && (pixelObject.getArea() < 250 && (Math.abs(last.getYMin() - pixelObject.getYMin()) < 3 || (iterator.hasNext() & Math.abs(iterator.next().getYMin() - pixelObject.getYMin()) < 3 & iterator.previous() == pixelObject)))) {
                // Letter let1 = pixelObject.toLetter();
                // let1.toBlackAndWhite();
                // LetterComperator r2 = captcha.owner.getLetter(let1);
                // let1.detected = r2;
                ret.addAll(getSplitted(pixelObject, captcha, 0));
                // last=pixelObject;
            }
        }
        replaceLetters(ret);
        return ret.toArray(new Letter[] {});
    }

    private static void replaceLetters(ArrayList<Letter> lets) {
        int i = 0;
        String add = null;
        for (Letter letter : lets) {
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
            Letter re = new Letter();
            re.setDecodedValue(add);
            LetterComperator let1 = new LetterComperator(re, re);
            let1.setValityPercent(0);
            re.detected = let1;
            lets.add(i + 1, re);
            replaceLetters(lets);
        }
    }

    static Vector<PixelObject> getObjects(PixelGrid grid, int neighbourradius) {
        Vector<PixelObject> ret = new Vector<PixelObject>();
        Vector<PixelObject> merge;
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                if (grid.getGrid()[x][y] < 0 || grid.getGrid()[x][y] != 0x000000) continue;

                PixelObject n = new PixelObject(grid);
                n.add(x, y, grid.getGrid()[x][y]);

                merge = new Vector<PixelObject>();
                for (Iterator<PixelObject> iterator = ret.iterator(); iterator.hasNext();) {
                    PixelObject o = (PixelObject) iterator.next();
                    if (o.isTouching(x, y, true, neighbourradius, neighbourradius)) {
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

    /**
     * teilt recht zuverlässig ist aber auch ressourcen lastig sollte nur bei
     * kleinen Letters bentzt werden
     * 
     * @param pixelObject
     * @param captcha
     * @param index
     * @return
     */
    private static ArrayList<Letter> getSplitted(PixelObject pixelObject, Captcha captcha, int index) {
        ArrayList<Letter> ret = new ArrayList<Letter>();
        if (pixelObject.getArea() < 4) return ret;
        Letter let1 = pixelObject.toLetter();
        let1.toBlackAndWhite();
        LetterComperator r2 = captcha.owner.getLetter(let1);
        let1.detected = r2;
        if (r2.getValityPercent() < 3 || index == 2 || pixelObject.getArea() < 15) {
            ret.add(let1);
            return ret;
        } else if (r2.getValityPercent() < 10) {
            PixelObject[] bestAArray = pixelObject.splitAt(r2.getB().getWidth());
            if (bestAArray[0].getArea() > 4) {
                Letter bestA = bestAArray[0].toLetter();
                bestA.toBlackAndWhite();
                r2 = captcha.owner.getLetter(bestA);
                bestA.detected = r2;
                ret.add(bestA);
            }
            if (bestAArray[1].getArea() > 4) {
                Letter bestB = bestAArray[1].toLetter();
                bestB.toBlackAndWhite();
                LetterComperator r3 = captcha.owner.getLetter(bestB);
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
            PixelObject[] aArray = pixelObject.splitAt(a);
            Letter letA = aArray[0].toLetter();
            letA.toBlackAndWhite();
            LetterComperator ra = captcha.owner.getLetter(letA);
            letA.detected = ra;
            if (ra != null && (bestA.detected == null || ra.getValityPercent() < bestA.detected.getValityPercent())) {
                bestA = letA;
                bestAArray = aArray;
                if (ra.getValityPercent() < 1) break;
            }
            PixelObject[] bArray = pixelObject.splitAt(b);
            Letter letB = bArray[0].toLetter();
            letB.toBlackAndWhite();
            LetterComperator rb = captcha.owner.getLetter(letB);
            letB.detected = rb;
            if (rb != null && (bestB.detected == null || rb.getValityPercent() < bestB.detected.getValityPercent())) {
                bestB = letB;
                bestBArray = bArray;
                if (rb.getValityPercent() < 1) break;
            }
        }
        if (let1.detected != null && ((bestA.detected == null || let1.detected.getValityPercent() < bestA.detected.getValityPercent()) || (bestB.detected == null || let1.detected.getValityPercent() < bestB.detected.getValityPercent()))) {
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
        } else if (bestA.detected != null && bestB.detected == null || (bestA.detected != null && bestA.detected.getValityPercent() < bestB.detected.getValityPercent())) {
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

    public static Letter[] letterFilter(Letter[] org, JAntiCaptcha jac) {
        return org;
    }
}
