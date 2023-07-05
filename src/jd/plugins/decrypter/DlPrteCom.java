//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

/**
 *
 * @version raz_Template
 * @author raztoki, psp
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DlPrteCom extends antiDDoSForDecrypt {
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "dl-protect.net", "dl-protect.info", "dl-protect.link" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/[a-z0-9]+(\\?.*)");
        }
        return ret.toArray(new String[0]);
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

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String go = new Regex(param.getCryptedUrl(), "go\\.php\\?url=(aHR.+)").getMatch(0);
        if (go != null) {
            final DownloadLink dl = createDownloadlink(go);
            ret.add(dl);
        }
        final String encoded = new Regex(param.getCryptedUrl(), "/([^/]+)$").getMatch(0);
        final String decoded = decode(encoded);
        if (StringUtils.isNotEmpty(decoded) && StringUtils.startsWithCaseInsensitive(decoded, "http")) {
            final DownloadLink dl = createDownloadlink(decoded);
            ret.add(dl);
            return ret;
        }
        br.setFollowRedirects(true);
        getPage(param.getCryptedUrl());
        /* Error handling */
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("Page Not Found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)>\\s*Lien introuvable")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        {
            // additional form
            Form continu = br.getFormBySubmitvalue("Continuer");
            if (continu != null) {
                submitForm(continu);
                continu = br.getFormBySubmitvalue("Continuer");
                if (continu != null) {
                    submitForm(continu);
                }
                // test link had no magic/captcha
                final String link = br.getRegex("<div class=\"lienet\"><a[^<>]*href=\"(.*?)\">").getMatch(0);
                if (link != null) {
                    final DownloadLink dl = createDownloadlink(link);
                    ret.add(dl);
                    return ret;
                }
            } else {
                String strEncodedPart = br._getURL().getFile();
                strEncodedPart = strEncodedPart.replace("/voirlien/", "");
                final int iPosition = strEncodedPart.indexOf("?");
                if (iPosition > 0) {
                    strEncodedPart = strEncodedPart.substring(0, iPosition);
                }
                continu = br.getFormbyAction("/telecharger/" + strEncodedPart + "}");
                if (continu != null) {
                    submitForm(continu);
                    go = br.getRegex("class\\s*=\\s*\"showURL\"\\s*>\\s*(.*?)\\s*</p>\\s*</a>").getMatch(0);
                    if (go != null) {
                        final DownloadLink dl = createDownloadlink(go);
                        ret.add(dl);
                        return ret;
                    }
                }
            }
        }
        go = br.getRegex("go\\.php\\?url=(https?.*?|aHR.+)\"").getMatch(0);
        if (go != null) {
            final DownloadLink dl = createDownloadlink(go);
            ret.add(dl);
            return ret;
        }
        /* 2020-10-29: Possible "protections": Without protection, reCaptchaV2, Password (only one can be active) */
        Form continueForm = br.getFormByInputFieldKeyValue("subform", "unlock");
        if (continueForm == null) {
            /* Fallback */
            continueForm = br.getForm(0);
        }
        if (continueForm == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String passCode = null;
        if (CaptchaHelperCrawlerPluginRecaptchaV2.containsRecaptchaV2Class(br)) {
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            continueForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
        } else if (continueForm.hasInputFieldByName("password")) {
            passCode = getUserInput("Password?", param);
            continueForm.put("password", Encoding.urlEncode(passCode));
        }
        this.submitForm(continueForm);
        String link = br.getRegex("<div class=\"lienet\"><a href=\"(.*?)\">").getMatch(0);
        if (link == null) {
            /* 2020-10-29 */
            link = br.getRegex("<a href=\"(https?://[^\"]+)\" rel=\"external nofollow\">").getMatch(0);
        }
        if (link == null) {
            if (passCode != null) {
                /* Assume that user entered wrong password */
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
            return null;
        }
        ret.add(createDownloadlink(link));
        return ret;
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

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }
}