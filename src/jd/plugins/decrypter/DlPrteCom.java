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
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.requests.FormData;
import jd.http.requests.PostFormDataRequest;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

/**
 *
 * @version raz_Template
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "dl-protecte.com" }, urls = { "https?://(?:www\\.)?(?:dl-protecte\\.(?:com|org)|protect-lien\\.com|protect-zt\\.com|protecte-link\\.com|liens-telechargement\\.com|dl-protect1\\.com?|dl-protect\\.top)/\\S+" })
public class DlPrteCom extends antiDDoSForDecrypt {
    @Override
    public String[] siteSupportedNames() {
        return new String[] { "dl-protect.top", "dl-protecte.com", "dl-protecte.org", "protect-lien.com", "protect-zt.com", "protecte-link.com", "liens-telechargement.com", "dl-protect1.com", "dl-protect1.co" };
    }

    public DlPrteCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String decode(final String input) {
        final LinkedHashMap<String, String> replace = new LinkedHashMap<String, String>();
        replace.put("060", ":");
        replace.put("061", ".");
        replace.put("062", "?");
        replace.put("063", "#");
        replace.put("064", "-");
        replace.put("065", "/");
        replace.put("0f", "0");
        replace.put("0l", "1");
        replace.put("0r", "2");
        replace.put("0k", "3");
        replace.put("0z", "4");
        replace.put("0x", "5");
        replace.put("0h", "6");
        replace.put("0o", "7");
        replace.put("0m", "8");
        replace.put("0n", "9");
        replace.put("34", "a");
        replace.put("35", "b");
        replace.put("36", "c");
        replace.put("37", "d");
        replace.put("38", "e");
        replace.put("39", "f");
        replace.put("40", "g");
        replace.put("41", "h");
        replace.put("42", "i");
        replace.put("43", "j");
        replace.put("44", "k");
        replace.put("45", "l");
        replace.put("46", "m");
        replace.put("47", "n");
        replace.put("48", "o");
        replace.put("49", "p");
        replace.put("50", "q");
        replace.put("51", "r");
        replace.put("52", "s");
        replace.put("53", "t");
        replace.put("54", "u");
        replace.put("55", "v");
        replace.put("56", "w");
        replace.put("57", "x");
        replace.put("58", "y");
        replace.put("59", "z");
        int index = 0;
        final StringBuilder ret = new StringBuilder();
        StringBuilder search = new StringBuilder();
        while (index < input.length()) {
            search.append(input.charAt(index++));
            final String write = replace.get(search.toString());
            if (write != null) {
                ret.append(write);
                search.delete(0, search.length());
            } else if (search.length() == 4) {
                if (search.charAt(0) == '0' && search.charAt(1) == '0') {
                    final String upper = replace.get(search.substring(2));
                    if (upper != null) {
                        ret.append(upper.toUpperCase(Locale.ENGLISH));
                        search.delete(0, search.length());
                    }
                }
                if (search.length() > 0) {
                    logger.info("Decoded:" + ret.toString() + "|Unknown:" + search);
                    return null;
                }
            }
        }
        if (input.endsWith("0f")) {
            return ret.substring(0, ret.length() - 1);
        } else {
            return ret.toString();
        }
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        String go = new Regex(parameter, "go\\.php\\?url=(aHR.+)").getMatch(0);
        if (go != null) {
            final DownloadLink dl = createDownloadlink(go);
            decryptedLinks.add(dl);
        }
        final String encoded = new Regex(parameter, "/([^/]+)$").getMatch(0);
        final String decoded = decode(encoded);
        if (StringUtils.isNotEmpty(decoded) && StringUtils.startsWithCaseInsensitive(decoded, "http")) {
            final DownloadLink dl = createDownloadlink(decoded);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        getPage(parameter);
        /* Error handling */
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("Page Not Found")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        {
            // additional form
            final Form continu = br.getFormBySubmitvalue("Continuer");
            if (continu != null) {
                submitForm(continu);
                // test link had no magic/captcha
                final String link = br.getRegex("<div class=\"lienet\"><a[^<>]*href=\"(.*?)\">").getMatch(0);
                if (link != null) {
                    final DownloadLink dl = createDownloadlink(link);
                    decryptedLinks.add(dl);
                    return decryptedLinks;
                }
            }
        }
        go = br.getRegex("go\\.php\\?url=(https?.*?|aHR.+)\"").getMatch(0);
        if (go != null) {
            final DownloadLink dl = createDownloadlink(go);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (br.containsHTML("captcha\\.php")) {
            /* 2018-08-02: New */
            boolean failed = true;
            for (int i = 0; i <= 2; i++) {
                final String code = this.getCaptchaCode("https://www.dl-protect1.com/captcha.php?rand=%3C?php%20echo%20rand();%20?%3E", param);
                final PostFormDataRequest authReq = br.createPostFormDataRequest(br.getURL());
                authReq.addFormData(new FormData("submit", ""));
                authReq.addFormData(new FormData("captchaCode", code));
                super.sendRequest(authReq);
                // final Form captchaForm = br.getFormbyKey("captchaCode");
                // if (captchaForm == null) {
                // return null;
                // }
                if (!br.containsHTML("captcha\\.php")) {
                    failed = false;
                    break;
                }
            }
            if (failed) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        } else {
            // some weird form that does jack
            final Form f = br.getFormbyProperty("class", "magic");
            if (f == null) {
                if (decryptedLinks.size() > 0) {
                    return decryptedLinks;
                }
                return null;
            }
            // insert some magic
            final String magic = getSoup();
            f.put(magic, "");
            // ajax stuff
            {
                final Browser ajax = br.cloneBrowser();
                ajax.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                postPage(ajax, "/php/Qaptcha.jquery.php", "action=qaptcha&qaptcha_key=" + magic);
                // should say error false.
            }
            submitForm(f);
        }
        // link
        final String link = br.getRegex("<div class=\"lienet\"><a href=\"(.*?)\">").getMatch(0);
        if (link == null) {
            return null;
        }
        decryptedLinks.add(createDownloadlink(link));
        return decryptedLinks;
    }

    private String getSoup() {
        final Random r = new Random();
        final String soup = "azertyupqsdfghjkmwxcvbn23456789AZERTYUPQSDFGHJKMWXCVBN_-#@";
        String v = "";
        for (int i = 0; i < 31; i++) {
            v = v + soup.charAt(r.nextInt(soup.length()));
        }
        return v;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}