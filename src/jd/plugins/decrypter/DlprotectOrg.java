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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "dlprotect.org" }, urls = { "https?://(?:www\\.)?dlprotect\\.org/\\?type=[a-z0-9]+\\&id=[a-z0-9]+\\&ps=\\d+" })
public class DlprotectOrg extends PluginForDecrypt {
    public DlprotectOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* Avoid multiple captcha - usually we should only need 1 every 4 hours. */
        return 1;
    }

    protected static HashMap<String, Cookies>     cookies        = new HashMap<String, Cookies>();
    protected final WeakHashMap<Browser, Boolean> browserPrepped = new WeakHashMap<Browser, Boolean>();

    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if ((browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            return prepBr;
        }
        synchronized (cookies) {
            if (!cookies.isEmpty()) {
                for (final Map.Entry<String, Cookies> cookieEntry : cookies.entrySet()) {
                    final String key = cookieEntry.getKey();
                    if (key != null && key.equals(host)) {
                        try {
                            prepBr.setCookies(key, cookieEntry.getValue(), false);
                        } catch (final Throwable e) {
                        }
                    }
                }
            }
        }
        // we now set
        browserPrepped.put(prepBr, Boolean.TRUE);
        return prepBr;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String type = new Regex(parameter, "type=([a-z0-9]+)").getMatch(0);
        final String id = new Regex(parameter, "id=([a-z0-9]+)").getMatch(0);
        final String ps = new Regex(parameter, "ps=(\\d+)").getMatch(0);
        prepBrowser(this.br, this.getHost());
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (br.containsHTML("g\\-recaptcha")) {
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            br.postPage(br.getURL(), "g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response));
            br.getPage(parameter);
        } else if (this.br.containsHTML("class=\"QapTcha\"")) {
            final String magic = this.getSoup();
            br.postPage("/qcap/qaptcha.php", "action=qaptcha&qaptcha_key=" + magic + "&type=" + type + "&id=" + id + "&ps=" + ps);
            br.getPage("/slinks.php?id=" + id + "&type=" + type + "&ps=" + ps + "&action=qaptcha");
        }
        String fpName = null;
        String[] links = br.getRegex("class=\"link\\-value\" valign=\"top\">\\s*?<a href=\"(http[^<>\"]+)\"").getColumn(0);
        if (links == null || links.length == 0) {
            /* 2017-05-04: Added this RegEx along with the qaptcha implementation. */
            links = br.getRegex("target=\"_blank\">(http[^<>\"]+)</td>").getColumn(0);
            if (links == null || links.length == 0) {
                links = br.getRegex("<a href=\"(http[^<>\"]+)\" target=\"_blank\">").getColumn(0);
            }
        }
        if (links == null || links.length == 0) {
            if (br.containsHTML("class=\"link\\-value\"") && this.br.containsHTML("<a href=\"\"")) {
                /* No urls available --> Empty/Offline */
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            if (singleLink.contains(this.getHost())) {
                continue;
            }
            decryptedLinks.add(createDownloadlink(singleLink));
        }
        // save the session!
        synchronized (cookies) {
            cookies.put(br.getHost(), br.getCookies(br.getHost()));
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
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
}
