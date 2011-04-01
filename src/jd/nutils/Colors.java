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

package jd.nutils;

import java.awt.Color;

public class Colors {
    /**
     * Returns the same color, but with the given alpha channel
     * 
     * @param color
     * @param alpha
     *            (0-100)
     * @return
     */
    public static Color getColor(Color color, int alpha) {
        alpha *= 255;
        alpha /= 100;

        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /**
     * converts a rgb integer to cmyk Cyan, Magenta, Yellow and Key(black)
     * 
     * @param rgb
     * @return
     */
    public static float[] rgb2cmyk(int rgb) {
        float r = (rgb >> 16) & 0xFF;
        float g = (rgb >> 8) & 0xFF;
        float b = rgb & 0xFF;
        float C = 1.0f - (r / 255);
        float M = 1.0f - (g / 255);
        float Y = 1.0f - (b / 255);
        float var_K = 1;

        if (C < var_K) var_K = C;
        if (M < var_K) var_K = M;
        if (Y < var_K) var_K = Y;

        C = (C - var_K) / (1 - var_K);
        M = (M - var_K) / (1 - var_K);
        Y = (Y - var_K) / (1 - var_K);
        return new float[] { C * 100, M * 100, Y * 100, var_K * 100 };
    }

    /**
     * Rechnet RGB werte in den LAB Farbraum um. Der LAB Farbraum wird vor allem
     * zu Farbabstandsberechnungen verwendet Wert für L* enthält die
     * Helligkeitsinformation, und hat einen Wertebereich von 0 bis 100. Die
     * Werte a* und b* enthalten die Farbinformation. a* steht für die
     * Farbbalance zwischen Grün und Rot, b* steht für die Farbbalance zwischen
     * Blau und Gelb. a* und b* haben einen Wertebereich von -120 bis + 120.
     * 
     * @param R
     * @param G
     * @param B
     * @return int[] LAB Farbwerte
     */
    public static int[] rgb2lab(int R, int G, int B) {
        // http://www.brucelindbloom.com
        int[] lab = new int[3];
        float r, g, b, X, Y, Z, fx, fy, fz, xr, yr, zr;
        float Ls, as, bs;
        float eps = 216.f / 24389.f;
        float k = 24389.f / 27.f;

        float Xr = 0.964221f; // reference white D50
        float Yr = 1.0f;
        float Zr = 0.825211f;

        // RGB to XYZ
        r = R / 255.f; // R 0..1
        g = G / 255.f; // G 0..1
        b = B / 255.f; // B 0..1

        // assuming sRGB (D65)
        if (r <= 0.04045) {
            r = r / 12;
        } else {
            r = (float) Math.pow((r + 0.055) / 1.055, 2.4);
        }

        if (g <= 0.04045) {
            g = g / 12;
        } else {
            g = (float) Math.pow((g + 0.055) / 1.055, 2.4);
        }

        if (b <= 0.04045) {
            b = b / 12;
        } else {
            b = (float) Math.pow((b + 0.055) / 1.055, 2.4);
        }

        X = 0.436052025f * r + 0.385081593f * g + 0.143087414f * b;
        Y = 0.222491598f * r + 0.71688606f * g + 0.060621486f * b;
        Z = 0.013929122f * r + 0.097097002f * g + 0.71418547f * b;

        // XYZ to Lab
        xr = X / Xr;
        yr = Y / Yr;
        zr = Z / Zr;

        if (xr > eps) {
            fx = (float) Math.pow(xr, 1 / 3.);
        } else {
            fx = (float) ((k * xr + 16.) / 116.);
        }

        if (yr > eps) {
            fy = (float) Math.pow(yr, 1 / 3.);
        } else {
            fy = (float) ((k * yr + 16.) / 116.);
        }

        if (zr > eps) {
            fz = (float) Math.pow(zr, 1 / 3.);
        } else {
            fz = (float) ((k * zr + 16.) / 116);
        }

        Ls = 116 * fy - 16;
        as = 500 * (fx - fy);
        bs = 200 * (fy - fz);

        lab[0] = (int) (2.55 * Ls + .5);
        lab[1] = (int) (as + .5);
        lab[2] = (int) (bs + .5);
        return lab;
    }

    /**
     * Wandelt einen farbwert vom RGB Farbraum in HSB um (Hue, Saturation,
     * Brightness)
     * 
     * @param r
     * @param g
     * @param b
     * @return hsb Farbwerte
     */
    public static float[] rgb2hsb(int r, int g, int b) {
        float[] hsbvals = new float[3];
        Color.RGBtoHSB(r, g, b, hsbvals);
        return hsbvals;
    }

    public static float[] rgb2hsb(int pixelValue) {
        int[] rgbA = Colors.hexToRgb(pixelValue);
        return rgb2hsb(rgbA[0], rgbA[1], rgbA[2]);
    }

    /**
     * this is hsv we know from gimp or photoshop (hsb[0]*360 hsb[1]*100
     * hsb[2]*100)
     * 
     * @param pixelValue
     * @return
     */
    public static int[] rgb2hsv(int pixelValue) {
        int[] rgbA = Colors.hexToRgb(pixelValue);
        float[] hsb = rgb2hsb(rgbA[0], rgbA[1], rgbA[2]);
        return new int[] { Math.round(hsb[0] * 360), Math.round(hsb[1] * 100), Math.round(hsb[2] * 100) };
    }

    /**
     * Wandelt ein RGB Array in die zugehöroge decimale hexzahl um.
     * 
     * @param value
     * @return 32 BIt Farbwert
     */
    public static int rgbToHex(int[] value) {
        return (value[0] << 16) + (value[1] << 8) + value[2];
    }

    /**
     * Mischt zwei decimal zahlen. dabei werden ga und gb als
     * gewichtungsfaktoren verwendet mixColors(0xff0000,0x00ff00,3,1) mischt 3
     * Teile Rot und einen teil grün
     * 
     * @param a
     * @param b
     * @param ga
     * @param gb
     * @return 32 Bit Farbwert
     */
    public static int mixColors(int a, int b, int ga, int gb) {

        int[] av = Colors.hexToRgb(a);
        int[] bv = Colors.hexToRgb(b);
        int R, G, B;

        R = (av[0] * ga + bv[0] * gb) / (ga + gb);
        G = (av[1] * ga + bv[1] * gb) / (ga + gb);
        B = (av[2] * ga + bv[2] * gb) / (ga + gb);

        return rgbToHex(new int[] { R, G, B });

    }

    /**
     * Mischt zwei Fraben 1:1
     * 
     * @param a
     * @param b
     * @return 32 Bit Farbwert
     */
    public static int mixColors(int a, int b) {
        int[] av = Colors.hexToRgb(a);
        int[] bv = Colors.hexToRgb(b);
        int[] ret = { (int) (((long) av[0] + (long) bv[0]) >> 1), (int) (((long) av[1] + (long) bv[1]) >> 1), (int) (((long) av[2] + (long) bv[2]) >> 1) };
        return rgbToHex(ret);
    }

    /**
     * Wandelt eine decimale hexzahl(0-256^3) in die 3 RGB Farbkomponenten um.
     * 
     * @param value
     * @return RGB Werte
     */
    public static int[] hexToRgb(int value) {
        // int[] v = { (value / 65536), ((value - value / 65536 * 65536) / 256),
        // (value - value / 65536 * 65536 - (value - value / 65536 * 65536) /
        // 256 * 256), 0 };

        return new int[] { (value >> 16) & 0xFF, (value >> 8) & 0xFF, value & 0xFF, 0 };
    }

    public static int[] getRGB(int a) {
        Color aa = new Color(a);
        return new int[] { aa.getRed(), aa.getGreen(), aa.getBlue() };
    }

    /**
     * unterschied des Farbtons
     * 
     * @param a
     * @param b
     * @return
     */
    public static double getHueColorDifference(int a, int b) {
        if (a == b) return 0;
        return getHueColorDifference(getRGB(a), getRGB(b));

    }

    /**
     * unterschied des Farbtons
     * 
     * @param a
     * @param b
     * @return
     */
    public static double getHueColorDifference(int[] rgbA, int[] rgbB) {
        float hsbA = rgb2hsb(rgbA[0], rgbA[1], rgbA[2])[0] * 100;
        float hsbB = rgb2hsb(rgbB[0], rgbB[1], rgbB[2])[0] * 100;
        double dif = Math.abs((double) (hsbA - hsbB));
        return dif;
    }

    /**
     * unterschied des Farbtons | hue*360° entspricht wirklichen hue werten
     * 
     * @param a
     * @param b
     * @return
     */
    public static double getHueColorDifference360(int a, int b) {
        if (a == b) return 0;
        return getHueColorDifference360(getRGB(a), getRGB(b));

    }

    /**
     * unterschied des Farbtons | hue*360° entspricht wirklichen hue werten
     * 
     * @param a
     * @param b
     * @return
     */
    public static double getHueColorDifference360(int[] rgbA, int[] rgbB) {
        float hsbA = rgb2hsb(rgbA[0], rgbA[1], rgbA[2])[0] * 360;
        float hsbB = rgb2hsb(rgbB[0], rgbB[1], rgbB[2])[0] * 360;
        double dif = Math.abs((double) (hsbA - hsbB));
        return dif;
    }

    /**
     * unterschied des Farbsättigung
     * 
     * @param a
     * @param b
     * @return
     */
    public static double getSaturationColorDifference(int a, int b) {
        if (a == b) return 0;
        return getSaturationColorDifference(getRGB(a), getRGB(b));

    }

    /**
     * unterschied des Farbsättigung
     * 
     * @param a
     * @param b
     * @return
     */
    public static double getSaturationColorDifference(int[] rgbA, int[] rgbB) {
        float hsbA = rgb2hsb(rgbA[0], rgbA[1], rgbA[2])[1] * 100;
        float hsbB = rgb2hsb(rgbB[0], rgbB[1], rgbB[2])[1] * 100;
        double dif = Math.abs((double) (hsbA - hsbB));
        return dif;
    }

    /**
     * unterschied der absoluten Helligkeit
     * 
     * @param a
     * @param b
     * @return
     */
    public static double getBrightnessColorDifference(int a, int b) {
        if (a == b) return 0;
        return getBrightnessColorDifference(getRGB(a), getRGB(b));

    }

    /**
     * unterschied der absoluten Helligkeit
     * 
     * @param a
     * @param b
     * @return
     */
    public static double getBrightnessColorDifference(int[] rgbA, int[] rgbB) {
        float hsbA = rgb2hsb(rgbA[0], rgbA[1], rgbA[2])[2] * 100;
        float hsbB = rgb2hsb(rgbB[0], rgbB[1], rgbB[2])[2] * 100;
        double dif = Math.abs((double) (hsbA - hsbB));
        return dif;
    }

    /**
     * Farbunterschied durch LAB raum berechnet
     * 
     * @param a
     * @param b
     * @return
     */
    public static double getColorDifference(int[] rgbA, int[] rgbB) {
        int[] labA = rgb2lab(rgbA[0], rgbA[1], rgbA[2]);
        int[] labB = rgb2lab(rgbB[0], rgbB[1], rgbB[2]);
        int dif0 = labA[0] - labB[0];
        int dif1 = labA[1] - labB[1];
        int dif2 = labA[2] - labB[2];
        return Math.sqrt(dif0 * dif0 + dif1 * dif1 + dif2 * dif2);
    }

    public static double getColorDifference(int a, int b) {
        if (a == b) return 0;
        return getColorDifference(getRGB(a), getRGB(b));
    }

    public static int getRGBDistance(int color) {
        Color c = new Color(color);
        int br = Math.abs(c.getBlue() - c.getRed());
        int bg = Math.abs(c.getBlue() - c.getGreen());
        int rg = Math.abs(c.getGreen() - c.getRed());

        return (br + bg + rg) / 3;

    }

    /**
     * Schnell berrechneter Farbunterschied im RGB Raum nicht wirklich gut aber
     * da der Farbunterschied unter umständen so berechnet wurde nützlich
     * 
     * @param color
     * @param color2
     * @return
     */
    public static double getRGBColorDifference2(int color, int color2) {
        if (color == color2) return 0;
        Color c = new Color(color);
        Color c2 = new Color(color2);
        int dif0 = c.getRed() - c2.getRed();
        int dif1 = c.getGreen() - c2.getGreen();
        int dif2 = c.getBlue() - c2.getBlue();
        return (Math.abs(dif0) + Math.abs(dif1) + Math.abs(dif2)) / 3;
    }

    /**
     * Schnell berrechneter Farbunterschied im RGB Raum nicht wirklich gut aber
     * da der Farbunterschied unter umständen so berechnet wurde nützlich
     * 
     * @param color
     * @param color2
     * @return
     */
    public static double getRGBColorDifference3(int color, int color2) {
        if (color == color2) return 0;
        Color c = new Color(color);
        Color c2 = new Color(color2);
        int dif0 = c.getRed() - c2.getRed();
        int dif1 = c.getGreen() - c2.getGreen();
        int dif2 = c.getBlue() - c2.getBlue();
        return Math.max(Math.abs(dif0), Math.max(Math.abs(dif1), Math.abs(dif2)));
    }

    /**
     * Farbunterschied im RGB Raum
     * 
     * @param color
     * @param color2
     * @return
     */
    public static double getRGBColorDifference1(int color, int color2) {
        if (color == color2) return 0;
        Color c = new Color(color);
        Color c2 = new Color(color2);
        int dif0 = c.getRed() - c2.getRed();
        int dif1 = c.getGreen() - c2.getGreen();
        int dif2 = c.getBlue() - c2.getBlue();
        return Math.sqrt(dif0 * dif0 + dif1 * dif1 + dif2 * dif2);
    }

    /**
     * CMYK Colordifference
     * 
     * @param rgbcolor
     * @param rgbcolor2
     * @return
     */
    public static double getCMYKColorDifference1(int rgbcolor, int rgbcolor2) {
        if (rgbcolor == rgbcolor2) return 0;
        float[] cmyk1 = rgb2cmyk(rgbcolor);
        float[] cmyk2 = rgb2cmyk(rgbcolor2);
        double dif0 = cmyk1[0] - cmyk2[0];
        double dif1 = cmyk1[1] - cmyk2[1];
        double dif2 = cmyk1[2] - cmyk2[2];
        double dif3 = cmyk1[3] - cmyk2[3];
        return Math.sqrt(dif0 * dif0 + dif1 * dif1 + dif2 * dif2 + dif3 * dif3);
    }

}
