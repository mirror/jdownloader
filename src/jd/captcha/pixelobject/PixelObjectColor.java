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

package jd.captcha.pixelobject;

public class PixelObjectColor implements Comparable<PixelObjectColor> {
    public int color = 0;
    public int count = 1;

    public PixelObjectColor(int color) {
        this.color = color;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof PixelObjectColor && ((PixelObjectColor) obj).color == color;
    }

    public int compareTo(PixelObjectColor o) {
        return Integer.valueOf(o.count).compareTo(count);
    }

    @Override
    public String toString() {
        return "color:#" + Integer.toHexString(color) + " count:" + count;
    }
}
