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

package tests.singletests;

import static org.junit.Assert.assertFalse;
import jd.utils.JDGeoCode;

import org.junit.Test;

public class JDGeoCodeTest {

    @Test
    public void geoCodeTest() {

        String lng;
        String[] res;
        String longLng, reshort;

        /* malformed */
        lng = "english";
        res = JDGeoCode.parseLanguageCode(lng);
        assertFalse("Result error for " + lng, res != null);

        /* language and country aand extension */
        lng = "en-GB-2";
        res = JDGeoCode.parseLanguageCode(lng);
        assertFalse("Result error for " + lng, res == null);
        assertFalse("Result error for " + lng, !res[0].equals("en"));
        assertFalse("Result error for " + lng, !res[1].equals("GB"));
        assertFalse("Result error for " + lng, !res[2].equals("2"));
        longLng = JDGeoCode.toLongerNative(lng);
        assertFalse("Result error for " + lng + "", !"English [United Kingdom (UK) | 2]".equals(longLng));
        reshort = JDGeoCode.longToShort(longLng);
        assertFalse("Result error for " + longLng + "->" + lng, !lng.equals(reshort));

        /* language and country */
        lng = "en-GB";
        res = JDGeoCode.parseLanguageCode(lng);
        assertFalse("Result error for " + lng, res == null);
        assertFalse("Result error for " + lng, !res[0].equals("en"));
        assertFalse("Result error for " + lng, !res[1].equals("GB"));
        assertFalse("Result error for " + lng, res[2] != null);
        longLng = JDGeoCode.toLongerNative(lng);
        assertFalse("Result error for " + lng + "", !"English [United Kingdom (UK)]".equals(longLng));
        reshort = JDGeoCode.longToShort(longLng);
        assertFalse("Result error for " + longLng + "->" + lng, !lng.equals(reshort));

        lng = "zh-hant";
        res = JDGeoCode.parseLanguageCode(lng);
        assertFalse("Result error for " + lng, res == null);
        assertFalse("Result error for " + lng, !res[0].equals("zh"));
        assertFalse("Result error for " + lng, res[1] != null);
        assertFalse("Result error for " + lng, !res[2].equals("hant"));
        longLng = JDGeoCode.toLongerNative(lng);
        assertFalse("Result error for " + lng + "", !"中文 (Zhōngwén), 汉语, 漢語 [traditional]".equals(longLng));
        reshort = JDGeoCode.longToShort(longLng);
        assertFalse("Result error for " + longLng + "->" + lng, !lng.equals(reshort));
        /* language only */
        lng = "en";
        res = JDGeoCode.parseLanguageCode(lng);
        assertFalse("Result error for " + lng, res == null);
        assertFalse("Result error for " + lng, !res[0].equals("en"));
        assertFalse("Result error for " + lng, res[1] != null);
        assertFalse("Result error for " + lng, res[2] != null);

        longLng = JDGeoCode.toLongerNative(lng);
        assertFalse("Result error for " + lng + "", !"English".equals(longLng));
        reshort = JDGeoCode.longToShort(longLng);
        assertFalse("Result error for " + longLng + "->" + lng, !lng.equals(reshort));

        /* DD is not a valid country... is an extension */
        lng = "en-DD";
        res = JDGeoCode.parseLanguageCode(lng);
        assertFalse("Result error for " + lng, res == null);
        assertFalse("Result error for " + lng, !res[0].equals("en"));
        assertFalse("Result error for " + lng, res[1] != null);
        assertFalse("Result error for " + lng, !res[2].equals("DD"));
        longLng = JDGeoCode.toLongerNative(lng);
        assertFalse("Result error for " + lng + "", !"English [DD]".equals(longLng));
        reshort = JDGeoCode.longToShort(longLng);
        assertFalse("Result error for " + longLng + "->" + lng, !lng.equals(reshort));

        /* getNativelanguage */
        assertFalse("Result error for tr->Türkçe", !"Türkçe".equals(JDGeoCode.getNativeLanguage("tr")));
    }

}
