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

package jd.captcha.pixelgrid;

import java.util.Vector;

import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.gui.BasicWindow;
import jd.captcha.pixelobject.PixelObject;
import jd.captcha.utils.Utilities;

/**
 * Diese Klasse beinhaltet alle Methoden für einzellne Letter.
 *
 * @author JD-Team
 */
public class Letter extends PixelGrid {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private int               angle;
    /**
     * Gibt an wie oft dieser letter negativ aufgefallen ist
     */
    private int               badDetections    = 0;
    /*
     * der decoded Value wird heir abgelegt
     */
    private String            decodedValue;

    public LetterComperator   detected         = null;

    private int               elementPixel     = 0;

    /**
     * Gibt an wie oft dieser letter positov aufgefallen ist
     */
    private int               goodDetections   = 0;

    /**
     * ID des letters in der Lettermap
     */
    private int               id;

    /**
     * Hash des sourcecaptchas (Fürs training wichtig)
     */
    private String            sourcehash;

    /**
     *
     */
    public Letter() {
        super(0, 0);
    }

    public Letter(int averageWidth, int height) {
        super(averageWidth, height);
    }

    /**
     * Versucht den Buchstaben automatisch auszurichten. Als kriterium dient das minimale Breite/Höhe Verhältniss Es wird zuerst die
     * optimale DRehrichtung ermittelt und dann gedreht. Die Methode funktioniert nicht immer zuverlässig. align(double contrast,double
     * objectContrast,int angleA, int angleB) braucht länger, liefert aber bessere Ergebnisse
     *
     * @param objectContrast
     * @return Ausgerichteter Buchstabe
     */
    public Letter align(double objectContrast) {
        PixelObject obj = toPixelObject(objectContrast);

        PixelObject aligned = obj.align();
        Letter newLetter = aligned.toLetter();
        setGrid(newLetter.grid);
        return this;

    }

    /**
     * Gibt einen Ausgerichteten Buchstaben zurück. Es wird vom Winkel angleA bis angleB nach der Besten Ausrichtung (Breite/Höhe) gesucht.
     * Ist zuverlässiger als align(double contrast,double objectContrast)
     *
     * @param objectContrast
     * @param angleA
     * @param angleB
     * @return Ausgerichteter Buchstabe
     */
    public Letter align(double objectContrast, int angleA, int angleB) {
        PixelObject obj = toPixelObject(objectContrast);

        PixelObject aligned = obj.align(angleA, angleB);
        Letter newLetter = aligned.toLetter();

        setGrid(newLetter.grid);
        return this;

    }

    /**
     * Autoasurichtung. Diese Funktion geht nicht den Umweg über ein Pixelobject. Braucht etwas mehr zeit und liefrt dafür deutlich bessere
     * Ergebnisse!
     *
     * @param angleA
     * @param angleB
     * @return Gedrehter buchstabe
     */
    public Letter align(int angleA, int angleB) {
        if (angleB < angleA) {
            int tmp = angleB;
            angleB = angleA;
            angleA = tmp;
        }
        int accuracy = owner.getJas().getInteger("AlignAngleSteps");
        double bestValue = Double.MAX_VALUE;
        Letter res = null;
        Letter tmp;
        // if(Utilities.isLoggerActive())logger.info("JJ"+angleA+" -
        // "+angleB);
        for (int angle = angleA; angle < angleB; angle += accuracy) {

            tmp = turn(angle < 0 ? 360 + angle : angle);
            // if(Utilities.isLoggerActive())logger.info("..
            // "+((double)tmp.getWidth()/(double)tmp.getHeight()));
            // BasicWindow.showImage(tmp.getImage(1),(angle<0?360+angle:angle)+"_"
            // );
            if ((double) tmp.getWidth() / (double) tmp.getHeight() < bestValue) {
                bestValue = (double) tmp.getWidth() / (double) tmp.getHeight();
                res = tmp;
            }
        }

        return res;

    }

