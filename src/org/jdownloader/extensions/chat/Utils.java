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

package org.jdownloader.extensions.chat;

import java.awt.Color;

import jd.nutils.encoding.HTMLEntities;

public class Utils {
    public static String getRandomColor() {

        String col = Integer.toHexString(new Color((int) (Math.random() * 0xffffff)).darker().getRGB());
        while (col.length() < 6) {
            col = "0" + col;
        }
        return col.substring(col.length() - 6);
    }

    public static String prepareMsg(String msg) {
        msg = HTMLEntities.htmlAngleBrackets(msg + " ");
        String tmp = msg.replaceAll("((http://)|(www\\.))([^\\s\"]+)", "<a href=\"http://$3$4\">$3$4</a>").trim();
        return tmp;

    }
}
