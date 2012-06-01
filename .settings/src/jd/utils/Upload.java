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

package jd.utils;

import java.io.IOException;

import jd.controlling.JDLogger;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;

public class Upload {

    public static String toJDownloader(String str, String desc) {
        try {
            Browser br = new Browser();
            br.postPage("http://jdownloader.org/pastebin", "version=2&upload=1&desc=" + Encoding.urlEncode(desc) + "&log=" + Encoding.urlEncode(str));
            return br.getRegex("<pastebinurl>(.*?)</pastebinurl>").getMatch(0);
        } catch (IOException e) {
            JDLogger.exception(e);
        }
        return null;
    }

}