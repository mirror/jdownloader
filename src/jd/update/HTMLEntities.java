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

package jd.update;

import java.util.Hashtable;

/**
 * Collection of static methods to convert special and extended characters into
 * HTML entitities and vice versa.<br>
 * <br>
 * Copyright (c) 2004-2005 Tecnick.com S.r.l (www.tecnick.com) Via Ugo Foscolo
 * n.19 - 09045 Quartu Sant'Elena (CA) - ITALY - www.tecnick.com -
 * info@tecnick.com<br>
 * Project homepage: <a href="http://htmlentities.sourceforge.net"
 * target="_blank">http://htmlentities.sourceforge.net</a><br>
 * License: http://www.gnu.org/copyleft/lesser.html LGPL
 * 
 * @author Nicola Asuni [www.tecnick.com].
 * @version 1.0.004
 */
public class HTMLEntities {

    /**
     * Translation table for HTML entities.<br>
     * reference: W3C - Character entity references in HTML 4 [<a
     * href="http://www.w3.org/TR/html401/sgml/entities.html"
     * target="_blank">http://www.w3.org/TR/html401/sgml/entities.html</a>].
     */
    private static final Object[][] html_entities_table = { { new String("&Aacute;"), new Integer(193) }, { new String("&aacute;"), new Integer(225) }, { new String("&Acirc;"), new Integer(194) }, { new String("&acirc;"), new Integer(226) }, { new String("&acute;"), new Integer(180) }, { new String("&AElig;"), new Integer(198) }, { new String("&aelig;"), new Integer(230) }, { new String("&Agrave;"), new Integer(192) }, { new String("&agrave;"), new Integer(224) }, { new String("&alefsym;"), new Integer(8501) }, { new String("&Alpha;"), new Integer(913) }, { new String("&alpha;"), new Integer(945) }, { new String("&amp;"), new Integer(38) }, { new String("&and;"), new Integer(8743) }, { new String("&ang;"), new Integer(8736) }, { new String("&Aring;"), new Integer(197) }, { new String("&aring;"), new Integer(229) }, { new String("&asymp;"), new Integer(8776) },
            { new String("&Atilde;"), new Integer(195) }, { new String("&atilde;"), new Integer(227) }, { new String("&Auml;"), new Integer(196) }, { new String("&auml;"), new Integer(228) }, { new String("&bdquo;"), new Integer(8222) }, { new String("&Beta;"), new Integer(914) }, { new String("&beta;"), new Integer(946) }, { new String("&brvbar;"), new Integer(166) }, { new String("&bull;"), new Integer(8226) }, { new String("&cap;"), new Integer(8745) }, { new String("&Ccedil;"), new Integer(199) }, { new String("&ccedil;"), new Integer(231) }, { new String("&cedil;"), new Integer(184) }, { new String("&cent;"), new Integer(162) }, { new String("&Chi;"), new Integer(935) }, { new String("&chi;"), new Integer(967) }, { new String("&circ;"), new Integer(710) }, { new String("&clubs;"), new Integer(9827) }, { new String("&cong;"), new Integer(8773) },
            { new String("&copy;"), new Integer(169) }, { new String("&crarr;"), new Integer(8629) }, { new String("&cup;"), new Integer(8746) }, { new String("&curren;"), new Integer(164) }, { new String("&dagger;"), new Integer(8224) }, { new String("&Dagger;"), new Integer(8225) }, { new String("&darr;"), new Integer(8595) }, { new String("&dArr;"), new Integer(8659) }, { new String("&deg;"), new Integer(176) }, { new String("&Delta;"), new Integer(916) }, { new String("&delta;"), new Integer(948) }, { new String("&diams;"), new Integer(9830) }, { new String("&divide;"), new Integer(247) }, { new String("&Eacute;"), new Integer(201) }, { new String("&eacute;"), new Integer(233) }, { new String("&Ecirc;"), new Integer(202) }, { new String("&ecirc;"), new Integer(234) }, { new String("&Egrave;"), new Integer(200) }, { new String("&egrave;"), new Integer(232) },
            { new String("&empty;"), new Integer(8709) }, { new String("&emsp;"), new Integer(8195) }, { new String("&ensp;"), new Integer(8194) }, { new String("&Epsilon;"), new Integer(917) }, { new String("&epsilon;"), new Integer(949) }, { new String("&equiv;"), new Integer(8801) }, { new String("&Eta;"), new Integer(919) }, { new String("&eta;"), new Integer(951) }, { new String("&ETH;"), new Integer(208) }, { new String("&eth;"), new Integer(240) }, { new String("&Euml;"), new Integer(203) }, { new String("&euml;"), new Integer(235) }, { new String("&euro;"), new Integer(8364) }, { new String("&exist;"), new Integer(8707) }, { new String("&fnof;"), new Integer(402) }, { new String("&forall;"), new Integer(8704) }, { new String("&frac12;"), new Integer(189) }, { new String("&frac14;"), new Integer(188) }, { new String("&frac34;"), new Integer(190) },
            { new String("&frasl;"), new Integer(8260) }, { new String("&Gamma;"), new Integer(915) }, { new String("&gamma;"), new Integer(947) }, { new String("&ge;"), new Integer(8805) }, { new String("&harr;"), new Integer(8596) }, { new String("&hArr;"), new Integer(8660) }, { new String("&hearts;"), new Integer(9829) }, { new String("&hellip;"), new Integer(8230) }, { new String("&Iacute;"), new Integer(205) }, { new String("&iacute;"), new Integer(237) }, { new String("&Icirc;"), new Integer(206) }, { new String("&icirc;"), new Integer(238) }, { new String("&iexcl;"), new Integer(161) }, { new String("&Igrave;"), new Integer(204) }, { new String("&igrave;"), new Integer(236) }, { new String("&image;"), new Integer(8465) }, { new String("&infin;"), new Integer(8734) }, { new String("&int;"), new Integer(8747) }, { new String("&Iota;"), new Integer(921) },
            { new String("&iota;"), new Integer(953) }, { new String("&iquest;"), new Integer(191) }, { new String("&isin;"), new Integer(8712) }, { new String("&Iuml;"), new Integer(207) }, { new String("&iuml;"), new Integer(239) }, { new String("&Kappa;"), new Integer(922) }, { new String("&kappa;"), new Integer(954) }, { new String("&Lambda;"), new Integer(923) }, { new String("&lambda;"), new Integer(955) }, { new String("&lang;"), new Integer(9001) }, { new String("&laquo;"), new Integer(171) }, { new String("&larr;"), new Integer(8592) }, { new String("&lArr;"), new Integer(8656) }, { new String("&lceil;"), new Integer(8968) }, { new String("&ldquo;"), new Integer(8220) }, { new String("&le;"), new Integer(8804) }, { new String("&lfloor;"), new Integer(8970) }, { new String("&lowast;"), new Integer(8727) }, { new String("&loz;"), new Integer(9674) },
            { new String("&lrm;"), new Integer(8206) }, { new String("&lsaquo;"), new Integer(8249) }, { new String("&lsquo;"), new Integer(8216) }, { new String("&macr;"), new Integer(175) }, { new String("&mdash;"), new Integer(8212) }, { new String("&micro;"), new Integer(181) }, { new String("&middot;"), new Integer(183) }, { new String("&minus;"), new Integer(8722) }, { new String("&Mu;"), new Integer(924) }, { new String("&mu;"), new Integer(956) }, { new String("&nabla;"), new Integer(8711) }, { new String("&nbsp;"), new Integer(160) }, { new String("&ndash;"), new Integer(8211) }, { new String("&ne;"), new Integer(8800) }, { new String("&ni;"), new Integer(8715) }, { new String("&not;"), new Integer(172) }, { new String("&notin;"), new Integer(8713) }, { new String("&nsub;"), new Integer(8836) }, { new String("&Ntilde;"), new Integer(209) },
            { new String("&ntilde;"), new Integer(241) }, { new String("&Nu;"), new Integer(925) }, { new String("&nu;"), new Integer(957) }, { new String("&Oacute;"), new Integer(211) }, { new String("&oacute;"), new Integer(243) }, { new String("&Ocirc;"), new Integer(212) }, { new String("&ocirc;"), new Integer(244) }, { new String("&OElig;"), new Integer(338) }, { new String("&oelig;"), new Integer(339) }, { new String("&Ograve;"), new Integer(210) }, { new String("&ograve;"), new Integer(242) }, { new String("&oline;"), new Integer(8254) }, { new String("&Omega;"), new Integer(937) }, { new String("&omega;"), new Integer(969) }, { new String("&Omicron;"), new Integer(927) }, { new String("&omicron;"), new Integer(959) }, { new String("&oplus;"), new Integer(8853) }, { new String("&or;"), new Integer(8744) }, { new String("&ordf;"), new Integer(170) },
            { new String("&ordm;"), new Integer(186) }, { new String("&Oslash;"), new Integer(216) }, { new String("&oslash;"), new Integer(248) }, { new String("&Otilde;"), new Integer(213) }, { new String("&otilde;"), new Integer(245) }, { new String("&otimes;"), new Integer(8855) }, { new String("&Ouml;"), new Integer(214) }, { new String("&ouml;"), new Integer(246) }, { new String("&para;"), new Integer(182) }, { new String("&part;"), new Integer(8706) }, { new String("&permil;"), new Integer(8240) }, { new String("&perp;"), new Integer(8869) }, { new String("&Phi;"), new Integer(934) }, { new String("&phi;"), new Integer(966) }, { new String("&Pi;"), new Integer(928) }, { new String("&pi;"), new Integer(960) }, { new String("&piv;"), new Integer(982) }, { new String("&plusmn;"), new Integer(177) }, { new String("&pound;"), new Integer(163) },
            { new String("&prime;"), new Integer(8242) }, { new String("&Prime;"), new Integer(8243) }, { new String("&prod;"), new Integer(8719) }, { new String("&prop;"), new Integer(8733) }, { new String("&Psi;"), new Integer(936) }, { new String("&psi;"), new Integer(968) }, { new String("&radic;"), new Integer(8730) }, { new String("&rang;"), new Integer(9002) }, { new String("&raquo;"), new Integer(187) }, { new String("&rarr;"), new Integer(8594) }, { new String("&rArr;"), new Integer(8658) }, { new String("&rceil;"), new Integer(8969) }, { new String("&rdquo;"), new Integer(8221) }, { new String("&real;"), new Integer(8476) }, { new String("&reg;"), new Integer(174) }, { new String("&rfloor;"), new Integer(8971) }, { new String("&Rho;"), new Integer(929) }, { new String("&rho;"), new Integer(961) }, { new String("&rlm;"), new Integer(8207) },
            { new String("&rsaquo;"), new Integer(8250) }, { new String("&rsquo;"), new Integer(8217) }, { new String("&sbquo;"), new Integer(8218) }, { new String("&Scaron;"), new Integer(352) }, { new String("&scaron;"), new Integer(353) }, { new String("&sdot;"), new Integer(8901) }, { new String("&sect;"), new Integer(167) }, { new String("&shy;"), new Integer(173) }, { new String("&Sigma;"), new Integer(931) }, { new String("&sigma;"), new Integer(963) }, { new String("&sigmaf;"), new Integer(962) }, { new String("&sim;"), new Integer(8764) }, { new String("&spades;"), new Integer(9824) }, { new String("&sub;"), new Integer(8834) }, { new String("&sube;"), new Integer(8838) }, { new String("&sum;"), new Integer(8721) }, { new String("&sup1;"), new Integer(185) }, { new String("&sup2;"), new Integer(178) }, { new String("&sup3;"), new Integer(179) },
            { new String("&sup;"), new Integer(8835) }, { new String("&supe;"), new Integer(8839) }, { new String("&szlig;"), new Integer(223) }, { new String("&Tau;"), new Integer(932) }, { new String("&tau;"), new Integer(964) }, { new String("&there4;"), new Integer(8756) }, { new String("&Theta;"), new Integer(920) }, { new String("&theta;"), new Integer(952) }, { new String("&thetasym;"), new Integer(977) }, { new String("&thinsp;"), new Integer(8201) }, { new String("&THORN;"), new Integer(222) }, { new String("&thorn;"), new Integer(254) }, { new String("&tilde;"), new Integer(732) }, { new String("&times;"), new Integer(215) }, { new String("&trade;"), new Integer(8482) }, { new String("&Uacute;"), new Integer(218) }, { new String("&uacute;"), new Integer(250) }, { new String("&uarr;"), new Integer(8593) }, { new String("&uArr;"), new Integer(8657) },
            { new String("&Ucirc;"), new Integer(219) }, { new String("&ucirc;"), new Integer(251) }, { new String("&Ugrave;"), new Integer(217) }, { new String("&ugrave;"), new Integer(249) }, { new String("&uml;"), new Integer(168) }, { new String("&upsih;"), new Integer(978) }, { new String("&Upsilon;"), new Integer(933) }, { new String("&upsilon;"), new Integer(965) }, { new String("&Uuml;"), new Integer(220) }, { new String("&uuml;"), new Integer(252) }, { new String("&weierp;"), new Integer(8472) }, { new String("&Xi;"), new Integer(926) }, { new String("&xi;"), new Integer(958) }, { new String("&Yacute;"), new Integer(221) }, { new String("&yacute;"), new Integer(253) }, { new String("&yen;"), new Integer(165) }, { new String("&yuml;"), new Integer(255) }, { new String("&Yuml;"), new Integer(376) }, { new String("&Zeta;"), new Integer(918) },
            { new String("&zeta;"), new Integer(950) }, { new String("&zwj;"), new Integer(8205) }, { new String("&zwnj;"), new Integer(8204) } };

