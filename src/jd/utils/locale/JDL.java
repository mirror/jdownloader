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

package jd.utils.locale;

import java.util.HashMap;
import java.util.Random;

import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;

import org.jdownloader.logging.LogController;

public final class JDL {
    /**
     * Don't let anyone instantiate this class.
     */
    private JDL() {

    }

    private static final HashMap<String, JDLocale> CACHE          = new HashMap<String, JDLocale>();

    public static final String                     CONFIG         = "LOCALE";

    private static String                          COUNTRY_CODE   = null;

    public static boolean                          DEBUG          = false;

    public static final JDLocale                   DEFAULT_LOCALE = JDL.getInstance("en");

    /**
     * returns the correct country code
     * 
     * @return
     */
    public static String getCountryCodeByIP() {
        if (COUNTRY_CODE != null) return COUNTRY_CODE;

        if ((COUNTRY_CODE = SubConfiguration.getConfig(JDL.CONFIG).getStringProperty("DEFAULTLANGUAGE", null)) != null) { return COUNTRY_CODE; }
        final Browser br = new Browser();
        br.setFollowRedirects(true);
        br.setConnectTimeout(10000);
        br.setReadTimeout(10000);
        try {
            COUNTRY_CODE = br.getPage("http://www.jdownloader.org/advert/getLanguage.php?id=" + System.currentTimeMillis() + new Random(System.currentTimeMillis()).nextLong());
            if (!br.getRequest().getHttpConnection().isOK()) {
                COUNTRY_CODE = null;
            } else {
                COUNTRY_CODE = COUNTRY_CODE.trim().toUpperCase();

                SubConfiguration.getConfig(JDL.CONFIG).setProperty("DEFAULTLANGUAGE", COUNTRY_CODE);
                SubConfiguration.getConfig(JDL.CONFIG).save();
            }
        } catch (Exception e) {
            COUNTRY_CODE = null;
        }
        return COUNTRY_CODE;
    }

    /**
     * Creates a new JDLocale instance or uses a cached one
     * 
     * @param lngGeoCode
     * @return
     */
    public synchronized static JDLocale getInstance(final String lngGeoCode) {
        JDLocale ret;
        if ((ret = CACHE.get(lngGeoCode)) != null) return ret;
        ret = new JDLocale(lngGeoCode);
        CACHE.put(lngGeoCode, ret);
        return ret;
    }

    public static boolean isGerman() {
        final String country = System.getProperty("user.country");
        return country != null && country.equalsIgnoreCase("DE");
    }

    public static String L(final String key, final String def) {

        return def;
    }

    /**
     * Wrapper für String.format(JDL.L(..),args)
     * 
     * @param KEY
     * @param def
     * @param args
     * @return
     */
    public static String LF(final String key, final String def, final Object... args) {
        if (DEBUG) return key;
        if (args == null || args.length == 0) {
            LogController.CL().severe("FIXME: " + key);
        }
        try {
            return String.format(JDL.L(key, def), args);
        } catch (Exception e) {
            LogController.CL().severe("FIXME: " + key);
            return "FIXME: " + key;
        }
    }

    public static String translate(final String from, final String to, final String msg) {
        try {
            final Browser br = new Browser();
            br.getPage("http://www.google.com/uds/Gtranslate?callback=google.language.callbacks.id101&context=22&q=" + Encoding.urlEncode(msg) + "&langpair=|en&key=notsupplied&v=1.0");
            String[] match = br.getRegex("\"translatedText\":\"(.*?)\",\"detectedSourceLanguage\":\"(.*?)\"").getRow(0);
            return Encoding.UTF8Decode(Encoding.htmlDecode(match[0]));
        } catch (Exception e) {
            LogController.CL().log(e);
            return null;
        }
    }

}