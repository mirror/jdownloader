package jd.captcha.specials;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import jd.captcha.gui.BasicWindow;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.pixelobject.PixelObject;
import jd.nutils.Colors;

public class ShrLnksBz {
    private final static int[] bgColors = new int[] { 0x746a5c, 0x847e5c, 0x847a64, 0x6c6244, 0x6c6654, 0x84765c, 0x8c7e58, 0x8c8274, 0x8c8264, 0x645e4c, 0x6c6249, 0x645e44, 0x7c725e, 0x746e54, 0x94825c, 0x7c6e50, 0x74664c, 0x94866c, 0x8c7e64 };

    private static boolean check(PixelGrid captcha, int x, int y) {
        if (x >= 0 && y >= 0 && captcha.getWidth() > x && captcha.getHeight() > y) {
            if (captcha.grid[x][y] == 0xffffff || Colors.getRGBColorDifference3(0x645e44, captcha.grid[x][y]) > 10) {
                captcha.grid[x][y] = 0xffffff;
                return false;
            } else
                return true;
        }
        return false;
    }

    private static int quad(PixelGrid captcha, int x, int y, int distance) {
        int i = 0;
        // if (x >= 0 && y >= 0 && captcha.getWidth() > x && captcha.getHeight()
        // > y && captcha.grid[x][y] >= 0 && captcha.grid[x][y] != 0xffffff) {
        for (int x1 = -distance; x1 <= distance; x1++) {
            for (int y1 = -distance; y1 <= distance; y1++) {
                if (check(captcha, x + x1, y + y1)) i++;
            }
        }

        // }
        return i;
    }

    /**
     * overwrite the colored dots in digits with black dots die bunten punkte in
     * den Zahlen werden mit schwarzen punkten ersetzt
     * 
     * @param captcha
     */
    private static void setDotsInDigits(Captcha captcha, int[][] grid2) {
        int dist = 4;
        int[][] grid = PixelGrid.getGridCopy(captcha.grid);
        for (int x = 1; x < captcha.getWidth() - 1; x++) {
            for (int y = 1; y < captcha.getHeight() - 1; y++) {
                if (captcha.grid[x][y] == 0xffffff && Colors.getRGBColorDifference3(0x8c7e64, grid2[x][y]) > dist && Colors.getRGBColorDifference3(0x645e44, grid2[x][y]) > 10) {
                    int w = (captcha.grid[x + 1][y]) != 0xffffff ? 6 : Colors.getRGBColorDifference3(0x8c7e64, grid2[x + 1][y]) > dist ? 1 : 0;
                    w += (captcha.grid[x + 1][y + 1]) != 0xffffff ? 6 : Colors.getRGBColorDifference3(0x8c7e64, grid2[x + 1][y + 1]) > dist ? 1 : 0;
                    w += (captcha.grid[x][y + 1]) != 0xffffff ? 6 : Colors.getRGBColorDifference3(0x8c7e64, grid2[x][y + 1]) > dist ? 1 : 0;
                    w += (captcha.grid[x - 1][y + 1]) != 0xffffff ? 6 : Colors.getRGBColorDifference3(0x8c7e64, grid2[x - 1][y + 1]) > dist ? 1 : 0;
                    w += (captcha.grid[x - 1][y - 1]) != 0xffffff ? 6 : Colors.getRGBColorDifference3(0x8c7e64, grid2[x - 1][y - 1]) > dist ? 1 : 0;
                    w += (captcha.grid[x + 1][y - 1]) != 0xffffff ? 6 : Colors.getRGBColorDifference3(0x8c7e64, grid2[x + 1][y - 1]) > dist ? 1 : 0;
                    w += (captcha.grid[x - 1][y]) != 0xffffff ? 6 : Colors.getRGBColorDifference3(0x8c7e64, grid2[x - 1][y]) > dist ? 1 : 0;
                    w += (captcha.grid[x][y - 1]) != 0xffffff ? 6 : Colors.getRGBColorDifference3(0x8c7e64, grid2[x][y - 1]) > dist ? 1 : 0;
                    if (w > 20) grid[x][y] = 0x645e4c;

                }
            }
        }
        captcha.grid = grid;
    }

    private static int follow2(PixelGrid captcha, int x, int y, int color, int lastColor, int distanceX, int distanceY, int distx2, int disty2, boolean lcSensitive) {
        int i = 0;
        if (x >= 0 && y >= 0 && captcha.getWidth() > x && captcha.getHeight() > y && captcha.grid[x][y] >= 0 && captcha.grid[x][y] != 0xffffff && (!lcSensitive || captcha.grid[x][y] == lastColor)) {
            int lc = captcha.grid[x][y];
            captcha.grid[x][y] = color;
            i++;
            for (int x1 = -distx2; x1 <= distx2; x1++) {
                for (int y1 = -disty2; y1 <= disty2; y1++) {
                    i += follow2(captcha, x + x1, y + y1, color, lc, distanceX, distanceY, distx2, disty2, (x1 > distanceX || y1 > distanceY || x1 < (-distanceX) || y1 < (-distanceY)));
                }
            }

        }
        return i;
    }

