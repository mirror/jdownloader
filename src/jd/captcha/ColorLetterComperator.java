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

package jd.captcha;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.ArrayList;

import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelgrid.PixelGrid;
import jd.captcha.pixelobject.PixelObject;
import jd.nutils.Colors;

import com.jhlabs.image.PosterizeFilter;

public class ColorLetterComperator {
    private Letter a;
    private Letter b;
    private PixelObject[] aLayers;
    private PixelObject[] bLayers;
    private Captcha ca;
    private Captcha cb;
    private JAntiCaptcha owner;
    public int lettersPerfect = 2;
    public int lettersPerfectPercent = 20;
    public int maxColorDifference = 30;
    protected double valityPercent = 10000.0;
    /**
     * Farbebenen die untersucht werden sollen
     */
    public int colorLevels = 2;

    /**
     * vergleicht Farbebenen eignet sich um echte Bilder zu vergleichen
     */
    public ColorLetterComperator(Letter a, Letter b) {
        this.a = a;
        this.b = b;
        if (a.owner != null)
            owner = a.owner;
        else if (b.owner != null) owner = b.owner;
    }

    public void setLetterA(Letter a) {
        this.a = a;
        ca = null;
        aLayers = null;
    }

    public void setLetterB(Letter b) {
        this.b = b;
        cb = null;
        bLayers = null;
    }

    private Letter getSWLetter(PixelObject obj) {
        int[][] lgrid = new int[obj.getWidth()][obj.getHeight()];
        for (int x = 0; x < obj.getWidth(); x++) {
            for (int y = 0; y < obj.getHeight(); y++) {
                lgrid[x][y] = 0xffffff;

            }
        }
        int w = 0;
        for (int d = 0; d < obj.getSize(); d++) {
            w++;
            int[] akt = obj.elementAt(d);
            lgrid[akt[0] - obj.getXMin()][akt[1] - obj.getYMin()] = 0x000000;

        }
        // invertieren wenn der schwarze bereich größer ist als der weiße
        if (obj.getArea() / 2 < w) {
            for (int x = 0; x < obj.getWidth(); x++) {
                for (int y = 0; y < obj.getHeight(); y++) {
                    if (lgrid[x][y] == 0xffffff)
                        lgrid[x][y] = 0x000000;
                    else
                        lgrid[x][y] = 0xffffff;
                }
            }
        }
        Letter l = obj.owner.createLetter();
        l.setElementPixel(obj.getSize());
        l.setLocation(new int[] { obj.getXMin(), obj.getYMin() });
        l.setGrid(lgrid);
        l.detected = obj.detected;
        return l;
    }

    /**
     * vergleicht alle Farbebenen die untersucht werden sollen und gibt den
     * durchschnittlichen ValityPercent
     * 
     * @return Prozentwert 0(gut) bis 100 (schlecht) der Übereinstimmung
     */
    public double run() {
        if (aLayers != null && bLayers != null) return valityPercent;
        if ((a.getArea() / 5) > b.getArea() || (b.getArea() / 5) > a.getArea()) {
            valityPercent = 10000.0;
            return valityPercent;
        }
        if (ca == null) {
            ca = reduceColors(a);
        }
        if (cb == null) {
            cb = reduceColors(b);
        }

        if (aLayers == null) {
            aLayers = getObjects(ca);
        }
        if (bLayers == null) {
            bLayers = getObjects(cb);
        }
        double ret = 0;
        int minl = Math.min(aLayers.length, bLayers.length);
        int perf = 0;
        int maxl = Math.max(aLayers.length, bLayers.length);
        boolean minla = aLayers.length == minl;
        PixelObject[][] bestarray = new PixelObject[minl][2];
        for (int i = 0; i < minl; i++) {
            double best = Double.MAX_VALUE;
            PixelObject[] bestobj = null;
            for (int j = maxl - i - 1; j >= 0; j--) {
                double value = 0;
                int a = j;
                int b = i;

                if (minla) {
                    a = i;
                    b = j;
                }
                value = Colors.getColorDifference(aLayers[a].getAverage(), bLayers[b].getAverage());

                if (value == 0) {
                    bestobj = new PixelObject[] { aLayers[a], bLayers[b] };
                    break;
                } else if (value < best) {
                    best = value;
                    bestobj = new PixelObject[] { aLayers[a], bLayers[b] };
                }

            }
            bestarray[i] = bestobj;
        }
        for (int i = 0; i < bestarray.length; i++) {
            if (Colors.getColorDifference(bestarray[i][0].getAverage(), bestarray[i][1].getAverage()) > maxColorDifference)
                ret += 100;
            else {
                try {
                    Letter leta = getSWLetter(bestarray[i][0]);
                    LetterComperator lc = new LetterComperator(leta, getSWLetter(bestarray[i][1]));
                    lc.setOwner(getJac());
                    bestarray[i][0].detected = lc;
                    lc.run();
                    double vall = lc.getValityPercent();
                    if (vall < lettersPerfectPercent) {
                        perf++;
                        valityPercent = 0;
                        if (perf == lettersPerfect) return 0;
                    }
                    ret += lc.getValityPercent();
                } catch (Exception e) {
                    ret += ret / i;
                }

            }
        }
        ret += (maxl - minl) * 100;
        valityPercent = ret / maxl;
        return valityPercent;
    }

    private JAntiCaptcha getJac() {
        if (owner != null) return owner;
        String hoster = "EasyCaptcha";
        owner = new JAntiCaptcha(hoster);
        return owner;
    }

    /**
     * Erstellt aus jeder Farbebene ein Letter Achtung Farben müssen reduziert
     * sein
     * 
     * @param grid
     * @return
     */
    public static PixelObject[] getObjects(PixelGrid grid) {
        ArrayList<PixelObject> ret = new ArrayList<PixelObject>();
        ArrayList<PixelObject> merge;
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                PixelObject n = new PixelObject(grid);
                n.add(x, y, grid.getGrid()[x][y]);

                merge = new ArrayList<PixelObject>();
                for (PixelObject o : ret) {
                    if (grid.getGrid()[x][y] == o.getAverage()) {
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

        return ret.toArray(new PixelObject[] {});
    }

    /**
     * reduziert die farben (colorLevels)
     * 
     * @param letter
     * @return
     */
    private Captcha reduceColors(Letter letter) {
        PosterizeFilter qf = new PosterizeFilter();
        BufferedImage image = letter.getImage();
        BufferedImage dest = qf.createCompatibleDestImage(image, ColorModel.getRGBdefault());
        qf.setNumLevels(colorLevels);
        qf.filter(image, dest);
        return getJac().createCaptcha(dest);
    }
}
