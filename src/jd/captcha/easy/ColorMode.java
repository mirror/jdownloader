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

public class ColorMode {
    /**
     * Array mit allen Farbmodellen aus CPoint
     */
    public static final ColorMode[] cModes = new ColorMode[] { new ColorMode(CPoint.LAB_DIFFERENCE, "RGB3 Difference"), new ColorMode(CPoint.LAB_DIFFERENCE, "LAB Difference"), new ColorMode(CPoint.RGB_DIFFERENCE1, "RGB1 Difference"), new ColorMode(CPoint.RGB_DIFFERENCE2, "RGB2 Difference"), new ColorMode(CPoint.CMYK_DIFFERENCE, "CMYK Difference"), new ColorMode(CPoint.HUE_DIFFERENCE, "Hue Difference"), new ColorMode(CPoint.SATURATION_DIFFERENCE, "Saturation Difference"), new ColorMode(CPoint.BRIGHTNESS_DIFFERENCE, "Brightness Difference"), new ColorMode(CPoint.RED_DIFFERENCE, "Red Difference"), new ColorMode(CPoint.GREEN_DIFFERENCE, "Green Difference"), new ColorMode(CPoint.BLUE_DIFFERENCE, "Blue Difference") };
    /**
     * Farbmodus CPoint.LAB_DIFFERENCE
     */
    protected byte                  mode;
    /**
     * Name des Farbmodells (Für die ComboBox)
     */
    private String                  modeString;

    public ColorMode(byte mode) {
        this.mode = mode;
        for (ColorMode m : cModes) {
            if (mode == m.mode) {
                this.modeString = m.modeString;
                break;
            }
        }
    }

    public ColorMode(byte mode, String modestString) {
        this.mode = mode;
        this.modeString = modestString;
    }

    @Override
    public boolean equals(Object arg0) {
        if ((arg0 == null) || !(arg0 instanceof ColorMode)) return false;
        return mode == ((ColorMode) arg0).mode;
    }

    @Override
    public int hashCode() {
        return mode;
    }

    public String toString() {
        return modeString;
    }
}
