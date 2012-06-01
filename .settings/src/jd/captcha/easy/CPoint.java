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

package jd.captcha.easy;

import java.awt.Color;
import java.awt.Point;
import java.io.Serializable;

import jd.captcha.pixelgrid.Captcha;
import jd.nutils.Colors;

public class CPoint extends Point implements Serializable, Cloneable {
    /**
     * Die LAB Differenz ist der Menschlichen Farbempfindung sehr nahe
     */
    public final static byte LAB_DIFFERENCE = 1;
    /**
     * Schnell berrechneter RGB Farbunterschied nicht wirklich gut aber da der
     * Farbunterschied unter umständen so berechnet wurde nützlich
     */
    public final static byte RGB_DIFFERENCE1 = 2;
    /**
     * Schnell berrechneter RGB Farbunterschied 
     * der höchste unterschied in einem Kanal wird gewertet
     */
    public final static byte RGB_DIFFERENCE3 = 11;
    /**
     * Farbunterschied im Raum
     */
    public final static byte RGB_DIFFERENCE2 = 3;
    /**
     * unterschied des Farbtons
     */
    public final static byte HUE_DIFFERENCE = 4;
    /**
     * Unterschied der Farbsättigung
     */
    public final static byte SATURATION_DIFFERENCE = 5;
    /**
     * Unterschied der absoluten Helligkeit
     */
    public final static byte BRIGHTNESS_DIFFERENCE = 6;
    /**
     * Rotunterschied
     */
    public final static byte RED_DIFFERENCE = 7;
    /**
     * Grünunterschied
     */
    public final static byte GREEN_DIFFERENCE = 8;
    /**
     * Blauunterschied
     */
    public final static byte BLUE_DIFFERENCE = 9;
    /**
     * CMYK Color difference
     */
    public final static byte CMYK_DIFFERENCE = 10;

    private static final long serialVersionUID = 333616481245029882L;
    private int color, distance;
    /**
     * Fordergrund oder Hintergrund Buchstaben oder Hintergrund
     */
    private boolean foreground = true;

    private byte colorDifferenceMode = LAB_DIFFERENCE;

    /**
     * Beim CPoint wird der Point um Farbeigenschaften erweitert und stellt
     * verschiedene möglichkeiten zur verfügung um Farbunterschiede zu berechnen
     */
    public CPoint() {
    }

    /**
     * Beim CPoint wird der Point um Farbeigenschaften erweitert und stellt
     * verschiedene möglichkeiten zur verfügung um Farbunterschiede zu berechnen
     * 
     * @param x
     * @param y
     * @param distance
     * @param captcha
     */
    public CPoint(int x, int y, int distance, Captcha captcha) {
        this(x, y, distance, captcha.getPixelValue(x, y));
    }

    /**
     * Beim CPoint wird der Point um Farbeigenschaften erweitert und stellt
     * verschiedene möglichkeiten zur verfügung um Farbunterschiede zu berechnen
     * 
     * @param x
     * @param y
     * @param distance
     * @param color
     */
    public CPoint(int x, int y, int distance, int color) {
        super(x, y);
        this.color = color;
        this.distance = distance;
    }

    /**
     * Farbe
     * 
     * @return
     */
    public int getColor() {
        return color;
    }

    /**
     * Farbe
     * 
     * @param color
     */
    public void setColor(int color) {
        this.color = color;
    }

    /**
     * Farbmodus
     * 
     * @param colorDistanceMode
     *            CPoint.LAB_DIFFERENCE CPoint.RGB_DIFFERENCE1 ...
     */
    public byte getColorDistanceMode() {
        return colorDifferenceMode;
    }

    /**
     * Farbmodus
     * 
     * @param colorDistanceMode
     *            CPoint.LAB_DIFFERENCE CPoint.RGB_DIFFERENCE1 ...
     */
    public void setColorDistanceMode(byte colorDistanceMode) {
        this.colorDifferenceMode = colorDistanceMode;
    }

    /**
     * handelt es sich um einen Fordergrund / Buchstaben oder um Hintergrund
     * 
     * @return false wenn Hintergrund
     */
    public boolean isForeground() {
        return foreground;
    }

    /**
     * @param Wenn
     *            Fordergrund / Buchstaben dann true beim Hintergrund false
     */
    public void setForeground(boolean foreground) {
        this.foreground = foreground;
    }

    /**
     * Erlaubter Farbunterschied
     * 
     * @return
     */
    public int getDistance() {
        return distance;
    }

    /**
     * gibt anhand von colorDifferenceMode unterschied zur übergebenen Farbe aus
     * 
     * @param color
     * @return
     */
    public double getColorDifference(int color) {
        double dst = 0;

        if (color == this.color) return dst;
        switch (colorDifferenceMode) {
        case LAB_DIFFERENCE:
            dst = Colors.getColorDifference(color, this.color);
            break;
        case RGB_DIFFERENCE1:
            dst = Colors.getRGBColorDifference1(color, this.color);
            break;
        case RGB_DIFFERENCE2:
            dst = Colors.getRGBColorDifference2(color, this.color);
            break;
        case RGB_DIFFERENCE3:
            dst = Colors.getRGBColorDifference3(color, this.color);
            break;
        case HUE_DIFFERENCE:
            dst = Colors.getHueColorDifference360(color, this.color);
            break;
        case SATURATION_DIFFERENCE:
            dst = Colors.getSaturationColorDifference(color, this.color);
            break;
        case BRIGHTNESS_DIFFERENCE:
            dst = Colors.getBrightnessColorDifference(color, this.color);
            break;
        case RED_DIFFERENCE:
            dst = Math.abs(new Color(color).getRed() - new Color(this.color).getRed());
            break;
        case GREEN_DIFFERENCE:
            dst = Math.abs(new Color(color).getGreen() - new Color(this.color).getGreen());
            break;
        case BLUE_DIFFERENCE:
            dst = Math.abs(new Color(color).getBlue() - new Color(this.color).getBlue());
            break;
        case CMYK_DIFFERENCE:
            dst = Colors.getCMYKColorDifference1(color, this.color);
            break;
        default:
            dst = Colors.getColorDifference(color, this.color);
            break;
        }
        return dst;
    }

    /**
     * 
     * @param Erlaubter
     *            Farbunterschied
     */
    public void setDistance(int distance) {
        this.distance = distance;
    }

    @Override
    public Object clone() {
        CPoint ret = new CPoint(x, y, distance, color);
        ret.colorDifferenceMode = colorDifferenceMode;
        ret.foreground = foreground;
        return ret;
    }

    /**
     * prüft ob die farbe und
     */
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) || (obj != null && obj instanceof CPoint && ((CPoint) obj).color == color);
    }
}
