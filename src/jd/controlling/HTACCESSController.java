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

package jd.controlling;

import jd.http.Browser;
import jd.nutils.encoding.Encoding;

import org.appwork.utils.Regex;

/**
 * @Deprecated Only here for stable plugin compatibility
 */
public class HTACCESSController {

    private final static HTACCESSController INSTANCE = new HTACCESSController();

    private HTACCESSController() {
    }

    public static String[] getUserDatafromBasicauth(String basicauth) {
        if (basicauth == null || basicauth.length() == 0) return null;
        if (basicauth.startsWith("Basic")) basicauth = new Regex(basicauth, "Basic (.*?)$").getMatch(0);
        basicauth = Encoding.Base64Decode(basicauth);
        final String[] dat = new Regex(basicauth, ("(.*?):(.*?)$")).getRow(0);
        return new String[] { dat[0], dat[1] };
    }

    public void add(final String url, final String basicauth) {
        if (url != null && url.length() > 0) {
            final String host = Browser.getHost(url.trim()).toLowerCase();
            final String[] user = getUserDatafromBasicauth(basicauth);
            if (user == null) return;

        }
    }

    public String get(final String url) {
        if (url != null && url.length() > 0) {
            final String host = Browser.getHost(url.trim()).toLowerCase();

            // return "Basic " + Encoding.Base64Encode(LIST.get(host)[0] + ":" +
            // LIST.get(host)[1]);

        }
        return null;
    }

    public void remove(final String url) {

    }

    @Deprecated
    public static HTACCESSController getInstance() {
        return INSTANCE;
    }

}
