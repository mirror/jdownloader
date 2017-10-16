//jDownloader - Downloadmanager
//Copyright (C) 2014  JD-Team support@jdownloader.org
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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import org.appwork.utils.StringUtils;

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

/**
 * Earn money sharing shrinked links<br />
 * Shrink URLs and earn money with Linkshrink.net<br />
 *
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "linkshrink.net" }, urls = { "https?://(?:www\\.)?linkshrink\\.net/([A-Za-z0-9]{5,6}|[A-Za-z0-9]{4}=(?:https?|ftp)://.+)" })
public class LnkShnkNt extends antiDDoSForDecrypt {

    public LnkShnkNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String type_direct  = "https?://(www\\.)?linkshrink\\.net/[A-Za-z0-9]{4}=https?://.+";
    private static final String type_invalid = "https?://(www\\.)?linkshrink\\.net/(report|login)";

    @Override
    protected boolean useRUA() {
        return true;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br = new Browser();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches(type_invalid)) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        br.setFollowRedirects(false);
        if (parameter.matches(type_direct)) {
            final String finallink = new Regex(parameter, "linkshrink\\.net/[A-Za-z0-9]{4}=((?:https?|ftp)://.+)").getMatch(0).replace("{d}", "?d=").replace("%7Bd%7D", "?d=");
            decryptedLinks.add(this.createDownloadlink(finallink));
            return decryptedLinks;
        }
        br.setCookie(getHost(), "s32", "1");
        String link;
        getPage(parameter);
        while (true) {
            if (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 404 || (br.getRedirectLocation() != null && br.getRedirectLocation().matches(type_invalid)) || br.containsHTML(">Link does not exist")) {
                decryptedLinks.add(createOfflinelink(parameter));
                return decryptedLinks;
            } else if (br.getRedirectLocation() == null && br.toString().length() < 100) {
                decryptedLinks.add(createOfflinelink(parameter));
                return decryptedLinks;
            }
            link = br.getRedirectLocation();
            if (link != null && link.contains("linkshrink.net/")) {
                getPage(link);
                link = null;
            }
            if (link == null) {
                if (br.containsHTML("api\\.solvemedia\\.com/papi")) {
                    /* This part was coded blindly! */
                    for (int i = 0; i <= 3; i++) {

                        final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                        File cf = null;
                        try {
                            cf = sm.downloadCaptcha(getLocalCaptchaFile());
                        } catch (final Exception e) {
                            if (org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia.FAIL_CAUSE_CKEY_MISSING.equals(e.getMessage())) {
                                throw new PluginException(LinkStatus.ERROR_FATAL, "Host side solvemedia.com captcha error - please contact the " + this.getHost() + " support");
                            }
                            throw e;
                        }
                        final String code = getCaptchaCode("solvemedia", cf, param);
                        final String chid = sm.getChallenge(code);
                        postPage(br.getURL(), "adcopy_response=manual_challenge&adcopy_challenge=" + Encoding.urlEncode(chid));
                        if (br.containsHTML("api\\.solvemedia\\.com/papi")) {
                            continue;
                        }
                        break;
                    }
                    if (br.containsHTML("api\\.solvemedia\\.com/papi")) {
                        throw new DecrypterException(DecrypterException.CAPTCHA);
                    }
                }
                String continu = br.getRegex("href=(\"|')([^\r\n]+)\\1 (?:onClick)?[^>]*>Continue").getMatch(1);
                if ("#".equals(continu)) {
                    // new method 20170122
                    continu = br.getRegex("<script>\\s*g\\.href\\s*=\\s*\"(https?://linkshrink.net/[a-zA-Z0-9]+)\";</script>").getMatch(0);
                    if (continu == null) {
                        // new method 20170127
                        continu = br.getRegex("\\.href\\s*=\\s*\\w+\\(\"([a-zA-Z0-9_/+=\\-%]+)\"\\)</script>").getMatch(0);
                        if (continu == null) {
                            // same shit different js/placement 201710xx
                            continu = br.getRegex("\\.onclick\\s*=\\s*function\\(\\)\\s*\\{\\s*window\\.open\\(revC\\(\"(.*?)\"\\)").getMatch(0);
                        }
                        if (continu != null) {
                            continu = "/" + Encoding.Base64Decode(continu);
                        }
                    }
                }
                if (continu == null) {
                    return null;
                }
                final String ref = br.getURL();
                getPage(continu);
                if (StringUtils.contains(br.getRedirectLocation(), "linkshrink.net/")) {
                    // referer bug, or not so bug, since we are not following redirects browser treats it as standard request.
                    br.getHeaders().put("Referer", ref);
                    getPage(br.getRedirectLocation());
                    continue;
                }
            }
            break;
        }
        if (link != null) {
            decryptedLinks.add(createDownloadlink(link));
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}