    /**
     * Richtet sich an den Pixelzahlen der oberen und unteren Zeile und gibt unter umständen ein besseres Ergebnis aus
     *
     * @param angleA
     * @param angleB
     * @return Gedrehter buchstabe
     */
    public Letter betterAlign(int angleA, int angleB) {
        if (angleB < angleA) {
            int tmp = angleB;
            angleB = angleA;
            angleA = tmp;
        }
        int accuracy = owner.getJas().getInteger("AlignAngleSteps");
        int bestPix = getFirstAndLastLinePixels();
        Letter res = null;
        Letter tmp = this;
        for (int angle = angleA; angle < angleB; angle += accuracy) {

            tmp = turn(angle < 0 ? 360 + angle : angle);
            int pix = tmp.getFirstAndLastLinePixels();
            if (pix > bestPix) {
                bestPix = pix;
                res = tmp;
            }
        }

        return res;

    }

    public void invertIfMoreBackground() {
        long b = 0;
        long w = 0;
        int avg = getAverage();

        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                if (isElement(getPixelValue(x, y), avg)) {
                    b++;
                } else {
                    w++;
                }
            }
        }
        if (b > w) {
            invert();
        }
    }

    public void colorize(int color) {
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                if (grid[x][y] < 40) {
                    grid[x][y] = color;
                }
            }
        }
    }

    /**
     * Füllt alle Pixel des Objects in dem Letter mit einer Farbe,. Ist color<0, so woird die farebd es Objects verwendet.
     */
    public void fillWithObject(PixelObject p, int color) {
        for (int i = 0; i < p.getSize(); i++) {
            int[] point = p.elementAt(i);
            grid[point[0]][point[1]] = color < 0 ? point[2] : color;
        }
    }

    private void followPixelObject(int x, int y, int[][] map, boolean[][] bmap) {
        if (bmap[x][y]) {
            return;
        }
        bmap[x][y] = true;
        // links oben
        int posX = -1;
        int posY = -1;
        try {
            if (map[x + posX][y + posY] == 0) {
                followPixelObject(x + posX, y + posY, map, bmap);
            }
        } catch (Exception e) {
        }
        // links
        posX = -1;
        posY = 0;
        try {
            if (map[x + posX][y + posY] == 0) {
                followPixelObject(x + posX, y + posY, map, bmap);
            }
        } catch (Exception e) {
        }
        // links unten
        posX = -1;
        posY = 1;
        try {
            if (map[x + posX][y + posY] == 0) {
                followPixelObject(x + posX, y + posY, map, bmap);
            }
        } catch (Exception e) {
        }
        // unten
        posX = 0;
        posY = 1;
        try {
            if (map[x + posX][y + posY] == 0) {
                followPixelObject(x + posX, y + posY, map, bmap);
            }
        } catch (Exception e) {
        }
        // rechtsunten
        posX = 1;
        posY = 1;
        try {
            if (map[x + posX][y + posY] == 0) {
                followPixelObject(x + posX, y + posY, map, bmap);
            }
        } catch (Exception e) {
        }
        // rechts
        posX = 1;
        posY = 0;
        try {
            if (map[x + posX][y + posY] == 0) {
                followPixelObject(x + posX, y + posY, map, bmap);
            }
        } catch (Exception e) {
        }
        // rechts oben
        posX = 1;
        posY = -1;
        try {
            if (map[x + posX][y + posY] == 0) {
                followPixelObject(x + posX, y + posY, map, bmap);
            }
        } catch (Exception e) {
        }
        // oben
        posX = 0;
        posY = -1;
        try {
            if (map[x + posX][y + posY] == 0) {
                followPixelObject(x + posX, y + posY, map, bmap);
            }
        } catch (Exception e) {
        }

    }

    /**
     * Gib den drehwinkel des letterszurück
     *
     * @return drehwinkel
     */
    public int getAngle() {
        return angle;

    }

    /**
     *
     * @return Gibt die Fläche des Objekts zurück
     */
    public int getArea() {
        return getWidth() * getHeight();
    }

    /**
     * @return the badDetections
     */
    public int getBadDetections() {
        return badDetections;
    }

    /**
     * @return the decodedValue
     */
    public String getDecodedValue() {
        return decodedValue;
    }

    /**
     * @return the elementPixel
     */
    public int getElementPixel() {
        if (elementPixel > 0) {
            return elementPixel;
        }
        elementPixel = 0;
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                if (grid[x][y] == 0) {
                    elementPixel++;
                }
            }
        }
        return elementPixel;
    }

    private int getFirstAndLastLinePixels() {
        int c = 0;
        int avg = getAverage();
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                if (isElement(getPixelValue(x, y), avg)) {
                    c++;
                }
            }
            if (c > 0) {
                break;
            }
        }
        int c2 = 0;
        for (int y = getHeight() - 1; y > 0; y--) {
            for (int x = 0; x < getWidth(); x++) {
                if (isElement(getPixelValue(x, y), avg)) {
                    c2++;
                }
            }
            if (c2 > 0) {
                break;
            }
        }
        return c + c2;
    }

    /**
     * @return Anzahl der Erfolgreichen Erkennungen durch diesen letter
     */
    public int getGoodDetections() {
        return goodDetections;

    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    public Letter getLinedLetter() {
        Letter ret = new Letter();
        ret.setOwner(owner);
        int count = 0;
        int firstChange = 0;
        while (true) {
            count++;
            int changed = 0;
            PixelObject po = toPixelObject(0.85);
            Vector<int[]> border = po.getBorderVector(this);

            for (int i = 0; i < border.size(); i++) {
                int ax = border.get(i)[0];

                int ay = border.get(i)[1];

                logger.info(ax + "/" + ay);

                int[][] map = getLocalMap(grid, ax, ay);

                int a = getObjectsNum(map);
                map[1][1] = 0xff000;

                int b = getObjectsNum(map);

                logger.info(a + " --->> " + b);

                if (a == b) {
                    changed++;
                    this.setPixelValue(ax, ay, 0xff0000);
                } else {

                    logger.info(map[0][0] + "-" + map[1][0] + "-" + map[2][0]);

                    logger.info(map[0][1] + "-" + map[1][1] + "-" + map[2][1]);

                    logger.info(map[0][2] + "-" + map[1][2] + "-" + map[2][2]);

                    this.setPixelValue(ax, ay, 0);
                }

            }
            BasicWindow.showImage(this.getImage(5));

            logger.info("changed " + changed);

            if (firstChange == 0) {
                firstChange = changed;
            }
            if (changed * 20 <= firstChange) {
                break;
            }

        }

        //
        // for (int x = 0; x < getWidth(); x++) {
        // for (int y = 0; y < getHeight(); y++) {
        //
        //
        // }
        // }
        //
        ret.setGrid(grid);

        return ret;

    }

    /*
     * private int[] getPartnerPixel(int x, int y, Vector<int[]> border) {
     *
     * int radius = 1; boolean[][] badgrid = new boolean[this.getWidth()][this.getHeight()]; badgrid[x][y] = true; double dir =
     * getBorderDir(x, y);
     *
     * return null; }
     *
     * private double getBorderDir(int x, int y) { int count = 0; double angle = 0; int[][] angleMap = new int[3][3]; angleMap[2][1] = 0;
     * angleMap[2][0] = 45; angleMap[1][0] = 90; angleMap[0][0] = 135; angleMap[0][1] = 180; angleMap[0][2] = 225; angleMap[1][2] = 270;
     * angleMap[2][2] = 315; angleMap[1][1] = -1; for (int xx = -1; xx < 2; xx++) { for (int yy = -1; yy < 2; yy++) { try { // ((x + xx < 0
     * || x + xx > this.getWidth() - 1 || y + yy < // 0 || y + yy > this.getHeight() - 1) || if ((grid[x + xx][y + yy] > 0) && angleMap[xx +
     * 1][yy + 1] >= 0) { // if(Utilities.isLoggerActive())logger.info(xx+","+yy+" - "+angleMap[xx+1][yy+1]); angle = count angle +
     * angleMap[xx + 1][yy + 1]; count++; angle /= count; } } catch (Exception e) { } } } return angle; }
     */

    public int[][] getLocalMap(int[][] grid, int x, int y) {
        int[][] map = new int[3][3];

        for (int xx = -1; xx <= 1; xx++) {
            for (int yy = -1; yy <= 1; yy++) {
                try {
                    map[xx + 1][yy + 1] = grid[x + xx][y + yy];
                } catch (Exception e) {
                    map[xx + 1][yy + 1] = -1;
                }
            }
        }
        return map;

    }

    public Letter getMassLetter() {
        Letter ret = new Letter();
        ret.setOwner(owner);

        int[][] newGrid = new int[getWidth()][getHeight()];
        int[][] filterGrid = new int[getWidth()][getHeight()];
        long[][] grd = new long[getWidth()][getHeight()];
        PixelObject po = toPixelObject(0.85);
        long max = 0;
        long min = Long.MAX_VALUE;
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                long mass = po.getMassValue(x, y);
                max = Math.max(max, mass);
                min = Math.min(min, mass);

                grd[x][y] = mass;
            }
        }

        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                filterGrid[x][y] = 0;
                newGrid[x][y] = (int) (this.getMaxPixelValue() * (double) (grd[x][y] - min) / (max - min));
            }
        }

        // long akt;
        int x = 0;
        int y = 0;
        int[][] map;
        int i = 1000000;
        while (true) {

            map = getLocalMap(newGrid, x, y);
            // if(Utilities.isLoggerActive())logger.info(x+" - "+y);
            try {
                filterGrid[x][y] = newGrid[x][y];

                int xMax = -2;
                int yMax = -2;
                int value = -1;
                for (int xx = -1; xx <= 1; xx++) {
                    for (int yy = -1; yy <= 1; yy++) {
                        if (yy != 0 && xx != 0) {
                            if (map[xx + 1][yy + 1] >= value && map[xx + 1][yy + 1] > 0 && filterGrid[x + xx][y + yy] == 0) {
                                value = map[xx + 1][yy + 1];
                                xMax = xx;
                                yMax = yy;
                            }
                        }
                    }
                }
                if (xMax < -1) {
                    xMax = 1;
                }
                if (yMax < -1) {
                    yMax = 1;
                }
                x += xMax;
                y += yMax;
                i--;
                if (i == 0) {
                    break;
                }
            } catch (Exception e) {
                break;
            }
        }

        ret.setGrid(newGrid);

        return ret;

    }

    private int getObjectsNum(int[][] map) {
        boolean[][] bmap = new boolean[3][3];

        int ret = 0;
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                if (!bmap[x][y] && map[x][y] == 0) {
                    followPixelObject(x, y, map, bmap);
                    ret++;
                }

            }
        }

        // if(Utilities.isLoggerActive())logger.info("elements: " + ret);
        // if(Utilities.isLoggerActive())logger.info(map[0][0] + "-" +
        // map[1][0] +
        // "-" + map[2][0]);
        // if(Utilities.isLoggerActive())logger.info(map[0][1] + "-" +
        // map[1][1] +
        // "-" + map[2][1]);
        // if(Utilities.isLoggerActive())logger.info(map[0][2] + "-" +
        // map[1][2] +
        // "-" + map[2][2]);
        return ret;
    }

    /**
     * Gibt den Pixelstring zurück. Pixelsep: * ZeilenSep: |
     *
     * @return Pixelstring 1*0*1|0*0*1|...
     */
    public String getPixelString() {

        StringBuilder ret = new StringBuilder();

        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                ret.append(getPixelValue(x, y) / getMaxPixelValue());

            }
            ret.append('|');
        }
        String ps = ret.toString();
        if (ps.length() == 0) {
            System.out.println("IK");
        }
        return ps.length() == 0 ? ps : ps.substring(0, ps.length() - 1);
    }

    /**
     * TODO: EINE saubere verkleinerung * gibt den Letter um den faktor faktor verkleinert zurück. Es wird ein Kontrastvergleich vorgenommen
     *
     * @param faktor
     * @return Vereinfachter Buchstabe
     */
    public Letter getSimplified(double faktor) {
        if (faktor == 1.0 || faktor == 0.0) {
            return this;
        }

        int newWidth = (int) Math.ceil(getWidth() / faktor);
        int newHeight = (int) Math.ceil(getHeight() / faktor);
        Letter ret = new Letter();

        ret.setOwner(owner);
        int avg = getAverage();

        logger.info(newWidth + " - " + newHeight);
        int[][] newGrid = new int[newWidth][newHeight];

        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                PixelGrid.setPixelValue(x, y, newGrid, getMaxPixelValue());

            }
        }
        elementPixel = 0;
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {

                if (isElement(getPixelValue(x, y), avg)) {
                    int newX = (int) Math.floor(x / faktor);
                    int newY = (int) Math.floor(y / faktor);
                    PixelGrid.setPixelValue(newX, newY, newGrid, 0);

                }

            }
        }

        ret.setGrid(newGrid);

        ret.clean();

        return ret;
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
        // ? 0 :
        // getMaxPixelValue(), this.owner);
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

    /**
     * @return the sourcehash
     */
    public String getSourcehash() {
        return sourcehash;
    }

    /**
     * Addiert eine Schlechte Erkennung
     */
    public void markBad() {
        badDetections++;

        logger.warning("Bad detection : (" + toString() + ") ");

    }

    /**
     * Addiert eine gute Erkennung zu dem letter
     */
    public void markGood() {
        goodDetections++;

        logger.warning("GOOD detection : (" + toString() + ") ");

    }

    /**
     * Resize auf newWidth. die proportionen bleiben erhalten
     *
     * @param newWidth
     */
    public void resizetoWidth(int newWidth) {
        double faktor = (double) newWidth / (double) getWidth();

        int newHeight = (int) Math.ceil(getHeight() * faktor);

        int[][] newGrid = new int[newWidth][newHeight];
        int elementPixel = 0;
        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                int v = grid[(int) Math.floor(x / faktor)][(int) Math.floor(y / faktor)];
                newGrid[x][y] = v;
                if (newGrid[x][y] == 0) {
                    elementPixel++;
                }

            }
        }
        setElementPixel(elementPixel);
        setGrid(newGrid);

    }

    /**
     * Resize auf newHeight. die proportionen bleiben erhalten
     *
     * @param newHeight
     */
    public void resizetoHeight(int newHeight) {
        double faktor = (double) newHeight / (double) getHeight();

        int newWidth = (int) Math.ceil(getWidth() * faktor);

        int[][] newGrid = new int[newWidth][newHeight];
        int elementPixel = 0;
        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                int v = grid[(int) Math.floor(x / faktor)][(int) Math.floor(y / faktor)];
                newGrid[x][y] = v;
                if (newGrid[x][y] == 0) {
                    elementPixel++;
                }

            }
        }
        setElementPixel(elementPixel);
        setGrid(newGrid);

    }

    /**
     * Skaliert den Letter auf die höhe
     *
     * @param newHeight
     * @param d
     *            Grenzfaktor. darunter wird nicht skaliert
     */
    public void resizetoHeight(int newHeight, double d) {
        double faktor = (double) newHeight / (double) getHeight();
        if (Math.abs(faktor - 1) < d) {
            resizetoHeight(newHeight);
        }

    }

    private void setAngle(int angle) {
        this.angle = angle;

    }

    /**
     * @param badDetections
     *            the badDetections to set
     */
    public void setBadDetections(int badDetections) {
        this.badDetections = badDetections;
    }

    /**
     * @param decodedValue
     *            the decodedValue to set
     */
    public void setDecodedValue(String decodedValue) {
        this.decodedValue = decodedValue;
    }

    /**
     * @param elementPixel
     *            the elementPixel to set
     */
    public void setElementPixel(int elementPixel) {
        this.elementPixel = elementPixel;
    }

    /**
     * @param goodDetections
     *            the goodDetections to set
     */
    public void setGoodDetections(int goodDetections) {
        this.goodDetections = goodDetections;
    }

    /**
     * @param owner
     *            the owner to set
     */
    // @Override
    @Override
    public void setOwner(JAntiCaptcha owner) {
        this.owner = owner;
    }

    /**
     * @param sourcehash
     *            the sourcehash to set
     */
    public void setSourcehash(String sourcehash) {
        this.sourcehash = sourcehash;
    }

    /**
     * Setzt das grid aus einem TextString. PixelSeparator: * Zeilenseparator: |
     *
     * @param content
     *            PixelString
     * @return true/false
     */
    public boolean setTextGrid(String content) {
        String[] code = content.split("\\|");
        grid = null;
        int width = 0;
        int elementPixel = 0;
        for (int y = 0; y < code.length; y++) {
            String line = code[y];
            width = line.length();
            if (grid == null) {
                grid = new int[width][code.length];
                // TODO war vorher width < 2 kann zu regressionen führen
                // beobachten
                if (width < 1 || code.length < 2) {
                    return false;
                }

            }
            for (int x = 0; x < width; x++) {
                try {
                    grid[x][y] = Integer.parseInt(String.valueOf(line.charAt(x))) * getMaxPixelValue();
                } catch (Exception e) {
                    logger.log(e);
                    return false;
                }
                if (grid[x][y] == 0) {
                    elementPixel++;
                }
            }
        }
        setElementPixel(elementPixel);
        return true;

    }

    public PixelObject toPixelObject(double objectContrast) {
        PixelObject object = new PixelObject(this);
        object.setWhiteContrast(objectContrast);
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                if (getPixelValue(x, y) < getMaxPixelValue() * objectContrast) {
                    object.add(x, y, getPixelValue(x, y));
                }
            }
        }

        return object;
    }

    // @Override
    @Override
    public String toString() {
        return getDecodedValue() + " [" + getSourcehash() + "][" + getGoodDetections() + "/" + getBadDetections() + "]";
    }

    /**
     * Entfernt die Reihen 0-left und right-ende aus dem interne Grid
     *
     * @param left
     * @param right
     * @return true bei Erfolg, sonst false
     */
    public boolean trim(int left, int right) {
        int width = right - left;
        int[][] tmp = new int[width][getHeight()];
        setLocation(new int[] { left, 0 });
        if (getWidth() < right) {
            logger.severe("Letter dim: " + getWidth() + " - " + getHeight() + ". Cannot trim to " + left + "-" + right);
            return false;
        }
        for (int x = 0; x < width; x++) {

            tmp[x] = grid[x + left];

        }
        grid = tmp;
        return true;
    }

    /**
     * Dreht den buchstaben um angle. Dabei wird breite und höhe angepasst. Das drehen dauert länger als über PixelObject, leidet dafür
     * deutlich weniger unter Pixelfehlern
     *
     * @param angle
     * @return new letter
     */
    public Letter turn(double angle) {
        if (angle == 0.0) {
            return this;
        }
        while (angle < 0) {
            angle += 360;
        }
        angle /= 180;
        Letter l = createLetter();

        int newWidth = (int) (Math.abs(Math.cos(angle * Math.PI) * getWidth()) + Math.abs(Math.sin(angle * Math.PI) * getHeight()));
        int newHeight = (int) (Math.abs(Math.sin(angle * Math.PI) * getWidth()) + Math.abs(Math.cos(angle * Math.PI) * getHeight()));
        // if(Utilities.isLoggerActive())logger.info(getWidth()+"/"+getHeight(
        // )+"
        // --> "+newWidth+"/"+newHeight
        // +"("+angle+"/"+(angle*180)+"/"+Math.cos(sizeAngle *
        // Math.PI)+"/"+Math.sin(sizeAngle * Math.PI));
        int left = (newWidth - getWidth()) / 2;
        int top = (newHeight - getHeight()) / 2;
        int elementPixel = 0;
        int[][] newGrid = new int[newWidth][newHeight];
        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                int[] n = Utilities.turnCoordinates(x - left, y - top, getWidth() / 2, getHeight() / 2, -(angle * 180));
                if (n[0] < 0 || n[0] >= getWidth() || n[1] < 0 || n[1] >= getHeight()) {
                    newGrid[x][y] = owner.getJas().getColorFaktor() - 1;
                    if (newGrid[x][y] == 0) {
                        elementPixel++;
                    }
                    continue;
                }

                newGrid[x][y] = grid[n[0]][n[1]];
                if (newGrid[x][y] == 0) {
                    elementPixel++;
                }

            }
        }
        l.setGrid(newGrid);
        l.setElementPixel(elementPixel);
        l.clean();
        l.id = this.id;
        l.setDecodedValue(getDecodedValue());
        l.setAngle((int) (angle * 180.0));
        l.setProperties(getProperties());
        // BasicWindow.showImage(l.getImage(), sizeAngle+" angle "+angle+" -
        // "+newWidth+"/"+newHeight+" - "+getWidth()+"/"+getHeight());

        return l;

    }

    public void setId(int id) {
        this.id = id;
    }

}