    private static void toBlack(PixelGrid captcha) {
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xffffff) captcha.grid[x][y] = 0x000000;

            }
        }
    }

    private static Letter[] getBigLetters(Captcha captcha) {
        captcha.owner.loadMTHFile(captcha.owner.getResourceFile("lettersbig.mth"));
        int[][] grid = PixelGrid.getGridCopy(captcha.grid);

        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    // System.out.println(quad(captcha, x, y, 1));
                    if (quad(captcha, x, y, 1) < 5) captcha.grid[x][y] = 0xffffff;
                }

            }
        }
        setDotsInDigits(captcha, grid);
        setDotsInDigits(captcha, grid);
        setDotsInDigits(captcha, grid);
        setDotsInDigits(captcha, grid);
        setDotsInDigits(captcha, grid);
        captcha.resizetoHeight(100);
        ArrayList<PixelObject> coLetters = getFastObjects(captcha, 1, 3, 1, 3, 200);

        Letter[] letters = new Letter[coLetters.size()];
        int i = 0;
        for (PixelObject pixelObject : coLetters) {
            Letter let = pixelObject.toLetter();
            toBlack(let);
            let.removeSmallObjects(0.9, 0.9, 4, 2, 2);
            let.clean();
            // let.autoAlign();
            let.resizetoHeight(20);
            letters[i] = let;
            i++;
        }
        captcha.grid = grid;
        letters = EasyCaptcha.letterFilter(letters, captcha.owner);
        captcha.owner.loadMTHFile();
        if (letters.length > 0 && letters[0].getDecodedValue() != null && letters[0].getDecodedValue().equals("1")) letters[0].setDecodedValue("i");
        return letters;
    }

    private static ArrayList<PixelObject> getFastObjects(PixelGrid captcha, int xDistance, int yDistance, int distx2, int disty2, int minAnzahl) {
        int i = -2;

        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    follow2(captcha, x, y, i, captcha.grid[x][y], xDistance, yDistance, distx2, disty2, false);
                    i--;
                }

            }
        }

        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xffffff) captcha.grid[x][y] *= -300;
            }
        }

        ArrayList<PixelObject> coLetters = new ArrayList<PixelObject>();
        for (int x = 0; x < captcha.getWidth(); x++) {
            outerY: for (int y = 0; y < captcha.getHeight(); y++) {
                if (captcha.grid[x][y] != 0xffffff) {
                    for (PixelObject pixelObject : coLetters) {
                        if (pixelObject.getAverage() == captcha.grid[x][y]) {
                            pixelObject.add(x, y, captcha.grid[x][y]);
                            continue outerY;
                        }
                    }
                    PixelObject pix = new PixelObject(captcha);
                    pix.add(x, y, captcha.grid[x][y]);
                    coLetters.add(pix);

                }
            }
        }
        for (Iterator<PixelObject> iterator = coLetters.iterator(); iterator.hasNext();) {
            PixelObject pixelObject = (PixelObject) iterator.next();
            if (pixelObject.getSize() <= minAnzahl) iterator.remove();
        }
        Collections.sort(coLetters);
        return coLetters;
    }

    private static ArrayList<ArrayList<PixelObject>> getSmallLetters(Captcha captcha) {
        int[][] grid = PixelGrid.getGridCopy(captcha.grid);
        for (int x = 0; x < captcha.getWidth(); x++) {
            for (int y = 0; y < captcha.getHeight(); y++) {
                for (int col : bgColors) {
                    if (Colors.getRGBColorDifference3(col, captcha.grid[x][y]) < 15 || Colors.getRGBColorDifference3(col, captcha.grid[x][y]) < 15) captcha.grid[x][y] = 0xffffff;

                }
            }
        }
        ArrayList<PixelObject> coLetters = getFastObjects(captcha, 2, 1, 9, 2, 8);
        int i = 0;

        ArrayList<ArrayList<PixelObject>> coLetters2 = new ArrayList<ArrayList<PixelObject>>();

        while (coLetters.size() > 0) {
            coLetters2.add(new ArrayList<PixelObject>());
            int minY = Integer.MAX_VALUE;
            for (PixelObject pixelObject : coLetters) {
                if (pixelObject.getYMax() < minY) {
                    minY = pixelObject.getYMax();
                }
            }
            for (Iterator<PixelObject> iterator = coLetters.iterator(); iterator.hasNext();) {
                PixelObject pixelObject = (PixelObject) iterator.next();
                if (Math.abs(pixelObject.getYMax() - minY) < 10) {
                    coLetters2.get(i).add(pixelObject);
                    iterator.remove();
                }

            }
            outer: for (Iterator<PixelObject> iterator = coLetters.iterator(); iterator.hasNext();) {
                PixelObject pixelObject = (PixelObject) iterator.next();
                if (Math.abs(pixelObject.getYMin() - minY) < 20) {
                    for (PixelObject arrayList : coLetters2.get(i)) {
                        if (Math.abs(pixelObject.getXMin() - arrayList.getXMin()) < 4) continue outer;

                    }
                    coLetters2.get(i).add(pixelObject);
                    iterator.remove();
                }

            }
            i++;
        }

        for (ArrayList<PixelObject> arrayList : coLetters2) {
            Collections.sort(arrayList);
        }
        captcha.grid = grid;
        return coLetters2;

    }

    public static Letter[] getLetters(Captcha captcha) {
        Letter[] letbig = getBigLetters(captcha);
        String tx = "";
        for (Letter letter : letbig) {
            if (letter.getDecodedValue() != null) tx += letter.getDecodedValue();
        }
        tx = tx.substring(1);
        int numorg = Integer.parseInt(tx);
        int b = 0;
        char c = letbig[0].getDecodedValue().charAt(0);
        for (char i = 'a'; i < 'z'; i++) {
            if (i == c) break;
            b++;
        }
//        System.out.println("c:" + c + "" + numorg);
//        System.out.println("b:" + b);
        ArrayList<ArrayList<PixelObject>> letterList = getSmallLetters(captcha);
        int last = -1;
        PixelObject lastObje = null;
        int x = -1;
        int y = -1;
        for (PixelObject pixelObject : letterList.get(b)) {
            try {
                if (pixelObject.getSize() > 8) {
                    Letter let = pixelObject.toColoredLetter();
                    toBlack(let);
                    ArrayList<Letter> lets3 = new ArrayList<Letter>();

                    ArrayList<PixelObject> lets2 = getFastObjects(let, 1, 1, 1, 1, 1);
                    for (PixelObject pixelObject2 : lets2) {
                        if (pixelObject2.getSize() > 5) {
                            Letter let2 = pixelObject2.toColoredLetter();
                            toBlack(let2);
                            let2.autoAlign();
                            let2.resizetoHeight(30);
                            lets3.add(let2);
                        }
                    }
                    int number = -1;
                    Letter[] letters = lets3.toArray(new Letter[] {});
                    letters = EasyCaptcha.letterFilter(letters, captcha.owner);

                    if (lets3.size() > 0) {
                        if (letters[0].getDecodedValue().endsWith(letbig[0].getDecodedValue())) {
                            String num = "";
                            for (Letter letter : letters) {
                                if (letter.getDecodedValue() != null) num += letter.getDecodedValue();
                            }
//                            System.out.println("num:" + num);
                            num = num.substring(1);
                            if (num.length() > 0) {
                                if (num.length()>1 && num.charAt(0) == 'z') {
                                    num = num.substring(1);
                                }
                                if (num.length() > 0) {
                                    String numnew = num.replaceAll("[^0-9]", "1");
//                                    System.out.println("newnum" + numnew);
                                    number = Integer.parseInt(numnew);
                                }
                            }
                        }
                    }
                    if (number >= 0 && number > (last - 8) && number < (last + 12)) {
                        last = number;
                    } else {
                        last++;
                        number = last;
                    }
//                    System.out.println(number);
                    if (numorg == number) {
                        x = let.getLocation()[0] + (let.getWidth() / 2);
                        y = let.getLocation()[1] + (let.getHeight() / 2);
                        break;
                    } else if (number > numorg && lastObje != null) {
                        x = lastObje.getLocation()[0] + (lastObje.getWidth() / 2);
                        y = lastObje.getLocation()[1] + (lastObje.getHeight() / 2);
                        break;

                    }
                    lastObje = pixelObject;
                }
            } catch (Exception e) {
            }
        }
        if(x==-1)
        {
            if(lastObje!=null)
            {
                x = lastObje.getLocation()[0] + (lastObje.getWidth() / 2);
                y = lastObje.getLocation()[1] + (lastObje.getHeight() / 2);
            }
            else
            {
                x = 10;
                y = 10;
            }
        }
//        captcha.setPixelValue(x, y, 0xff0000);
//        BasicWindow.showImage(captcha.getImage());
        return Circle.getPostionLetters(x, y);
    }
}