    /**
     * Map to convert html entities in exteden characters.
     */
    private static final Hashtable<String, Integer> unhtmlentities_map = new Hashtable<String, Integer>();

    // ==============================================================================
    // METHODS
    // ==============================================================================

    /**
     * Initialize HTML entities table.
     */
    private static void initializeEntitiesTables() {
        // initialize html translation maps
        for (int i = 0; i < html_entities_table.length; ++i) {
            unhtmlentities_map.put((String) html_entities_table[i][0], (Integer) html_entities_table[i][1]);
        }
    }

    /**
     * Convert HTML entities to special and extended unicode characters
     * equivalents.
     * 
     * @param str
     *            input string
     * @return formatted string
     */
    public static String unhtmlentities(String str) {

        // initialize html translation maps table the first time is called
        if (unhtmlentities_map.isEmpty()) {
            HTMLEntities.initializeEntitiesTables();
        }

        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < str.length(); ++i) {
            char ch = str.charAt(i);
            if (ch == '&') {
                int semi = str.indexOf(';', i + 1);
                if (semi == -1 || semi - i > 7) {
                    buf.append(ch);
                    continue;
                }
                String entity = str.substring(i, semi + 1);
                Integer iso;
                if (entity.charAt(1) == ' ') {
                    buf.append(ch);
                    continue;
                }
                if (entity.charAt(1) == '#') {
                    if (entity.charAt(2) == 'x') {
                        iso = new Integer(Integer.parseInt(entity.substring(3, entity.length() - 1), 16));
                    } else {
                        iso = new Integer(entity.substring(2, entity.length() - 1));
                    }
                } else {
                    iso = unhtmlentities_map.get(entity);
                }
                if (iso == null) {
                    buf.append(entity);
                } else {
                    buf.append((char) iso.intValue());
                }
                i = semi;
            } else {
                buf.append(ch);
            }
        }
        return buf.toString();
    }

}