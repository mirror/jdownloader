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
package jd.plugins.decrypter;

import java.util.ArrayList;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hoerbuch.in" }, urls = { "https?://(?:www\\.)?hoerbuch\\.(in|us)/(protection/(folder_\\d+|[a-z0-9]+/[a-z0-9]+)\\.html|wp/goto/Download/\\d+)" })
public class RsHrbchn extends antiDDoSForDecrypt {
    private final String ua = RandomUserAgent.generate();

    public RsHrbchn(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String        FOLDERLINK          = ".+/protection/folder_\\d+\\.html";
    private final String        FOLDER_REDIRECTLINK = ".+/wp/goto/Download/\\d+";
    private static final String current_domain      = "hoerbuch.us";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final ArrayList<String> passwords = new ArrayList<String>();
        passwords.add("www.hoerbuch.in");
        passwords.add("www.hoerbuch.us");
        br.setFollowRedirects(false);
        br.getHeaders().put("User-Agent", ua);
        String parameter = param.toString().replace("hoerbuch.in", "hoerbuch.us").replace("http://", "https://");
        getPage("https://" + current_domain + "/wp/");
        boolean done = false;
        int counter = 0;
        do {
            /*
             * 2017-01-26: New: do-while-loop to detect- and avoid redirects to external websites here - this resulted in failures before.
             */
            try {
                getPage(parameter);
                if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">\\s*404 error - Page not found")) {
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    return decryptedLinks;
                }
                if (parameter.matches(FOLDER_REDIRECTLINK)) {
                    br.setFollowRedirects(true);
                    if (br.getRedirectLocation() != null) {
                        getPage(br.getRedirectLocation());
                    }
                    final String newid = new Regex(br.getURL(), "folder_(\\d+)\\.html").getMatch(0);
                    if (newid == null && !this.br.getURL().contains(current_domain)) {
                        logger.info("Retry on ad-redirect");
                        continue;
                    } else if (newid == null) {
                        logger.info("Retry on newid == null");
                        continue;
                    }
                    final String newlink = "https://hoerbuch.us/protection/folder_" + newid + ".html";
                    parameter = newlink;
                    getPage(newlink);
                }
                if (this.br.getURL().contains(current_domain)) {
                    done = true;
                }
            } finally {
                this.sleep(2000l, param);
                counter++;
            }
        } while (!done && counter <= 2);
        if (!done) {
            return null;
        }
        if (parameter.matches(FOLDERLINK)) {
            final Form form = br.getForm(1);
            if (form == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            if (form.containsHTML("g-recaptcha")) {
                /* 2020-02-13: New */
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                form.put("g-recaptcha-response", recaptchaV2Response);
            }
            br.submitForm(form);
            final String links[] = br.getRegex("on\\.png.*?href=\"(http.*?)\"").getColumn(0);
            for (String link : links) {
                if (this.isAbort()) {
                    return decryptedLinks;
                }
                if (link.contains("us/protection")) {
                    final Browser brc = br.cloneBrowser();
                    brc.setFollowRedirects(false);
                    getPage(brc, link);
                    if (brc.getRedirectLocation() != null) {
                        decryptedLinks.add(this.createDownloadlink(brc.getRedirectLocation()));
                    }
                } else {
                    decryptedLinks.add(this.createDownloadlink(link));
                }
            }
        } else {
            final String finallink = br.getRedirectLocation();
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(this.createDownloadlink(finallink));
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}